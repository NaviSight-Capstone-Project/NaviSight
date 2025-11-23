package edu.capstone.navisight.caregiver.ui.feature_scanqr

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import edu.capstone.navisight.R

class ScannerOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val porterDuffXfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    private val boxRect = RectF()
    private val cornerRadius = 32f

    // Colors
    private val maskColor = Color.parseColor("#99000000") // Semi-transparent black
    private val frameColor = Color.WHITE

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paint.color = maskColor
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        val boxSize = width * 0.7f
        val cx = width / 2f
        val cy = height / 2f
        boxRect.set(
            cx - boxSize / 2,
            cy - boxSize / 2,
            cx + boxSize / 2,
            cy + boxSize / 2
        )


        paint.xfermode = porterDuffXfermode
        canvas.drawRoundRect(boxRect, cornerRadius, cornerRadius, paint)
        paint.xfermode = null


        paint.color = frameColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        canvas.drawRoundRect(boxRect, cornerRadius, cornerRadius, paint)
    }
}