package com.example.data.media

data class VideoMetadata(
    val uriString: String,
    val fileName: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotation: Int,
    val bitrate: Long,
    val mimeType: String,
    val sizeBytes: Long,
    val hasAudioTrack: Boolean,
    val hasVideoTrack: Boolean,
    val videoCodec: String? = null
) {
    fun formattedDuration(): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val millis = durationMs % 1000
        return String.format("%02d:%02d.%03d", minutes, seconds, millis)
    }

    fun formattedSize(): String {
        val mb = sizeBytes / (1024.0 * 1024.0)
        return String.format("%.2f MB", mb)
    }
}
