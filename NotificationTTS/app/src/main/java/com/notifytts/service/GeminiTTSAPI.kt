package com.notifytts.service

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.notifytts.data.GeminiVoice
import com.notifytts.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

class GeminiTTSAPI(private val cacheDir: File) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        private const val TAG = "GeminiTTSAPI"
        private const val GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val SAMPLE_RATE = 24000
        private const val CHANNELS = 1
        private const val BITS_PER_SAMPLE = 16

        /**
         * Tone presets using Gemini 3.1's Director's Notes framework.
         * These give the model a full character profile + performance direction,
         * producing far more consistent and natural-sounding output than simple "say in X tone".
         */
        val TONE_PRESETS: Map<String, String> = linkedMapOf(
            "neutral" to """
                |Audio Profile: A composed, professional narrator with a steady, even delivery.
                |Scene: Reading a notification aloud in a quiet room.
                |Director's Notes: Keep tone completely neutral and measured. No emotional coloring.
                |Maintain consistent pacing throughout. Read clearly and precisely.
            """.trimMargin(),
            "calm" to """
                |Audio Profile: A gentle, reassuring voice — like a late-night radio host.
                |Scene: Softly reading a message to someone resting nearby.
                |Director's Notes: Speak slowly and softly with warmth. Lower register preferred.
                |Let words breathe — slight pauses between phrases. Never rush. [gentle] [soft-spoken]
            """.trimMargin(),
            "friendly" to """
                |Audio Profile: An upbeat, approachable friend sharing good news.
                |Scene: Casually telling a friend about a message they just got.
                |Director's Notes: Warm smile in the voice — 'The Vocal Smile' technique with soft palate raised.
                |Conversational and natural. Light and pleasant, but not over-the-top. [cheerful]
            """.trimMargin(),
            "professional" to """
                |Audio Profile: A polished news anchor delivering a brief update.
                |Scene: A crisp, authoritative readout of important information.
                |Director's Notes: Clear enunciation, confident pacing. Slight formality without being stiff.
                |Emphasis on clarity and precision. No filler words. Measured tempo.
            """.trimMargin(),
            "energetic" to """
                |Audio Profile: A lively, enthusiastic presenter with infectious energy.
                |Scene: Excitedly announcing something interesting.
                |Director's Notes: Higher energy, slightly faster pace. Dynamic pitch variation.
                |Genuine excitement without yelling. [excited] Emphasize key words naturally.
            """.trimMargin(),
            "whisper" to """
                |Audio Profile: Someone whispering discreetly.
                |Scene: Quietly relaying a private message so only the listener hears.
                |Director's Notes: [whispers] Breathy, hushed delivery throughout. Very quiet.
                |Intimate and private-sounding. Slow, deliberate pacing.
            """.trimMargin(),
            "storyteller" to """
                |Audio Profile: A captivating storyteller with rich, expressive delivery.
                |Scene: Narrating a passage from a book to an engaged listener.
                |Director's Notes: Dynamic range — vary pace and pitch to match content.
                |Paint pictures with the voice. Slight dramatic pauses for effect.
                |Let important words land with weight.
            """.trimMargin(),
            "custom" to "" // User-defined instruction
        )

        val TONE_LABELS: Map<String, String> = linkedMapOf(
            "neutral" to "Neutral",
            "calm" to "Calm & Relaxed",
            "friendly" to "Warm & Friendly",
            "professional" to "Professional",
            "energetic" to "Energetic",
            "whisper" to "Whisper",
            "storyteller" to "Storyteller",
            "custom" to "Custom"
        )

        /** All 30 built-in Gemini TTS voices */
        val VOICES: List<GeminiVoice> = listOf(
            // Female voices
            GeminiVoice("Zephyr", "Female", "Polished, warm, modern American"),
            GeminiVoice("Kore", "Female", "Neutral, professional"),
            GeminiVoice("Achernar", "Female", "Soft and gentle"),
            GeminiVoice("Aoede", "Female", "Bright and clear"),
            GeminiVoice("Autonoe", "Female", "Warm and smooth"),
            GeminiVoice("Callirrhoe", "Female", "Expressive and vivid"),
            GeminiVoice("Despina", "Female", "Clear and pleasant"),
            GeminiVoice("Erinome", "Female", "Warm and refined"),
            GeminiVoice("Gacrux", "Female", "Balanced and composed"),
            GeminiVoice("Laomedeia", "Female", "Natural and engaging"),
            GeminiVoice("Leda", "Female", "Smooth and elegant"),
            GeminiVoice("Pulcherrima", "Female", "Rich and polished"),
            GeminiVoice("Sulafat", "Female", "Calm and measured"),
            GeminiVoice("Vindemiatrix", "Female", "Bright and energetic"),
            // Male voices
            GeminiVoice("Puck", "Male", "Conversational, friendly"),
            GeminiVoice("Charon", "Male", "Deep, authoritative"),
            GeminiVoice("Achird", "Male", "Friendly and approachable"),
            GeminiVoice("Algenib", "Male", "Gravelly texture"),
            GeminiVoice("Algieba", "Male", "Smooth and pleasant"),
            GeminiVoice("Alnilam", "Male", "Firm and strong"),
            GeminiVoice("Enceladus", "Male", "Breathy and soft"),
            GeminiVoice("Fenrir", "Male", "Excitable and dynamic"),
            GeminiVoice("Iapetus", "Male", "Clear and articulate"),
            GeminiVoice("Orus", "Male", "Firm and decisive"),
            GeminiVoice("Rasalgethi", "Male", "Informative and professional"),
            GeminiVoice("Sadachbia", "Male", "Lively and animated"),
            GeminiVoice("Sadaltager", "Male", "Knowledgeable and authoritative"),
            GeminiVoice("Schedar", "Male", "Even and balanced"),
            GeminiVoice("Umbriel", "Male", "Easy-going and calm"),
            GeminiVoice("Zubenelgenubi", "Male", "Casual and conversational"),
        )
    }

    suspend fun synthesize(
        text: String,
        apiKey: String,
        voiceName: String = Constants.DEFAULT_VOICE_NAME,
        modelId: String = Constants.DEFAULT_GEMINI_MODEL,
        speed: Float = Constants.DEFAULT_SPEED,
        toneInstruction: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            // Gemini 3.1 Flash TTS supports Director's Notes for precise tone control.
            // Wrap the notification text with the character profile + the actual text to read.
            val finalText = if (!toneInstruction.isNullOrBlank()) {
                "$toneInstruction\n\nNow read this notification aloud:\n\"$text\""
            } else {
                text
            }

            val body = JsonObject().apply {
                add("contents", gson.toJsonTree(listOf(
                    mapOf("parts" to listOf(mapOf("text" to finalText)))
                )))
                add("generationConfig", JsonObject().apply {
                    add("responseModalities", gson.toJsonTree(listOf("AUDIO")))
                    add("speechConfig", JsonObject().apply {
                        add("voiceConfig", JsonObject().apply {
                            add("prebuiltVoiceConfig", JsonObject().apply {
                                addProperty("voiceName", voiceName)
                            })
                        })
                    })
                })
            }

            val url = "$GEMINI_API_BASE/$modelId:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()

            // Use .use {} to ensure response is always closed properly
            response.use { resp ->
                if (!resp.isSuccessful) {
                    val errorBody = resp.body?.string() ?: "Unknown error"
                    Log.e(TAG, "Gemini API error ${resp.code}: $errorBody")
                    return@withContext Result.failure(
                        Exception("Gemini TTS API error ${resp.code}: $errorBody")
                    )
                }

                val responseJson = resp.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response from Gemini"))

                val root = gson.fromJson(responseJson, JsonObject::class.java)
                val candidates = root.getAsJsonArray("candidates")
                    ?: return@withContext Result.failure(Exception("No candidates in response"))

                if (candidates.size() == 0) {
                    return@withContext Result.failure(Exception("Empty candidates array in response"))
                }

                val content = candidates[0].asJsonObject
                    .getAsJsonObject("content")
                    ?: return@withContext Result.failure(Exception("No content in candidate"))

                val parts = content.getAsJsonArray("parts")
                if (parts == null || parts.size() == 0) {
                    return@withContext Result.failure(Exception("No parts in response content"))
                }

                val inlineData = parts[0].asJsonObject
                    .getAsJsonObject("inlineData")
                    ?: return@withContext Result.failure(Exception("No inlineData in response"))

                val mimeType = inlineData.get("mimeType")?.asString ?: ""
                val audioBase64 = inlineData.get("data")?.asString
                    ?: return@withContext Result.failure(Exception("No audio data in response"))

                val audioBytes = Base64.decode(audioBase64, Base64.DEFAULT)

                if (audioBytes.isEmpty()) {
                    return@withContext Result.failure(Exception("Decoded audio data is empty"))
                }

                Log.d(TAG, "Received audio: mimeType=$mimeType, size=${audioBytes.size} bytes, voice=$voiceName, model=$modelId")

                val wavFile = File(cacheDir, "tts_${System.currentTimeMillis()}.wav")

                // Check if data is already a WAV file or needs wrapping
                if (mimeType.contains("wav", ignoreCase = true) || isWavData(audioBytes)) {
                    // Already WAV-formatted, write directly
                    wavFile.writeBytes(audioBytes)
                    Log.d(TAG, "Audio was already WAV-formatted, wrote directly")
                } else {
                    // Raw PCM data, wrap in WAV container
                    writeWavFile(wavFile, audioBytes)
                    Log.d(TAG, "Wrapped raw PCM in WAV container")
                }

                if (!wavFile.exists() || wavFile.length() < 45) {
                    return@withContext Result.failure(Exception("Generated audio file is empty or too small (${wavFile.length()} bytes)"))
                }

                Log.d(TAG, "Audio file ready: ${wavFile.length()} bytes")
                Result.success(wavFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "synthesize() exception: ${e.javaClass.simpleName}: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Verifies the API key with a simple GET to the model endpoint.
     * Uses HttpURLConnection (not OkHttp) for maximum simplicity/stability.
     */
    suspend fun verifyApiKey(apiKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("API key is empty"))
        }
        var conn: java.net.HttpURLConnection? = null
        try {
            val url = java.net.URL("$GEMINI_API_BASE/${Constants.DEFAULT_GEMINI_MODEL}?key=$apiKey")
            conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            val code = conn.responseCode
            conn.disconnect()
            when (code) {
                200 -> Result.success(true)
                401 -> Result.failure(Exception("Invalid API key"))
                403 -> Result.failure(Exception("API key not authorized"))
                404 -> Result.failure(Exception("TTS model not found"))
                429 -> Result.failure(Exception("Rate limited - try again later"))
                else -> Result.failure(Exception("HTTP error $code"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "verifyApiKey: ${e.message}")
            try { conn?.disconnect() } catch (_: Exception) {}
            Result.failure(Exception(e.message ?: "Connection failed"))
        }
    }

    private fun extractErrorMessage(responseBody: String): String? {
        return try {
            val root = gson.fromJson(responseBody, JsonObject::class.java)
            root?.getAsJsonObject("error")?.get("message")?.asString
        } catch (e: Exception) {
            null
        }
    }

    /** Check if audio bytes start with RIFF/WAVE header */
    private fun isWavData(data: ByteArray): Boolean {
        if (data.size < 12) return false
        return data[0] == 'R'.code.toByte() &&
                data[1] == 'I'.code.toByte() &&
                data[2] == 'F'.code.toByte() &&
                data[3] == 'F'.code.toByte() &&
                data[8] == 'W'.code.toByte() &&
                data[9] == 'A'.code.toByte() &&
                data[10] == 'V'.code.toByte() &&
                data[11] == 'E'.code.toByte()
    }

    private fun writeWavFile(file: File, pcmData: ByteArray) {
        val dataSize = pcmData.size
        val byteRate = SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8
        val blockAlign = CHANNELS * BITS_PER_SAMPLE / 8

        file.outputStream().use { out ->
            val header = ByteBuffer.allocate(44).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                // RIFF header
                put("RIFF".toByteArray())
                putInt(36 + dataSize)
                put("WAVE".toByteArray())
                // fmt sub-chunk
                put("fmt ".toByteArray())
                putInt(16) // sub-chunk size
                putShort(1) // PCM format
                putShort(CHANNELS.toShort())
                putInt(SAMPLE_RATE)
                putInt(byteRate)
                putShort(blockAlign.toShort())
                putShort(BITS_PER_SAMPLE.toShort())
                // data sub-chunk
                put("data".toByteArray())
                putInt(dataSize)
            }
            out.write(header.array())
            out.write(pcmData)
        }
    }

    fun cleanup() {
        cacheDir.listFiles()?.filter { it.name.startsWith("tts_") && it.name.endsWith(".wav") }
            ?.forEach { it.delete() }
    }
}
