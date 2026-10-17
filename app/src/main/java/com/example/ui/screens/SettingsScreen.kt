package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.security.EncryptedKeyStorage
import com.example.data.security.EncryptedKeyStorage.ProviderPriority
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeonCyanPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    keyStorage: EncryptedKeyStorage,
    onSaveSettings: (
        openRouterKey: String,
        geminiKey: String,
        priority: ProviderPriority,
        frameAccurate: Boolean,
        maxDuration: Int,
        consent: Boolean
    ) -> Unit
) {
    var openRouterKey by remember { mutableStateOf(keyStorage.openRouterApiKey) }
    var geminiKey by remember { mutableStateOf(keyStorage.geminiApiKey) }
    var priority by remember { mutableStateOf(keyStorage.providerPriority) }
    var frameAccurate by remember { mutableStateOf(keyStorage.frameAccurateReencode) }
    var maxDuration by remember { mutableStateOf(keyStorage.maxDurationMinutes) }
    var consent by remember { mutableStateOf(keyStorage.cloudConsentGranted) }
    var showSavedToast by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen")
    ) {
        Text(
            text = "API Keys & Preferences / सेटिंग्स",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
        Text(
            text = "Configure user-supplied credentials, provider priority, and cutting accuracy.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // OpenRouter Key Field
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .border(1.dp, BorderHighlight, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "OpenRouter API Key (Primary) / ओपनराउटर कुंजी",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonCyanPrimary
                )
                OutlinedTextField(
                    value = openRouterKey,
                    onValueChange = { openRouterKey = it },
                    visualTransformation = PasswordVisualTransformation(),
                    placeholder = { Text("sk-or-v1-...", color = TextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag("openrouter_key_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyanPrimary,
                        unfocusedBorderColor = BorderHighlight
                    ),
                    singleLine = true
                )
            }
        }

        // Gemini Key Field
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .border(1.dp, BorderHighlight, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Google Gemini API Key (Fallback) / जेमिनी कुंजी",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonCyanPrimary
                )
                OutlinedTextField(
                    value = geminiKey,
                    onValueChange = { geminiKey = it },
                    visualTransformation = PasswordVisualTransformation(),
                    placeholder = { Text("AIzaSy...", color = TextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag("gemini_key_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyanPrimary,
                        unfocusedBorderColor = BorderHighlight
                    ),
                    singleLine = true
                )
            }
        }

        // Provider Priority Selection
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .border(1.dp, BorderHighlight, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Provider Priority / प्राथमिकता",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )

                PriorityRadioRow(
                    title = "OpenRouter First → Gemini Fallback",
                    selected = priority == ProviderPriority.OPENROUTER_FIRST,
                    onSelect = { priority = ProviderPriority.OPENROUTER_FIRST }
                )

                PriorityRadioRow(
                    title = "Gemini First → OpenRouter Fallback",
                    selected = priority == ProviderPriority.GEMINI_FIRST,
                    onSelect = { priority = ProviderPriority.GEMINI_FIRST }
                )

                PriorityRadioRow(
                    title = "Local Scene Scan Only (Offline Mode)",
                    selected = priority == ProviderPriority.LOCAL_ONLY,
                    onSelect = { priority = ProviderPriority.LOCAL_ONLY }
                )
            }
        }

        // Cutting Strategy Toggles
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .border(1.dp, BorderHighlight, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Frame-Accurate Re-encode / सटीक कटिंग",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "When enabled, re-encodes video for frame-perfect cut accuracy. Default is instant stream copy.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = frameAccurate,
                        onCheckedChange = { frameAccurate = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyanPrimary,
                            checkedTrackColor = CharcoalSurface
                        ),
                        modifier = Modifier.testTag("frame_accurate_switch")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Explicit Keyframe Upload Consent",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "Allow sending sampled low-res keyframes to AI provider for prompt analysis.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = consent,
                        onCheckedChange = { consent = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyanPrimary,
                            checkedTrackColor = CharcoalSurface
                        ),
                        modifier = Modifier.testTag("consent_switch")
                    )
                }
            }
        }

        // Save Action Button
        Button(
            onClick = {
                onSaveSettings(openRouterKey, geminiKey, priority, frameAccurate, maxDuration, consent)
                showSavedToast = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyanPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(50.dp)
                .testTag("save_settings_button")
        ) {
            Text("Save Settings / सहेजें", style = MaterialTheme.typography.labelLarge)
        }

        if (showSavedToast) {
            Text(
                text = "✓ Settings saved securely! API keys redacted in logs.",
                style = MaterialTheme.typography.bodyMedium,
                color = EmeraldSuccess,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun PriorityRadioRow(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = NeonCyanPrimary)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
