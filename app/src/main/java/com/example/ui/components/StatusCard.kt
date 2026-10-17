package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeonCyanPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StatusCard(
    isExporting: Boolean,
    progress: Int,
    stageMessage: String,
    successUri: String?,
    errorMessage: String?,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(
                1.dp,
                when {
                    successUri != null -> EmeraldSuccess
                    errorMessage != null -> CrimsonError
                    else -> BorderHighlight
                },
                RoundedCornerShape(16.dp)
            )
            .testTag("status_card"),
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
                    text = when {
                        successUri != null -> "Export Verified! / निर्यात सत्यापित!"
                        errorMessage != null -> "Export Failed / निर्यात विफल"
                        else -> "Exporting Clip / क्लिप निर्यात हो रहा है..."
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = when {
                        successUri != null -> EmeraldSuccess
                        errorMessage != null -> CrimsonError
                        else -> TextPrimary
                    }
                )

                if (isExporting) {
                    Text(
                        text = "$progress%",
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonCyanPrimary
                    )
                }
            }

            if (isExporting) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    color = NeonCyanPrimary,
                    trackColor = BorderHighlight
                )
            }

            Text(
                text = stageMessage.ifBlank {
                    errorMessage ?: (successUri?.let { "Saved to: $it" } ?: "")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (!isExporting && (successUri != null || errorMessage != null)) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (successUri != null) EmeraldSuccess else CrimsonError,
                        contentColor = TextPrimary
                    ),
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp)
                        .testTag("dismiss_status_button")
                ) {
                    Text("OK / ठीक है")
                }
            }
        }
    }
}
