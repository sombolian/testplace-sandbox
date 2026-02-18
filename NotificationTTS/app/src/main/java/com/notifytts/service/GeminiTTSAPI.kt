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

    companion object {
        private const val TAG = "GeminiTTSAPI"
        private const val GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val SAMPLE_RATE = 24000
        private const val CHANNELS = 1
        private const val BITS_PER_SAMPLE = 16

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

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun synthesize(
        text: String,
        apiKey: String,
        voiceName: String = Constants.DEFAULT_VOICE_NAME,
        modelId: String = Constants.DEFAULT_GEMINI_MODEL,
        speed: Float = Constants.DEFAULT_SPEED
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                add("contents", gson.toJsonTree(listOf(
                    mapOf("parts" to listOf(mapOf("text" to text)))
                )))
                add("generationConfig", JsonObject().apply {
                    add("response_modalities", gson.toJsonTree(listOf("AUDIO")))
                    add("speech_config", JsonObject().apply {
                        add("voice_config", JsonObject().apply {
                            add("prebuilt_voice_config", JsonObject().apply {
                                addProperty("voice_name", voiceName)
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
     * Verifies that the API key is valid by fetching the TTS model info (lightweight GET).
     * Does NOT generate audio - just checks the key can access the model.
     */
    suspend fun verifyApiKey(apiKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("API key is empty"))
            }

            // Lightweight: just GET the model info, no audio generation
            val url = "$GEMINI_API_BASE/${Constants.DEFAULT_GEMINI_MODEL}?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val verifyClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            var response: okhttp3.Response? = null
            try {
                response = verifyClient.newCall(request).execute()
                val code = response.code
                val responseBody = try { response.body?.string() ?: "" } catch (_: Exception) { "" }

                when {
                    response.isSuccessful -> {
                        Log.d(TAG, "API key verified successfully (model info returned)")
                        Result.success(true)
                    }
                    code == 400 -> {
                        val detail = extractErrorMessage(responseBody)
                        Result.failure(Exception("Invalid request${if (detail != null) ": $detail" else ""}"))
                    }
                    code == 401 -> Result.failure(Exception("Invalid API key"))
                    code == 403 -> Result.failure(Exception("API key not authorized for Gemini TTS"))
                    code == 404 -> Result.failure(Exception("TTS model not found (${Constants.DEFAULT_GEMINI_MODEL}). The model may have been updated."))
                    code == 429 -> Result.failure(Exception("Rate limited - try again later"))
                    else -> {
                        val detail = extractErrorMessage(responseBody)
                        Result.failure(Exception("API error $code${if (detail != null) ": $detail" else ""}"))
                    }
                }
            } finally {
                try { response?.close() } catch (_: Exception) {}
            }
        } catch (e: Throwable) {
            // Catch Throwable (not just Exception) to prevent any crash
            Log.e(TAG, "verifyApiKey error: ${e.javaClass.simpleName}: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Verification failed"))
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
