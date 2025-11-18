package edu.capstone.navisight.caregiver.ui.feature_map

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


private val PinVioletColor = Color(0xFF6041EC)


private val OuterPinSize = 17.dp
private val MiddlePinSize = 16.5.dp
private val InnerPinSize = 14.dp


private const val PulseDurationMs = 1200
private const val PulseInitialScale = 1f
private const val PulseTargetScale = 3f
private const val PulseInitialAlpha = 0.7f
private const val PulseTargetAlpha = 0f


@Composable
fun LocatorPin() {
    Box(contentAlignment = Alignment.Center) {
        PulsingGlow(
            modifier = Modifier.size(OuterPinSize)
        )
        StaticPin(
            modifier = Modifier.size(OuterPinSize)
        )
    }
}

@Composable
private fun PulsingGlow(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = PulseInitialScale,
        targetValue = PulseTargetScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = PulseDurationMs, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = PulseInitialAlpha,
        targetValue = PulseTargetAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = PulseDurationMs, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = modifier
            .scale(pulseScale)
            .background(
                color = PinVioletColor.copy(alpha = pulseAlpha),
                shape = CircleShape
            )
    )
}

@Composable
private fun StaticPin(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(PinVioletColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(MiddlePinSize)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(InnerPinSize)
                    .background(PinVioletColor, CircleShape)
            )
        }
    }
}