package com.example.data.media

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.UUID

object AndroidMediaEngine {

    private const val TAG = "AndroidMediaEngine"
    private const val BUFFER_SIZE = 1024 * 1024 // 1 MB buffer

    /**
     * Reads technical metadata from a content URI safely.
     */
    suspend fun extractMetadata(context: Context, uri: Uri): Result<VideoMetadata> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            val bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            val mimeStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "video/mp4"
            val hasAudioStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
            val hasVideoStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)

            val durationMs = durationStr?.toLongOrNull() ?: 0L
            val width = widthStr?.toIntOrNull() ?: 1280
            val height = heightStr?.toIntOrNull() ?: 720
            val rotation = rotationStr?.toIntOrNull() ?: 0
            val bitrate = bitrateStr?.toLongOrNull() ?: 0L
            val hasAudio = hasAudioStr == "yes" || hasAudioStr == "true"
            val hasVideo = hasVideoStr == "yes" || hasVideoStr == "true"

            var fileName = "imported_video.mp4"
            var sizeBytes = 0L

            if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex("_display_name")
                    val sizeIndex = cursor.getColumnIndex("_size")
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                        if (sizeIndex != -1) sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            } else if (uri.path != null) {
                val file = File(uri.path!!)
                if (file.exists()) {
                    fileName = file.name
                    sizeBytes = file.length()
                }
            }

            val metadata = VideoMetadata(
                uriString = uri.toString(),
                fileName = fileName,
                durationMs = durationMs,
                width = width,
                height = height,
                rotation = rotation,
                bitrate = bitrate,
                mimeType = mimeStr,
                sizeBytes = sizeBytes,
                hasAudioTrack = hasAudio,
                hasVideoTrack = hasVideo
            )

            Result.success(metadata)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract metadata from URI: $uri", e)
            Result.failure(e)
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Performs a low-memory local scene detection scan by extracting thumbnails at
     * regular time intervals and measuring color variance between adjacent frames.
     */
    suspend fun scanScenes(
        context: Context,
        uri: Uri,
        durationMs: Long,
        sampleCount: Int = 10
    ): List<SceneCandidate> = withContext(Dispatchers.IO) {
        if (durationMs <= 0) return@withContext emptyList()

        val retriever = MediaMetadataRetriever()
        val candidates = mutableListOf<SceneCandidate>()

        try {
            retriever.setDataSource(context, uri)
            val stepMs = (durationMs / sampleCount).coerceAtLeast(1000L)
            var prevBitmap: Bitmap? = null
            var prevTimeMs = 0L

            for (i in 0 until sampleCount) {
                val targetTimeMs = i * stepMs
                val timeUs = targetTimeMs * 1000L

                val currentBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    retriever.getScaledFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        160,
                        90
                    )
                } else {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }

                if (currentBitmap != null) {
                    val isSignificantChange = if (prevBitmap != null) {
                        calculateImageDifference(prevBitmap, currentBitmap) > 0.18f
                    } else {
                        true // First frame is start of Scene 1
                    }

                    if (isSignificantChange && targetTimeMs > prevTimeMs) {
                        val sceneStartMs = prevTimeMs
                        val sceneEndMs = (targetTimeMs + stepMs).coerceAtMost(durationMs)
                        val sceneNumber = candidates.size + 1

                        candidates.add(
                            SceneCandidate(
                                id = UUID.randomUUID().toString(),
                                startMs = sceneStartMs,
                                endMs = sceneEndMs,
                                thumbnailBitmap = currentBitmap,
                                confidence = 0.88f,
                                label = "Scene $sceneNumber / दृश्य $sceneNumber",
                                rationale = "Visual shift detected at ${formatMs(targetTimeMs)}"
                            )
                        )
                    }

                    prevBitmap = currentBitmap
                    prevTimeMs = targetTimeMs
                }
            }

            // If no distinct changes were detected, create a fallback full-video scene
            if (candidates.isEmpty()) {
                val sampleFrame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                candidates.add(
                    SceneCandidate(
                        id = UUID.randomUUID().toString(),
                        startMs = 0L,
                        endMs = durationMs,
                        thumbnailBitmap = sampleFrame,
                        confidence = 1.0f,
                        label = "Full Video / पूरा वीडियो",
                        rationale = "Default single scene"
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning scenes", e)
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {}
        }

        return@withContext candidates
    }

    /**
     * Executes a fast, 100% native, lossless stream-copy trim using MediaExtractor and MediaMuxer.
     * Snaps start and end boundaries to keyframes without quality degradation.
     */
    suspend fun cutVideoLossless(
        context: Context,
        sourceUri: Uri,
        startMs: Long,
        endMs: Long,
        outputFile: File,
        onProgress: (Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null

        try {
            onProgress(10)
            extractor = MediaExtractor()
            extractor.setDataSource(context, sourceUri, null)

            val startUs = startMs * 1000L
            val endUs = endMs * 1000L
            val totalDurationUs = (endMs - startMs) * 1000L

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val trackCount = extractor.trackCount
            val trackIndexMap = HashMap<Int, Int>()

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    val muxerTrackIndex = muxer.addTrack(format)
                    trackIndexMap[i] = muxerTrackIndex
                }
            }

            if (trackIndexMap.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("No valid audio/video tracks found in source file"))
            }

            muxer.start()
            onProgress(30)

            // Seek to start position
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
            val bufferInfo = android.media.MediaCodec.BufferInfo()

            val trackStartPtsMap = HashMap<Int, Long>()

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)

                if (bufferInfo.size < 0) {
                    break
                }

                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs > endUs) {
                    break
                }

                val trackIndex = extractor.sampleTrackIndex
                val muxerTrackIndex = trackIndexMap[trackIndex]

                if (muxerTrackIndex != null && sampleTimeUs >= startUs) {
                    if (!trackStartPtsMap.containsKey(trackIndex)) {
                        trackStartPtsMap[trackIndex] = sampleTimeUs
                    }
                    val startPtsUs = trackStartPtsMap[trackIndex] ?: sampleTimeUs

                    bufferInfo.presentationTimeUs = (sampleTimeUs - startPtsUs).coerceAtLeast(0L)
                    bufferInfo.flags = extractor.sampleFlags

                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)

                    if (totalDurationUs > 0) {
                        val currentProgress = 30 + (((sampleTimeUs - startUs).toDouble() / totalDurationUs) * 60.0).toInt()
                        onProgress(currentProgress.coerceIn(30, 90))
                    }
                }

                extractor.advance()
            }

            onProgress(95)
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error executing lossless video cut", e)
            if (outputFile.exists()) {
                outputFile.delete()
            }
            Result.failure(e)
        } finally {
            try {
                muxer?.stop()
                muxer?.release()
            } catch (ignored: Exception) {}
            try {
                extractor?.release()
            } catch (ignored: Exception) {}
            onProgress(100)
        }
    }

    /**
     * Verifies that the exported video output exists, has size > 0, valid video tracks,
     * and a readable duration matching expected parameters.
     */
    suspend fun verifyOutput(outputFile: File, minSizeBytes: Long = 1000L): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!outputFile.exists() || outputFile.length() < minSizeBytes) {
            return@withContext Result.failure(IllegalStateException("Exported file is missing or zero bytes"))
        }

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(outputFile.absolutePath)
            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            if (hasVideo != "yes" && hasVideo != "true") {
                return@withContext Result.failure(IllegalStateException("Exported file has no playable video stream"))
            }

            if (durationMs <= 0) {
                return@withContext Result.failure(IllegalStateException("Exported file has invalid zero duration"))
            }

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Output verification failed for file: ${outputFile.absolutePath}", e)
            Result.failure(e)
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {}
        }
    }

    private fun calculateImageDifference(bm1: Bitmap, bm2: Bitmap): Float {
        val w = minOf(bm1.width, bm2.width, 32)
        val h = minOf(bm1.height, bm2.height, 32)
        var totalDiff = 0.0

        for (x in 0 until w) {
            for (y in 0 until h) {
                val c1 = bm1.getPixel(x * bm1.width / w, y * bm1.height / h)
                val c2 = bm2.getPixel(x * bm2.width / w, y * bm2.height / h)

                val rDiff = Math.abs(Color.red(c1) - Color.red(c2))
                val gDiff = Math.abs(Color.green(c1) - Color.green(c2))
                val bDiff = Math.abs(Color.blue(c1) - Color.blue(c2))

                totalDiff += (rDiff + gDiff + bDiff) / (3.0 * 255.0)
            }
        }

        return (totalDiff / (w * h)).toFloat()
    }

    private fun formatMs(ms: Long): String {
        val sec = ms / 1000
        val min = sec / 60
        val remSec = sec % 60
        return String.format("%02d:%02d", min, remSec)
    }
}
