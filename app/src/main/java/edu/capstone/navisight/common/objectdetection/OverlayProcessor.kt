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
import edu.capstone.navisight.common.Constants.OUTDOOR_ITEMS
import edu.capstone.navisight.common.Constants.VIBRATE_OBJECT_DETECTED
import edu.capstone.navisight.common.TextToSpeechHelper
import edu.capstone.navisight.common.VibrationHelper
import kotlin.math.pow

private const val PROXIMITY_PIXEL_THRESHOLD = 150
private const val CONFIDENCE_THRESHOLD = 0.8f
private const val INDOOR_MODE = true
private const val REPEAT_DELAY_MS = 10000L

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results: List<ObjectDetection> = LinkedList<ObjectDetection>()
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()
    private var lastSpokenStatement = "";
    private var scaleFactor: Float = 1f
    private var bounds = Rect()

    // For TTS and handling Bounding Box Area changes
    private var lastDetectedArea = 0F
    private var currentAreaThreshold = 0.0F
    private val areaThresholds = listOf(0.70F, 0.35F, 0.10F) // In percentages
    private var lastSpokenTimestamp: Long = 0L
    private val speechQueueThreshold = 5 // Speeches per queue until it auto-skips

    init {
        initPaints()
    }

    data class StableDetection(
        val trackId: Int,             // Simplified: just a unique ID for aggregation (0 for now)
        val labelName: String,
        val confidence: Float,
        val position: String,         // "AHEAD", "TO YOUR LEFT", or "TO YOUR RIGHT"
        val proximityThreshold: Float, // Proximity based on area (0.0F - 100.0F)
        val box: RectF                // The current bounding box
    )

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

        // Filter for allowed classes and stuff
        val filteredResults = filterForIrrelevantDetections(results)

        val stableDetections = filteredResults.mapIndexed { index, result ->
            val proximity = calculateCurrentBBArea(result)
            StableDetection(
                trackId = index,
                labelName = result.category.label,
                confidence = result.category.confidence,
                position = getObjectPosition(result),
                proximityThreshold = getThresholdLevel(proximity),
                box = result.boundingBox
            )
        }

        // Process and speech lahat na dito (make a story time)
        processAllDetectionsAndSpeak(context, stableDetections)

        // Draw. just draw
        for (result in filteredResults) {

            // Check if label is in indoors (if activated, else just detect)
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
                left + textWidth + BOUNDING_RECT_TEXT_PADDING,
                top + textHeight + BOUNDING_RECT_TEXT_PADDING,
                textBackgroundPaint
            )

            // Draw text for detected object
            canvas.drawText(drawableText, left, top + bounds.height(), textPaint)
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

    private fun calculateCurrentBBArea(result: ObjectDetection): Float {
        val boundingBox = result.boundingBox
        val l = abs(boundingBox.right - boundingBox.left) // x2 - x1
        val w = abs(boundingBox.top - boundingBox.bottom) // y1 - y2
        val a = l * w
        return a
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

    private fun filterForIrrelevantDetections(rawResults: List<ObjectDetection>): List<ObjectDetection> {
        return rawResults.filter { detection ->
            val isConfident = detection.category.confidence >= CONFIDENCE_THRESHOLD
            if (INDOOR_MODE) { // Remove outdoor items if indoor mode detected
                val isRelevant = !OUTDOOR_ITEMS.contains(detection.category.label)
                isRelevant && isConfident
            } else {
                isConfident
            }
        }
    }

    private fun getObjectPosition(detection: ObjectDetection): String {
        val screenSizes = getScreenSizeOldApi(context)
        val centerX = detection.boundingBox.centerX()

        val leftBoundary = screenSizes.first * 0.06
        val rightBoundary = screenSizes.first * 0.12

        val result = when {
            centerX < leftBoundary -> "TO YOUR LEFT"
            centerX > rightBoundary -> "TO YOUR RIGHT"
            else -> "AHEAD"
        }

        Log.d("GETOBJECTPOSITION", "LeftBoundary is $leftBoundary. RightBoundary is $rightBoundary. Width is ${screenSizes.first}. The center X is $centerX, current result is '$result'")
        return result
    }

    private fun processAllDetectionsAndSpeak(context: Context?, stableDetections: List<StableDetection>){
        // This calls generateAggregatedStatement(), which is the Semantic Aggregation step
        val finalStatement = generateAggregatedStatement(stableDetections)

        if (finalStatement.isEmpty()) {
            return
        }

        val isNewStatement = finalStatement != lastSpokenStatement
        val currentTime = System.currentTimeMillis()
        val isRepeatDue = (currentTime - lastSpokenTimestamp) >= REPEAT_DELAY_MS

        // CORE TEMPORAL STABILIZATION CHECK:
        // Only speak if the new statement is NOT empty AND it is DIFFERENT from the last one.
        if (isNewStatement || isRepeatDue) {

            // Safety check: Clear the queue if too many speeches are pending
            val currentSpeechQueue = TextToSpeechHelper.getSpeechQueue()
            if (currentSpeechQueue.size > speechQueueThreshold) TextToSpeechHelper.clearQueue()

            // Speak the narrative
            TextToSpeechHelper.queueSpeakLatest(context, finalStatement)

            // UPDATE STATE: Update both the statement and the timestamp upon successful speech
            lastSpokenStatement = finalStatement
            lastSpokenTimestamp = currentTime

            // Vibrate on any new or repeated critical detection
            VibrationHelper.vibratePattern(context, VIBRATE_OBJECT_DETECTED)
        }
    }


    private fun groupDetectionsByProximity(stableDetections: List<StableDetection>): List<List<StableDetection>> {
        // NYEHEHEH If there's nothing to group, return an empty list immediately.
        if (stableDetections.isEmpty()) return emptyList()

        // The final list of all clusters found.
        val groupsOfObjects = mutableListOf<MutableList<StableDetection>>()

        // A set to track which objects have already been placed into a group.
        // This prevents double-counting and ensures efficiency.
        val processedIndices = mutableSetOf<Int>()

        // 1st loop, Iterate through every object (i) as a potential new group leader.
        for (i in stableDetections.indices) {

            // If this object has already been assigned to a group, skip it and continue.
            if (i in processedIndices) continue

            val primaryObject = stableDetections[i]

            // Start a new group with the current object.
            val currentGroup = mutableListOf(primaryObject)
            processedIndices.add(i) // Mark the primary object as processed.

            // 3rd loop, Compare the primary object (i) against every other object (j).
            for (j in stableDetections.indices) {

                // Skip the comparison if:
                // a) It's the same object (i == j).
                // b) The neighbor has already been assigned to a different group (j in processedIndices).
                if (i == j || j in processedIndices) continue

                val neighborObject = stableDetections[j]

                // Get the center points of the two bounding boxes.
                val center1X = primaryObject.box.centerX()
                val center1Y = primaryObject.box.centerY()
                val center2X = neighborObject.box.centerX()
                val center2Y = neighborObject.box.centerY()

                // Calculate the horizontal and vertical differences between the centers.
                val deltaX = center1X - center2X
                val deltaY = center1Y - center2Y

                /*
                    Euclidean distance
                    (A^2 + B^2 = C^2)
                    straight-line pixel distance, for checking proximity ito
                 */
                val distance = kotlin.math.sqrt(
                    deltaX.pow(2) +
                            deltaY.pow(2)
                )

                // Check Proximity Threshold: If the distance is smaller than the allowed limit,
                // they are considered part of the same cluster.
                if (distance < PROXIMITY_PIXEL_THRESHOLD) {
                    currentGroup.add(neighborObject)
                    processedIndices.add(j) // Mark the neighbor as processed so it won't start its own group later
                }
            }

            // After checking all neighbors, the current cluster is complete. Add it to the final list.
            groupsOfObjects.add(currentGroup)
        }

        return groupsOfObjects
    }

    private fun generateAggregatedStatement(stableDetections: List<StableDetection>): String {
        if (stableDetections.isEmpty()) return ""

        val groupedItems = groupDetectionsByProximity(stableDetections)
        val statements = mutableListOf<String>()
        var stateToBeMoreCautionary = false // If item is going to be bad if its going to collide, then add extra caution

        for (group in groupedItems) {
            val primary = group.first()
            val position = primary.position
            val proximity = primary.proximityThreshold

            Log.d("getobje", "Values from group item: $primary, $position, $proximity")

            val proximityWord = when {
                proximity >= areaThresholds[0] -> "VERY CLOSE"
                proximity >= areaThresholds[1] -> "CLOSE"
                proximity >= areaThresholds[2] -> "NEAR"
                else -> ""
            }


            val allSameLabel = group.all { it.labelName == primary.labelName }
            val baseName = primary.labelName.uppercase()
            val primaryLabel = primary.labelName.lowercase()

            if (allSameLabel) {
                // IF #1: HOMOGENEOUS GROUP (e.g., CHAIR, CHAIR, CHAIR) ---
                val count = group.size

                val statement = if (count > 1) {
                    // Use a pluralized/counted statement
                    val countDescription = if (count == 2) "TWO" else "MULTIPLE"
                    // E.g., "VERY CLOSE MULTIPLE CHAIRS TO YOUR RIGHT"
                    "$proximityWord $countDescription ${baseName}S ${position}"
                } else {
                    // Count is 1 (Should rarely happen in this block, but safe)
                    "$proximityWord $baseName ${position}"
                }
                statements.add(statement.trim())

            } else if (group.size > 1) {
                // IF #2: Group ALL objects (including the primary) by label and get counts
                val allCounts = group
                    .groupingBy { it.labelName.lowercase() }
                    .eachCount()

                // Map the counts into descriptive phrases, correctly handling the primary label's duplicates
                val secondaryNamesList = allCounts.mapNotNull { (label, count) ->

                    if (label == primaryLabel) {
                        // This label is the same as the primary object. We only list the *remaining* count.
                        val remainingCount = count - 1

                        if (remainingCount > 0) {
                            when (remainingCount) {
                                1 -> "another $label" // "another chair" instead of "a chair"
                                2 -> "two more ${label}s"
                                else -> "multiple other ${label}s"
                            }
                        } else {
                            null // If count was 1, no need to list it again.
                        }
                    } else {
                        // For different labels, use the standard count aggregation
                        when (count) {
                            1 -> "a $label"
                            2 -> "two ${label}s"
                            else -> "multiple ${label}s"
                        }
                    }
                }

                // Join the descriptive phrases with " and "
                val secondaryNames = secondaryNamesList.joinToString(" and ")

                // Construct the final 'Primary near Secondary' phrase
                val primaryName = if (proximityWord.isNotEmpty()) "$proximityWord $baseName" else baseName

                // Final Statement Example: "CHAIR near another chair and a dining table TO YOUR RIGHT"
                statements.add("$primaryName near $secondaryNames ${position}")

            } else {
                // IF #3: Single Object: "Chair AHEAD"
                val statement = if (proximityWord.isNotEmpty()) {
                    "$proximityWord ${primary.labelName.uppercase()} ${position}"
                } else {
                    "${primary.labelName.uppercase()} ${position}"
                }

                // Triggers at least even one is detected ito dapat:
                if (proximity >= areaThresholds[0] && COLLISION_ITEMS.contains(primary.labelName)) stateToBeMoreCautionary = true
                statements.add(statement)
            }
        }

        var finalNarrative = "WARNING. ${statements.joinToString(". ")}."
        if (stateToBeMoreCautionary) finalNarrative += ". Please proceed with caution."

        // Combine all statements into a single narrative
        return finalNarrative
    }
}