package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.Dialog
import com.example.data.security.EncryptedKeyStorage
import com.example.data.security.EncryptedKeyStorage.ProviderPriority
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.NeonCyanPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsDialog(
    keyStorage: EncryptedKeyStorage,
    onDismiss: () -> Unit,
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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeonCyanPrimary, RoundedCornerShape(20.dp))
                .testTag("settings_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "AI Key & Model Settings / सेटिंग्स",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // OpenRouter Key
                Text("OpenRouter API Key:", style = MaterialTheme.typography.bodyMedium, color = NeonCyanPrimary)
                OutlinedTextField(
                    value = openRouterKey,
                    onValueChange = { openRouterKey = it },
                    visualTransformation = PasswordVisualTransformation(),
                    placeholder = { Text("sk-or-v1-...", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth().testTag("openrouter_key_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyanPrimary,
                        unfocusedBorderColor = BorderHighlight
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Gemini Key
                Text("Google Gemini API Key:", style = MaterialTheme.typography.bodyMedium, color = NeonCyanPrimary)
                OutlinedTextField(
                    value = geminiKey,
                    onValueChange = { geminiKey = it },
                    visualTransformation = PasswordVisualTransformation(),
                    placeholder = { Text("AIzaSy...", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth().testTag("gemini_key_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyanPrimary,
                        unfocusedBorderColor = BorderHighlight
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Provider Choice
                Text("Provider Priority:", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = priority == ProviderPriority.OPENROUTER_FIRST,
                        onClick = { priority = ProviderPriority.OPENROUTER_FIRST },
                        colors = RadioButtonDefaults.colors(selectedColor = NeonCyanPrimary)
                    )
                    Text("OpenRouter Primary", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = priority == ProviderPriority.GEMINI_FIRST,
                        onClick = { priority = ProviderPriority.GEMINI_FIRST },
                        colors = RadioButtonDefaults.colors(selectedColor = NeonCyanPrimary)
                    )
                    Text("Gemini Primary", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onSaveSettings(openRouterKey, geminiKey, priority, frameAccurate, maxDuration, consent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyanPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Save & Close / सहेजें", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
