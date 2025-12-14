package edu.capstone.navisight.viu.ui.camera.managers

import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import edu.capstone.navisight.R
import edu.capstone.navisight.common.TextToSpeechHelper
import edu.capstone.navisight.viu.ui.camera.CameraFragment
import java.util.LinkedList

class ScreensaverHandler (
    private val cameraFragment : CameraFragment
) {

    // Init. screensaver vars
    private val screensaverStateHistory = LinkedList<Boolean>().apply {
        add(false) // Initial state 1
        add(false) // Initial state 2
    }
    private var isScreensaverActive = false
    private var currentBrightness = 0.0F // Default.
    private val idleTimeout = 10_000L
    private val idleHandler = Handler(Looper.getMainLooper())
    private val idleRunnable = Runnable {
        if (!isScreensaverActive) {
            cameraFragment.context?.let { safeContext ->
                toggleScreenSaver()
            }
        }
    }

    init {
        // PURE WHITE
        cameraFragment.fragmentCameraBinding?.screensaverEyeAndPreviewLock?.setColorFilter(
            ContextCompat.getColor(
                cameraFragment.requireContext(),
                android.R.color.white),
            android.graphics.PorterDuff.Mode.SRC_IN)
    }

    fun startAutoScreenSaver() {
        stopAutoScreenSaver()
        idleHandler.postDelayed(idleRunnable, idleTimeout)
    }

    fun stopAutoScreenSaver(){
        idleHandler.removeCallbacks(idleRunnable)
    }

    fun toggleScreenSaver(forcePreview:Boolean=false) {
         println("TOGGLED SCREENSAVER is screen saver on state: $isScreensaverActive")
        cameraFragment.fragmentCameraBinding?.let { binding ->
            if (!cameraFragment.isPreviewLocked) {
                if (!isScreensaverActive && !forcePreview) {
                    TextToSpeechHelper.speak(cameraFragment.requireContext(),"Screensaving")
                    isScreensaverActive = true
                    currentBrightness = Settings.System.getInt(
                        cameraFragment.requireContext().contentResolver, Settings.System.SCREEN_BRIGHTNESS
                    ) / 255f
                    changeScreenBrightness(0.0F)
                    binding.screensaverEyeAndPreviewLock.setVisibility(View.VISIBLE)
                    binding.previewModeOverlay.setBackgroundColor(
                        ContextCompat.getColor(
                            cameraFragment.context,
                            R.color.screensaver_color
                        ))
                    binding.tooltipTitle.setText(R.string.screensaver_mode_tooltip_title)
                    binding.tooltipDescription1.setText(R.string.screensaver_mode_tooltip_1)
                    binding.tooltipDescription2.setText(R.string.screensaver_mode_tooltip_2)
                } else {
                    TextToSpeechHelper.speak(cameraFragment.requireContext(),"Preview mode")
                    isScreensaverActive = false
                    binding.screensaverEyeAndPreviewLock.setVisibility(View.INVISIBLE)
                    changeScreenBrightness(currentBrightness)
                    binding.previewModeOverlay.setBackgroundColor(0)
                    binding.tooltipTitle.setText(R.string.preview_mode_tooltip_title)
                    binding.tooltipDescription1.setText(R.string.preview_mode_tooltip_1)
                    binding.tooltipDescription2.setText(R.string.preview_mode_tooltip_2)
                }
            }
        }
    }

    fun resetScreensaverBrightness() {
        if (isScreensaverActive) {
            changeScreenBrightness(currentBrightness)
        }
    }

    fun changeScreenBrightness(screenBrightnessValue: Float) {
        val window = cameraFragment.requireActivity().window
        val layoutParams = window.attributes
        layoutParams.screenBrightness = screenBrightnessValue
        window.attributes = layoutParams
    }

    fun cleanupScreensaverHandler() {
        idleHandler.removeCallbacksAndMessages(null)
    }
}