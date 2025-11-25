package edu.capstone.navisight.viu.ui.camera

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import edu.capstone.navisight.R
import edu.capstone.navisight.databinding.FragmentCameraBinding
import edu.capstone.navisight.viu.detectors.ObjectDetection
import edu.capstone.navisight.viu.ui.call.ViuCallActivity
import edu.capstone.navisight.viu.ui.profile.ProfileFragment
import edu.capstone.navisight.viu.utils.ObjectDetectorHelper
import edu.capstone.navisight.common.TTSHelper
import edu.capstone.navisight.webrtc.model.DataModel
import edu.capstone.navisight.webrtc.model.DataModelType
import edu.capstone.navisight.webrtc.service.MainService
import edu.capstone.navisight.webrtc.utils.getCameraAndMicPermission
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment(R.layout.fragment_camera), ObjectDetectorHelper.DetectorListener, MainService.Listener {

    private val TAG = "CameraFragment"

    // For WebRTC popup call
    private var callRequestDialog: AlertDialog? = null

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
    private var currentBrightness = 0.0F // Default.

    private val QUADRUPLE_TAP_TIMEOUT = 500L
    private val quadrupleTapHandler = Handler(Looper.getMainLooper())
    private val quadrupleTapRunnable = Runnable {
        clickCount = 0
    }

    private val idleTimeout = 10_000L
    private val idleHandler = Handler(Looper.getMainLooper())
    private val idleRunnable = Runnable {
        if (!isScreensaverActive) {
            context?.let { safeContext ->
                toggleScreenSaver(safeContext)
            }
        }
    }

    // For ringtone
    private var incomingMediaPlayer: MediaPlayer? = null

    // Prepare activity for re-instantiation after WebRTC
    private val callActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // This callback is executed when CallActivity finishes
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            // Check for a specific result code if needed, but RESULT_OK is usually enough
            Log.d(TAG, "CallActivity finished. Re-binding camera use cases.")
            // Reinitialize camera here
            setUpCamera()
            doAutoScreensaver() // Re-start the screensaver timer
        } else {
            Log.d(TAG, "CallActivity finished with result code ${result.resultCode}. Re-binding camera use cases anyway.")
            // Even on failure/cancellation, the camera needs to be restored
            setUpCamera()
            doAutoScreensaver()
        }
    }

    // Setup onPause/onResume for WebRTC
    override fun onPause() {
        super.onPause()
        releaseCamera()
        // Unregister the listener to prevent the call dialog from showing up
        if (MainService.listener == this) {
            MainService.listener = null
        }
        releaseMediaPlayer()
    }

    //  Rebind camera once either:
    //      No calls incoming or not about to call
    override fun onResume() {
        super.onResume()
        // Re-register as the active listener for incoming calls
        MainService.listener = this
        // TODO: Check if camera is already permitted (very unlikely, as this app should be running na, last TODO)
        setUpCamera()
    }

    private lateinit var cameraExecutor: ExecutorService

    override fun onDestroyView() {
        idleHandler.removeCallbacks(idleRunnable)
        _fragmentCameraBinding = null
        super.onDestroyView()
        cameraExecutor.shutdown()
        if (MainService.listener == this) {
            MainService.listener = null
        }
    }

    override fun onStop() {
        super.onStop()
        idleHandler.removeCallbacks(idleRunnable)
    }

    @SuppressLint("MissingPermission", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Link Main Service listener
        MainService.listener = this

        // Start camera bind with object detector
        _fragmentCameraBinding = FragmentCameraBinding.bind(view)
        objectDetectorHelper = ObjectDetectorHelper(
            context = requireContext(),
            objectDetectorListener = this
        )
        cameraExecutor = Executors.newSingleThreadExecutor()
        fragmentCameraBinding?.viewFinder?.post {
            setUpCamera()
        }


        // Bind/set extra functionalities here
        toggleScreenSaver(requireContext()) // Begin screen saving
        fragmentCameraBinding?.previewModeHitbox?.setOnTouchListener { _, event ->
            doAutoScreensaver()
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    quadrupleTapHandler.removeCallbacks(quadrupleTapRunnable)
                    clickCount++
                    if (clickCount == 4) {
                        context?.let { safeContext ->
                            if (isAdded) {
                                // Start profile.
                                context?.let { safeContext ->
                                    TTSHelper.speak(safeContext, "Navigating to Profile Page")
                                    if (isAdded) {
                                        requireActivity().supportFragmentManager.commit {
                                            setReorderingAllowed(true)
                                            replace(R.id.fragment_container, ProfileFragment())
                                            addToBackStack(null)
                                        }
                                    }
                                }
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
                changeScreenBrightness(context, 0.0F)
                binding.previewModeOverlay.setBackgroundColor(resources.getColor(R.color.screensaver_color))
                binding.tooltipTitle.setText(R.string.screensaver_mode_tooltip_title)
                binding.tooltipDescription1.setText(R.string.screensaver_mode_tooltip_1)
                binding.tooltipDescription2.setText(R.string.screensaver_mode_tooltip_2)
            } else {
                isScreensaverActive = false
                changeScreenBrightness(context, currentBrightness)
                binding.previewModeOverlay.setBackgroundColor(0)
                binding.tooltipTitle.setText(R.string.preview_mode_tooltip_title)
                binding.tooltipDescription1.setText(R.string.preview_mode_tooltip_1)
                binding.tooltipDescription2.setText(R.string.preview_mode_tooltip_2)
            }
        }
    }

    fun changeScreenBrightness(context: Context, screenBrightnessValue: Float) {
        val window = requireActivity().window
        val layoutParams = window.attributes
        layoutParams.screenBrightness = screenBrightnessValue
        window.attributes = layoutParams
    }

    // WebRTC stuffs.
    override fun onCallReceived(model: DataModel) {
        // Ensure switch to the Main thread to handle UI
        activity?.runOnUiThread {
            beginCall(model)
        }
    }

    override fun onCallAborted() {
        activity?.runOnUiThread {
            Log.d("abortcall", "onCallAborted launching")

            // Check if the dialog is currently showing and dismiss it
            if (callRequestDialog?.isShowing == true) {
                callRequestDialog?.dismiss()

                // Show a toast message to the user
                Toast.makeText(
                    context,
                    "Caregiver aborted their call.",
                    Toast.LENGTH_LONG
                ).show()

                // Stop the ringing
                releaseMediaPlayer()
                Log.d(TAG, "Incoming call aborted, dialog dismissed and ringing stopped.")
            } else {
                Log.d(TAG, "Call aborted, but no dialog was active.")
            }
        }
    }

    override fun onCallMissed(senderId: String) {
        activity?.runOnUiThread {
            // Check if the dialog is currently showing and dismiss it
            if (callRequestDialog?.isShowing == true) {
                callRequestDialog?.dismiss()

                // Show a toast message to the user
                Toast.makeText(
                    context,
                    "You've missed your caregiver's call!",
                    Toast.LENGTH_LONG
                ).show()

                // Stop the ringing
                releaseMediaPlayer()
                Log.d(TAG, "Incoming call missed, dialog dismissed and ringing stopped.")
            } else {
                Log.d(TAG, "Call missed, but no dialog was active.")
            }
        }
    }


    fun beginCall(model: DataModel) {
        Log.d(TAG, "DETECTED AN INCOMING CALL: ${model.sender}")

        val isVideoCall = model.type == DataModelType.StartVideoCall
        val callMessage = "Your caregiver is requesting a call. Accept?"

        // Non-repeating
        if (callRequestDialog?.isShowing == true) {
            Log.w(TAG, "Call received but dialog is already visible. Ignoring.")
            return
        }

        try {
            // For ringtone
            incomingMediaPlayer?.stop()
            incomingMediaPlayer?.release()
            incomingMediaPlayer = null
            val notificationUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            incomingMediaPlayer = MediaPlayer.create(requireActivity().applicationContext, notificationUri)?.apply {
                isLooping = true
                start()
            }


            // Inflate the custom layout
            val inflater = requireActivity().layoutInflater
            val customLayout = inflater.inflate(R.layout.popup_call_request, null)

            // Find and set the dynamic content on the custom views
            val messageTitle = customLayout.findViewById<TextView>(R.id.messageTitle)
            val caregiverTitle = customLayout.findViewById<TextView>(R.id.caregiverTitle)
            val btnAccept = customLayout.findViewById<AppCompatImageButton>(R.id.btnAccept)
            val btnDecline = customLayout.findViewById<AppCompatImageButton>(R.id.btnDecline)
            messageTitle.text = if (isVideoCall) "Incoming Video Call" else "Incoming Audio Call"

            // Create the custom AlertDialog, injecting the view
            callRequestDialog = AlertDialog.Builder(requireActivity())
                .setView(customLayout)
                .setCancelable(false) // Force user to choose
                .create()

            callRequestDialog?.window?.setBackgroundDrawableResource(R.drawable.bg_popup_rounded)

            // CLEANUP: Stop and release when the dialog is dismissed for any reason
            callRequestDialog?.setOnDismissListener { // Use safe call
                releaseMediaPlayer()
                callRequestDialog = null // Clear the reference when dismissed
            }


            // Set up the ACCEPT button click listener
            btnAccept.setOnClickListener {
                releaseMediaPlayer() // Stop ringtone upon user action

                Log.w(TAG, "Getting reference for CallActivity...")
                val activity = requireActivity() as? AppCompatActivity
                Log.w(TAG, "Reference for CallActivity retrieved.")

                Log.w(TAG, "Requesting permissions for camera and mic...")
                (requireActivity() as AppCompatActivity).getCameraAndMicPermission {
                    // This code runs AFTER permissions are granted.
                    Log.w(TAG, "Mic and Camera permissions retrieved. Proceeding to starting activity.")
                    Log.w(TAG, "Mic and Camera permissions retrieved. Releasing camera...")
                    releaseCamera {
                        // Launch CallActivity using the launcher instead of startActivity
                        val intent = Intent(requireActivity(), ViuCallActivity::class.java).apply {
                            putExtra("target", model.sender)
                            putExtra("isVideoCall", isVideoCall)
                            putExtra("isCaller", false)
                            putExtra("sender_id", model.sender)
                        }
                        callActivityLauncher.launch(intent) // <--- IMPORTANT CHANGE HERE
                        Log.w(TAG, "Call activity started via launcher.")
                    }
                } ?: run {
                    // Handle the case where the Activity is not an AppCompatActivity (shouldn't happen)
                    Log.e(TAG, "Failed requesting camera and mic permissions for CallActivity.")
                    Toast.makeText(context, "Cannot request permissions. Activity is missing.", Toast.LENGTH_LONG).show()
                }

                callRequestDialog?.dismiss()
            }

            // Set DECLINE button click listener
            btnDecline.setOnClickListener {
                releaseMediaPlayer() // Stop ringtone upon user action
                callRequestDialog?.dismiss()
                Log.d(TAG, "Call declined by user.")
                MainService.getMainRepository()?.sendDeniedCall(model.sender)
            }

            // Show the custom dialog
            callRequestDialog?.show()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to show custom AlertDialog or play ringtone. Error: ${e.message}")
            Toast.makeText(context, "Error showing call prompt.", Toast.LENGTH_LONG).show()
            releaseMediaPlayer() // Cleanup even if an error occurs
        }
    }


    // For ringtone
    private fun releaseMediaPlayer() {
        incomingMediaPlayer?.stop()
        incomingMediaPlayer?.release()
        incomingMediaPlayer = null
    }


    // Override these lifecycle methods in your Fragment class
    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
    }

    private fun releaseCamera(onReleased: (() -> Unit)? = null) {
        try {
            cameraProvider?.unbindAll()
            camera = null
            preview = null
            imageAnalyzer = null
            Log.d(TAG, "Camera released successfully.")

            // Wait a bit for CameraX to release hardware resources
            Handler(Looper.getMainLooper()).postDelayed({
                Log.d(TAG, "Camera release delay complete. Proceeding.")
                onReleased?.invoke()
            }, 500) // 0.5 second delay (tweakable)
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing camera: ${e.message}", e)
            onReleased?.invoke()
        }
    }


}