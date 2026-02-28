package com.notifytts.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.util.Log
import com.notifytts.data.PreferencesManager
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

data class TTSQueueItem(
    val text: String,
    val logEntryId: Long = 0,
    val enqueuedAt: Long = System.currentTimeMillis(),
    val onResult: ((success: Boolean, error: String?) -> Unit)? = null
)

class TTSManager(private val context: Context, private val bluetoothMonitor: BluetoothMonitor? = null) {

    companion object {
        private const val TAG = "TTSManager"
    }

    private val prefs = PreferencesManager(context)
    private val geminiTTSAPI = GeminiTTSAPI(context.cacheDir)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val queue = ConcurrentLinkedQueue<TTSQueueItem>()
    private val mutex = Mutex()
    private var isProcessing = false
    @Volatile private var isPlayingAudio = false
    private var currentMediaPlayer: MediaPlayer? = null
    private var deviceTts: TextToSpeech? = null
    private var deviceTtsReady = false
    private var focusRequest: AudioFocusRequest? = null
    private var isPaused = false
    private val wakeLock: PowerManager.WakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NotificationTTS:TTSPlayback")

    // Deferreds for current playback - allows skip/pause to unblock await immediately
    @Volatile private var audioDeferred: CompletableDeferred<Pair<Boolean, String>>? = null
    @Volatile private var deviceTtsDeferred: CompletableDeferred<Boolean>? = null

    // Real-time audio route monitoring - stops playback if output switches to speaker
    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            if (!isPlayingAudio) return
            val removedHeadphone = removedDevices?.any { device ->
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_USB_HEADSET
            } == true

