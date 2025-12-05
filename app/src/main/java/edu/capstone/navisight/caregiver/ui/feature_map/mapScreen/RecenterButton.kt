package edu.capstone.navisight.caregiver.ui.feature_map.mapScreen

import edu.capstone.navisight.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

private val ButtonSize = 50.dp
private val ShadowElevation = 8.dp
private val ShadowAmbientAlpha = 0.3f
private val ShadowSpotAlpha = 0.5f
private val OuterGlowRadius = 100f
private val InnerGlowRadius = 60f
private val IconTintColor = Color.White
private val IconSize = 24.dp

private val RecenterGradient = listOf(Color(0xFFB644F1), Color(0xFF6041EC))
private val RecenterShadow = Color(0xFF6041EC)

private val GeofenceGradient = listOf(Color(0xFF77F7ED), Color(0xFF4800FF))
private val GeofenceShadow = Color(0xFF4800FF)

@Composable
fun MapControlButtons(
    modifier: Modifier = Modifier,
    onRecenterClick: () -> Unit,
    onGeofenceListClick: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MapActionButton(
            iconResId = R.drawable.ic_geofence_list,
            contentDescription = "Show Geofence List",
            gradientColors = GeofenceGradient,
            baseColor = GeofenceShadow,
            onClick = onGeofenceListClick
        )

        MapActionButton(
            iconResId = R.drawable.ic_recenter,
            contentDescription = "Recenter Map",
            gradientColors = RecenterGradient,
            baseColor = RecenterShadow,
            onClick = onRecenterClick
        )
    }
}

@Composable
private fun MapActionButton(
    iconResId: Int,
    contentDescription: String,
    gradientColors: List<Color>,
    baseColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(ButtonSize)
            .shadow(
                elevation = ShadowElevation,
                shape = CircleShape,
                ambientColor = baseColor.copy(alpha = ShadowAmbientAlpha),
                spotColor = baseColor.copy(alpha = ShadowSpotAlpha)
            )
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(baseColor.copy(alpha = 0.25f), Color.Transparent),
                    radius = OuterGlowRadius
                )
            )
            // Main Gradient
            .background(
                brush = Brush.horizontalGradient(
                    colors = gradientColors
                )
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = baseColor),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Inner Glow Overlay
        Box(
            modifier = Modifier
                .size(ButtonSize)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(baseColor.copy(alpha = 0.1f), Color.Transparent),
                        radius = InnerGlowRadius
                    )
                )
        )

        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = contentDescription,
            tint = IconTintColor,
            modifier = Modifier.size(IconSize)
        )
    }
}