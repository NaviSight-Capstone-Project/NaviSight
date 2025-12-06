package edu.capstone.navisight.viu.ui.camera

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.Size
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import edu.capstone.navisight.R
import edu.capstone.navisight.databinding.FragmentCameraBinding
import edu.capstone.navisight.viu.detectors.ObjectDetection
import edu.capstone.navisight.viu.ui.call.CallActivity
import edu.capstone.navisight.viu.ui.profile.ProfileFragment
import edu.capstone.navisight.viu.utils.ObjectDetectorHelper
import edu.capstone.navisight.common.TTSHelper
import edu.capstone.navisight.common.VibrationHelper
import edu.capstone.navisight.common.webrtc.model.DataModel
import edu.capstone.navisight.common.webrtc.model.DataModelType
import edu.capstone.navisight.common.webrtc.repository.MainRepository
import edu.capstone.navisight.common.webrtc.service.MainService
import edu.capstone.navisight.common.webrtc.utils.getCameraAndMicPermission
import edu.capstone.navisight.viu.data.remote.ViuDataSource
import edu.capstone.navisight.viu.ui.profile.ProfileViewModel
import edu.capstone.navisight.viu.utils.BatteryAlertListener
import edu.capstone.navisight.viu.utils.BatteryStateReceiver
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.LinkedList
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.apply
import androidx.core.content.edit
import edu.capstone.navisight.common.DeveloperTools
import edu.capstone.navisight.viu.ui.emergency.EmergencyActivity
import edu.capstone.navisight.viu.ui.ocr.DocumentReaderFragment

private const val TAG = "CameraFragment"
private const val QUICK_MENU_TAG = "QuickMenu"
private const val SHARED_PREFERENCES_NAME = "NaviData"
private const val LOW_BATTERY_WARN_FLAG_SHARED_PREF_NAME = "IsUserWarnedOnLowBatteryLevel"
private var HAS_BATTERY_BEEN_DETECTED_ONCE = false
private const val EMERGENCY_SYS_TAG = "EmergencySystem"

// TODO: Make this fragment's camera front facing on deployment time

