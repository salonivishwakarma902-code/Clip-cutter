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

class OpenRouterClient(private val apiKey: String) {

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
        modelId: String = ModelRegistry.DEFAULT_OPENROUTER_MODEL,
        videoDurationMs: Long,
        userPrompt: String,
        sceneContext: String
    ): Result<EditPlan> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("OpenRouter API Key is empty"))
        }

        try {
            val systemPrompt = """
                You are an AI Smart Video Cutter assistant. Analyze the user's editing request for a video of duration $videoDurationMs milliseconds.
                Scene Context: $sceneContext
                
                Respond ONLY with a valid JSON object matching this schema exactly (no markdown block wrappers):
                {
                  "planId": "plan_12345",
                  "taskDescription": "Short description of action",
                  "suggestedClips": [
                    {
                      "id": "clip_1",
                      "title": "Clip Title",
                      "startMs": 0,
                      "endMs": 15000,
                      "rationale": "Reason for selection",
                      "confidence": 0.92
                    }
                  ],
                  "requiresClarification": false,
                  "clarificationPrompt": null
                }
            """.trimIndent()

            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            }

            val requestBodyJson = JSONObject().apply {
                put("model", modelId)
                put("messages", messagesArray)
                put("temperature", 0.2)
                put("response_format", JSONObject().put("type", "json_object"))
            }

            val request = Request.Builder()
                .url("https://openrouter.ai/api/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("HTTP-Referer", "https://aistudio.google.com")
                .addHeader("X-Title", "AI Video Cutter")
                .addHeader("Content-Type", "application/json")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val statusCode = response.code
                Log.e(TAG, "OpenRouter API request failed with status $statusCode: $responseBody")
                if (statusCode == 429) {
                    return@withContext Result.failure(QuotaExceededException("OpenRouter rate limit exceeded (HTTP 429)"))
                }
                return@withContext Result.failure(Exception("OpenRouter error HTTP $statusCode: $responseBody"))
            }

            val jsonObject = JSONObject(responseBody)
            val choices = jsonObject.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                return@withContext Result.failure(Exception("OpenRouter returned empty choices array"))
            }

            val messageObj = choices.getJSONObject(0).optJSONObject("message")
            val contentStr = messageObj?.optString("content") ?: ""

            val cleanJson = cleanJsonString(contentStr)
            val editPlan = editPlanAdapter.fromJson(cleanJson)

            if (editPlan != null) {
                Result.success(editPlan)
            } else {
                Result.failure(Exception("Failed to parse EditPlan JSON: $contentStr"))
            }

        } catch (e: QuotaExceededException) {
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during OpenRouter call", e)
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
        private const val TAG = "OpenRouterClient"
    }
}

class QuotaExceededException(message: String) : Exception(message)
