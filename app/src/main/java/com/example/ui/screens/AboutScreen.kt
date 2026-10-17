package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.NeonCyanPrimary
import com.example.ui.theme.NeonPurpleAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("about_screen")
    ) {
        Text(
            text = "AI Smart Video Cutter",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
        Text(
            text = "Version 1.0 • Native Android Architecture / नेटिव आर्किटेक्चर",
            style = MaterialTheme.typography.bodyMedium,
            color = NeonCyanPrimary,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

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
                    text = "Core Engine / मुख्य विशेषताएं",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonPurpleAccent
                )
                Spacer(modifier = Modifier.height(8.dp))
                FeatureBullet("Native Android Media Engine", "Zero external binary dependencies. Uses MediaExtractor, MediaMuxer, and MediaMetadataRetriever for instant lossless stream extraction.")
                FeatureBullet("Dual AI Provider Fallback", "OpenRouter primary with automatic quota-aware failover to Google Gemini API (gemini-2.5-flash & gemini-4-argon high thinking).")
                FeatureBullet("Privacy & Secret Security", "Encrypted key storage with automatic log redaction. Videos stay 100% on-device unless explicit keyframe consent is granted.")
                FeatureBullet("Durable Room Job Recovery", "Every export status (QUEUED, CUTTING, VERIFYING, COMPLETED) is persisted in SQLite for crash resilience on 4 GB RAM devices.")
            }
        }

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
                    text = "Open Source & Licenses",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Built with Kotlin, Jetpack Compose, AndroidX Media, Room, OkHttp, and Moshi under Apache 2.0 / MIT licenses.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun FeatureBullet(title: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(text = "• $title", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Text(text = desc, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.padding(start = 12.dp, top = 2.dp))
    }
}
