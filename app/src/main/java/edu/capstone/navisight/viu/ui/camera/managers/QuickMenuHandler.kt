package edu.capstone.navisight.viu.ui.camera.managers

import android.content.ContentValues
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import edu.capstone.navisight.R
import edu.capstone.navisight.common.TextToSpeechHelper
import edu.capstone.navisight.common.VibrationHelper
import edu.capstone.navisight.viu.ui.camera.CameraFragment
import edu.capstone.navisight.viu.ui.camera.QuickMenuFragment
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Locale

private const val QUICK_MENU_TAG = "QuickMenu"

class QuickMenuHandler (
    private val cameraFragment : CameraFragment
){
     fun showQuickMenuFragment() {
        if (cameraFragment.quickMenuFragment == null) {
            cameraFragment.quickMenuFragment = QuickMenuFragment().also { fragment ->
                // Use childFragmentManager to overlay the fragment
                cameraFragment.childFragmentManager.commit {
                    setReorderingAllowed(true)
                    // Target the new container ID inside the CameraFragment's view
                    replace( R.id.quick_menu_container, fragment, "QuickMenu")
                }
            }
        }
    }


    fun observeCaregiverUid() {
        cameraFragment.viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            cameraFragment.profileViewModel.caregiverUid.collectLatest { uid ->
                // This will automatically update the local variable
                // whenever the ViewModel's StateFlow changes.
                cameraFragment.caregiverUid = uid
                Log.d(QUICK_MENU_TAG, "Caregiver UID updated in CameraFragment: $uid")
            }
        }
    }

    fun takePicture() {
        val imageCapture = cameraFragment.imageCapture ?: run {
            Log.e(QUICK_MENU_TAG, "ImageCapture use case not bound.")
            TextToSpeechHelper.speak(cameraFragment.requireContext(), "Photo capture failed. Camera is not ready.")
            return
        }

        // Create the output file options using MediaStore (Modern Android best practice)
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(
                android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/NaviSight")
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(cameraFragment.requireContext().contentResolver,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(cameraFragment.requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(QUICK_MENU_TAG, "Photo capture failed: ${exc.message}", exc)
                    TextToSpeechHelper.speak(cameraFragment.requireContext(), "Photo capture failed.")
                    Toast.makeText(cameraFragment.requireContext(), "Photo capture failed.", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture successful. Saved to Gallery."
                    Log.d(QUICK_MENU_TAG, msg)
                    TextToSpeechHelper.speak(cameraFragment.requireContext(), msg)
                    VibrationHelper.vibrate(cameraFragment.requireContext())
                }
            }
        )
    }

    fun switchCamera() {
        if (cameraFragment.cameraProvider == null) {
            Log.e(QUICK_MENU_TAG, "Cannot switch camera: CameraProvider is null.")
            return
        }

        // Toggle the camera facing state
        cameraFragment.currentCameraFacing = if (cameraFragment.currentCameraFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }

        // Announce the switch via TTS
        cameraFragment.context?.let { safeContext ->
            val cameraName = if (cameraFragment.currentCameraFacing == CameraSelector.LENS_FACING_FRONT) "Front" else "Back"
            TextToSpeechHelper.speak(safeContext, "$cameraName camera activated")
        }

        // Rebind the camera use cases with the new selector
        cameraFragment.cameraBindsHandler.bindCameraUseCases()
    }
}