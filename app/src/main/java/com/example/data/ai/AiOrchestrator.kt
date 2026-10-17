package com.example.data.ai

import android.util.Log
import com.example.data.media.SceneCandidate
import com.example.data.security.EncryptedKeyStorage
import com.example.data.security.EncryptedKeyStorage.ProviderPriority

class AiOrchestrator(private val keyStorage: EncryptedKeyStorage) {

    suspend fun analyzeVideoPrompt(
        userPrompt: String,
        videoDurationMs: Long,
        scenes: List<SceneCandidate>
    ): AnalysisResult {
        val openRouterKey = keyStorage.openRouterApiKey
        val geminiKey = keyStorage.geminiApiKey
        val priority = keyStorage.providerPriority

        val sceneContext = buildSceneContext(scenes)

        // Determine execution order based on user settings
        when (priority) {
            ProviderPriority.OPENROUTER_FIRST -> {
                if (openRouterKey.isNotBlank()) {
                    Log.i(TAG, "Attempting primary analysis via OpenRouter...")
                    val openRouter = OpenRouterClient(openRouterKey)
                    val result = openRouter.analyzeVideoPrompt(
                        videoDurationMs = videoDurationMs,
                        userPrompt = userPrompt,
                        sceneContext = sceneContext
                    )
                    if (result.isSuccess) {
                        return AnalysisResult(
                            editPlan = result.getOrThrow(),
                            providerUsed = "OpenRouter",
                            isLocalFallback = false
                        )
                    } else {
                        Log.w(TAG, "OpenRouter failed: ${result.exceptionOrNull()?.message}. Trying Gemini fallback...")
                    }
                }

                if (geminiKey.isNotBlank()) {
                    Log.i(TAG, "Attempting fallback analysis via Gemini...")
                    val gemini = GeminiClient(geminiKey)
                    val result = gemini.analyzeVideoPrompt(
                        videoDurationMs = videoDurationMs,
                        userPrompt = userPrompt,
                        sceneContext = sceneContext
                    )
                    if (result.isSuccess) {
                        return AnalysisResult(
                            editPlan = result.getOrThrow(),
                            providerUsed = "Google Gemini (Fallback)",
                            isLocalFallback = false
                        )
                    }
                }
            }

            ProviderPriority.GEMINI_FIRST -> {
                if (geminiKey.isNotBlank()) {
                    Log.i(TAG, "Attempting primary analysis via Gemini...")
                    val gemini = GeminiClient(geminiKey)
                    val result = gemini.analyzeVideoPrompt(
                        videoDurationMs = videoDurationMs,
                        userPrompt = userPrompt,
                        sceneContext = sceneContext
                    )
                    if (result.isSuccess) {
                        return AnalysisResult(
                            editPlan = result.getOrThrow(),
                            providerUsed = "Google Gemini",
                            isLocalFallback = false
                        )
                    } else {
                        Log.w(TAG, "Gemini failed: ${result.exceptionOrNull()?.message}. Trying OpenRouter fallback...")
                    }
                }

                if (openRouterKey.isNotBlank()) {
                    Log.i(TAG, "Attempting fallback analysis via OpenRouter...")
                    val openRouter = OpenRouterClient(openRouterKey)
                    val result = openRouter.analyzeVideoPrompt(
                        videoDurationMs = videoDurationMs,
                        userPrompt = userPrompt,
                        sceneContext = sceneContext
                    )
                    if (result.isSuccess) {
                        return AnalysisResult(
                            editPlan = result.getOrThrow(),
                            providerUsed = "OpenRouter (Fallback)",
                            isLocalFallback = false
                        )
                    }
                }
            }

            ProviderPriority.LOCAL_ONLY -> {
                Log.i(TAG, "Local-only mode enabled in settings")
            }
        }

        // Fallback to offline local scene-based EditPlan if API keys are missing or calls failed
        Log.i(TAG, "Generating local scene-based EditPlan fallback...")
        val fallbackPlan = generateLocalFallbackPlan(userPrompt, videoDurationMs, scenes)
        return AnalysisResult(
            editPlan = fallbackPlan,
            providerUsed = "Local Scene Scanner (Offline / Key Unconfigured)",
            isLocalFallback = true
        )
    }

    private fun buildSceneContext(scenes: List<SceneCandidate>): String {
        if (scenes.isEmpty()) return "No distinct scene shifts detected."
        val builder = StringBuilder("Detected Scene Candidates:\n")
        scenes.forEachIndexed { idx, scene ->
            builder.append("Scene ${idx + 1}: ${scene.formattedTimeRange()} (${scene.rationale})\n")
        }
        return builder.toString()
    }

    private fun generateLocalFallbackPlan(
        userPrompt: String,
        durationMs: Long,
        scenes: List<SceneCandidate>
    ): EditPlan {
        val suggestedClips = mutableListOf<ClipProposal>()

        if (scenes.isNotEmpty()) {
            scenes.take(3).forEachIndexed { idx, scene ->
                suggestedClips.add(
                    ClipProposal(
                        id = "local_clip_${idx + 1}",
                        title = "Scene ${idx + 1} / दृश्य ${idx + 1}",
                        startMs = scene.startMs,
                        endMs = scene.endMs,
                        rationale = "Locally detected visual scene shift. Configure API keys in Settings for AI prompt analysis.",
                        confidence = 0.85f
                    )
                )
            }
        } else {
            // Half video clip as safe default
            val endMs = (durationMs * 0.50).toLong().coerceAtLeast(5000L)
            suggestedClips.add(
                ClipProposal(
                    id = "local_default_clip",
                    title = "Primary Segment / प्राथमिक भाग",
                    startMs = 0L,
                    endMs = endMs,
                    rationale = "First 50% of the video duration. You can adjust start and end handles manually on the timeline scrubber.",
                    confidence = 0.80f
                )
            )
        }

        return EditPlan(
            planId = "local_plan_${System.currentTimeMillis()}",
            taskDescription = "Local estimated clips based on visual histogram scan: $userPrompt",
            suggestedClips = suggestedClips,
            requiresClarification = false,
            clarificationPrompt = null
        )
    }

    data class AnalysisResult(
        val editPlan: EditPlan,
        val providerUsed: String,
        val isLocalFallback: Boolean
    )

    companion object {
        private const val TAG = "AiOrchestrator"
    }
}
