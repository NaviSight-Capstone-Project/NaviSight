package edu.capstone.navisight.caregiver.ui.feature_map

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.capstone.navisight.caregiver.model.Viu

@Composable
fun MapScreen(
    vius: List<Viu>,
    selectedViu: Viu?,
    onViuSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val heightAnim by animateDpAsState(
        targetValue = if (expanded) 180.dp else 35.dp,
        animationSpec = tween(300),
        label = "heightAnim"
    )

    val alphaAnim by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.7f,
        animationSpec = tween(250),
        label = "alphaAnim"
    )

    val dynamicWidth by remember(selectedViu, expanded) {
        mutableStateOf(
            if (expanded) 200.dp else calculateCapsuleWidth(selectedViu)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 2.dp, bottom = 0.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        CapsuleContainer(
            expanded = expanded,
            height = heightAnim,
            width = dynamicWidth,
            alpha = alphaAnim,
            onClick = { expanded = !expanded }
        ) {
            if (expanded) {
                ExpandedCapsule(
                    vius = vius,
                    selectedViu = selectedViu,
                    onViuSelected = {
                        expanded = false
                        onViuSelected(it)
                    }
                )
            } else {
                CollapsedCapsule(selectedViu)
            }
        }
    }
}

@Composable
fun CapsuleContainer(
    expanded: Boolean,
    height: Dp,
    width: Dp,
    alpha: Float,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .blurShadow(
                blur = if (expanded) 20f else 25f,
                color = Color.Black.copy(alpha = 0.15f)
            )
            .border(
                width = if (expanded) 0.dp else 1.dp,
                color = if (expanded) Color.Transparent else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(if (expanded) 20.dp else 50.dp)
            )
            .background(
                color = Color.White.copy(alpha = 0.98f),
                shape = RoundedCornerShape(if (expanded) 20.dp else 50.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color(0xFF6041EC)),
                onClick = onClick
            )
            .width(width)
            .height(height)
            .alpha(alpha)
            .padding(horizontal = 12.dp, vertical = if (expanded) 8.dp else 0.dp),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun CollapsedCapsule(selectedViu: Viu?) {
    Box(modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center) {
        Text(
            text = selectedViu?.let { "${it.firstName} ${it.lastName}" } ?: "Select VIU",
            fontSize = 12.sp,
            color = Color(0xFF6041EC),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ExpandedCapsule(
    vius: List<Viu>,
    selectedViu: Viu?,
    onViuSelected: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Choose VIU",
            fontSize = 10.sp,
            color = Color.Gray,
            fontStyle = FontStyle.Italic
        )
        Text(
            text = selectedViu?.let { "${it.firstName} ${it.lastName}" } ?: "Select VIU",
            fontSize = 15.sp,
            color = Color(0xFF6041EC),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(1.dp)
                .background(Color(0x206041EC))
        )
        vius.forEach { viu ->
            Text(
                text = "${viu.firstName} ${viu.lastName}",
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = Color(0xFF6041EC))
                    ) { onViuSelected(viu.uid) }
                    .padding(vertical = 4.dp)
            )
        }
    }
}

fun calculateCapsuleWidth(selectedViu: Viu?): Dp {
    val name = selectedViu?.let { "${it.firstName} ${it.lastName}" } ?: "Select VIU"
    val length = name.length
    val calculated = (length * 8).dp
    return calculated.coerceIn(160.dp, 220.dp)
}

fun Modifier.blurShadow(
    color: Color = Color.Black,
    blur: Float = 45f,
    cornerRadius: Float = 50f
): Modifier = this.then(
    Modifier.drawBehind {
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                this.color = color
                this.asFrameworkPaint().maskFilter =
                    android.graphics.BlurMaskFilter(blur, android.graphics.BlurMaskFilter.Blur.NORMAL)
            }
            withTransform({ translate(0f, 0f) }) {
                canvas.drawRoundRect(
                    0f,
                    0f,
                    size.width,
                    size.height,
                    cornerRadius,
                    cornerRadius,
                    paint
                )
            }
        }
    }
)