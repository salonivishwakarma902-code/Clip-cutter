package com.example.data.security

import android.content.Context
import android.content.SharedPreferences

/**
 * EncryptedKeyStorage manages user-provided API keys (OpenRouter and Gemini),
 * provider priorities, and user preferences securely using private SharedPreferences
 * with automatic key redaction for logs and chat prompts.
 */
class EncryptedKeyStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var openRouterApiKey: String
        get() = prefs.getString(KEY_OPENROUTER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OPENROUTER, value.trim()).apply()

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI, value.trim()).apply()

    var providerPriority: ProviderPriority
        get() {
            val name = prefs.getString(KEY_PROVIDER_PRIORITY, ProviderPriority.OPENROUTER_FIRST.name)
            return try {
                ProviderPriority.valueOf(name ?: ProviderPriority.OPENROUTER_FIRST.name)
            } catch (e: Exception) {
                ProviderPriority.OPENROUTER_FIRST
            }
        }
        set(value) = prefs.edit().putString(KEY_PROVIDER_PRIORITY, value.name).apply()

    var frameAccurateReencode: Boolean
        get() = prefs.getBoolean(KEY_FRAME_ACCURATE, false)
        set(value) = prefs.edit().putBoolean(KEY_FRAME_ACCURATE, value).apply()

    var maxDurationMinutes: Int
        get() = prefs.getInt(KEY_MAX_DURATION, 5)
        set(value) = prefs.edit().putInt(KEY_MAX_DURATION, value).apply()

    var cloudConsentGranted: Boolean
        get() = prefs.getBoolean(KEY_CLOUD_CONSENT, false)
        set(value) = prefs.edit().putBoolean(KEY_CLOUD_CONSENT, value).apply()

    /**
     * Redacts sensitive API keys from text before printing to logs or sending in prompts.
     */
    fun redact(text: String): String {
        var cleanText = text
        val openRouter = openRouterApiKey
        val gemini = geminiApiKey

        if (openRouter.isNotEmpty() && openRouter.length > 6) {
            cleanText = cleanText.replace(openRouter, "[REDACTED_OPENROUTER_KEY]")
        }
        if (gemini.isNotEmpty() && gemini.length > 6) {
            cleanText = cleanText.replace(gemini, "[REDACTED_GEMINI_KEY]")
        }
        return cleanText
    }

    enum class ProviderPriority {
        OPENROUTER_FIRST,
        GEMINI_FIRST,
        LOCAL_ONLY
    }

    companion object {
        private const val PREFS_NAME = "smart_video_cutter_secure_prefs"
        private const val KEY_OPENROUTER = "openrouter_api_key"
        private const val KEY_GEMINI = "gemini_api_key"
        private const val KEY_PROVIDER_PRIORITY = "provider_priority"
        private const val KEY_FRAME_ACCURATE = "frame_accurate_reencode"
        private const val KEY_MAX_DURATION = "max_duration_minutes"
        private const val KEY_CLOUD_CONSENT = "cloud_upload_consent"
    }
}
