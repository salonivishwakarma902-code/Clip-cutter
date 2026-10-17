package com.example.data.ai

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ClipProposal(
    val id: String = "",
    val title: String = "",
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val rationale: String = "",
    val confidence: Float = 0.90f
) {
    fun formattedTimeRange(): String {
        val startSec = startMs / 1000
        val startMin = startSec / 60
        val startRemSec = startSec % 60

        val endSec = endMs / 1000
        val endMin = endSec / 60
        val endRemSec = endSec % 60

        return String.format("%02d:%02d - %02d:%02d", startMin, startRemSec, endMin, endRemSec)
    }
}

@JsonClass(generateAdapter = true)
data class EditPlan(
    val planId: String = "",
    val taskDescription: String = "",
    val suggestedClips: List<ClipProposal> = emptyList(),
    val requiresClarification: Boolean = false,
    val clarificationPrompt: String? = null
)

data class ChatMessage(
    val id: String,
    val sender: ChatSender,
    val text: String,
    val editPlan: EditPlan? = null,
    val providerUsed: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ChatSender {
    USER,
    AI,
    SYSTEM
}
