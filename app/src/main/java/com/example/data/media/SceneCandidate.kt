package com.example.data.media

import android.graphics.Bitmap

data class SceneCandidate(
    val id: String,
    val startMs: Long,
    val endMs: Long,
    val thumbnailBitmap: Bitmap? = null,
    val thumbnailPath: String? = null,
    val confidence: Float = 0.85f,
    val label: String = "",
    val rationale: String = ""
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
