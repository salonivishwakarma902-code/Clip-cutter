package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.db.CutJobEntity
import com.example.data.media.VideoMetadata
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeonCyanPrimary
import com.example.ui.theme.NeonPurpleAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    metadata: VideoMetadata?,
    recentJobs: List<CutJobEntity>,
    onVideoImported: (Uri) -> Unit,
    onNavigateToTimeline: () -> Unit,
    onNavigateToChat: () -> Unit
) {
    val context = LocalContext.current

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onVideoImported(uri)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 16.dp)
            .testTag("home_screen")
    ) {
        // Video Import CTA Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, NeonCyanPrimary, RoundedCornerShape(20.dp))
                    .clickable {
                        pickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    }
                    .testTag("import_video_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(ElevatedSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = "Import Video",
                            tint = NeonCyanPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Import Video / वीडियो चुनें",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )

                    Text(
                        text = "Supports MP4, MOV, MKV up to 5 minutes default",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Button(
                        onClick = {
                            pickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyanPrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .testTag("select_video_button")
                    ) {
                        Text("Select File / फाइल चुनें")
                    }
                }
            }
        }

        // Active Metadata Preflight Card
        if (metadata != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .border(1.dp, BorderHighlight, RoundedCornerShape(16.dp))
                        .testTag("metadata_preflight_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Active Video Metadata / वीडियो विवरण",
                            style = MaterialTheme.typography.titleLarge,
                            color = NeonCyanPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        MetadataRow("File Name / नाम", metadata.fileName)
                        MetadataRow("Duration / अवधि", metadata.formattedDuration())
                        MetadataRow("Resolution / पिक्सेल", "${metadata.width} x ${metadata.height}")
                        MetadataRow("File Size / आकार", metadata.formattedSize())
                        MetadataRow("Mime Type / टाइप", metadata.mimeType)
                        MetadataRow("Audio Stream / ऑडियो", if (metadata.hasAudioTrack) "Present / मौजूद" else "None / कोई नहीं")

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onNavigateToTimeline,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyanPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("go_to_timeline_button")
                            ) {
                                Text("Edit Timeline")
                            }

                            Button(
                                onClick = onNavigateToChat,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurpleAccent),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("go_to_ai_chat_button")
                            ) {
                                Text("AI Chat")
                            }
                        }
                    }
                }
            }
        }

        // Recent Jobs List
        item {
            Text(
                text = "Recent Export Jobs / हाल के निर्यातित कार्य",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        if (recentJobs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No export jobs yet. Import a video and trim your first clip!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            items(recentJobs.take(5)) { job ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .border(1.dp, BorderHighlight, RoundedCornerShape(12.dp))
                        .testTag("recent_job_card_${job.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = job.outputFileName,
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = "Status: ${job.status.name} • Range: ${formatMs(job.clipStartMs)} - ${formatMs(job.clipEndMs)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (job.status.name == "COMPLETED") EmeraldSuccess else TextSecondary
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Playback",
                            tint = NeonCyanPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}

private fun formatMs(ms: Long): String {
    val sec = ms / 1000
    val min = sec / 60
    val remSec = sec % 60
    return String.format("%02d:%02d", min, remSec)
}
