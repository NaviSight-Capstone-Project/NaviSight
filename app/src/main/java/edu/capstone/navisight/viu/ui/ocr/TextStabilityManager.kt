package edu.capstone.navisight.viu.ui.ocr

import android.graphics.Rect
import kotlin.math.abs


 //Helper class to determine if the text in the camera frame is steady.
 //This prevents the "Auto-Capture" from taking a blurry photo while the user is moving the phone.

class TextStabilityManager(
    private val stabilityDurationMs: Long = 1200L,
    private val movementThreshold: Int = 60
) {
    private var lastTextBounds: Rect? = null
    private var steadyStartTime: Long = 0

    enum class StabilityStatus {
        NO_TEXT,
        MOVING,          // Text detected but camera is moving
        STABILIZING,     // Text is steady, timer is counting down
        STEADY_AND_READY // Timer finished, safe to capture!
    }

    fun checkStability(currentBounds: Rect?): StabilityStatus {
        if (currentBounds == null) {
            reset()
            return StabilityStatus.NO_TEXT
        }

        val last = lastTextBounds
        if (last == null) {
            // New text detected
            lastTextBounds = currentBounds
            steadyStartTime = System.currentTimeMillis()
            return StabilityStatus.STABILIZING
        }

        // Calculate how much the text center has moved
        val dx = abs(currentBounds.centerX() - last.centerX())
        val dy = abs(currentBounds.centerY() - last.centerY())

        if (dx > movementThreshold || dy > movementThreshold) {
            // Too much movement, reset the timer
            lastTextBounds = currentBounds
            steadyStartTime = System.currentTimeMillis()
            return StabilityStatus.MOVING
        }

        // Text is steady. Check if we have held it long enough.
        val elapsed = System.currentTimeMillis() - steadyStartTime
        return if (elapsed >= stabilityDurationMs) {
            reset() // Reset so we don't trigger multiple times instantly
            StabilityStatus.STEADY_AND_READY
        } else {
            StabilityStatus.STABILIZING
        }
    }

    fun reset() {
        lastTextBounds = null
        steadyStartTime = 0
    }
}