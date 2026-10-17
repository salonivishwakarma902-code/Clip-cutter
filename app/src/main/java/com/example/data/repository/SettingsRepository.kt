package com.example.data.repository

import com.example.data.security.EncryptedKeyStorage

class SettingsRepository(val keyStorage: EncryptedKeyStorage) {

    fun getOpenRouterKey(): String = keyStorage.openRouterApiKey

    fun setOpenRouterKey(key: String) {
        keyStorage.openRouterApiKey = key
    }

    fun getGeminiKey(): String = keyStorage.geminiApiKey

    fun setGeminiKey(key: String) {
        keyStorage.geminiApiKey = key
    }

    fun getProviderPriority(): EncryptedKeyStorage.ProviderPriority = keyStorage.providerPriority

    fun setProviderPriority(priority: EncryptedKeyStorage.ProviderPriority) {
        keyStorage.providerPriority = priority
    }

    fun isFrameAccurate(): Boolean = keyStorage.frameAccurateReencode

    fun setFrameAccurate(enabled: Boolean) {
        keyStorage.frameAccurateReencode = enabled
    }

    fun getMaxDurationMinutes(): Int = keyStorage.maxDurationMinutes

    fun setMaxDurationMinutes(minutes: Int) {
        keyStorage.maxDurationMinutes = minutes
    }

    fun isCloudConsentGranted(): Boolean = keyStorage.cloudConsentGranted

    fun setCloudConsentGranted(granted: Boolean) {
        keyStorage.cloudConsentGranted = granted
    }
}
