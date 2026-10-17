package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyanPrimary
import com.example.ui.theme.NeonPurpleAccent

@Composable
fun AiOrbVisualizer(
    isThinking: Boolean,
    modifier: Modifier = Modifier
) {
    val scaleAnim = remember { Animatable(1f) }
    val rotateAnim = remember { Animatable(0f) }

    LaunchedEffect(isThinking) {
        if (isThinking) {
            scaleAnim.animateTo(
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            scaleAnim.snapTo(1f)
        }
    }

    LaunchedEffect(isThinking) {
        if (isThinking) {
            rotateAnim.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }

    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(100.dp)) {
            val radius = (size.minDimension / 2) * scaleAnim.value

            // Background Outer Glow Ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonCyanPrimary.copy(alpha = 0.5f),
                        NeonPurpleAccent.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                ),
                radius = radius * 1.3f
            )

            // Inner Orb Circle
            drawCircle(
                brush = Brush.linearGradient(
                    colors = if (isThinking) {
                        listOf(NeonPurpleAccent, NeonCyanPrimary)
                    } else {
                        listOf(NeonCyanPrimary, NeonPurpleAccent)
                    }
                ),
                radius = radius * 0.7f
            )

            // Rotating Pulse Stroke
            drawCircle(
                color = NeonCyanPrimary,
                radius = radius * 0.9f,
                style = Stroke(width = 4.dp.toPx())
            )
        }
    }
}
