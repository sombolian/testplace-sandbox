package com.notifytts.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import com.notifytts.data.PreferencesManager
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * An item in the TTS queue with metadata for result tracking.
 */
data class TTSQueueItem(
    val text: String,
    val logEntryId: Long = 0,
    val onResult: ((success: Boolean, error: String?) -> Unit)? = null
)

class TTSManager(private val context: Context) {

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
    private var currentMediaPlayer: MediaPlayer? = null
    private var deviceTts: TextToSpeech? = null
    private var deviceTtsReady = false
    private var focusRequest: AudioFocusRequest? = null
    private var isPaused = false

    init {
        initDeviceTts()
    }

    private fun initDeviceTts() {
        deviceTts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                deviceTtsReady = true
                deviceTts?.language = Locale("he", "IL")
            }
        }
    }

    /**
     * Enqueue text for TTS with optional result callback.
     * The callback reports whether TTS was actually generated and played successfully.
     */
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

        queue.add(TTSQueueItem(truncated, logEntryId, onResult))
        processQueue()
    }

    private fun processQueue() {
        scope.launch {
            mutex.withLock {
                if (isProcessing) return@launch
                isProcessing = true
            }

            while (queue.isNotEmpty() && !isPaused) {
                val item = queue.poll() ?: break
                try {
                    speak(item)
                } catch (e: Exception) {
                    Log.e(TAG, "Error speaking: ${e.message}", e)
                    item.onResult?.invoke(false, "Exception: ${e.message}")
                }
            }

            mutex.withLock {
                isProcessing = false
            }
        }
    }

    private suspend fun speak(item: TTSQueueItem) {
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

        Log.d(TAG, "Generating TTS: voice=${prefs.geminiVoiceName}, model=${prefs.geminiModel}, text='${item.text.take(50)}...'")

        val result = geminiTTSAPI.synthesize(
            text = item.text,
            apiKey = apiKey,
            voiceName = prefs.geminiVoiceName,
            modelId = prefs.geminiModel,
            speed = prefs.ttsSpeed
        )

        result.fold(
            onSuccess = { audioFile ->
                val playResult = playAudioFile(audioFile)
                if (playResult.first) {
                    Log.d(TAG, "TTS played successfully: duration=${playResult.second}ms")
                    item.onResult?.invoke(true, null)
                } else {
                    val error = "Audio playback failed: ${playResult.second}"
                    Log.e(TAG, error)
                    if (prefs.fallbackToDeviceTts) {
                        Log.d(TAG, "Falling back to device TTS after playback failure")
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
                    Log.d(TAG, "Falling back to device TTS after generation failure")
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

    /**
     * Play an audio file and return (success, detail).
     * detail is duration in ms on success, or error message on failure.
     */
    private suspend fun playAudioFile(file: File): Pair<Boolean, String> = withContext(Dispatchers.Main) {
        // Validate file before attempting playback
        if (!file.exists()) {
            return@withContext Pair(false, "Audio file does not exist")
        }
        if (file.length() < 45) { // WAV header is 44 bytes minimum
            file.delete()
            return@withContext Pair(false, "Audio file too small: ${file.length()} bytes")
        }

        val completable = CompletableDeferred<Pair<Boolean, String>>()
        var mediaPlayer: MediaPlayer? = null

        requestAudioFocus()

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                prepare()
            }

            // Verify the audio is actually playable
            val duration = mediaPlayer.duration
            if (duration <= 0) {
                mediaPlayer.release()
                file.delete()
                abandonAudioFocus()
                return@withContext Pair(false, "Audio file has no playable content (duration=$duration)")
            }

            Log.d(TAG, "Audio prepared: duration=${duration}ms, file=${file.length()} bytes")

            currentMediaPlayer = mediaPlayer

            mediaPlayer.setOnCompletionListener { mp ->
                Log.d(TAG, "Audio playback completed successfully (duration=${duration}ms)")
                mp.release()
                currentMediaPlayer = null
                file.delete()
                abandonAudioFocus()
                completable.complete(Pair(true, "${duration}ms"))
            }

            mediaPlayer.setOnErrorListener { mp, what, extra ->
                val errorMsg = "MediaPlayer error: what=$what, extra=$extra"
                Log.e(TAG, errorMsg)
                mp.release()
                currentMediaPlayer = null
                file.delete()
                abandonAudioFocus()
                completable.complete(Pair(false, errorMsg))
                true
            }

            // Apply speed if supported
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && prefs.ttsSpeed != 1.0f) {
                    mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(prefs.ttsSpeed)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not set playback speed: ${e.message}")
            }

            mediaPlayer.start()

            // Verify playback actually started
            if (!mediaPlayer.isPlaying) {
                mediaPlayer.release()
                currentMediaPlayer = null
                file.delete()
                abandonAudioFocus()
                return@withContext Pair(false, "MediaPlayer.start() called but isPlaying=false")
            }

            completable.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio: ${e.javaClass.simpleName}: ${e.message}", e)
            file.delete()
            abandonAudioFocus()
            // Release the media player we created (not the old one)
            mediaPlayer?.let {
                try { it.release() } catch (_: Exception) {}
            }
            currentMediaPlayer = null
            Pair(false, "${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /**
     * Speak with device TTS. Returns true if speech completed, false on error.
     */
    private suspend fun speakWithDeviceTts(text: String): Boolean = withContext(Dispatchers.Main) {
        // Wait up to 3 seconds for TTS engine to initialize
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

        requestAudioFocus()

        deviceTts?.apply {
            language = Locale("he", "IL")
            setSpeechRate(prefs.ttsSpeed)
            setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "Device TTS started speaking")
                }
                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "Device TTS completed successfully")
                    abandonAudioFocus()
                    completable.complete(true)
                }
                override fun onError(utteranceId: String?) {
                    Log.e(TAG, "Device TTS error for utterance: $utteranceId")
                    abandonAudioFocus()
                    completable.complete(false)
                }
            })
            speak(text, TextToSpeech.QUEUE_FLUSH, null, "ntts_${System.currentTimeMillis()}")
        } ?: run {
            completable.complete(false)
        }

        completable.await()
    }

    private fun requestAudioFocus() {
        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
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

    fun pause() {
        isPaused = true
        currentMediaPlayer?.let {
            try {
                it.stop()
                it.release()
            } catch (_: Exception) {}
        }
        currentMediaPlayer = null
        deviceTts?.stop()
        // Report failure for all queued items
        while (queue.isNotEmpty()) {
            val item = queue.poll()
            item?.onResult?.invoke(false, "Paused - cleared from queue")
        }
        abandonAudioFocus()
    }

    fun resume() {
        isPaused = false
    }

    fun clearQueue() {
        while (queue.isNotEmpty()) {
            val item = queue.poll()
            item?.onResult?.invoke(false, "Queue cleared")
        }
    }

    fun destroy() {
        pause()
        scope.cancel()
        deviceTts?.shutdown()
        geminiTTSAPI.cleanup()
    }
}
