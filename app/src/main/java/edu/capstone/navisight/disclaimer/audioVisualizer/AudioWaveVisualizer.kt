package edu.capstone.navisight.disclaimer.audioVisualizer

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun AudioWaveVisualizer(
    volumeLevel: Float,
    modifier: Modifier = Modifier
) {
    // Light-mode gradient colors
    val gradientStart = Color(0xFFB644F1).copy(alpha = 0.7f) // slightly lighter
    val gradientEnd = Color(0xFF6041EC).copy(alpha = 0.6f)

    // Core scale animation based on volume
    val animatedScale by animateFloatAsState(
        targetValue = 1f + (volumeLevel * 0.5f),
        animationSpec = tween(durationMillis = 200, easing = LinearEasing)
    )

    // Wave animation
    val infiniteTransition = rememberInfiniteTransition()
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val baseRadius = size.minDimension / 6 * animatedScale

        // Subtle outer wave
        val waveRadius = baseRadius * (1f + waveProgress * 0.6f)
        val strokeWidth = 5f * (1f - waveProgress.coerceIn(0f, 1f))

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    gradientStart.copy(alpha = 0.3f),
                    gradientEnd.copy(alpha = 0.2f),
                    Color.Transparent
                ),
                center = center,
                radius = waveRadius * 1.5f
            ),
            radius = waveRadius,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        // Core glowing orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    gradientEnd.copy(alpha = 0.8f),
                    gradientStart.copy(alpha = 0.6f),
                    Color.Transparent
                ),
                center = center,
                radius = baseRadius * 1.5f
            ),
            radius = baseRadius,
            center = center
        )
    }
}