            if (removedHeadphone && !isAudioOutputSafe()) {
                Log.w(TAG, "Audio device removed during playback - stopping to prevent speaker output")
                stopPlaybackImmediately()
            }
        }
    }

    // ACTION_AUDIO_BECOMING_NOISY: fired when audio output switches from private (headphones) to public (speaker)
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY && isPlayingAudio) {
                Log.w(TAG, "Audio becoming noisy (headphones removed) - stopping playback immediately")
                stopPlaybackImmediately()
            }
        }
    }

    init {
        initDeviceTts()
        // Register real-time audio route monitoring
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
        context.registerReceiver(
            noisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        )
    }

    /**
     * Check if audio output is safe (going to headphones/earbuds, not the phone speaker).
     * Returns true if the user explicitly allows speaker output (alsoSpeaker setting),
     * or if headphones/earbuds are currently connected.
     */
    private fun isAudioOutputSafe(): Boolean {
        if (prefs.alsoSpeaker) return true
        return bluetoothMonitor?.isAnyHeadphonesConnected() ?: true
    }

    /** Immediately stop all playback and clear the queue. Used when audio route becomes unsafe. */
    private fun stopPlaybackImmediately() {
        isPlayingAudio = false

        // Stop MediaPlayer
        currentMediaPlayer?.let {
            try { it.stop(); it.release() } catch (_: Exception) {}
        }
        currentMediaPlayer = null

        // Stop device TTS
        deviceTts?.stop()

        // Fail the pending deferreds - audio route is unsafe
        audioDeferred?.complete(Pair(false, "Audio route changed to speaker - stopped"))
        audioDeferred = null
        deviceTtsDeferred?.complete(false)
        deviceTtsDeferred = null

        // Clear the queue - don't let anything else play through the speaker
        while (queue.isNotEmpty()) {
            val item = queue.poll()
            item?.onResult?.invoke(false, "Headphones disconnected - queue cleared")
        }

        abandonAudioFocus()
    }

    private fun initDeviceTts() {
        deviceTts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                deviceTtsReady = true
                deviceTts?.language = Locale("he", "IL")
            }
        }
    }

    fun enqueue(text: String, logEntryId: Long = 0, onResult: ((Boolean, String?) -> Unit)? = null) {
        if (isPaused) {
            onResult?.invoke(false, "TTS is paused")
            return
        }
        if (queue.size >= prefs.maxQueueSize) {
            Log.w(TAG, "Queue full, dropping notification")
            onResult?.invoke(false, "Queue full")
            return
        }

        val truncated = if (text.length > prefs.maxTextLength) {
            text.take(prefs.maxTextLength) + "..."
        } else {
            text
        }

        queue.add(TTSQueueItem(text = truncated, logEntryId = logEntryId, onResult = onResult))
        processQueue()
    }

    private fun processQueue() {
        scope.launch {
            mutex.withLock {
                if (isProcessing) return@launch
                isProcessing = true
            }

            try {
                while (queue.isNotEmpty() && !isPaused) {
                    val item = queue.poll() ?: break
                    // Skip stale items (queued > 60s ago)
                    val age = System.currentTimeMillis() - item.enqueuedAt
                    if (age > 60_000L) {
                        Log.w(TAG, "Dropping stale queued item (${age / 1000}s old): ${item.text.take(30)}")
                        item.onResult?.invoke(false, "Stale - queued ${age / 1000}s ago")
                        continue
                    }
                    try {
                        speak(item)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error speaking: ${e.message}", e)
                        item.onResult?.invoke(false, "Exception: ${e.message}")
                    }
                }
            } finally {
                mutex.withLock {
                    isProcessing = false
                    isPlayingAudio = false
                }
                // Re-check: items may have been enqueued while we were shutting down
                if (queue.isNotEmpty() && !isPaused) {
                    processQueue()
                }
            }
        }
    }

    private fun stripEmojis(text: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < text.length) {
            val cp = Character.codePointAt(text, i)
            val charCount = Character.charCount(cp)
            if (!isEmoji(cp)) {
                sb.appendCodePoint(cp)
            }
            i += charCount
        }
        return sb.toString().replace(Regex("\\s{2,}"), " ").trim()
    }

    private fun isEmoji(codePoint: Int): Boolean {
        return codePoint in 0x1F600..0x1F64F ||
               codePoint in 0x1F300..0x1F5FF ||
               codePoint in 0x1F680..0x1F6FF ||
               codePoint in 0x1F900..0x1F9FF ||
               codePoint in 0x1FA00..0x1FA6F ||
               codePoint in 0x1FA70..0x1FAFF ||
               codePoint in 0x2600..0x26FF ||
               codePoint in 0x2700..0x27BF ||
               codePoint in 0xFE00..0xFE0F ||
               codePoint == 0x200D ||
               codePoint in 0xE0020..0xE007F ||
               codePoint in 0x1F1E0..0x1F1FF
    }

    private suspend fun speak(item: TTSQueueItem) {
        try { wakeLock.acquire(120_000L) } catch (_: Exception) {}
        try {
            speakInternal(item)
        } finally {
            try { if (wakeLock.isHeld) wakeLock.release() } catch (_: Exception) {}
        }
    }

    private suspend fun speakInternal(item: TTSQueueItem) {
        // CRITICAL: Re-check audio output right before playback.
        // The original shouldSpeak() check happened when the notification arrived,
        // but earbuds may have been removed between then and now.
        if (!isAudioOutputSafe()) {
            Log.w(TAG, "Audio output unsafe (no headphones) - refusing to speak")
            item.onResult?.invoke(false, "No headphones connected - refused to play through speaker")
            return
        }

        if (prefs.useDeviceTtsOnly) {
            val success = speakWithDeviceTts(item.text)
            item.onResult?.invoke(success, if (success) null else "Device TTS failed")
            return
        }

        val apiKey = prefs.geminiApiKey
        if (apiKey.isBlank()) {
            if (prefs.fallbackToDeviceTts) {
                val success = speakWithDeviceTts(item.text)
                item.onResult?.invoke(success, if (success) null else "Device TTS fallback failed (no API key)")
            } else {
                item.onResult?.invoke(false, "No API key configured")
            }
            return
        }

        val ttsText = if (prefs.stripEmojis) stripEmojis(item.text) else item.text
        if (ttsText.isBlank()) {
            item.onResult?.invoke(false, "Text empty after emoji removal")
            return
        }

        // Resolve tone instruction
        val toneKey = prefs.ttsTone
        val toneInstruction = if (toneKey == "custom") {
            prefs.customToneInstruction
        } else {
            GeminiTTSAPI.TONE_PRESETS[toneKey]
        }

        Log.d(TAG, "Generating TTS: voice=${prefs.geminiVoiceName}, model=${prefs.geminiModel}, tone=$toneKey, text='${ttsText.take(50)}...'")

        val result = geminiTTSAPI.synthesize(
            text = ttsText,
            apiKey = apiKey,
            voiceName = prefs.geminiVoiceName,
            modelId = prefs.geminiModel,
            speed = prefs.ttsSpeed,
            toneInstruction = toneInstruction
        )

        result.fold(
            onSuccess = { audioFile ->
                val playResult = playAudioFile(audioFile)
                if (playResult.first) {
                    item.onResult?.invoke(true, null)
                } else {
                    val error = "Audio playback failed: ${playResult.second}"
                    Log.e(TAG, error)
                    if (prefs.fallbackToDeviceTts) {
                        val fbSuccess = speakWithDeviceTts(item.text)
                        item.onResult?.invoke(fbSuccess, if (fbSuccess) "Gemini audio failed, device TTS used" else error)
                    } else {
                        item.onResult?.invoke(false, error)
                    }
                }
            },
            onFailure = { error ->
                Log.e(TAG, "Gemini TTS generation failed: ${error.message}")
                if (prefs.fallbackToDeviceTts) {
                    val fbSuccess = speakWithDeviceTts(item.text)
                    item.onResult?.invoke(
                        fbSuccess,
                        if (fbSuccess) "Gemini failed (${error.message}), device TTS used"
                        else "Gemini failed: ${error.message}, device TTS also failed"
                    )
                } else {
                    item.onResult?.invoke(false, "Gemini TTS failed: ${error.message}")
                }
            }
        )
    }

    private suspend fun playAudioFile(file: File): Pair<Boolean, String> = withContext(Dispatchers.Main) {
        if (!file.exists()) {
            return@withContext Pair(false, "Audio file does not exist")
        }
        if (file.length() < 45) {
            file.delete()
            return@withContext Pair(false, "Audio file too small: ${file.length()} bytes")
        }

        val completable = CompletableDeferred<Pair<Boolean, String>>()
        audioDeferred = completable
        var mediaPlayer: MediaPlayer? = null

        requestAudioFocus()

        try {
            // When Bluetooth is connected, use USAGE_MEDIA to ensure audio routes to A2DP.
            // USAGE_NOTIFICATION plays through the phone speaker on most Android devices
            // even when Bluetooth headphones are connected.
            val effectiveUsage = if (!prefs.alsoSpeaker && findHeadphoneOutputDevice() != null) {
                AudioAttributes.USAGE_MEDIA
            } else {
                prefs.audioUsageType
            }
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(effectiveUsage)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                prepare()
            }

            // Force audio to headphones/earbuds if available - never fall back to speaker
            if (!prefs.alsoSpeaker) {
                val headphoneDevice = findHeadphoneOutputDevice()
                if (headphoneDevice != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        mediaPlayer.setPreferredDevice(headphoneDevice)
                        Log.d(TAG, "Forced audio to device: ${headphoneDevice.productName} (type=${headphoneDevice.type})")
                    }
                } else {
                    // No headphone device found at playback time - abort
                    Log.w(TAG, "No headphone device found at playback time - aborting")
                    mediaPlayer.release()
                    file.delete()
                    abandonAudioFocus()
                    audioDeferred = null
                    return@withContext Pair(false, "No headphones connected at playback time")
                }
            }

            val duration = mediaPlayer.duration
            if (duration <= 0) {
                mediaPlayer.release()
                file.delete()
                abandonAudioFocus()
                audioDeferred = null
                return@withContext Pair(false, "Audio file has no playable content (duration=$duration)")
            }

            currentMediaPlayer = mediaPlayer

            mediaPlayer.setOnCompletionListener { mp ->
                // Guard: skip/pause may have already completed the deferred
                if (!completable.isCompleted) {
                    isPlayingAudio = false
                    try { mp.release() } catch (_: Exception) {}
                    currentMediaPlayer = null
                    file.delete()
                    abandonAudioFocus()
                    completable.complete(Pair(true, "${duration}ms"))
                }
            }

            mediaPlayer.setOnErrorListener { mp, what, extra ->
                if (!completable.isCompleted) {
                    val errorMsg = "MediaPlayer error: what=$what, extra=$extra"
                    Log.e(TAG, errorMsg)
                    isPlayingAudio = false
                    try { mp.release() } catch (_: Exception) {}
                    currentMediaPlayer = null
                    file.delete()
                    abandonAudioFocus()
                    completable.complete(Pair(false, errorMsg))
                }
                true
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && prefs.ttsSpeed != 1.0f) {
                    mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(prefs.ttsSpeed)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not set playback speed: ${e.message}")
            }

            mediaPlayer.start()
            isPlayingAudio = true

            if (!mediaPlayer.isPlaying) {
                isPlayingAudio = false
                mediaPlayer.release()
                currentMediaPlayer = null
                file.delete()
                abandonAudioFocus()
                audioDeferred = null
                return@withContext Pair(false, "MediaPlayer.start() called but isPlaying=false")
            }

            try {
                withTimeout(90_000L) { completable.await() }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Audio playback timed out after 90s")
                isPlayingAudio = false
                try { mediaPlayer.stop(); mediaPlayer.release() } catch (_: Exception) {}
                currentMediaPlayer = null
                file.delete()
                abandonAudioFocus()
                Pair(false, "Playback timed out")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio: ${e.javaClass.simpleName}: ${e.message}", e)
            isPlayingAudio = false
            file.delete()
            abandonAudioFocus()
            mediaPlayer?.let { try { it.release() } catch (_: Exception) {} }
            currentMediaPlayer = null
            Pair(false, "${e.javaClass.simpleName}: ${e.message}")
        } finally {
            audioDeferred = null
        }
    }

    private suspend fun speakWithDeviceTts(text: String): Boolean = withContext(Dispatchers.Main) {
        var waited = 0
        while (!deviceTtsReady && waited < 3000) {
            delay(100)
            waited += 100
        }
        if (!deviceTtsReady) {
            Log.w(TAG, "Device TTS not ready after ${waited}ms")
            return@withContext false
        }

        val completable = CompletableDeferred<Boolean>()
        deviceTtsDeferred = completable

        requestAudioFocus()

        deviceTts?.apply {
            language = Locale("he", "IL")
            setSpeechRate(prefs.ttsSpeed)

            // Set audio attributes to USAGE_MEDIA when headphones are connected
            // so Android routes TTS audio through A2DP, not the phone speaker
            val effectiveUsage = if (!prefs.alsoSpeaker && findHeadphoneOutputDevice() != null) {
                AudioAttributes.USAGE_MEDIA
            } else {
                prefs.audioUsageType
            }
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(effectiveUsage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )

            setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isPlayingAudio = true
                }
                override fun onDone(utteranceId: String?) {
                    if (!completable.isCompleted) {
                        isPlayingAudio = false
                        abandonAudioFocus()
                        completable.complete(true)
                    }
                }
                override fun onError(utteranceId: String?) {
                    if (!completable.isCompleted) {
                        isPlayingAudio = false
                        abandonAudioFocus()
                        completable.complete(false)
                    }
                }
            })
            speak(text, TextToSpeech.QUEUE_FLUSH, null, "ntts_${System.currentTimeMillis()}")
        } ?: run {
            completable.complete(false)
        }

        try {
            withTimeout(30_000L) { completable.await() }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Device TTS timed out after 30s")
            isPlayingAudio = false
            abandonAudioFocus()
            false
        } finally {
            deviceTtsDeferred = null
        }
    }

    /** Find a connected headphone/earbud output device to force audio routing. */
    private fun findHeadphoneOutputDevice(): AudioDeviceInfo? {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        // Prefer Bluetooth A2DP (music quality), then BLE, then wired
        return devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
            ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLE_HEADSET }
            ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER }
            ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
            ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES }
            ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET }
            ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_USB_HEADSET }
    }

    private fun requestAudioFocus() {
        // Use USAGE_MEDIA when headphones are connected to ensure audio focus goes through the right stream
        val effectiveUsage = if (!prefs.alsoSpeaker && findHeadphoneOutputDevice() != null) {
            AudioAttributes.USAGE_MEDIA
        } else {
            prefs.audioUsageType
        }
        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(effectiveUsage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .build()
        focusRequest?.let { audioManager.requestAudioFocus(it) }
    }

    private fun abandonAudioFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    fun isSpeaking(): Boolean {
        return isProcessing || currentMediaPlayer?.isPlaying == true
    }

    fun isAudioPlaying(): Boolean {
        return isPlayingAudio
    }

    /**
     * Skip the currently playing TTS. Marks it as success (played).
     * Does NOT stop queue processing - next items will play.
     */
    fun skip() {
        Log.d(TAG, "Skipping current TTS")
        isPlayingAudio = false

        // Stop MediaPlayer
        currentMediaPlayer?.let {
            try { it.stop(); it.release() } catch (_: Exception) {}
        }
        currentMediaPlayer = null

        // Stop device TTS
        deviceTts?.stop()

        // Complete the pending deferreds as SUCCESS so item is marked "played"
        audioDeferred?.complete(Pair(true, "skipped"))
        audioDeferred = null
        deviceTtsDeferred?.complete(true)
        deviceTtsDeferred = null

        abandonAudioFocus()
    }

    /**
     * Pause all TTS. Stops current playback (marks as failed) and clears queue.
     */
    fun pause() {
        isPaused = true
        isPlayingAudio = false

        currentMediaPlayer?.let {
            try { it.stop(); it.release() } catch (_: Exception) {}
        }
        currentMediaPlayer = null
        deviceTts?.stop()

        // Complete deferreds as failure (paused)
        audioDeferred?.complete(Pair(false, "Paused"))
        audioDeferred = null
        deviceTtsDeferred?.complete(false)
        deviceTtsDeferred = null

        while (queue.isNotEmpty()) {
            val item = queue.poll()
            item?.onResult?.invoke(false, "Paused - cleared from queue")
        }
        abandonAudioFocus()
        try { if (wakeLock.isHeld) wakeLock.release() } catch (_: Exception) {}
    }

    fun resume() {
        isPaused = false
        if (queue.isNotEmpty()) {
            processQueue()
        }
    }

    fun clearQueue() {
        while (queue.isNotEmpty()) {
            val item = queue.poll()
            item?.onResult?.invoke(false, "Queue cleared")
        }
    }

    fun destroy() {
        pause()
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        try { context.unregisterReceiver(noisyReceiver) } catch (_: Exception) {}
        scope.cancel()
        deviceTts?.shutdown()
        geminiTTSAPI.cleanup()
    }
}
