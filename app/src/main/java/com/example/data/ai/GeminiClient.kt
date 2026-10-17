package com.example.data.ai

import android.util.Log
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

class GeminiClient(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val editPlanAdapter = moshi.adapter(EditPlan::class.java)

    suspend fun analyzeVideoPrompt(
        modelId: String = "gemini-2.5-flash",
        videoDurationMs: Long,
        userPrompt: String,
        sceneContext: String
    ): Result<EditPlan> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API Key is empty"))
        }

        try {
            val systemPrompt = """
                You are an AI Smart Video Cutter assistant. Analyze the user's editing request for a video of duration $videoDurationMs milliseconds.
                Scene Context: $sceneContext
                
                Respond ONLY with a valid JSON object matching this schema exactly (no markdown block wrappers):
                {
                  "planId": "plan_gemini_${System.currentTimeMillis()}",
                  "taskDescription": "Short description of action",
                  "suggestedClips": [
                    {
                      "id": "clip_1",
                      "title": "Clip Title",
                      "startMs": 0,
                      "endMs": 15000,
                      "rationale": "Reason for selection",
                      "confidence": 0.95
                    }
                  ],
                  "requiresClarification": false,
                  "clarificationPrompt": null
                }
            """.trimIndent()

            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "$systemPrompt\n\nUser Prompt: $userPrompt"))
                    })
                })
            }

            val requestBodyJson = JSONObject().apply {
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                })
            }

            val targetModel = if (modelId.contains("/")) modelId.substringAfter("/") else modelId
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$targetModel:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val statusCode = response.code
                Log.e(TAG, "Gemini API request failed HTTP $statusCode: $responseBody")
                if (statusCode == 429) {
                    return@withContext Result.failure(QuotaExceededException("Gemini API rate limit exceeded (HTTP 429)"))
                }
                return@withContext Result.failure(Exception("Gemini HTTP $statusCode: $responseBody"))
            }

            val jsonObject = JSONObject(responseBody)
            val candidates = jsonObject.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.failure(Exception("Gemini API returned empty candidates array"))
            }

            val content = candidates.getJSONObject(0).optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textStr = parts?.optJSONObject(0)?.optString("text") ?: ""

            val cleanJson = cleanJsonString(textStr)
            val editPlan = editPlanAdapter.fromJson(cleanJson)

            if (editPlan != null) {
                Result.success(editPlan)
            } else {
                Result.failure(Exception("Failed to parse Gemini EditPlan JSON: $textStr"))
            }

        } catch (e: QuotaExceededException) {
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini call", e)
            Result.failure(e)
        }
    }

    private fun cleanJsonString(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }

    companion object {
        private const val TAG = "GeminiClient"
    }
}
