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
import androidx.fragment.app.Fragment
import edu.capstone.navisight.R
import edu.capstone.navisight.auth.AuthActivity
import edu.capstone.navisight.databinding.FragmentCameraBinding
import edu.capstone.navisight.viu.detectors.ObjectDetection
import edu.capstone.navisight.viu.utils.ObjectDetectorHelper
import edu.capstone.navisight.common.TTSHelper
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class GuestFragment : Fragment(R.layout.fragment_camera), ObjectDetectorHelper.DetectorListener {

    private val TAG = "ObjectDetection"

    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private val fragmentCameraBinding get() = _fragmentCameraBinding

    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private lateinit var bitmapBuffer: Bitmap
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var clickCount = 0
    private var isScreensaverActive = false
    private var currentBrightness = 0.0F

    private val idleTimeout = 10_000L
    private val idleHandler = Handler(Looper.getMainLooper())
    private val idleRunnable = Runnable {
        if (!isScreensaverActive) {
            context?.let { safeContext ->
                toggleScreenSaver(safeContext)
            }
        }
    }

    private val QUADRUPLE_TAP_TIMEOUT = 500L
    private val quadrupleTapHandler = Handler(Looper.getMainLooper())
    private val quadrupleTapRunnable = Runnable {
        clickCount = 0
    }

    private lateinit var cameraExecutor: ExecutorService

    override fun onDestroyView() {
        idleHandler.removeCallbacks(idleRunnable)
        quadrupleTapHandler.removeCallbacks(quadrupleTapRunnable)

        _fragmentCameraBinding = null
        super.onDestroyView()
        cameraExecutor.shutdown()
    }

    override fun onStop() {
        super.onStop()
        idleHandler.removeCallbacks(idleRunnable)
        quadrupleTapHandler.removeCallbacks(quadrupleTapRunnable)
    }

    @SuppressLint("MissingPermission", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _fragmentCameraBinding = FragmentCameraBinding.bind(view)

        objectDetectorHelper = ObjectDetectorHelper(
            context = requireContext(),
            objectDetectorListener = this
        )

        cameraExecutor = Executors.newSingleThreadExecutor()

        fragmentCameraBinding?.viewFinder?.post {
            setUpCamera()
        }

        toggleScreenSaver(requireContext())

        fragmentCameraBinding?.previewModeHitbox?.setOnTouchListener { _, event ->
            doAutoScreensaver()
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {

                    quadrupleTapHandler.removeCallbacks(quadrupleTapRunnable)
                    clickCount++

                    if (clickCount == 4) {
                        context?.let { safeContext ->
                            TTSHelper.speak(safeContext, "Navigating to Login Page")
                            if (isAdded) {
                                // Start activity to Login.
                                val intent = Intent(
                                    requireContext(),
                                    AuthActivity::class.java
                                )
                                startActivity(intent)
                            }
                        }
                        clickCount = 0
                    }

                    if (clickCount >= 3 && clickCount < 4) {
                        context?.let { safeContext -> toggleScreenSaver(safeContext) }
                    }

                    if (clickCount > 0 && clickCount < 4) {
                        quadrupleTapHandler.postDelayed(quadrupleTapRunnable, QUADRUPLE_TAP_TIMEOUT)
                    }
                }
            }
            true
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
                isScreensaverActive = true
                currentBrightness = Settings.System.getInt(
                    context.contentResolver, Settings.System.SCREEN_BRIGHTNESS
                ) / 255f
                changeScreenBrightness( 0.0F)
                binding.previewModeOverlay.setBackgroundColor(resources.getColor(R.color.screensaver_color))
                binding.tooltipTitle.setText(R.string.screensaver_mode_tooltip_title)
                binding.tooltipDescription1.setText(R.string.screensaver_mode_tooltip_1)
                binding.tooltipDescription2.setText(R.string.screensaver_mode_tooltip_2)
                binding.screensaverEye.setVisibility(View.VISIBLE)
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

    fun changeScreenBrightness(screenBrightnessValue: Float) {
        val window = requireActivity().window
        val layoutParams = window.attributes
        layoutParams.screenBrightness = screenBrightnessValue
        window.attributes = layoutParams
    }
}