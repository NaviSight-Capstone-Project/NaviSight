package edu.capstone.navisight.common.objectdetection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import edu.capstone.navisight.viu.detectors.ObjectDetection
import java.util.LinkedList
import kotlin.math.abs
import kotlin.math.max
import edu.capstone.navisight.R
import edu.capstone.navisight.common.Constants.COLLISION_ITEMS
import edu.capstone.navisight.common.Constants.INDOOR_MODE
import edu.capstone.navisight.common.Constants.OUTDOOR_ITEMS
import edu.capstone.navisight.common.Constants.VIBRATE_OBJECT_DETECTED
import edu.capstone.navisight.common.TextToSpeechHelper
import edu.capstone.navisight.common.VibrationHelper
import kotlin.math.pow

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results: List<ObjectDetection> = LinkedList<ObjectDetection>()
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    private var lastLabel = "";
    private var lastSpokenStatement = "";

    private var scaleFactor: Float = 1f

    private var bounds = Rect()

    // For TTS and handling Bounding Box Area changes
    private var lastDetectedArea = 0F
    private var lastAreaThreshold = 0.0F
    private var currentAreaThreshold = 0.0F
    private val areaThresholds = listOf(0.70F, 0.35F, 0.10F) // In percentages
    private val redundancyDelay = 3000L // Default to three (3) seconds
    private val speechQueueThreshold = 5 // Speeches per queue until it auto-skips

    init {
        initPaints()
    }

