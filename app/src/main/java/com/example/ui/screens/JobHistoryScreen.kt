package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.db.CutJobEntity
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeonCyanPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun JobHistoryScreen(
    jobs: List<CutJobEntity>,
    onDeleteJob: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("job_history_screen")
    ) {
        Text(
            text = "Export Job History / निर्यात इतिहास",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
        Text(
            text = "Durable Room database state tracking for crash recovery and verified exports.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        if (jobs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No job history recorded yet. Export your first clip!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(jobs) { job ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderHighlight, RoundedCornerShape(16.dp))
                            .testTag("history_item_${job.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = job.outputFileName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
                                )

                                Text(
                                    text = job.status.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = when (job.status.name) {
                                        "COMPLETED" -> EmeraldSuccess
                                        "FAILED" -> CrimsonError
                                        else -> AmberWarning
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Source: ${job.sourceFileName} (${formatMs(job.clipStartMs)} - ${formatMs(job.clipEndMs)})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )

                            if (!job.currentStageMessage.isBlank()) {
                                Text(
                                    text = job.currentStageMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Size: ${formatBytes(job.outputSizeBytes)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyanPrimary
                                )

                                IconButton(
                                    onClick = { onDeleteJob(job.id) },
                                    modifier = Modifier.testTag("delete_job_${job.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Job",
                                        tint = CrimsonError
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val sec = ms / 1000
    val min = sec / 60
    val remSec = sec % 60
    return String.format("%02d:%02d", min, remSec)
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return String.format("%.2f MB", mb)
}
