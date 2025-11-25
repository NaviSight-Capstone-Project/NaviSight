package edu.capstone.navisight.viu.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import edu.capstone.navisight.viu.detectors.ObjectDetection
import java.util.LinkedList
import kotlin.math.abs
import kotlin.math.max
import edu.capstone.navisight.R
import edu.capstone.navisight.common.TTSHelper
import edu.capstone.navisight.common.VibrationHelper

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results: List<ObjectDetection> = LinkedList<ObjectDetection>()
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    private var lastLabel = "";

    private var scaleFactor: Float = 1f

    private var bounds = Rect()

    // For TTS and handling Bounding Box Area changes
    private var lastDetectedArea =  0F
    private var lastAreaThreshold = 0.0F
    private var currentAreaThreshold = 0.0F
    private val areaThresholds = listOf(0.70F, 0.35F, 0.10F) // In percentages
    private val redundancyDelay = 3000L // Default to three (3) seconds
    private val speechQueueThreshold = 5 // Speeches per queue until it auto-skips

    // Init. vibration helper
    private val vibrationHelper = VibrationHelper(context)

    init {
        initPaints()
    }

    fun clear() {
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        for (result in results) {
            val boundingBox = result.boundingBox

            val top = boundingBox.top * scaleFactor
            val bottom = boundingBox.bottom * scaleFactor
            val left = boundingBox.left * scaleFactor
            val right = boundingBox.right * scaleFactor

            // Draw bounding box around detected objects
            val drawableRect = RectF(left, top, right, bottom)
            canvas.drawRect(drawableRect, boxPaint)

            // Create text to display alongside detected objects
            val drawableText =
                result.category.label + " " +
                        String.format("%.2f %.2f", result.category.confidence,
                            getThresholdLevel(calculateCurrentBBArea(result)))

            // Draw rect behind display text
            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()
            canvas.drawRect(
                left,
                top,
                left + textWidth + Companion.BOUNDING_RECT_TEXT_PADDING,
                top + textHeight + Companion.BOUNDING_RECT_TEXT_PADDING,
                textBackgroundPaint
            )

            // Draw text for detected object
            canvas.drawText(drawableText, left, top + bounds.height(), textPaint)

            // Do action if detected
            doOnDetection(result)
        }
    }

    fun setResults(
        detectionResults: List<ObjectDetection>,
        imageHeight: Int,
        imageWidth: Int,
    ) {
        results = detectionResults

        // PreviewView is in FILL_START mode. So we need to scale up the bounding box to match with
        // the size that the captured images will be displayed.
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }

    private fun getScreenSizeOldApi(context: Context): Pair<Int, Int> {
        val displayMetrics = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }

    private fun doOnDetection(result : ObjectDetection) {
        speakWhenDetected(context, result)  // Do Text to Speech, with variability.
        vibrationHelper.vibrate()
    }

    private fun calculateCurrentBBArea(result: ObjectDetection): Float {
        val boundingBox = result.boundingBox
        val l = abs(boundingBox.right - boundingBox.left) // x2 - x1
        val w = abs(boundingBox.top - boundingBox.bottom) // y1 - y2
        val a = l * w
        return a
    }

    // Handle proximity level in bounding boxes.
    // top: Float, left: Float, bottom: Float, right: Float)
    private fun bbEstimatedProximity(currentDetectedArea: Float, label: String): String {
        val percent = getThresholdLevel(currentDetectedArea)
        // DEBUG LOGS.
        // Log.println(Log.INFO, "screen size", "$results")
        // Log.println(Log.INFO, "bounds check (l/x1 t/y1 r/x2 b/y2)", "$left $top $right $bottom")
        return when {
            percent >= areaThresholds[0] -> "A $label is on front of you. Please proceed with caution."
            percent >= areaThresholds[1] -> "A $label is close to you"
            percent >= areaThresholds[2] -> "A $label is nearby"
            else -> "A $label has been detected"
        }
    }

    private fun didBBAThresholdChanged(currentDetectedArea: Float): Boolean {
        currentAreaThreshold = getThresholdLevel(currentDetectedArea)
        val lastAreaThreshold = getThresholdLevel(lastDetectedArea)
        return currentAreaThreshold != lastAreaThreshold
    }

    private fun getThresholdLevel(area: Float): Float {
        val results = getScreenSizeOldApi(context)
        val screenArea = (results.first) * (results.second)
        val percent = ((area / screenArea) / 3) * 100 // TODO: Optimize equation.

        for (areaThreshold in areaThresholds) {
            if (percent > areaThreshold) {
                return areaThreshold
            }
        }
        return 0.0F // No threshold met. Defaults to just "detected"
    }

    // Handle when TTS should speak and avoid speech flurry.
    private fun speakWhenDetected(context: Context, result: ObjectDetection){
        val currentLabel = result.category.label
        val currentDetectedArea = calculateCurrentBBArea(result)
        val currentSpeechQueue = TTSHelper.getSpeechQueue()
        if (lastLabel != currentLabel || didBBAThresholdChanged(currentDetectedArea)) {
            if (currentSpeechQueue.size > speechQueueThreshold) TTSHelper.clearQueue()
            val proximityStatus = bbEstimatedProximity(currentDetectedArea, currentLabel)
            if (lastLabel == currentLabel) {
                if (currentAreaThreshold > lastAreaThreshold) TTSHelper.queueSpeakLatest(
                    context,
                    proximityStatus, redundancyDelay
                )
                lastAreaThreshold = currentAreaThreshold
            } else TTSHelper.queueSpeakLatest(context, proximityStatus)
            lastLabel = currentLabel
            lastDetectedArea = currentDetectedArea
        }
    }
}