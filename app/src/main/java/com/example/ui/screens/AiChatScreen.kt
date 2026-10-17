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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.ai.ChatMessage
import com.example.data.ai.ChatSender
import com.example.data.ai.ClipProposal
import com.example.data.ai.EditPlan
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.NeonCyanPrimary
import com.example.ui.theme.NeonPurpleAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AiChatScreen(
    messages: List<ChatMessage>,
    isAiThinking: Boolean,
    onSendMessage: (String) -> Unit,
    onApplyProposal: (ClipProposal) -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("ai_chat_screen")
    ) {
        // Chat History List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { msg ->
                ChatMessageBubble(
                    message = msg,
                    onApplyProposal = onApplyProposal
                )
            }

            if (isAiThinking) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = NeonPurpleAccent,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Era AI is analyzing scene frames / एआई विचार कर रहा है...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonPurpleAccent
                        )
                    }
                }
            }
        }

        // Input Field Bar
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
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            "e.g. Find funny 30s moment / 30 सेकंड का मुख्य भाग काटो...",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyanPrimary,
                        unfocusedBorderColor = BorderHighlight,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText.trim())
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !isAiThinking,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) NeonCyanPrimary else ElevatedSurface)
                        .testTag("send_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    onApplyProposal: (ClipProposal) -> Unit
) {
    val isUser = message.sender == ChatSender.USER
    val isSystem = message.sender == ChatSender.SYSTEM

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isUser -> NeonCyanPrimary
                    isSystem -> ElevatedSurface
                    else -> CharcoalSurface
                }
            ),
            modifier = Modifier
                .border(
                    1.dp,
                    if (isUser) NeonCyanPrimary else if (isSystem) BorderHighlight else NeonPurpleAccent,
                    RoundedCornerShape(16.dp)
                )
                .testTag("chat_bubble_${message.id}")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI",
                            tint = NeonPurpleAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = message.providerUsed ?: "AI Engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonPurpleAccent
                        )
                    }
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else TextPrimary
                )

                // Render EditPlan Card if present in response
                message.editPlan?.let { plan ->
                    Spacer(modifier = Modifier.height(10.dp))
                    EditPlanCard(plan = plan, onApplyProposal = onApplyProposal)
                }
            }
        }
    }
}

@Composable
private fun EditPlanCard(
    plan: EditPlan,
    onApplyProposal: (ClipProposal) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .border(1.dp, NeonPurpleAccent, RoundedCornerShape(12.dp))
            .testTag("edit_plan_card_${plan.planId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedSurface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Proposed Edit Plan / सुझाई गई कट योजना",
                style = MaterialTheme.typography.titleLarge,
                color = NeonPurpleAccent
            )
            if (plan.taskDescription.isNotBlank()) {
                Text(
                    text = plan.taskDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )
            }

            plan.suggestedClips.forEach { clip ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, BorderHighlight, RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = clip.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = clip.formattedTimeRange(),
                                style = MaterialTheme.typography.labelLarge,
                                color = NeonCyanPrimary
                            )
                        }

                        if (clip.rationale.isNotBlank()) {
                            Text(
                                text = clip.rationale,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        Button(
                            onClick = { onApplyProposal(clip) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyanPrimary),
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 4.dp)
                                .testTag("apply_clip_button_${clip.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCut,
                                contentDescription = "Cut",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Load into Timeline / टाइमलाइन में लोड करें")
                        }
                    }
                }
            }
        }
    }
}
