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

    @Volatile private var audioDeferred: CompletableDeferred<Pair<Boolean, String>>? = null
    @Volatile private var deviceTtsDeferred: CompletableDeferred<Boolean>? = null

    // Real-time audio route monitoring - stops playback if output switches to speaker
    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            if (!isPlayingAudio) return
            if (prefs.alsoSpeaker) return // User explicitly allows speaker

            val removedHeadphone = removedDevices?.any { device ->
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_USB_HEADSET
            } == true

            if (removedHeadphone) {
                Log.w(TAG, "EMERGENCY: Audio device removed during playback - stopping IMMEDIATELY")
                emergencyStop()
            }
        }
    }

    // ACTION_AUDIO_BECOMING_NOISY: fired when audio switches from headphones to speaker
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                if (prefs.alsoSpeaker) return // User allows speaker
                Log.w(TAG, "EMERGENCY: Audio becoming noisy (switching to speaker) - stopping IMMEDIATELY")
                emergencyStop()
            }
        }
    }

    init {
        initDeviceTts()
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
        context.registerReceiver(
            noisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        )
    }

    /**
     * STRICT check: Is audio output safe (going to headphones, NOT the phone speaker)?
     *
     * FAILSAFE LOGIC:
     * - If user allows speaker output: always safe
     * - If bluetoothMonitor is null: UNSAFE (assume speaker) — never default to true
     * - If no headphones detected: UNSAFE
     * - Must find an actual BT/wired output device: otherwise UNSAFE
     */
    private fun isAudioOutputSafe(): Boolean {
        if (prefs.alsoSpeaker) return true

        // CRITICAL: If no bluetooth monitor, assume UNSAFE — never play through speaker
        val monitor = bluetoothMonitor
        if (monitor == null) {
            Log.w(TAG, "BLOCKED: No BluetoothMonitor available - assuming speaker output (UNSAFE)")
            return false
        }

        val btConnected = monitor.isBluetoothAudioConnected()
        val wiredConnected = monitor.isWiredHeadphonesConnected()

        if (btConnected) return true
        if (prefs.alsoWiredHeadphones && wiredConnected) return true

        Log.w(TAG, "BLOCKED: No headphones detected (bt=$btConnected, wired=$wiredConnected)")
        return false
    }

    /**
     * Find a headphone output device for forced audio routing.
     * Returns null if no headphone device exists — in which case we MUST NOT play.
     */
    private fun findHeadphoneOutputDevice(): AudioDeviceInfo? {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        // Prefer Bluetooth A2DP, then BLE, then wired
        return devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
            ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLE_HEADSET }
            ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER }
            ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
            ?: if (prefs.alsoWiredHeadphones) {
                devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES }
                    ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET }
                    ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_USB_HEADSET }
            } else null
    }

    /**
     * EMERGENCY STOP: Immediately kill all audio and clear queue.
     * Called when headphones disconnect or audio route changes to speaker.
     * This is NOT the same as pause() — it does not set isPaused, so future
     * notifications can still play when headphones reconnect.
     */
    fun emergencyStop() {
        Log.w(TAG, "EMERGENCY STOP: Killing all audio to prevent speaker output")
        isPlayingAudio = false

        // Stop MediaPlayer
        currentMediaPlayer?.let {
            try { it.stop(); it.release() } catch (_: Exception) {}
        }
        currentMediaPlayer = null

        // Stop device TTS
        deviceTts?.stop()

        // Fail all pending deferreds
        audioDeferred?.complete(Pair(false, "Emergency stop - headphones disconnected"))
        audioDeferred = null
        deviceTtsDeferred?.complete(false)
        deviceTtsDeferred = null

        // Clear the entire queue
        while (queue.isNotEmpty()) {
            val item = queue.poll()
            item?.onResult?.invoke(false, "Emergency stop - headphones disconnected")
        }

        abandonAudioFocus()
        try { if (wakeLock.isHeld) wakeLock.release() } catch (_: Exception) {}
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

        // FAILSAFE: Check audio output BEFORE even queuing
        if (!isAudioOutputSafe()) {
            Log.w(TAG, "BLOCKED at enqueue: No headphones - refusing to queue")
            onResult?.invoke(false, "No headphones connected - blocked at enqueue")
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
                    // FAILSAFE: Re-check audio output before EACH item
                    if (!isAudioOutputSafe()) {
                        Log.w(TAG, "BLOCKED in processQueue: No headphones - clearing remaining queue")
                        while (queue.isNotEmpty()) {
                            val dropped = queue.poll()
                            dropped?.onResult?.invoke(false, "Headphones disconnected - queue cleared")
                        }
                        break
                    }

                    val item = queue.poll() ?: break
                    val age = System.currentTimeMillis() - item.enqueuedAt
                    if (age > 60_000L) {
                        Log.w(TAG, "Dropping stale queued item (${age / 1000}s old)")
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
        // ═══ TRIPLE CHECK: Audio output safety ═══
        // Check 1: Is audio output safe right now?
        if (!isAudioOutputSafe()) {
            Log.w(TAG, "BLOCKED (check 1/3): Audio output unsafe - no headphones")
            item.onResult?.invoke(false, "No headphones connected - refused to play")
            return
        }

        // Check 2: Can we find an actual headphone device to route to?
        if (!prefs.alsoSpeaker) {
            val headphoneDevice = findHeadphoneOutputDevice()
            if (headphoneDevice == null) {
                Log.w(TAG, "BLOCKED (check 2/3): No headphone output device found in AudioManager")
                item.onResult?.invoke(false, "No headphone output device found")
                return
            }
        }

        // Check 3: Ask BluetoothMonitor one more time
        if (!prefs.alsoSpeaker && bluetoothMonitor != null) {
            val btOk = bluetoothMonitor.isBluetoothAudioConnected()
            val wiredOk = prefs.alsoWiredHeadphones && bluetoothMonitor.isWiredHeadphonesConnected()
            if (!btOk && !wiredOk) {
                Log.w(TAG, "BLOCKED (check 3/3): BluetoothMonitor confirms no headphones")
                item.onResult?.invoke(false, "BluetoothMonitor confirms no headphones")
                return
            }
        }

        Log.d(TAG, "All 3 audio safety checks PASSED - proceeding with TTS")

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

        val toneKey = prefs.ttsTone
        val toneInstruction = if (toneKey == "custom") {
            prefs.customToneInstruction
        } else {
            GeminiTTSAPI.TONE_PRESETS[toneKey]
        }

        Log.d(TAG, "Generating TTS: voice=${prefs.geminiVoiceName}, model=${prefs.geminiModel}, tone=$toneKey")

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

        // FAILSAFE: One more check right before we actually play audio
        if (!isAudioOutputSafe()) {
            Log.w(TAG, "BLOCKED at playAudioFile: Audio output became unsafe before playback")
            file.delete()
            return@withContext Pair(false, "No headphones at playback time")
        }

        val completable = CompletableDeferred<Pair<Boolean, String>>()
        audioDeferred = completable
        var mediaPlayer: MediaPlayer? = null

        requestAudioFocus()

        try {
            // ALWAYS use USAGE_MEDIA when not using speaker — this routes through A2DP
            val effectiveUsage = if (!prefs.alsoSpeaker) {
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

            // CRITICAL: Force audio to headphone device — never fall back to speaker
            if (!prefs.alsoSpeaker) {
                val headphoneDevice = findHeadphoneOutputDevice()
                if (headphoneDevice != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        mediaPlayer.setPreferredDevice(headphoneDevice)
                        Log.d(TAG, "Forced audio to: ${headphoneDevice.productName} (type=${headphoneDevice.type})")
                    }
                } else {
                    // NO headphone device found — ABORT IMMEDIATELY
                    Log.w(TAG, "BLOCKED: No headphone device at playback - aborting to prevent speaker output")
                    mediaPlayer.release()
                    file.delete()
                    abandonAudioFocus()
                    audioDeferred = null
                    return@withContext Pair(false, "No headphones at playback time - aborted")
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

            // FAILSAFE: Verify playback actually started
            if (!mediaPlayer.isPlaying) {
                isPlayingAudio = false
                mediaPlayer.release()
                currentMediaPlayer = null
                file.delete()
                abandonAudioFocus()
                audioDeferred = null
                return@withContext Pair(false, "MediaPlayer.start() called but isPlaying=false")
            }

            // FAILSAFE: After start, verify audio is routing to the right device
            if (!prefs.alsoSpeaker && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val routedDevice = mediaPlayer.routedDevice
                if (routedDevice != null) {
                    val isHeadphone = routedDevice.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        routedDevice.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                        routedDevice.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                        routedDevice.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        routedDevice.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                        routedDevice.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        routedDevice.type == AudioDeviceInfo.TYPE_USB_HEADSET
                    if (!isHeadphone) {
                        Log.w(TAG, "EMERGENCY: Audio routed to non-headphone device type=${routedDevice.type} (${routedDevice.productName}) - STOPPING")
                        isPlayingAudio = false
                        mediaPlayer.stop()
                        mediaPlayer.release()
                        currentMediaPlayer = null
                        file.delete()
                        abandonAudioFocus()
                        audioDeferred = null
                        return@withContext Pair(false, "Audio routed to speaker (type=${routedDevice.type}) - stopped")
                    }
                    Log.d(TAG, "Audio routing verified: ${routedDevice.productName} (type=${routedDevice.type})")
                }
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
        // FAILSAFE: Check audio output before device TTS too
        if (!isAudioOutputSafe()) {
            Log.w(TAG, "BLOCKED: Device TTS blocked - no headphones connected")
            return@withContext false
        }

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

            // Use USAGE_MEDIA to route through BT A2DP
            val effectiveUsage = if (!prefs.alsoSpeaker) {
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

    private fun requestAudioFocus() {
        val effectiveUsage = if (!prefs.alsoSpeaker) {
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

    fun skip() {
        Log.d(TAG, "Skipping current TTS")
        isPlayingAudio = false

        currentMediaPlayer?.let {
            try { it.stop(); it.release() } catch (_: Exception) {}
        }
        currentMediaPlayer = null
        deviceTts?.stop()

        audioDeferred?.complete(Pair(true, "skipped"))
        audioDeferred = null
        deviceTtsDeferred?.complete(true)
        deviceTtsDeferred = null

        abandonAudioFocus()
    }

    fun pause() {
        isPaused = true
        isPlayingAudio = false

        currentMediaPlayer?.let {
            try { it.stop(); it.release() } catch (_: Exception) {}
        }
        currentMediaPlayer = null
        deviceTts?.stop()

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
