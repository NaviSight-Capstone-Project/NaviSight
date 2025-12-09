package edu.capstone.navisight.viu.ui.camera.managers

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import android.view.Surface
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import edu.capstone.navisight.viu.ui.camera.CameraFragment
import kotlin.use

private const val TAG = "CameraBindsHandler"

class CameraBindsHandler (
    private val cameraFragment : CameraFragment,
    ){
    private lateinit var bitmapBuffer : Bitmap

    fun setUpCamera() {
        val safeContext = cameraFragment.context ?: run {
            Log.w(TAG, "setUpCamera: Context is null. Aborting.")
            return
        }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(safeContext) // Use safeContext
        cameraProviderFuture.addListener(
            {

                if (!cameraFragment.isAdded) {
                    Log.w(TAG, "CameraProvider future resolved after fragment detachment. Aborting bind.")
                    return@addListener
                }

                cameraFragment.cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(safeContext) // Use safeContext here too
        )
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun bindCameraUseCases() {
        val binding = cameraFragment.fragmentCameraBinding
        if (cameraFragment.context == null || binding == null) {
            Log.w(TAG, "bindCameraUseCases: Context or View Binding is null. Aborting.")
            return
        }
        val cameraProvider =
            cameraFragment.cameraProvider ?: throw kotlin.IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(cameraFragment.currentCameraFacing).build()

        cameraFragment.preview =
            Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(cameraFragment.fragmentCameraBinding?.viewFinder?.display?.rotation ?: Surface.ROTATION_0)
                .build()

        cameraFragment.imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetRotation(cameraFragment.fragmentCameraBinding?.viewFinder?.display?.rotation ?: Surface.ROTATION_0)
            .build()

        cameraFragment.imageAnalyzer =
            ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 640))
                .setTargetRotation(cameraFragment.fragmentCameraBinding?.viewFinder?.display?.rotation ?: Surface.ROTATION_0)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(cameraFragment.cameraExecutor) { image ->
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
            cameraFragment.camera = cameraProvider.bindToLifecycle(cameraFragment, cameraSelector,
                cameraFragment.preview, cameraFragment.imageAnalyzer, cameraFragment.imageCapture)
            cameraFragment.preview?.setSurfaceProvider(cameraFragment.fragmentCameraBinding?.viewFinder?.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
            Toast.makeText(
                cameraFragment.requireContext(),
                "Error: Cannot switch camera. Device may lack the selected camera.",
                Toast.LENGTH_LONG).show()
        }
    }

    private fun detectObjects(image: ImageProxy) {
        val plane = image.planes[0].buffer
        if (plane.remaining() >= bitmapBuffer.byteCount) {
            image.use {
                bitmapBuffer.copyPixelsFromBuffer(plane)
            }
            val imageRotation = image.imageInfo.rotationDegrees
            cameraFragment.objectDetectorHelper.detect(bitmapBuffer, imageRotation)
        } else {
            image.close()
        }
    }
}