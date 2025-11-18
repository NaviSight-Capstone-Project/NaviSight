package edu.capstone.navisight.caregiver.ui.feature_geofence

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable

@Composable
fun GeofenceRadiusPin(radiusInPixels: Float) {
    val outerCircleFillColor = Color(0x330060FF)
    val innerCircleColor = Color(0xFF0060FF)

    val infiniteTransition = rememberInfiniteTransition()

    val pulseStrokeWidth by infiniteTransition.animateValue(
        initialValue = 2.dp,
        targetValue = 6.dp,
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val diameterInDp = with(LocalDensity.current) {
        (radiusInPixels * 2).toDp()
    }

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(diameterInDp)
                .background(outerCircleFillColor, CircleShape)
                .border(
                    width = pulseStrokeWidth,
                    color = Color.Black.copy(alpha = pulseAlpha),
                    shape = CircleShape
                )
        )

        // Inner solid circle
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(innerCircleColor, CircleShape)
        )
    }
}

@Composable
fun PositionedGeofencePin(
    offset: IntOffset,
    radius: Float,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)

                val x = offset.x - placeable.width / 2
                val y = offset.y - placeable.height / 2

                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(x, y)
                }
            }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        GeofenceRadiusPin(radiusInPixels = radius)
    }
}