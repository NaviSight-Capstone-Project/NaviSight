package edu.capstone.navisight.common.objectdetection

import edu.capstone.navisight.common.Constants.COLLISION_ITEMS
import edu.capstone.navisight.common.Constants.OUTDOOR_ITEMS
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.content.ContextCompat
import edu.capstone.navisight.R
import edu.capstone.navisight.viu.detectors.DetectionResult
import edu.capstone.navisight.viu.detectors.ObjectDetection
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

// --- Constants replicated from OverlayProcessor.kt ---
private const val PROXIMITY_PIXEL_THRESHOLD = 150 //
private const val CONFIDENCE_THRESHOLD = 0.8f //
private const val INDOOR_MODE = true //
private val areaThresholds = listOf(0.70F, 0.35F, 0.10F) //
private const val BOUNDING_RECT_TEXT_PADDING = 8 //

class DetectionPostProcessor(private val context: Context) {

    // Data class replicated from OverlayProcessor.kt
    data class StableDetection(
        val trackId: Int,
        val labelName: String,
        val confidence: Float,
        val position: String,
        val proximityThreshold: Float,
        val box: RectF
    )

    // Paints initialized within the processor
    private val boxPaint = Paint()
    private val textBackgroundPaint = Paint()
    private val textPaint = Paint()
    private var bounds = Rect()

    init {
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f //

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f //

        // Try to load color resource, fallback to RED if R is not available
        boxPaint.color = try {
            ContextCompat.getColor(context, R.color.bounding_box_color)
        } catch (e: Exception) {
            Color.RED
        }
        boxPaint.strokeWidth = 8F //
        boxPaint.style = Paint.Style.STROKE //
    }

    /**
     * The main function to process detection results, generate narrative, and draw overlays.
     * @param detectionResult The result from the ObjectDetector.
     * @return A Pair containing the Bitmap with bounding box overlays and the generated TTS narrative.
     */
    fun processDetectionResult(detectionResult: DetectionResult?): Pair<Bitmap, String> {
        if (detectionResult == null || detectionResult.detections.isEmpty()) {
            // FIX: Use .let and Elvis operator to handle nullable config and fallback
            val image = detectionResult?.image?.let { bitmap ->
                bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
            } ?: Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            return Pair(image, "No objects detected or detection failed.")
        }

        val originalBitmap = detectionResult.image
        val detectionList = detectionResult.detections

        // 1. Filter and map to StableDetection
        val filteredResults = filterForIrrelevantDetections(detectionList) //

        val stableDetections = filteredResults.mapIndexed { index, result ->
            val proximity = calculateCurrentBBArea(result) //
            StableDetection(
                trackId = index,
                labelName = result.category.label,
                confidence = result.category.confidence,
                position = getObjectPosition(result), //
                proximityThreshold = getThresholdLevel(proximity), //
                box = result.boundingBox
            )
        }

        // 2. Generate Narrative (String)
        val narrative = generateAggregatedStatement(stableDetections) //

        // 3. Draw Overlay (Bitmap)
        val imageWithOverlay = drawOverlay(originalBitmap, filteredResults)

        return Pair(imageWithOverlay, narrative)
    }

