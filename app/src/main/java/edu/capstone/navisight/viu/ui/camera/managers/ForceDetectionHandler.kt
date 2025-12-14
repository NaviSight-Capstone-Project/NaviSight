package edu.capstone.navisight.viu.ui.camera.managers

import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import edu.capstone.navisight.common.TextToSpeechHelper
import edu.capstone.navisight.common.objectdetection.DetectionPostProcessor
import edu.capstone.navisight.viu.ui.camera.CameraFragment

private const val TAG = "ForceDetectionHandler"

class ForceDetectionHandler (private val cameraFragment : CameraFragment) {

    fun forceDetection() {
        // Check if the detector is ready.
        if (cameraFragment.objectDetectorHelper == null) {
            Log.e(TAG, "ObjectDetector is null. Cannot force detection.")
            TextToSpeechHelper.speak(cameraFragment.requireContext(), "Detector is not initialized. Cannot perform immediate detection.")
            return
        }

        // Call the new helper function to capture, detect, process, and announce.
        captureAndProcessForDetection()
    }

    private fun captureAndProcessForDetection() {
        val imageCapture = cameraFragment.imageCapture ?: run {
            Log.e(TAG, "ImageCapture use case not bound.")
            TextToSpeechHelper.speak(cameraFragment.requireContext(), "Detection failed. Camera is not ready.")
            return
        }

        // Announce that capture is starting
        TextToSpeechHelper.queueSpeak(cameraFragment.requireContext(), "Capturing image for detection.")

        // Use capture method that provides an ImageProxy (in-memory) instead of saving to file
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(cameraFragment.requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture for detection failed: ${exc.message}", exc)
                    TextToSpeechHelper.speak(cameraFragment.requireContext(), "Detection failed. Photo error.")
                }

                override fun onCaptureSuccess(image: ImageProxy) {
                    // 1. Convert ImageProxy to Bitmap
                    val bitmap = image.toBitmap()

                    // 2. Perform Detection (using the returning detect overload)
                    // ASSUMPTION: cameraFragment.objectDetector is accessible and of the correct type
                    val detectionResult = cameraFragment.objectDetectorHelper?.detect(bitmap, image.imageInfo.rotationDegrees, isReturning = true) //

                    // Must close the image to avoid resource leaks
                    image.close()

                    // 3. Post-Process (Generate Narrative and Overlay Bitmap)
                    val postProcessor = DetectionPostProcessor(cameraFragment.requireContext())
                    val (imageWithOverlay, narrative) = postProcessor.processDetectionResult(detectionResult)

                    // 4. Announce Narrative
                    TextToSpeechHelper.queueSpeak(cameraFragment.requireContext(), narrative)

                    Log.d(TAG, "Detection successful. Narrative: $narrative")
                }
            }
        )
    }
}