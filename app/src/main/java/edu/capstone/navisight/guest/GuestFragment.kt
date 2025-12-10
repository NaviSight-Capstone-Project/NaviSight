package edu.capstone.navisight.guest

/*

GuestFragment.kt

This fragment is used primarily as a placeholder for non-registered users.
This fragment must default to object detection - camera mode.
This fragment will and only will be used if there is NO user logged in.

 */

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.Size
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.fragment.app.Fragment
import edu.capstone.navisight.R
import edu.capstone.navisight.auth.AuthActivity
import edu.capstone.navisight.viu.detectors.ObjectDetection
import edu.capstone.navisight.viu.utils.ObjectDetectorHelper
import edu.capstone.navisight.common.TextToSpeechHelper
import edu.capstone.navisight.databinding.FragmentGuestBinding
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

// TODO: Make this fragment's camera front facing on deployment time

class GuestFragment : Fragment(R.layout.fragment_guest), ObjectDetectorHelper.DetectorListener {

    private val TAG = "ObjectDetection"

    private var fragmentGuestBinding: FragmentGuestBinding? = null
    private val fragmentCameraBinding get() = fragmentGuestBinding

    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private lateinit var bitmapBuffer: Bitmap
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var isScreensaverActive = false
    private var currentBrightness = 0.0F

    // --- Crash Prevention: Timer to stop TTS from spamming ---
    private var lastAnnouncementTime = 0L
    private val announcementDelay = 2000L // 2 seconds

    private val idleTimeout = 10_000L
    private val idleHandler = Handler(Looper.getMainLooper())
    private val idleRunnable = Runnable {
        if (!isScreensaverActive) {
            context?.let { safeContext ->
                toggleScreenSaver(safeContext)
            }
        }
    }

    private lateinit var cameraExecutor: ExecutorService

    override fun onDestroyView() {
        idleHandler.removeCallbacks(idleRunnable)
        fragmentGuestBinding = null
        super.onDestroyView()
        cameraExecutor.shutdown()
    }

    override fun onStop() {
        super.onStop()
        idleHandler.removeCallbacks(idleRunnable)
    }