    private fun drawOverlay(
        originalBitmap: Bitmap,
        filteredResults: List<ObjectDetection>
    ): Bitmap {
        // Create a mutable copy of the bitmap to draw on
        val mutableBitmap = originalBitmap.copy(originalBitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        // For a detection result, the scale factor is 1.0 as the bounding box coordinates
        // are already relative to the bitmap's dimensions.
        val scaleFactor = 1f

        for (result in filteredResults) {
            val boundingBox = result.boundingBox

            val top = boundingBox.top * scaleFactor
            val bottom = boundingBox.bottom * scaleFactor
            val left = boundingBox.left * scaleFactor
            val right = boundingBox.right * scaleFactor

            // Draw bounding box
            val drawableRect = RectF(left, top, right, bottom)
            canvas.drawRect(drawableRect, boxPaint)

            // Create text
            val proximity = calculateCurrentBBArea(result)
            val drawableText =
                result.category.label + " " +
                        String.format(
                            "%.2f %.2f", result.category.confidence,
                            getThresholdLevel(proximity)
                        )

            // Draw text background
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

            // Draw text
            canvas.drawText(drawableText, left, top + bounds.height(), textPaint)
        }

        return mutableBitmap
    }

    // --- Helper Methods (Logic extracted from OverlayProcessor.kt) ---

    @Suppress("DEPRECATION")
    private fun getScreenSizeOldApi(context: Context): Pair<Int, Int> {
        val displayMetrics = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
    } //

    private fun calculateCurrentBBArea(result: ObjectDetection): Float {
        val boundingBox = result.boundingBox
        val l = abs(boundingBox.right - boundingBox.left)
        val w = abs(boundingBox.top - boundingBox.bottom)
        return l * w
    } //

    private fun getThresholdLevel(area: Float): Float {
        val screenSizes = getScreenSizeOldApi(context)
        val screenArea = (screenSizes.first) * (screenSizes.second)
        val percent = ((area / screenArea) / 3) * 100 //

        for (areaThreshold in areaThresholds) {
            if (percent > areaThreshold) {
                return areaThreshold
            }
        }
        return 0.0F
    } //

    private fun filterForIrrelevantDetections(rawResults: List<ObjectDetection>): List<ObjectDetection> {
        return rawResults.filter { detection ->
            val isConfident = detection.category.confidence >= CONFIDENCE_THRESHOLD //
            if (INDOOR_MODE) { //
                val isRelevant = !OUTDOOR_ITEMS.contains(detection.category.label)
                isRelevant && isConfident
            } else {
                isConfident
            }
        }
    } //

    private fun getObjectPosition(detection: ObjectDetection): String {
        val screenSizes = getScreenSizeOldApi(context)
        val centerX = detection.boundingBox.centerX()

        val leftBoundary = screenSizes.first * 0.06
        val rightBoundary = screenSizes.first * 0.12

        return when {
            centerX < leftBoundary -> "TO YOUR LEFT"
            centerX > rightBoundary -> "TO YOUR RIGHT"
            else -> "AHEAD"
        }
    } //

    private fun groupDetectionsByProximity(stableDetections: List<StableDetection>): List<List<StableDetection>> {
        if (stableDetections.isEmpty()) return emptyList()

        val groupsOfObjects = mutableListOf<MutableList<StableDetection>>()
        val processedIndices = mutableSetOf<Int>()

        for (i in stableDetections.indices) {
            if (i in processedIndices) continue

            val primaryObject = stableDetections[i]
            val currentGroup = mutableListOf(primaryObject)
            processedIndices.add(i)

            for (j in stableDetections.indices) {
                if (i == j || j in processedIndices) continue

                val neighborObject = stableDetections[j]
                val deltaX = primaryObject.box.centerX() - neighborObject.box.centerX()
                val deltaY = primaryObject.box.centerY() - neighborObject.box.centerY()
                val distance = kotlin.math.sqrt(deltaX.pow(2) + deltaY.pow(2))

                if (distance < PROXIMITY_PIXEL_THRESHOLD) {
                    currentGroup.add(neighborObject)
                    processedIndices.add(j)
                }
            }
            groupsOfObjects.add(currentGroup)
        }
        return groupsOfObjects
    } //

    private fun generateAggregatedStatement(stableDetections: List<StableDetection>): String {
        // Full logic extracted from OverlayProcessor.kt for semantic aggregation
        if (stableDetections.isEmpty()) return ""

        val groupedItems = groupDetectionsByProximity(stableDetections)
        val statements = mutableListOf<String>()
        var stateToBeMoreCautionary = false

        for (group in groupedItems) {
            val primary = group.first()
            val position = primary.position
            val proximity = primary.proximityThreshold

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
                val count = group.size
                val statement = if (count > 1) {
                    val countDescription = if (count == 2) "TWO" else "MULTIPLE"
                    "$proximityWord $countDescription ${baseName}S ${position}"
                } else {
                    "$proximityWord $baseName ${position}"
                }
                statements.add(statement.trim())
            } else if (group.size > 1) {
                val allCounts = group.groupingBy { it.labelName.lowercase() }.eachCount()
                val secondaryNamesList = allCounts.mapNotNull { (label, count) ->
                    if (label == primaryLabel) {
                        val remainingCount = count - 1
                        if (remainingCount > 0) {
                            when (remainingCount) { 1 -> "another $label"; 2 -> "two more ${label}s"; else -> "multiple other ${label}s" }
                        } else { null }
                    } else {
                        when (count) { 1 -> "a $label"; 2 -> "two ${label}s"; else -> "multiple ${label}s" }
                    }
                }

                val secondaryNames = secondaryNamesList.joinToString(" and ")
                val primaryName = if (proximityWord.isNotEmpty()) "$proximityWord $baseName" else baseName
                statements.add("$primaryName near $secondaryNames ${position}")
            } else {
                val statement = if (proximityWord.isNotEmpty()) {
                    "$proximityWord ${primary.labelName.uppercase()} ${position}"
                } else {
                    "${primary.labelName.uppercase()} ${position}"
                }

                if (proximity >= areaThresholds[0] && COLLISION_ITEMS.contains(primary.labelName)) stateToBeMoreCautionary = true
                statements.add(statement)
            }
        }

        var finalNarrative = "WARNING. ${statements.joinToString(". ")}."
        if (stateToBeMoreCautionary) finalNarrative += ". Please proceed with caution."
        return finalNarrative
    }
}