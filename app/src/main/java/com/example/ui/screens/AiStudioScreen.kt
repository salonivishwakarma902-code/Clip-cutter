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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.ai.ChatMessage
import com.example.data.ai.ClipProposal
import com.example.data.db.CutJobEntity
import com.example.data.media.VideoMetadata
import com.example.ui.components.AiOrbVisualizer
import com.example.ui.components.VideoPlayerCard
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeonCyanPrimary
import com.example.ui.theme.NeonPurpleAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiStudioScreen(
    metadata: VideoMetadata?,
    chatMessages: List<ChatMessage>,
    recentJobs: List<CutJobEntity>,
    isAiThinking: Boolean,
    isExporting: Boolean,
    exportProgress: Int,
    exportStageMessage: String,
    onImportVideo: (Uri) -> Unit,
    onSendPrompt: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onExportProposal: (ClipProposal) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val context = LocalContext.current

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onImportVideo(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("ai_studio_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AI Voice Video Cutter",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "बोल कर वीडियो काटो • Speak or type to auto-trim",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeonCyanPrimary
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("settings_top_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextPrimary
                )
            }
        }

        // Active Export Progress Bar
        if (isExporting) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .border(1.dp, NeonCyanPrimary, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Auto-Trimming Clip / कटिंग जारी...",
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonCyanPrimary
                        )
                        Text(
                            text = "$exportProgress%",
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonCyanPrimary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { exportProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        color = NeonCyanPrimary,
                        trackColor = BorderHighlight
                    )
                    Text(
                        text = exportStageMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Video Import / Active Video Card
            item {
                if (metadata == null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NeonCyanPrimary, RoundedCornerShape(20.dp))
                            .clickable {
                                pickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            }
                            .testTag("import_hero_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(ElevatedSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideoLibrary,
                                    contentDescription = "Import Video",
                                    tint = NeonCyanPrimary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Select Video to Auto-Cut / वीडियो चुनें",
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary
                            )

                            Text(
                                text = "Tap here to choose any MP4, MOV, or video file",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    VideoPlayerCard(
                        metadata = metadata,
                        selectionStartMs = 0L,
                        selectionEndMs = metadata.durationMs,
                        currentPlaybackMs = 0L,
                        isPlaying = false,
                        onTogglePlay = {},
                        onSeekTo = {}
                    )
                }
            }

            // Central Glowing AI Voice Orb Visualizer
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderHighlight, RoundedCornerShape(20.dp))
                        .testTag("ai_voice_orb_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AiOrbVisualizer(isThinking = isAiThinking)

                        Text(
                            text = if (isAiThinking) "Analyzing video & auto-cutting... / कटिंग चालू है..." else "Era AI Voice Assistant",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isAiThinking) NeonPurpleAccent else NeonCyanPrimary,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        // Quick AI Voice Presets
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VoicePresetPill("Cut all key scenes / मुख्य दृश्य काटो") {
                                onSendPrompt("Extract all distinct scenes and key moments from this video")
                            }
                            VoicePresetPill("30s Highlights / 30 सेकंड हाइलाइट") {
                                onSendPrompt("Find and cut the best 30-second highlight moment")
                            }
                            VoicePresetPill("Split Video / वीडियो अलग करो") {
                                onSendPrompt("Split this video into 3 equal engaging parts")
                            }
                        }
                    }
                }
            }

            // AI Generated Clips Feed
            item {
                Text(
                    text = "AI Processed Clips / तैयार किए गए क्लिप्स",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }

            if (recentJobs.isEmpty()) {
                item {
                    Text(
                        text = "No clips generated yet. Speak or type a prompt above to auto-cut!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(recentJobs) { job ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderHighlight, RoundedCornerShape(16.dp))
                            .testTag("clip_card_${job.id}"),
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
                                    text = "Ready / तैयार",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldSuccess
                                )
                            }

                            Text(
                                text = "Duration / सीमा: ${formatMs(job.clipStartMs)} - ${formatMs(job.clipEndMs)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Voice & Text Input Floating Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .border(1.dp, BorderHighlight, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Voice Mic Button
                IconButton(
                    onClick = {
                        // Quick Voice Command Trigger
                        if (metadata != null) {
                            onSendPrompt("Analyze and cut the most engaging parts of this video automatically")
                        } else {
                            pickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        }
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(NeonPurpleAccent)
                        .testTag("voice_mic_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            "बोलें या टाइप करें (e.g., Cut funny moments)...",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_studio_text_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyanPrimary,
                        unfocusedBorderColor = BorderHighlight,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendPrompt(inputText.trim())
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !isAiThinking && metadata != null,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank() && metadata != null) NeonCyanPrimary else ElevatedSurface)
                        .testTag("ai_studio_send_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank() && metadata != null) MaterialTheme.colorScheme.onPrimary else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun VoicePresetPill(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ElevatedSurface)
            .border(1.dp, BorderHighlight, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = NeonCyanPrimary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = text, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
        }
    }
}

private fun formatMs(ms: Long): String {
    val sec = ms / 1000
    val min = sec / 60
    val remSec = sec % 60
    return String.format("%02d:%02d", min, remSec)
}
