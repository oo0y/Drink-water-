package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SafetyAnalysis(
    val isToxic: Boolean,
    val reason: String,
    val suggestedAction: String // "Allow", "Flag", "Block"
)

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private fun getApiKey(): String {
        val key = BuildConfig.GEMINI_API_KEY
        return if (key == "MY_GEMINI_API_KEY" || key.isEmpty()) "" else key
    }

    val isApiKeyConfigured: Boolean
        get() = getApiKey().isNotEmpty()

    suspend fun analyzeMessageSafety(messageText: String): SafetyAnalysis = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext SafetyAnalysis(
                isToxic = false,
                reason = "API Key not configured. Auto-moderation offline.",
                suggestedAction = "Allow"
            )
        }

        val prompt = """
            You are a professional server auto-moderation assistant for community server management.
            Analyze this message posted by a user: "$messageText"
            
            Determine if the message violates standard community guidelines (toxicity, extreme profanity, severe hate speech, obvious spam, malicious links, or scam attempts).
            
            Return your response in strict JSON format with exactly these three keys:
            - "isToxic": boolean
            - "reason": string explaining why it was flagged or allowed (under 15 words)
            - "suggestedAction": string, either "Allow", "Flag", or "Block"
            
            Do not include any surrounding markdown like ```json or anything else. Output only raw JSON.
        """.trimIndent()

        try {
            val responseString = callGeminiRaw(prompt, apiKey) ?: return@withContext SafetyAnalysis(false, "No response from AI", "Allow")
            
            // Parse response cleanup in case models wrap in markdown blocks
            var cleanedJson = responseString.trim()
            if (cleanedJson.startsWith("```json")) {
                cleanedJson = cleanedJson.removePrefix("```json")
            }
            if (cleanedJson.endsWith("```")) {
                cleanedJson = cleanedJson.removeSuffix("```")
            }
            cleanedJson = cleanedJson.trim()

            val json = JSONObject(cleanedJson)
            val isToxic = json.optBoolean("isToxic", false)
            val reason = json.optString("reason", "No reason provided")
            val suggestedAction = json.optString("suggestedAction", "Allow")

            SafetyAnalysis(isToxic, reason, suggestedAction)
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing message: ", e)
            SafetyAnalysis(false, "Error: ${e.localizedMessage}", "Allow")
        }
    }

    suspend fun generateAnnouncement(topic: String, style: String, channelContext: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext "API Key not configured in Secrets panel. Please set your GEMINI_API_KEY to generate community announcements dynamically!"
        }

        val prompt = """
            You are a creative community moderator and engagement specialist.
            Generate a short, engaging community announcement.
            Topic: $topic
            Style/Tone: $style (e.g., Welcoming, Energetic, Professional, Fun)
            Channel Context: $channelContext
            
            Format the announcement using clean Markdown with relevant emojis. Keep it under 100 words. Make it punchy and inviting to increase server interaction.
        """.trimIndent()

        try {
            callGeminiRaw(prompt, apiKey) ?: "Failed to generate announcement from AI."
        } catch (e: Exception) {
            "Error generating announcement: ${e.localizedMessage}"
        }
    }

    private fun callGeminiRaw(prompt: String, apiKey: String): String? {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        
        // Construct the request payload manually with JSONObject to prevent escaping / serialization issues
        val partObj = JSONObject().put("text", prompt)
        val partsArr = JSONArray().put(partObj)
        val contentObj = JSONObject().put("parts", partsArr)
        val contentsArr = JSONArray().put(contentObj)
        val payloadObj = JSONObject().put("contents", contentsArr)

        val requestBody = payloadObj.toString().toRequestBody(mediaType)
        val url = "$BASE_URL?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(TAG, "Unsuccessful response from Gemini: Code ${response.code} - ${response.message}")
                return null
            }

            val body = response.body?.string() ?: return null
            val responseJson = JSONObject(body)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                if (content != null) {
                    val parts = content.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return parts.getJSONObject(0).optString("text")
                    }
                }
            }
            return null
        }
    }
}
