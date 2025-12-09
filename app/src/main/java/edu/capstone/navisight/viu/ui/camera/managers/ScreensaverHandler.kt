package edu.capstone.navisight.viu.ui.camera.managers

import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import androidx.core.content.ContextCompat
import edu.capstone.navisight.R
import edu.capstone.navisight.viu.ui.camera.CameraFragment

class ScreensaverHandler (
    private val cameraFragment : CameraFragment
) {

    // Init. screensaver vars
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

    fun doAutoScreensaver() {
        idleHandler.removeCallbacks(idleRunnable)
        idleHandler.postDelayed(idleRunnable, idleTimeout)
    }

    fun toggleScreenSaver() {
        println("TOGGLED SCREENSAVER is screen saver on state: $isScreensaverActive")
        cameraFragment.fragmentCameraBinding?.let { binding ->
            if (!isScreensaverActive) {
                isScreensaverActive = true
                currentBrightness = Settings.System.getInt(
                    cameraFragment.requireContext().contentResolver, Settings.System.SCREEN_BRIGHTNESS
                ) / 255f
                binding.screensaverEye.setVisibility(View.VISIBLE)
                changeScreenBrightness(0.0F)
                binding.previewModeOverlay.setBackgroundColor(
                    ContextCompat.getColor(
                        cameraFragment.context,
                        R.color.screensaver_color
                    ))
                binding.tooltipTitle.setText(R.string.screensaver_mode_tooltip_title)
                binding.tooltipDescription1.setText(R.string.screensaver_mode_tooltip_1)
                binding.tooltipDescription2.setText(R.string.screensaver_mode_tooltip_2)
            } else {
                isScreensaverActive = false
                binding.screensaverEye.setVisibility(View.INVISIBLE)
                changeScreenBrightness(currentBrightness)
                binding.previewModeOverlay.setBackgroundColor(0)
                binding.tooltipTitle.setText(R.string.preview_mode_tooltip_title)
                binding.tooltipDescription1.setText(R.string.preview_mode_tooltip_1)
                binding.tooltipDescription2.setText(R.string.preview_mode_tooltip_2)
            }
        }
    }

    private fun changeScreenBrightness(screenBrightnessValue: Float) {
        val window = cameraFragment.requireActivity().window
        val layoutParams = window.attributes
        layoutParams.screenBrightness = screenBrightnessValue
        window.attributes = layoutParams
    }

    fun cleanupScreensaverHandler() {
        idleHandler.removeCallbacksAndMessages(null)
    }

}