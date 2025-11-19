package edu.capstone.navisight.caregiver.ui.feature_map.mapScreen

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform

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
                    0f, 0f, size.width, size.height, cornerRadius, cornerRadius, paint
                )
            }
        }
    }
)

fun Double.format(digits: Int) = "%.${digits}f".format(this)