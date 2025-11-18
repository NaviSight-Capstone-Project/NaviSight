package edu.capstone.navisight.caregiver.ui.feature_map.mapsElementsDesigns

import edu.capstone.navisight.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
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
private val ShadowColor = Color(0xFF6041EC)
private val ShadowAmbientAlpha = 0.3f
private val ShadowSpotAlpha = 0.5f
private val OuterGlowColor = Color(0x406041EC)
private val OuterGlowRadius = 100f

private val GradientLeftColor = Color(0xFFB644F1)
private val GradientRightColor = Color(0xFF6041EC)
private val RippleColor = Color(0xFF6041EC)
private val InnerGlowColor = Color(0x106041EC)
private val InnerGlowRadius = 60f
private val IconTintColor = Color.White
private val IconSize = 24.dp

@Composable
fun RecenterButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(ButtonSize)
            .shadow(
                elevation = ShadowElevation,
                shape = CircleShape,
                ambientColor = ShadowColor.copy(alpha = ShadowAmbientAlpha),
                spotColor = ShadowColor.copy(alpha = ShadowSpotAlpha)
            )
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(OuterGlowColor, Color.Transparent),
                    radius = OuterGlowRadius
                )
            )
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(GradientLeftColor, GradientRightColor)
                )
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = RippleColor),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        ButtonGlowOverlay()
        RecenterIcon()
    }
}

@Composable
private fun ButtonGlowOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(ButtonSize)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(InnerGlowColor, Color.Transparent),
                    radius = InnerGlowRadius
                )
            )
    )
}

@Composable
private fun RecenterIcon(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(id = R.drawable.ic_recenter),
        contentDescription = "Recenter map",
        tint = IconTintColor,
        modifier = modifier.size(IconSize)
    )
}