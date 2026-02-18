package com.notifytts.service

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.notifytts.data.ElevenLabsVoice
import com.notifytts.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class ElevenLabsAPI(private val cacheDir: File) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun synthesize(
        text: String,
        apiKey: String,
        voiceId: String = Constants.DEFAULT_VOICE_ID,
        modelId: String = Constants.DEFAULT_MODEL,
        stability: Float = Constants.DEFAULT_STABILITY,
        similarityBoost: Float = Constants.DEFAULT_SIMILARITY_BOOST,
        style: Float = Constants.DEFAULT_STYLE,
        speed: Float = Constants.DEFAULT_SPEED
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                addProperty("text", text)
                addProperty("model_id", modelId)
                add("voice_settings", JsonObject().apply {
                    addProperty("stability", stability)
                    addProperty("similarity_boost", similarityBoost)
                    addProperty("style", style)
                    addProperty("use_speaker_boost", true)
                })
                // Detect Hebrew text and hint the language
                if (text.any { it in '\u0590'..'\u05FF' || it in '\uFB1D'..'\uFB4F' }) {
                    addProperty("language_code", "he")
                }
            }

            val url = "${Constants.ELEVENLABS_BASE_URL}/text-to-speech/$voiceId" +
                    "?output_format=mp3_44100_128"

            val request = Request.Builder()
                .url(url)
                .addHeader("xi-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "audio/mpeg")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                return@withContext Result.failure(
                    Exception("ElevenLabs API error ${response.code}: $errorBody")
                )
            }

            val audioFile = File(cacheDir, "tts_${System.currentTimeMillis()}.mp3")
            response.body?.byteStream()?.use { input ->
                audioFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            Result.success(audioFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getVoices(apiKey: String): Result<List<ElevenLabsVoice>> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${Constants.ELEVENLABS_BASE_URL}/voices")
                    .addHeader("xi-api-key", apiKey)
                    .get()
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Failed to fetch voices: ${response.code}")
                    )
                }

                val json = response.body?.string() ?: return@withContext Result.success(emptyList())
                val root = gson.fromJson(json, JsonObject::class.java)
                val voicesArray = root.getAsJsonArray("voices") ?: return@withContext Result.success(emptyList())

                val voices = voicesArray.map { element ->
                    val obj = element.asJsonObject
                    ElevenLabsVoice(
                        voiceId = obj.get("voice_id")?.asString ?: "",
                        name = obj.get("name")?.asString ?: "Unknown",
                        category = obj.get("category")?.asString ?: "",
                        previewUrl = obj.get("preview_url")?.asString
                    )
                }

                Result.success(voices)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getSubscriptionInfo(apiKey: String): Result<Pair<Int, Int>> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${Constants.ELEVENLABS_BASE_URL}/user/subscription")
                    .addHeader("xi-api-key", apiKey)
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed: ${response.code}"))
                }

                val json = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val root = gson.fromJson(json, JsonObject::class.java)
                val used = root.get("character_count")?.asInt ?: 0
                val limit = root.get("character_limit")?.asInt ?: 0
                Result.success(Pair(used, limit))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun cleanup() {
        cacheDir.listFiles()?.filter { it.name.startsWith("tts_") && it.name.endsWith(".mp3") }
            ?.forEach { it.delete() }
    }
}