    @SuppressLint("MissingPermission", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentGuestBinding = FragmentGuestBinding.bind(view)

        objectDetectorHelper = ObjectDetectorHelper(
            context = requireContext(),
            objectDetectorListener = this
        )

        cameraExecutor = Executors.newSingleThreadExecutor()

        fragmentCameraBinding?.viewFinder?.post {
            setUpCamera()
        }

        toggleScreenSaver(requireContext())

        //  Accessibility Action
        fragmentCameraBinding?.previewModeHitbox?.let { viewHitbox ->
            ViewCompat.setAccessibilityDelegate(viewHitbox, object : androidx.core.view.AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    // Login to the TalkBack actions menu
                    val action = AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_CLICK + 1, "Login"
                    )
                    info.addAction(action)
                }

                override fun performAccessibilityAction(host: View, action: Int, args: Bundle?): Boolean {
                    if (action == AccessibilityNodeInfoCompat.ACTION_CLICK + 1) {
                        navigateToLogin()
                        return true
                    }
                    return super.performAccessibilityAction(host, action, args)
                }
            })
        }

        // Gesture Detector
        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 != null && e1.x - e2.x > 100 && abs(velocityX) > 100) {
                    navigateToLogin()
                    return true
                }
                return false
            }
        })

        // Click Listener
        fragmentCameraBinding?.previewModeHitbox?.setOnClickListener {
            context?.let { safeContext -> toggleScreenSaver(safeContext) }
        }

        //Touch Listener
        fragmentCameraBinding?.previewModeHitbox?.setOnTouchListener { _, event ->
            doAutoScreensaver()
            gestureDetector.onTouchEvent(event)
        }
    }

    private fun navigateToLogin() {
        context?.let { safeContext ->
            TextToSpeechHelper.speak(safeContext, "Navigating to Login Page")
            if (isAdded) {
                val intent = Intent(requireContext(), AuthActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        val cameraProvider =
            cameraProvider ?: throw kotlin.IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        preview =
            Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding?.viewFinder?.display?.rotation ?: Surface.ROTATION_0)
                .build()

        imageAnalyzer =
            ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 640))
                .setTargetRotation(fragmentCameraBinding?.viewFinder?.display?.rotation ?: Surface.ROTATION_0)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        if (!::bitmapBuffer.isInitialized) {
                            bitmapBuffer = Bitmap.createBitmap(
                                image.width,
                                image.height,
                                Bitmap.Config.ARGB_8888
                            )
                        }
                        detectObjects(image)
                    }
                }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            preview?.setSurfaceProvider(fragmentCameraBinding?.viewFinder?.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectObjects(image: ImageProxy) {
        val plane = image.planes[0].buffer
        if (plane.remaining() >= bitmapBuffer.byteCount) {
            image.use {
                bitmapBuffer.copyPixelsFromBuffer(plane)
            }
            val imageRotation = image.imageInfo.rotationDegrees
            objectDetectorHelper.detect(bitmapBuffer, imageRotation)
        } else {
            image.close()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation =
            fragmentCameraBinding?.viewFinder?.display?.rotation ?: Surface.ROTATION_0
    }

    override fun onResults(
        results: List<ObjectDetection>,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        activity?.runOnUiThread {
            fragmentCameraBinding?.overlay?.setResults(
                results ?: LinkedList(),
                imageHeight,
                imageWidth
            )
            fragmentCameraBinding?.overlay?.invalidate()


            if (!isScreensaverActive && results.isNotEmpty()) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastAnnouncementTime > announcementDelay) {

                    lastAnnouncementTime = currentTime
                }
            }
        }
    }

    override fun onError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(context ?: return@runOnUiThread, error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun doAutoScreensaver() {
        idleHandler.removeCallbacks(idleRunnable)
        idleHandler.postDelayed(idleRunnable, idleTimeout)
    }

    private fun toggleScreenSaver(context: Context) {
        fragmentCameraBinding?.let { binding ->
            if (!isScreensaverActive) {
                // --- SCREENSAVER MODE ---
                isScreensaverActive = true
                currentBrightness = Settings.System.getInt(
                    context.contentResolver, Settings.System.SCREEN_BRIGHTNESS
                ) / 255f
                changeScreenBrightness( 0.0F)
                binding.previewModeOverlay.setBackgroundColor(resources.getColor(R.color.screensaver_color))

                binding.tooltipTitle.setText(R.string.screensaver_mode_tooltip_title)
                binding.tooltipDescription1.text = getString(R.string.screensaver_mode_tooltip_1) + "\n" + getString(R.string.screensaver_mode_tooltip_3) + "\n"
                binding.tooltipDescription2.setText(R.string.guest_mode_tooltip_1)
                binding.screensaverEye.setVisibility(View.VISIBLE)

                // TalkBack Description (Fixes "Unlabeled")
                val desc = getString(R.string.screensaver_mode_tooltip_title) + ". " +
                        getString(R.string.screensaver_mode_tooltip_1) + ". " +
                        getString(R.string.guest_mode_tooltip_1) + ". Double tap to open preview."
                binding.previewModeHitbox.contentDescription = desc

            } else {
                // --- PREVIEW MODE ---
                isScreensaverActive = false
                binding.screensaverEye.setVisibility(View.INVISIBLE)
                changeScreenBrightness(currentBrightness)
                binding.previewModeOverlay.setBackgroundColor(0)

                binding.tooltipTitle.setText(R.string.preview_mode_tooltip_title)
                binding.tooltipDescription1.text = getString(R.string.preview_mode_tooltip_1) + "\n" + getString(R.string.preview_mode_tooltip_2)
                binding.tooltipDescription2.setText(R.string.guest_mode_tooltip_1)

                // TalkBack Description (Fixes "Unlabeled")
                val desc = getString(R.string.preview_mode_tooltip_title) + ". " +
                        getString(R.string.preview_mode_tooltip_1) + ". " +
                        getString(R.string.guest_mode_tooltip_1) + ". Double tap to close preview. Two finger swipe left to login."
                binding.previewModeHitbox.contentDescription = desc

                TextToSpeechHelper.speak(context, "Preview active")
            }
        }
    }

    fun changeScreenBrightness(screenBrightnessValue: Float) {
        val window = requireActivity().window
        val layoutParams = window.attributes
        layoutParams.screenBrightness = screenBrightnessValue
        window.attributes = layoutParams
    }
}