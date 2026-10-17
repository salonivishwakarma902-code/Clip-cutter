package com.example.ui.components

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.media.VideoMetadata
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.NeonCyanPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun VideoPlayerCard(
    metadata: VideoMetadata?,
    selectionStartMs: Long,
    selectionEndMs: Long,
    currentPlaybackMs: Long,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onSeekTo: (Long) -> Unit
) {
    val context = LocalContext.current
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(1.dp, BorderHighlight, RoundedCornerShape(16.dp))
            .testTag("video_player_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (metadata != null) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                val uri = Uri.parse(metadata.uriString)
                                setVideoURI(uri)
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    seekTo(selectionStartMs.toInt())
                                }
                                videoViewRef = this
                            }
                        },
                        update = { view ->
                            val uri = Uri.parse(metadata.uriString)
                            if (view.tag != metadata.uriString) {
                                view.setVideoURI(uri)
                                view.tag = metadata.uriString
                            }

                            if (isPlaying) {
                                if (!view.isPlaying) view.start()
                            } else {
                                if (view.isPlaying) view.pause()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "No Video Selected / कोई वीडियो चयनित नहीं",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // Controls Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onTogglePlay,
                    enabled = metadata != null,
                    modifier = Modifier.testTag("play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = NeonCyanPrimary
                    )
                }

                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = metadata?.fileName ?: "No file loaded",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Selection / चयन: ${formatMs(selectionStartMs)} - ${formatMs(selectionEndMs)} (Duration: ${formatMs(selectionEndMs - selectionStartMs)})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    val remMs = ms % 1000
    return String.format("%02d:%02d.%03d", min, sec, remMs)
}
