package com.notifytts.service

import android.util.Base64
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

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                return@withContext Result.failure(
                    Exception("Gemini TTS API error ${response.code}: $errorBody")
                )
            }

            val responseJson = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response from Gemini"))

            val root = gson.fromJson(responseJson, JsonObject::class.java)
            val candidates = root.getAsJsonArray("candidates")
                ?: return@withContext Result.failure(Exception("No candidates in response"))

            val inlineData = candidates[0].asJsonObject
                .getAsJsonObject("content")
                .getAsJsonArray("parts")[0].asJsonObject
                .getAsJsonObject("inlineData")

            val audioBase64 = inlineData.get("data").asString
            val pcmData = Base64.decode(audioBase64, Base64.DEFAULT)

            // Wrap PCM data in WAV container
            val wavFile = File(cacheDir, "tts_${System.currentTimeMillis()}.wav")
            writeWavFile(wavFile, pcmData)

            Result.success(wavFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifies that the API key is valid by making a minimal request.
     */
    suspend fun verifyApiKey(apiKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                add("contents", gson.toJsonTree(listOf(
                    mapOf("parts" to listOf(mapOf("text" to "Hi")))
                )))
                add("generationConfig", JsonObject().apply {
                    add("response_modalities", gson.toJsonTree(listOf("AUDIO")))
                    add("speech_config", JsonObject().apply {
                        add("voice_config", JsonObject().apply {
                            add("prebuilt_voice_config", JsonObject().apply {
                                addProperty("voice_name", "Kore")
                            })
                        })
                    })
                })
            }

            val url = "$GEMINI_API_BASE/${Constants.DEFAULT_GEMINI_MODEL}:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            response.body?.close()

            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val code = response.code
                when (code) {
                    400 -> Result.failure(Exception("Invalid request"))
                    403 -> Result.failure(Exception("API key not authorized for Gemini TTS"))
                    429 -> Result.failure(Exception("Rate limited - try again later"))
                    else -> Result.failure(Exception("API error: $code"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
