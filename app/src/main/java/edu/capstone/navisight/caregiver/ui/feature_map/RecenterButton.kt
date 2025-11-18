package edu.capstone.navisight.caregiver.ui.feature_map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
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
import edu.capstone.navisight.R


// Dimensions
private val ButtonSize = 56.dp
private val IconSize = 36.dp
private val BottomPadding = 2.dp
private val EndPadding = 4.dp
private val ShadowElevation = 20.dp

// Colors
private val ShadowColor = Color(0xFF000000)
private val GradientColorTop = Color(0xFFE15BFF)
private val GradientColorBottom = Color(0xFF7A49F2)
private val IconTintColor = Color.White
private val RippleColor = Color.White.copy(alpha = 0.3f)

// Outer Glow
private val OuterGlowColor = Color.White.copy(alpha = 0.25f)
private const val OuterGlowRadius = 90f

// Inner Glow
private val InnerGlowColor = Color.White.copy(alpha = 0.10f)
private const val InnerGlowRadius = 60f

// Shadow
private const val ShadowAmbientAlpha = 0.25f
private const val ShadowSpotAlpha = 0.35f



@Composable
fun RecenterButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .padding(bottom = BottomPadding, end = EndPadding)
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
                brush = Brush.verticalGradient(
                    colors = listOf(GradientColorTop, GradientColorBottom)
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