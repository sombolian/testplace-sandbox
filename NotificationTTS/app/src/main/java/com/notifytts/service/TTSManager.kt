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

class TTSManager(private val context: Context) {

    companion object {
        private const val TAG = "TTSManager"
    }

    private val prefs = PreferencesManager(context)
    private val geminiTTSAPI = GeminiTTSAPI(context.cacheDir)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val queue = ConcurrentLinkedQueue<String>()
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

    fun enqueue(text: String) {
        if (isPaused) return
        if (queue.size >= prefs.maxQueueSize) {
            Log.w(TAG, "Queue full, dropping notification")
            return
        }

        val truncated = if (text.length > prefs.maxTextLength) {
            text.take(prefs.maxTextLength) + "..."
        } else {
            text
        }

        queue.add(truncated)
        processQueue()
    }

    private fun processQueue() {
        scope.launch {
            mutex.withLock {
                if (isProcessing) return@launch
                isProcessing = true
            }

            while (queue.isNotEmpty() && !isPaused) {
                val text = queue.poll() ?: break
                try {
                    speak(text)
                } catch (e: Exception) {
                    Log.e(TAG, "Error speaking: ${e.message}", e)
                }
            }

            mutex.withLock {
                isProcessing = false
            }
        }
    }

    private suspend fun speak(text: String) {
        if (prefs.useDeviceTtsOnly) {
            speakWithDeviceTts(text)
            return
        }

        val apiKey = prefs.geminiApiKey
        if (apiKey.isBlank()) {
            if (prefs.fallbackToDeviceTts) {
                speakWithDeviceTts(text)
            }
            return
        }

        val result = geminiTTSAPI.synthesize(
            text = text,
            apiKey = apiKey,
            voiceName = prefs.geminiVoiceName,
            modelId = prefs.geminiModel,
            speed = prefs.ttsSpeed
        )

        result.fold(
            onSuccess = { audioFile ->
                playAudioFile(audioFile)
            },
            onFailure = { error ->
                Log.e(TAG, "Gemini TTS failed: ${error.message}")
                if (prefs.fallbackToDeviceTts) {
                    speakWithDeviceTts(text)
                }
            }
        )
    }

    private suspend fun playAudioFile(file: File) = withContext(Dispatchers.Main) {
        val completable = CompletableDeferred<Unit>()

        requestAudioFocus()

        try {
            currentMediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                prepare()

                setOnCompletionListener {
                    it.release()
                    currentMediaPlayer = null
                    file.delete()
                    abandonAudioFocus()
                    completable.complete(Unit)
                }

                setOnErrorListener { mp, _, _ ->
                    mp.release()
                    currentMediaPlayer = null
                    file.delete()
                    abandonAudioFocus()
                    completable.complete(Unit)
                    true
                }

                // Apply speed if supported
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && prefs.ttsSpeed != 1.0f) {
                        playbackParams = playbackParams.setSpeed(prefs.ttsSpeed)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not set playback speed: ${e.message}")
                }

                start()
            }

            completable.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio: ${e.message}", e)
            file.delete()
            abandonAudioFocus()
            currentMediaPlayer?.release()
            currentMediaPlayer = null
        }
    }

    private suspend fun speakWithDeviceTts(text: String) = withContext(Dispatchers.Main) {
        // Wait up to 3 seconds for TTS engine to initialize
        var waited = 0
        while (!deviceTtsReady && waited < 3000) {
            delay(100)
            waited += 100
        }
        if (!deviceTtsReady) {
            Log.w(TAG, "Device TTS not ready after ${waited}ms")
            return@withContext
        }

        val completable = CompletableDeferred<Unit>()

        requestAudioFocus()

        deviceTts?.apply {
            language = Locale("he", "IL")
            setSpeechRate(prefs.ttsSpeed)
            setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    abandonAudioFocus()
                    completable.complete(Unit)
                }
                override fun onError(utteranceId: String?) {
                    abandonAudioFocus()
                    completable.complete(Unit)
                }
            })
            speak(text, TextToSpeech.QUEUE_FLUSH, null, "ntts_${System.currentTimeMillis()}")
        } ?: completable.complete(Unit)

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
            it.stop()
            it.release()
        }
        currentMediaPlayer = null
        deviceTts?.stop()
        queue.clear()
        abandonAudioFocus()
    }

    fun resume() {
        isPaused = false
    }

    fun clearQueue() {
        queue.clear()
    }

    fun destroy() {
        pause()
        scope.cancel()
        deviceTts?.shutdown()
        geminiTTSAPI.cleanup()
    }
}
