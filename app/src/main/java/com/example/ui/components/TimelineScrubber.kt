package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.media.SceneCandidate
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.NeonCyanPrimary
import com.example.ui.theme.NeonPurpleAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TimelineScrubber(
    totalDurationMs: Long,
    selectionStartMs: Long,
    selectionEndMs: Long,
    scenes: List<SceneCandidate>,
    onSelectionChange: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val duration = totalDurationMs.coerceAtLeast(1000L).toFloat()
    val startVal = selectionStartMs.toFloat().coerceIn(0f, duration)
    val endVal = selectionEndMs.toFloat().coerceIn(startVal + 500f, duration)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(1.dp, BorderHighlight, RoundedCornerShape(16.dp))
            .testTag("timeline_scrubber_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scene Timeline / समयरेखा",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "${formatMs(selectionStartMs)} ── ${formatMs(selectionEndMs)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonCyanPrimary
                )
            }

            // Dual thumb RangeSlider for Start & End cut boundaries
            RangeSlider(
                value = startVal..endVal,
                onValueChange = { range ->
                    onSelectionChange(range.start.toLong(), range.endInclusive.toLong())
                },
                valueRange = 0f..duration,
                colors = SliderDefaults.colors(
                    activeTrackColor = NeonCyanPrimary,
                    inactiveTrackColor = ElevatedSurface,
                    thumbColor = NeonCyanPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("trimmer_range_slider")
            )

            // Detected Scene Candidate Pills
            if (scenes.isNotEmpty()) {
                Text(
                    text = "Detected Scenes (Tap to Snap) / पहचाने गए दृश्य:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    scenes.forEach { scene ->
                        val isSelected = selectionStartMs <= scene.startMs && selectionEndMs >= scene.endMs
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonPurpleAccent else ElevatedSurface)
                                .border(
                                    1.dp,
                                    if (isSelected) NeonCyanPrimary else BorderHighlight,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    onSelectionChange(scene.startMs, scene.endMs)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("scene_pill_${scene.id}")
                        ) {
                            Text(
                                text = "${scene.label}: ${scene.formattedTimeRange()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) TextPrimary else TextSecondary
                            )
                        }
                    }
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
