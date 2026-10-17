package com.example.data.ai

data class ModelSpec(
    val id: String,
    val name: String,
    val provider: String,
    val supportsMultimodal: Boolean,
    val contextWindowTokens: Int,
    val description: String
)

object ModelRegistry {

    val DEFAULT_OPENROUTER_MODEL = "google/gemini-2.0-flash-001"
    val FALLBACK_OPENROUTER_MODEL = "openai/gpt-4o-mini"
    val DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"

    val AVAILABLE_OPENROUTER_MODELS = listOf(
        ModelSpec(
            id = "google/gemini-2.0-flash-001",
            name = "Gemini 2.0 Flash (OpenRouter)",
            provider = "OpenRouter",
            supportsMultimodal = true,
            contextWindowTokens = 1000000,
            description = "Fast, highly capable multimodal model for video and text analysis"
        ),
        ModelSpec(
            id = "openai/gpt-4o-mini",
            name = "GPT-4o Mini (OpenRouter)",
            provider = "OpenRouter",
            supportsMultimodal = true,
            contextWindowTokens = 128000,
            description = "Efficient, cost-conscious model for visual frame analysis"
        ),
        ModelSpec(
            id = "anthropic/claude-3.5-haiku",
            name = "Claude 3.5 Haiku (OpenRouter)",
            provider = "OpenRouter",
            supportsMultimodal = true,
            contextWindowTokens = 200000,
            description = "High accuracy for complex natural language video editing queries"
        )
    )

    val AVAILABLE_GEMINI_MODELS = listOf(
        ModelSpec(
            id = "gemini-2.5-flash",
            name = "Gemini 2.5 Flash Direct",
            provider = "Google Gemini",
            supportsMultimodal = true,
            contextWindowTokens = 1000000,
            description = "Direct Google AI Studio Gemini API integration"
        ),
        ModelSpec(
            id = "gemini-1.5-flash",
            name = "Gemini 1.5 Flash Direct",
            provider = "Google Gemini",
            supportsMultimodal = true,
            contextWindowTokens = 1000000,
            description = "Reliable fallback model for keyframe analysis"
        )
    )
}
