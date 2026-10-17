package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.media.SceneCandidate
import com.example.data.media.VideoMetadata
import com.example.ui.components.StatusCard
import com.example.ui.components.TimelineScrubber
import com.example.ui.components.VideoPlayerCard
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeonCyanPrimary
import com.example.ui.theme.TextPrimary

@Composable
fun TimelineScreen(
    metadata: VideoMetadata?,
    selectionStartMs: Long,
    selectionEndMs: Long,
    currentPlaybackMs: Long,
    isPlaying: Boolean,
    scenes: List<SceneCandidate>,
    isExporting: Boolean,
    exportProgress: Int,
    exportStageMessage: String,
    exportSuccessUri: String?,
    exportErrorMessage: String?,
    showExportDialog: Boolean,
    onTogglePlay: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSelectionChange: (Long, Long) -> Unit,
    onExportClip: () -> Unit,
    onDismissStatus: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
            .testTag("timeline_screen")
    ) {
        VideoPlayerCard(
            metadata = metadata,
            selectionStartMs = selectionStartMs,
            selectionEndMs = selectionEndMs,
            currentPlaybackMs = currentPlaybackMs,
            isPlaying = isPlaying,
            onTogglePlay = onTogglePlay,
            onSeekTo = onSeekTo
        )

        TimelineScrubber(
            totalDurationMs = metadata?.durationMs ?: 0L,
            selectionStartMs = selectionStartMs,
            selectionEndMs = selectionEndMs,
            scenes = scenes,
            onSelectionChange = onSelectionChange
        )

        // Primary Action: Export Trimmed Clip
        Button(
            onClick = onExportClip,
            enabled = metadata != null && !isExporting,
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldSuccess,
                contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(52.dp)
                .testTag("export_clip_button")
        ) {
            Text(
                text = "Export Trimmed Clip / क्लिप काटो और निर्यात करो",
                style = MaterialTheme.typography.labelLarge
            )
        }

        if (showExportDialog || isExporting || exportSuccessUri != null || exportErrorMessage != null) {
            StatusCard(
                isExporting = isExporting,
                progress = exportProgress,
                stageMessage = exportStageMessage,
                successUri = exportSuccessUri,
                errorMessage = exportErrorMessage,
                onDismiss = onDismissStatus
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