class CameraFragment : Fragment(R.layout.fragment_camera),
    ObjectDetectorHelper.DetectorListener, MainService.Listener, QuickMenuListener,
    BatteryAlertListener {

    // Init. battery receivers and related
    private lateinit var sharedPreferences: SharedPreferences // TODO: Probably remove this na
    private lateinit var batteryReceiver: BatteryStateReceiver
    private var batteryAlert: AlertDialog? = null

    // Init. WebRTC and pop-up call and quick menu action vars
    private lateinit var service: MainService
    private var callRequestDialog: AlertDialog? = null
    private lateinit var mainRepository : MainRepository

    // Init. camera system vars
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private var currentCameraFacing: Int = CameraSelector.LENS_FACING_BACK // Default to back camera
    private val fragmentCameraBinding get() = _fragmentCameraBinding
    private val cameraReleaseHandler = Handler(Looper.getMainLooper())
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private lateinit var bitmapBuffer: Bitmap
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null

    // Init. screensaver vars
    private var isScreensaverActive = false
    private var currentBrightness = 0.0F // Default.
    private val idleTimeout = 10_000L
    private val idleHandler = Handler(Looper.getMainLooper())
    private val idleRunnable = Runnable {
        if (!isScreensaverActive) {
            context?.let { safeContext ->
                toggleScreenSaver(safeContext)
            }
        }
    }

    // Init. clickCount for quadruple tap and screensaver mode
    private var clickCount = 0

    // Init. quadruple tap to profile page
    private val QUADRUPLE_TAP_TIMEOUT = 500L
    private val quadrupleTapHandler = Handler(Looper.getMainLooper())
    private val quadrupleTapRunnable = Runnable {
        clickCount = 0
    }

    // Init. emergency mode vars
    private val volumeKeyResetHandler = Handler(Looper.getMainLooper())
    private val volumeKeyResetDelay = 500L // Time the flag remains true after ACTION_UP
    private val volumeKeyResetRunnable = Runnable {
        isVolumeKeyPressed = false // Only truly reset the flag after the delay
    }
    private var didEmergencySequenceComplete = false
    private val emergencyHoldDuration = 1500L
    private val emergencyHoldHandler = Handler(Looper.getMainLooper())
    private var isVolumeKeyPressed: Boolean = false
    private val emergencyHoldRunnable = Runnable {
        if (isAdded) {
            initiateEmergencyModeSequence() // Trigger
        }
        isVolumeKeyPressed = false // Safety reset
        volumeKeyResetHandler.removeCallbacks(volumeKeyResetRunnable) // Stop any pending reset
    }

    //  Init. variables for menu activation (long press)
    private val viuDataSource: ViuDataSource by lazy { ViuDataSource.getInstance() }
    private val profileViewModel: ProfileViewModel by activityViewModels {
        ProfileViewModel.provideFactory(
            remoteDataSource = viuDataSource
        )
    }
    private var caregiverUid: String? = null
    private var quickMenuFragment: QuickMenuFragment? = null
    private val longPressDuration = 3_000L
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var initialDownX: Float = 0f
    private var initialDownY: Float = 0f
    private val longPressRunnable = Runnable {
        context?.let { safeContext ->
            // Say time
            val currentTime = CameraTTSHelper.getCurrentDateTime()
            TTSHelper.speak(safeContext, "Quick Menu activated. Current time is: $currentTime")

            // Show the drag fragment (the drop targets)
            showQuickMenuFragment()

            fragmentCameraBinding?.quickMenuContainer?.visibility = View.VISIBLE

            // Prepare the View for the Drag Shadow
            val shadowView = View(safeContext).apply {
                // Set drag shadow size dito
                layoutParams = ViewGroup.LayoutParams(50, 50)
                setBackgroundResource(R.drawable.quick_menu_drag_shadow)
            }

            // Force the view to measure and layout to set positive dimensions
            val widthSpec = View.MeasureSpec.makeMeasureSpec(50, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(50, View.MeasureSpec.EXACTLY)
            shadowView.measure(widthSpec, heightSpec)
            shadowView.layout(0, 0, shadowView.measuredWidth, shadowView.measuredHeight)

            // Initiate the Drag and Drop operation
            val clipItem = ClipData.Item("Quick Menu Drag")
            val dragData = ClipData(
                "Quick Menu Drag",
                arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
                clipItem
            )

            // Pass view to the DragShadowBuilder
            val shadowBuilder = View.DragShadowBuilder(shadowView)

            // Nullify the listener *before* starting drag to drop touch ownership
            fragmentCameraBinding?.previewModeHitbox?.setOnTouchListener(null)

            // Start the drag operation
            fragmentCameraBinding?.previewModeHitbox?.startDragAndDrop(
                dragData,
                shadowBuilder,
                null,
                0
            )
        }
    }

    // For ringtone
    private var incomingMediaPlayer: MediaPlayer? = null

    // Prepare activity for re-instantiation after WebRTC
    private val callActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Execute these when CallActivity finishes
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            // Check for a specific result code if needed, but RESULT_OK is usually enough
            Log.d(TAG, "CallActivity finished. Re-binding camera use cases.")
            // Reinitialize camera
            setUpCamera()
            doAutoScreensaver() // Re-start the screensaver timer
        } else {
            Log.d(TAG, "CallActivity finished with result code ${result.resultCode}. Re-binding camera use cases anyway.")
            // Even on failure/cancellation, the camera needs to be restored
            setUpCamera()
            doAutoScreensaver()
        }
    }

    //////////////////////////////////////////////////
    // END OF INITIALIZATIONS
    //////////////////////////////////////////////////

    private fun showQuickMenuFragment() {
         if (quickMenuFragment == null) {
            quickMenuFragment = QuickMenuFragment().also { fragment ->
                // Use childFragmentManager to overlay the fragment
                childFragmentManager.commit {
                    setReorderingAllowed(true)
                    // Target the new container ID inside the CameraFragment's view
                    replace( R.id.quick_menu_container, fragment, "QuickMenu")
                }
            }
        }
    }

    override fun onQuickMenuAction(actionId: Int) {
        when(actionId) {
            R.id.ball_video_call -> {
                handleStartCall(isVideoCall=true) // Start video call
                Log.d(QUICK_MENU_TAG, "Executed: Video Calling Primary Caregiver")
            }
            R.id.ball_audio_call -> {
                handleStartCall(isVideoCall=false) // Start audio call
                Log.d(QUICK_MENU_TAG, "Executed: Audio Calling Primary Caregiver")
            }
            R.id.ball_snap -> {
                takePicture() // Take a picture
                Log.d(QUICK_MENU_TAG, "Executed: Quick Action #1")
            }
            R.id.ball_flip_camera -> {
                switchCamera()
                Log.d(QUICK_MENU_TAG, "Executed: Switch Camera")
            }
            R.id.ball_ocr -> {
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, DocumentReaderFragment())
                    .addToBackStack(null)
                    .commit()
                Log.d(QUICK_MENU_TAG, "Executed: OCR")
            }
            R.id.ball_bk_note -> {
                DeveloperTools.showUnderConstruction(requireContext()) // TODO
                Log.d(QUICK_MENU_TAG, "Executed: Braille Keyboard note app")
            }
        }
    }

    override fun onQuickMenuDismissed() {
        // Find and remove the fragment when the drag operation ends
        if (isAdded) {
            childFragmentManager.commit {
                quickMenuFragment?.let { remove(it) }
            }
        }
        quickMenuFragment = null

        fragmentCameraBinding?.quickMenuContainer?.visibility = View.GONE

        // Re-enable the touch listener to allow long press detection again
        bindTouchListener()
        Log.d(TAG, "Quick Menu Drag ended and fragment removed.")
    }

    private fun observeCaregiverUid() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            profileViewModel.caregiverUid.collectLatest { uid ->
                // This will automatically update the local variable
                // whenever the ViewModel's StateFlow changes.
                caregiverUid = uid
                Log.d(TAG, "Caregiver UID updated in CameraFragment: $uid")
            }
        }
    }

    private fun takePicture() {
        val imageCapture = imageCapture ?: run {
            Log.e(TAG, "ImageCapture use case not bound.")
            TTSHelper.speak(requireContext(), "Photo capture failed. Camera is not ready.")
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
            .Builder(requireContext().contentResolver,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(QUICK_MENU_TAG, "Photo capture failed: ${exc.message}", exc)
                    TTSHelper.speak(requireContext(), "Photo capture failed.")
                    Toast.makeText(requireContext(), "Photo capture failed.", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture successful. Saved to Gallery."
                    Log.d(QUICK_MENU_TAG, msg)
                    TTSHelper.speak(requireContext(), msg)
                    VibrationHelper.vibrate(requireContext())
                }
            }
        )
    }

    ////////////////////////////////////////////////////
    // END OF QUICK MENU
    ////////////////////////////////////////////////////


    // Adjust binds here
    @SuppressLint("ClickableViewAccessibility")
    private fun bindTouchListener() {
        fragmentCameraBinding?.previewModeHitbox?.apply {
            // Make the view focusable to receive key events
            isFocusableInTouchMode = true
            requestFocus()

            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    when (event.action) {
                        KeyEvent.ACTION_DOWN -> {
                            // Stop any pending reset delay
                            volumeKeyResetHandler.removeCallbacks(volumeKeyResetRunnable)
                            if (!isVolumeKeyPressed) {
                                isVolumeKeyPressed = true
                            }
                            return@setOnKeyListener true // Consume the key event
                        }
                        KeyEvent.ACTION_UP -> {
                            // USE STICKY KEYS ARGH.
                            volumeKeyResetHandler.postDelayed(
                                volumeKeyResetRunnable, volumeKeyResetDelay)
                            return@setOnKeyListener true // Consume the key event
                        }
                    }
                }
                false
            }
        }

        fragmentCameraBinding?.previewModeHitbox?.setOnTouchListener { _, event ->
            doAutoScreensaver()
            doAutoScreensaver() // Start screensaver by default
            val action = event.actionMasked

            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    quadrupleTapHandler.removeCallbacks(quadrupleTapRunnable)
                    clickCount++

                    initialDownX = event.x
                    initialDownY = event.y
                    longPressHandler.postDelayed(
                        longPressRunnable,
                        longPressDuration)


                    // Start emergency timer, else go to quick menu if volume keys not pressed
                    // whilst long pressing screen.
                    Log.d(EMERGENCY_SYS_TAG, "setOnTouchListener isVolumeKeyPressed: ${isVolumeKeyPressed}")

                    if (isVolumeKeyPressed) {
                        longPressHandler.removeCallbacks(longPressRunnable)
                        volumeKeyResetHandler.removeCallbacks(volumeKeyResetRunnable)
                        // Begin emergency process
                        emergencyHoldHandler.postDelayed(
                            emergencyHoldRunnable,
                            emergencyHoldDuration
                        )
                    } else {
                        longPressHandler.postDelayed(
                            longPressRunnable,
                            longPressDuration)
                    }
                    if (clickCount == 4) {
                        context?.let { safeContext ->
                            if (isAdded) {
                                TTSHelper.speak(safeContext, "Navigating to Profile Page")
                                if (isAdded) {
                                    requireActivity().supportFragmentManager.commit {
                                        setReorderingAllowed(true)
                                        replace(
                                            R.id.fragment_container,
                                            ProfileFragment())
                                        addToBackStack(null)
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
                        quadrupleTapHandler.postDelayed(
                            quadrupleTapRunnable, QUADRUPLE_TAP_TIMEOUT)
                    }
                    return@setOnTouchListener true // Claim touch stream
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Cancel the long press if the finger is released early
                    longPressHandler.removeCallbacks(longPressRunnable)

                    // If the screen is lifted while a Volume key is held (but before 1.5s) then
                    // cancel the timer not unless the emergency has already been triggered
                    if (isVolumeKeyPressed && !didEmergencySequenceComplete) {
                        emergencyHoldHandler.removeCallbacks(emergencyHoldRunnable)
                        context?.let { safeContext ->
                            VibrationHelper.vibrate(safeContext)
                            TTSHelper.speak(safeContext, "Emergency activation cancelled.")
                        }
                    }
                    volumeKeyResetHandler.removeCallbacks(volumeKeyResetRunnable) // HUWAG KALIMUTAN

                    // Force reset the volume key flag immediately when screen is released
                    // (the sequence is over)
                    isVolumeKeyPressed = false
                }
            }
            // Maintain touch stream ownership
            return@setOnTouchListener true
        }
    }

    //////////////////////////////////////////////
    // END OF BIND/TOUCH LISTENER
    //////////////////////////////////////////////

    // Setup onPause/onResume for WebRTC
    override fun onPause() {
        super.onPause()
        cameraReleaseHandler.removeCallbacksAndMessages(null)
        idleHandler.removeCallbacksAndMessages(null)
        longPressHandler.removeCallbacksAndMessages(null)
        quadrupleTapHandler.removeCallbacksAndMessages(null)
        context?.unregisterReceiver(batteryReceiver) // Release battery receiver
        Log.d("battery", "finished adding battery intents")

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
        context?.let {
            fragmentCameraBinding?.viewFinder?.post {
                setUpCamera()
            }
        }
        batteryReceiver = BatteryStateReceiver(this)
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
        }
        context?.registerReceiver(batteryReceiver, intentFilter)
        Log.d("battery", "finished adding battery intents")

        // Check initially for battery here
        checkInitialBatteryStatus()
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()
        cameraExecutor.shutdown()
        if (MainService.listener == this) {
            MainService.listener = null
        }
    }

    @SuppressLint("MissingPermission", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize fully sharedPreferences for battery
        sharedPreferences = requireContext().getSharedPreferences(
            SHARED_PREFERENCES_NAME,
            Context.MODE_PRIVATE)

        // Link Main Service listener and Main Repository
        MainService.listener = this
        service = MainService.getInstance()
        mainRepository = MainRepository.getInstance(requireContext())

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
        bindTouchListener() // Set and start the binding. Do not remove.
        observeCaregiverUid() // Set for calling using Quick Menu
    }

    private fun setUpCamera() {
        val safeContext = context ?: run {
            Log.w(TAG, "setUpCamera: Context is null. Aborting.")
            return
        }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(safeContext) // Use safeContext
        cameraProviderFuture.addListener(
            {

                if (!isAdded) {
                    Log.w(TAG, "CameraProvider future resolved after fragment detachment. Aborting bind.")
                    return@addListener
                }

                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(safeContext) // Use safeContext here too
        )
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        val binding = fragmentCameraBinding
        if (context == null || binding == null) {
            Log.w(TAG, "bindCameraUseCases: Context or View Binding is null. Aborting.")
            return
        }
        val cameraProvider =
            cameraProvider ?: throw kotlin.IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(currentCameraFacing).build()

        preview =
            Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding?.viewFinder?.display?.rotation ?: Surface.ROTATION_0)
                .build()

        imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
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
            camera = cameraProvider.bindToLifecycle(this, cameraSelector,
                preview, imageAnalyzer, imageCapture)
            preview?.setSurfaceProvider(fragmentCameraBinding?.viewFinder?.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
            Toast.makeText(
                requireContext(),
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

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer() // Set to release media player
    }

    fun switchCamera() {
        if (cameraProvider == null) {
            Log.e(TAG, "Cannot switch camera: CameraProvider is null.")
            return
        }

        // Toggle the camera facing state
        currentCameraFacing = if (currentCameraFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }

        // Announce the switch via TTS
        context?.let { safeContext ->
            val cameraName = if (currentCameraFacing == CameraSelector.LENS_FACING_FRONT) "Front" else "Back"
            TTSHelper.speak(safeContext, "$cameraName camera activated")
        }

        // Rebind the camera use cases with the new selector
        bindCameraUseCases()
    }

    /////////////////////////////////////////////////////////
    // END OF MAIN APP FLOW AND CAMERA SYSTEM
    /////////////////////////////////////////////////////////

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
                binding.screensaverEye.setVisibility(View.VISIBLE)
                changeScreenBrightness(0.0F)
                binding.previewModeOverlay.setBackgroundColor(resources.getColor(R.color.screensaver_color))
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

    fun changeScreenBrightness(screenBrightnessValue: Float) {
        val window = requireActivity().window
        val layoutParams = window.attributes
        layoutParams.screenBrightness = screenBrightnessValue
        window.attributes = layoutParams
    }

    ///////////////////////////////////////////////////
    //  END OF SCREEN SAVER FLOW
    //////////////////////////////////////////////////

    private fun handleStartCall(isVideoCall: Boolean) {
        val targetUid = caregiverUid // Get the UID from the observed StateFlow

        if (targetUid.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Caregiver not found. Cannot start call.", Toast.LENGTH_LONG).show()
            Log.e("CallSignal", "Caregiver UID is null or empty.")
            return
        }

        (requireActivity() as AppCompatActivity).getCameraAndMicPermission {
            mainRepository.sendConnectionRequest(targetUid, isVideoCall) { success ->
                if (success) {
                    service.startCallTimeoutTimer()
                    val intent = Intent(requireActivity(), CallActivity::class.java).apply {
                        putExtra("target", targetUid)
                        putExtra("isVideoCall", isVideoCall)
                        putExtra("isCaller", true)
                    }
                    callActivityLauncher.launch(intent)
                } else {
                    Log.e("CallSignal", "Failed to send call request.")
                    Toast.makeText(requireContext(), "Failed to send call request.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

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
            val btnAccept = customLayout.findViewById<AppCompatImageButton>(R.id.btnAccept)
            val btnDecline = customLayout.findViewById<AppCompatImageButton>(R.id.btnDecline)
            messageTitle.text = if (isVideoCall) "Incoming Video Call" else "Incoming Audio Call"

            // Create the custom AlertDialog, injecting the view
            callRequestDialog = AlertDialog.Builder(requireActivity())
                .setView(customLayout)
                .setCancelable(false) // Force user to choose
                .create()

            callRequestDialog?.window?.setBackgroundDrawableResource(R.drawable.bg_popup_rounded)

            // Stop and release when the dialog is dismissed for any reason
            callRequestDialog?.setOnDismissListener { // Use safe call
                releaseMediaPlayer()
                callRequestDialog = null // Clear the reference when dismissed
            }


            // Set up the ACCEPT button click listener
            btnAccept.setOnClickListener {
                releaseMediaPlayer() // Stop ringtone upon user action

                // Stop timer on end/miss call timeout
                service.stopCallTimeoutTimer()

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
                        val intent = Intent(requireActivity(), CallActivity::class.java).apply {
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


    // Release media player if needed
    private fun releaseMediaPlayer() {
        incomingMediaPlayer?.stop()
        incomingMediaPlayer?.release()
        incomingMediaPlayer = null
    }

    private fun releaseCamera(onReleased: (() -> Unit)? = null) {
        try {
            cameraProvider?.unbindAll()
            camera = null
            preview = null
            imageAnalyzer = null
            Log.d(TAG, "Camera released successfully.")

            // Wait a bit for CameraX to release hardware resources
            cameraReleaseHandler.postDelayed({
                Log.d(TAG, "Camera release delay complete. Proceeding.")
                if (isAdded) {
                    onReleased?.invoke()
                } else {
                    Log.w(TAG, "Fragment detached before camera release callback completed. Aborting onReleased.")
                }
            }, 500) // 0.5 second delay (tweakable)
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing camera: ${e.message}", e)
            onReleased?.invoke()
        }
    }


    //////////////////////////////////////////////////
    //  END OF WEBRTC AND POP-UP CALLING
    //////////////////////////////////////////////////

    override fun onBatteryLow() {
        if (sharedPreferences.getBoolean(
                LOW_BATTERY_WARN_FLAG_SHARED_PREF_NAME,
                false) || !HAS_BATTERY_BEEN_DETECTED_ONCE) {
            activity?.runOnUiThread {
                showLowBatteryAlert()
                saveBatteryWarnFlag()
                HAS_BATTERY_BEEN_DETECTED_ONCE = true
            }
        }
    }

    override fun onBatteryOkay() {
        activity?.runOnUiThread {
            if (batteryAlert?.isShowing == true) {
                batteryAlert?.dismiss()
                batteryAlert = null
                removeBatteryWarnFlag()
            }
        }
    }

    private fun showLowBatteryAlert() {
        if (batteryAlert?.isShowing == true || !isAdded) {
            Log.w(TAG, "Battery alert already visible or fragment is not added. Ignoring.")
            return
        }

        TTSHelper.speak(requireContext(), "Warning! Battery is low. Please charge your device.")

        try {
            val inflater = requireActivity().layoutInflater
            val customLayout = inflater.inflate(R.layout.dialog_battery_alert, null)
            val btnAccept = customLayout.findViewById<Button>(R.id.ok)
            batteryAlert = AlertDialog.Builder(requireActivity())
                .setView(customLayout)
                .setCancelable(true)
                .create()

            // Dismiss the dialog when the user taps any part of the custom layout's background
            customLayout.setOnClickListener {
                batteryAlert?.dismiss()
            }
            batteryAlert?.setCanceledOnTouchOutside(true)
            batteryAlert?.window?.setBackgroundDrawableResource(R.drawable.bg_popup_rounded)

            // Dismiss action for both buttons
            val dismissAction: () -> Unit = {
                batteryAlert?.dismiss()
                batteryAlert = null
            }
            btnAccept.setOnClickListener { dismissAction() }
            batteryAlert?.show()

        } catch (e: Exception) {
            Log.e(TAG, "Error showing battery alert: ${e.message}", e)
            Toast.makeText(context, "Error showing battery alert.", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkInitialBatteryStatus() {
        val context = context ?: return

        // Get the sticky Intent that holds the current battery state
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let {
            filter -> context.registerReceiver(null, filter)
        }

        // Extract the percent and the scale (max level, always 100)
        val level: Int = batteryStatus?.getIntExtra(
            android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(
            android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1

        // Calculate the percentage
        val batteryPct: Float = if (level != -1 && scale != -1 && scale != 0) {
            level / scale.toFloat() * 100
        } else {
            0f
        }

        // Define the low battery threshold (default is 15)
        // TODO: Unify thresholds in a combined file?
        val lowThreshold = 15

        // Check if the battery percentage is below the threshold
        if (batteryPct <= lowThreshold) {
            Log.w(TAG, "Initial battery check found battery at $batteryPct%. Triggering alert.")
            activity?.runOnUiThread {
                onBatteryLow()
            }
        } else {
            Log.d(TAG, "Initial battery check found battery at $batteryPct%. Status is OK.")
            activity?.runOnUiThread {
                if (batteryAlert?.isShowing == true) {
                    onBatteryOkay()
                }
            }
        }
    }

    private fun saveBatteryWarnFlag() {
        sharedPreferences.edit { putBoolean(LOW_BATTERY_WARN_FLAG_SHARED_PREF_NAME, true) }
    }

    private fun removeBatteryWarnFlag() {
        sharedPreferences.edit { putBoolean(LOW_BATTERY_WARN_FLAG_SHARED_PREF_NAME, false) }
    }

    //////////////////////////
    // END OF BATTERY ALERT
    /////////////////////////

    private fun initiateEmergencyModeSequence() {
        // Todo: FIX TIMING, PERHAPS MAKE DEDICATED queueSpeakEmergency (then combine with vibration)
        VibrationHelper.vibrate(requireContext())
        TTSHelper.queueSpeak(requireContext(), "Emergency mode initiating now." +
                "Please hold for 3 seconds to continue.")
        TTSHelper.queueSpeak(requireContext(), "Three")
        VibrationHelper.vibrateAfterDelay(requireContext())
        TTSHelper.queueSpeak(requireContext(), "Two")
        VibrationHelper.vibrateAfterDelay(requireContext())
        TTSHelper.queueSpeak(requireContext(), "One")
        VibrationHelper.vibrateAfterDelay(requireContext())
        didEmergencySequenceComplete = true // Set flag TODO: DO NOT FORGET TO ADD FALSE ONCE EMERGENCY IS COMPLETE
        releaseCamera(onReleased = {
            if (isAdded) {
                val intent = Intent(requireContext(), EmergencyActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                // Do not remove immediately the camera, just in case of false alarm
            }
        })
    }

    /////////////////////////////////////
    // END OF EMERGENCY MODE INITIATION
    /////////////////////////////////////
}