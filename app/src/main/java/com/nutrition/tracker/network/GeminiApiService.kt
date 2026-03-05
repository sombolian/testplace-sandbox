package com.nutrition.tracker.network

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private fun com.google.ai.client.generativeai.type.GenerateContentResponse.extractText(): String? {
    // Access candidate text directly to avoid the exception thrown by .text
    // when finishReason is MAX_TOKENS
    return candidates.firstOrNull()
        ?.content
        ?.parts
        ?.filterIsInstance<com.google.ai.client.generativeai.type.TextPart>()
        ?.joinToString("") { it.text }
        ?.trim()
}

@Serializable
data class NutritionEstimate(
    val description: String = "",
    val calories: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0,
    val confidence: String = "medium",
    val tips: String = ""
)

class GeminiApiService {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun analyzeMeal(
        apiKey: String,
        modelName: String,
        textDescription: String?,
        image: Bitmap?,
        userContext: String = ""
    ): Result<NutritionEstimate> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("Please set your Gemini API key in Settings"))
            }

            val model = GenerativeModel(
                modelName = modelName.ifBlank { "gemini-3.1-pro-preview" },
                apiKey = apiKey,
                generationConfig = generationConfig {
                    temperature = 0.1f
                    topP = 0.95f
                    maxOutputTokens = 8192
                }
            )

            val prompt = buildString {
                append("You are a nutrition analysis expert. Analyze the following meal and provide a JSON estimation of its nutritional content.\n\n")
                if (userContext.isNotBlank()) {
                    append("User context: $userContext\n\n")
                }
                if (!textDescription.isNullOrBlank()) {
                    append("Meal description: $textDescription\n\n")
                }
                if (image != null) {
                    append("An image of the meal is also provided. Use both the image and any text description to make your estimate.\n\n")
                }
                append("Respond ONLY with a valid JSON object (no markdown, no code blocks, no explanation). Keep the tips field under 20 words. Fields: ")
                append("""{"description":"...","calories":0,"protein":0.0,"carbs":0.0,"fat":0.0,"fiber":0.0,"confidence":"medium","tips":"..."}""")
            }

            val response = if (image != null) {
                val inputContent = content {
                    image(image)
                    text(prompt)
                }
                model.generateContent(inputContent)
            } else {
                model.generateContent(prompt)
            }

            val responseText = response.extractText()
                ?: throw Exception("Empty response from Gemini")

            // Clean up response - remove markdown code blocks if present
            val cleanJson = responseText
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val estimate = json.decodeFromString<NutritionEstimate>(cleanJson)
            Result.success(estimate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDailyAdvice(
        apiKey: String,
        modelName: String,
        daySummary: String,
        userContext: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val model = GenerativeModel(
                modelName = modelName.ifBlank { "gemini-3.1-pro-preview" },
                apiKey = apiKey,
                generationConfig = generationConfig {
                    temperature = 0.7f
                    maxOutputTokens = 2048
                }
            )

            val prompt = """
                You are a supportive nutrition coach. Based on the user's daily food intake summary, provide brief, encouraging, and actionable advice.

                User context: $userContext

                Today's summary: $daySummary

                Give 2-3 sentences of personalized advice. Be supportive and motivating. If they're doing well, celebrate it. If there's room for improvement, be constructive and kind.
            """.trimIndent()

            val response = model.generateContent(prompt)
            Result.success(response.extractText() ?: "Keep going! You're making progress.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
