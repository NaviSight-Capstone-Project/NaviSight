package edu.capstone.navisight.guest

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import edu.capstone.navisight.R
import edu.capstone.navisight.common.Constants.PREF_DELEGATE
import edu.capstone.navisight.common.Constants.PREF_MAX_RESULTS
import edu.capstone.navisight.common.Constants.PREF_THREADS
import edu.capstone.navisight.common.Constants.PREF_THRESHOLD
import edu.capstone.navisight.common.Constants.GUEST_LOCAL_SETTINGS
import edu.capstone.navisight.common.TextToSpeechHelper
import edu.capstone.navisight.viu.ui.camera.CameraFragment

private const val TAG = "DetectionControlsHandler"

class GuestDetectionControlsHandler (
    private val cameraFragment : GuestFragment) {

    val fragmentCameraBinding = cameraFragment.fragmentCameraBinding
    val objectDetectorHelper = cameraFragment.objectDetectorHelper
    var isControlsVisible: Boolean = false // Track mode state

    private val detectionSharedPreferences: SharedPreferences
        get() = cameraFragment.requireContext().getSharedPreferences(
            GUEST_LOCAL_SETTINGS,
            Context.MODE_PRIVATE)

    fun synchronizeUiWithDetector() {
        Log.d(TAG, "Synchronizing UI controls with detector settings.")
        fragmentCameraBinding?.bottomSheetLayout?.maxResultsValue?.text =
            objectDetectorHelper.maxResults.toString()
        fragmentCameraBinding?.bottomSheetLayout?.thresholdValue?.text =
            String.format("%.2f", objectDetectorHelper.threshold)
        fragmentCameraBinding?.bottomSheetLayout?.threadsValue?.text =
            objectDetectorHelper.numThreads.toString()
        fragmentCameraBinding?.bottomSheetLayout?.spinnerDelegate?.setSelection(
            objectDetectorHelper.currentDelegate,
            false // Do not call onItemSelected listener
        )
        objectDetectorHelper.clearObjectDetector()
        fragmentCameraBinding?.overlay?.clear()
    }

    private fun onControlsChanged() {
        synchronizeUiWithDetector()
        saveDetectionSettings()
    }

    // Initialize all the listeners for the bottom sheet controls
    fun initControlsAndListeners() {
        // When clicked, lower detection score threshold floor
        fragmentCameraBinding?.bottomSheetLayout?.thresholdMinus?.setOnClickListener {
            TextToSpeechHelper.speak(cameraFragment.requireContext(), "Threshold decreased")
            if (objectDetectorHelper.threshold >= 0.1) {
                objectDetectorHelper.threshold -= 0.1f
                updateControlsUi()
            }
        }

        // When clicked, raise detection score threshold floor
        fragmentCameraBinding?.bottomSheetLayout?.thresholdPlus?.setOnClickListener {
            TextToSpeechHelper.speak(cameraFragment.requireContext(), "Threshold increased")
            if (objectDetectorHelper.threshold <= 0.8) {
                objectDetectorHelper.threshold += 0.1f
                updateControlsUi()
            }
        }

        // When clicked, reduce the number of objects that can be detected at a time
        fragmentCameraBinding?.bottomSheetLayout?.maxResultsMinus?.setOnClickListener {
            TextToSpeechHelper.speak(cameraFragment.requireContext(), "Max results decreased")
            if (objectDetectorHelper.maxResults > 1) {
                objectDetectorHelper.maxResults--
                updateControlsUi()
            }
        }

        // When clicked, increase the number of objects that can be detected at a time
        fragmentCameraBinding?.bottomSheetLayout?.maxResultsPlus?.setOnClickListener {
            TextToSpeechHelper.speak(cameraFragment.requireContext(), "Max results increased")
            if (objectDetectorHelper.maxResults < 5) {
                objectDetectorHelper.maxResults++
                updateControlsUi()
            }
        }

        // When clicked, decrease the number of threads used for detection
        fragmentCameraBinding?.bottomSheetLayout?.threadsMinus?.setOnClickListener {
            TextToSpeechHelper.speak(cameraFragment.requireContext(), "Thread count decreased")

            if (objectDetectorHelper.numThreads > 1) {
                objectDetectorHelper.numThreads--
                updateControlsUi()
            }
        }

        // When clicked, increase the number of threads used for detection
        fragmentCameraBinding?.bottomSheetLayout?.threadsPlus?.setOnClickListener {
            TextToSpeechHelper.speak(cameraFragment.requireContext(), "Thread count increased")
            if (objectDetectorHelper.numThreads < 4) {
                objectDetectorHelper.numThreads++
                updateControlsUi()
            }
        }

        // When clicked, change the underlying hardware used for inference. Current options are CPU
        // GPU, and NNAPI
        fragmentCameraBinding?.bottomSheetLayout?.spinnerDelegate?.setSelection(0, false)
        fragmentCameraBinding?.bottomSheetLayout?.spinnerDelegate?.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    objectDetectorHelper.currentDelegate = p2
                    TextToSpeechHelper.speak(cameraFragment.requireContext(),
                        "Hardware target changed successfully")
                    updateControlsUi()

                }
                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /* no op */
                }
            }

        // FIX: Consume touches on the bottom sheet root to prevent them from
        // propagating to the touch_interceptor_view (which closes the sheet).
        fragmentCameraBinding?.bottomSheetLayout?.bottomSheetLayout?.setOnTouchListener { _, _ ->
            true // Consumes the touch event
        }
    }

    // Update the values displayed in the bottom sheet. Reset detector.
    private fun updateControlsUi() {
        fragmentCameraBinding?.bottomSheetLayout?.maxResultsValue?.text =
            objectDetectorHelper.maxResults.toString()
        fragmentCameraBinding?.bottomSheetLayout?.thresholdValue?.text =
            String.format("%.2f", objectDetectorHelper?.threshold)
        fragmentCameraBinding?.bottomSheetLayout?.threadsValue?.text =
            objectDetectorHelper.numThreads.toString()

        // Needs to be cleared instead of reinitialized because the GPU
        // delegate needs to be initialized on the thread using it when applicable
        objectDetectorHelper.clearObjectDetector()
        fragmentCameraBinding?.overlay?.clear()
        saveDetectionSettings()
    }

    private fun saveDetectionSettings() {
        detectionSharedPreferences.edit().apply {
            putFloat(PREF_THRESHOLD, objectDetectorHelper.threshold)
            putInt(PREF_MAX_RESULTS, objectDetectorHelper.maxResults)
            putInt(PREF_THREADS, objectDetectorHelper.numThreads)
            putInt(PREF_DELEGATE, objectDetectorHelper.currentDelegate)
            apply()
        }
    }

    fun toggleBottomSheet(isVisible: Boolean) {
        val bottomSheet = fragmentCameraBinding?.bottomSheetLayout?.bottomSheetLayout ?: return
        val context = cameraFragment.context ?: return

        isControlsVisible = isVisible

        if (isVisible) {
            val slideUp = AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom)
            bottomSheet.startAnimation(slideUp)
            bottomSheet.visibility = View.VISIBLE
        } else {
            val slideDown = AnimationUtils.loadAnimation(context, R.anim.slide_out_bottom)
            bottomSheet.startAnimation(slideDown)
            // Use postDelayed to hide after the animation finishes
            slideDown.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    bottomSheet.visibility = View.GONE
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
        }
    }
}