//    data class StableDetection(
//        val trackId: Int,             // Simplified: just a unique ID for aggregation (0 for now)
//        val className: String,
//        val confidence: Float,
//        val position: String,         // "AHEAD", "TO YOUR LEFT", or "TO YOUR RIGHT"
//        val proximityThreshold: Float, // Proximity based on area (0.0F - 100.0F)
//        val box: RectF                // The current bounding box
//    )

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

            getObjectPosition(result)

            // Check if label is in indoors (if activated, else just detect)
            if (!INDOOR_MODE || !OUTDOOR_ITEMS.contains(result.category.label)) {
                val boundingBox = result.boundingBox

                val top = boundingBox.top * scaleFactor
                val bottom = boundingBox.bottom * scaleFactor
                val left = boundingBox.left * scaleFactor
                val right = boundingBox.right * scaleFactor

                // Draw bounding box around detected objects
                val drawableRect = RectF(left, top, right, bottom)
                canvas.drawRect(drawableRect, boxPaint)

                // Create text to display alongside detected objects
                // THE LABEL IS HERE: result.category.label
                val drawableText =
                    result.category.label + " " +
                            String.format(
                                "%.2f %.2f", result.category.confidence,
                                getThresholdLevel(calculateCurrentBBArea(result))
                            )

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
                // TODO: LOOK HERE TO PREPROCESS
                doOnDetection(result)
            }
            // Do nothing.
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

    private fun doOnDetection(result: ObjectDetection) {
        speakWhenDetected(context, result)  // Do Text to Speech, with variability.
        VibrationHelper.vibratePattern(context, VIBRATE_OBJECT_DETECTED)
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

        var estimatedLabel = when {
            percent >= areaThresholds[0] -> "A $label is on front of you"
            percent >= areaThresholds[1] -> "A $label is close to you"
            percent >= areaThresholds[2] -> "A $label is nearby"
            else -> "A $label has been detected"
        }

        // If item is going to be bad if its going to collide, then add extra caution
        if (percent >= areaThresholds[0] && COLLISION_ITEMS.contains(label)) {
            estimatedLabel += ". Please proceed with caution."
        }
        return estimatedLabel
    }

    private fun didBBAThresholdChanged(currentDetectedArea: Float): Boolean {
        currentAreaThreshold = getThresholdLevel(currentDetectedArea)
        val lastAreaThreshold = getThresholdLevel(lastDetectedArea)
        return currentAreaThreshold != lastAreaThreshold
    }

    private fun getThresholdLevel(area: Float): Float {
        val screenSizes = getScreenSizeOldApi(context)
        val screenArea = (screenSizes.first) * (screenSizes.second)
        val percent = ((area / screenArea) / 3) * 100 // TODO: Optimize equation.

        for (areaThreshold in areaThresholds) {
            if (percent > areaThreshold) {
                return areaThreshold
            }
        }
        return 0.0F // No threshold met. Defaults to just "detected"
    }

    // Handle when TTS should speak and avoid speech flurry.
    private fun speakWhenDetected(context: Context, result: ObjectDetection) {
        val currentLabel = result.category.label
        val currentDetectedArea = calculateCurrentBBArea(result)
        val currentSpeechQueue = TextToSpeechHelper.getSpeechQueue()
        if (lastLabel != currentLabel || didBBAThresholdChanged(currentDetectedArea)) {
            if (currentSpeechQueue.size > speechQueueThreshold) TextToSpeechHelper.clearQueue()

            // Labels the severity
            val proximityStatus = bbEstimatedProximity(currentDetectedArea, currentLabel)

            //

            if (lastLabel == currentLabel) {
                if (currentAreaThreshold > lastAreaThreshold) TextToSpeechHelper.queueSpeakLatest(
                    context,
                    proximityStatus, redundancyDelay
                )
                lastAreaThreshold = currentAreaThreshold
            } else TextToSpeechHelper.queueSpeakLatest(context, proximityStatus)
            lastLabel = currentLabel
            lastDetectedArea = currentDetectedArea
        }
    }

    private fun getObjectPosition(detection: ObjectDetection): String {
        val screenSizes = getScreenSizeOldApi(context)
        val centerX = detection.boundingBox.centerX()

        // FIX: Using a 30/40/30 split (Left/Ahead/Right) to make the side zones more responsive.
        // This solves the issue of objects feeling "to the right" but being classified as "AHEAD."
        val leftBoundary = screenSizes.first * 0.06
        val rightBoundary = screenSizes.first * 0.12

        val result = when {
            centerX < leftBoundary -> "TO YOUR LEFT"
            centerX > rightBoundary -> "TO YOUR RIGHT"
            else -> "AHEAD"
        }

//        Log.d("GETOBJECTPOSITION", "LeftBoundary is $leftBoundary. RightBoundary is $rightBoundary. Width is ${screenSizes.first}. The center X is $centerX, current result is '$result'")
        return ""
    }
}
//
//    private fun processAllDetectionsAndSpeak(context: Context?, stableDetections: List<StableDetection>){
//        if (stableDetections.isEmpty()) {
//            lastSpokenStatement = "" // Reset state if nothing is detected
//            return
//        }
//
//        // This calls generateAggregatedStatement(), which is the Semantic Aggregation step
//        val finalStatement = generateAggregatedStatement(stableDetections)
//
//        // CORE TEMPORAL STABILIZATION CHECK:
//        // Only speak if the new statement is NOT empty AND it is DIFFERENT from the last one.
//        if (finalStatement.isNotEmpty() && finalStatement != lastSpokenStatement) {
//            val currentSpeechQueue = TextToSpeechHelper.getSpeechQueue()
//
//            // Safety check: Clear the queue if too many speeches are pending
//            if (currentSpeechQueue.size > speechQueueThreshold) TextToSpeechHelper.clearQueue()
//
//            // Speak the new narrative
//            TextToSpeechHelper.queueSpeakLatest(context, finalStatement)
//
//            // UPDATE STATE: Save the new statement so it won't be repeated in the next frame
//            lastSpokenStatement = finalStatement
//
//            // Vibrate on new, critical detection
//            VibrationHelper.vibratePattern(context, VIBRATE_OBJECT_DETECTED)
//        }
//    }
//
//
//    private fun groupDetectionsByProximity(stableDetections: List<StableDetection>): List<List<StableDetection>> {
//        if (stableDetections.isEmpty()) return emptyList()
//
//        val groups = mutableListOf<MutableList<StableDetection>>()
//        val processedIndices = mutableSetOf<Int>()
//
//        for (i in stableDetections.indices) {
//            if (i in processedIndices) continue
//
//            val current = stableDetections[i]
//            val currentGroup = mutableListOf(current)
//            processedIndices.add(i)
//
//            for (j in stableDetections.indices) {
//                if (i == j || j in processedIndices) continue
//
//                val neighbor = stableDetections[j]
//
//                // Check if the center of the bounding boxes are within a threshold
//                val center1X = current.box.centerX()
//                val center1Y = current.box.centerY()
//                val center2X = neighbor.box.centerX()
//                val center2Y = neighbor.box.centerY()
//
//                // Calculate Euclidean distance between centers
//                val distance = kotlin.math.sqrt(
//                    (center1X - center2X).pow(2) +
//                            (center1Y - center2Y).pow(2)
//                )
//
////                if (distance < PROXIMITY_PIXEL_THRESHOLD) {
////                    currentGroup.add(neighbor)
////                    processedIndices.add(j)
////                }
//            }
//            groups.add(currentGroup)
//        }
//        return groups
//    }
//
//    private fun generateAggregatedStatement(stableDetections: List<StableDetection>): String {
//        if (stableDetections.isEmpty()) return ""
//
//        val groupedItems = groupDetectionsByProximity(stableDetections)
//        val statements = mutableListOf<String>()
//
//        for (group in groupedItems) {
//            val primary = group.first()
//            val position = primary.position
//            val proximity = primary.proximityThreshold
//
//            val proximityWord = when {
//                proximity >= 35.0F -> "VERY CLOSE" // Using the 0.35 threshold from old logic
//                proximity >= 10.0F -> "CLOSE"
//                else -> ""
//            }
//
//            if (group.size > 1) {
//                // Semantic Aggregation: "Person and dog to your right"
//                val secondaryNames = group.drop(1).joinToString(" and ") { it.className.lowercase() }
//
//                // Prioritize the nearest one in the group
//                val primaryName = if (proximityWord.isNotEmpty()) "$proximityWord ${primary.className.uppercase()}" else primary.className.uppercase()
//
//                statements.add("$primaryName near $secondaryNames ${position}")
//            } else {
//                // Single Object: "Chair AHEAD"
//                val statement = if (proximityWord.isNotEmpty()) {
//                    "$proximityWord ${primary.className.uppercase()} ${position}"
//                } else {
//                    "${primary.className.uppercase()} ${position}"
//                }
//                statements.add(statement)
//            }
//        }
//
//        // Combine all statements into a single, cohesive narrative
//        return "WARNING. ${statements.joinToString(". ")}."
//    }
//}