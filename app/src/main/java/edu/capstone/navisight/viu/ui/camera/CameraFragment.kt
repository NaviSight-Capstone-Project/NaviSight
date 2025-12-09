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
import edu.capstone.navisight.common.TextToSpeechHelper
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
import edu.capstone.navisight.common.Constants.SP_IS_EMERGENCY_MODE_ACTIVE
import edu.capstone.navisight.common.Constants.SP_IS_USER_WARNED_OF_LOWBAT
import edu.capstone.navisight.common.DeveloperTools
import edu.capstone.navisight.viu.ui.braillenote.BrailleNoteFragment
import edu.capstone.navisight.viu.ui.camera.managers.BatteryHandler
import edu.capstone.navisight.viu.ui.camera.managers.CameraBindsHandler
import edu.capstone.navisight.viu.ui.camera.managers.EmergencyManager
import edu.capstone.navisight.viu.ui.camera.managers.ScreensaverHandler
import edu.capstone.navisight.viu.ui.camera.managers.WebRTCManager
import edu.capstone.navisight.viu.ui.emergency.EmergencyActivity
import edu.capstone.navisight.viu.ui.ocr.DocumentReaderFragment
import java.lang.Thread.sleep

private const val TAG = "CameraFragment"
private const val QUICK_MENU_TAG = "QuickMenu"
private const val SHARED_PREFERENCES_NAME = "NaviData"

private var HAS_BATTERY_BEEN_DETECTED_ONCE = false
private const val EMERGENCY_SYS_TAG = "EmergencySystem"

// TODO: Make this fragment's camera front facing on deployment time

class CameraFragment : Fragment(R.layout.fragment_camera),
    ObjectDetectorHelper.DetectorListener,  QuickMenuListener {

    // Init. battery receivers and related
    private lateinit var sharedPreferences: SharedPreferences // TODO: Probably remove this na
    private lateinit var batteryReceiver: BatteryStateReceiver
    private var batteryAlert: AlertDialog? = null

    // Init. WebRTC and pop-up call and quick menu action vars
    lateinit var service: MainService
    lateinit var mainRepository : MainRepository

    // Init. camera system vars
    var imageCapture: ImageCapture? = null
    var camera: Camera? = null
    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    var currentCameraFacing: Int = CameraSelector.LENS_FACING_BACK // Default to back camera
    val fragmentCameraBinding get() = _fragmentCameraBinding
    val cameraReleaseHandler = Handler(Looper.getMainLooper())
    lateinit var cameraExecutor: ExecutorService
    lateinit var objectDetectorHelper: ObjectDetectorHelper
    var preview: Preview? = null
    var imageAnalyzer: ImageAnalysis? = null
    var cameraProvider: ProcessCameraProvider? = null

    private lateinit var emergencyManager : EmergencyManager
    private lateinit var batteryHandler: BatteryHandler
    private lateinit var screensaverHandler : ScreensaverHandler
    private lateinit var cameraBindsHandler : CameraBindsHandler
    private lateinit var webRTCManager : WebRTCManager

    // Init. screensaver vars
    private var isScreensaverActive = false
    private var currentBrightness = 0.0F // Default.
    private val idleTimeout = 10_000L
    private val idleHandler = Handler(Looper.getMainLooper())
    private val idleRunnable = Runnable {
        if (!isScreensaverActive) {
            context?.let { safeContext ->
                screensaverHandler.toggleScreenSaver()
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
            emergencyManager.initiateEmergencyModeSequence() // Trigger
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
    var caregiverUid: String? = null
    private var quickMenuFragment: QuickMenuFragment? = null
    private val longPressDuration = 3_000L
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var initialDownX: Float = 0f
    private var initialDownY: Float = 0f
    private val longPressRunnable = Runnable {
        context?.let { safeContext ->
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
    var incomingMediaPlayer: MediaPlayer? = null

    // Prepare activity for re-instantiation after WebRTC
    val callActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Execute these when CallActivity finishes
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            // Check for a specific result code if needed, but RESULT_OK is usually enough
            Log.d(TAG, "CallActivity finished. Re-binding camera use cases.")
            // Reinitialize camera
            cameraBindsHandler.setUpCamera()
            screensaverHandler.doAutoScreensaver() // Re-start the screensaver timer
        } else {
            Log.d(TAG, "CallActivity finished with result code ${result.resultCode}. Re-binding camera use cases anyway.")
            // Even on failure/cancellation, the camera needs to be restored
            cameraBindsHandler.setUpCamera()
            screensaverHandler.doAutoScreensaver()
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
                webRTCManager.handleStartCall(isVideoCall=true) // Start video call
                Log.d(QUICK_MENU_TAG, "Executed: Video Calling Primary Caregiver")
            }
            R.id.ball_audio_call -> {
                webRTCManager.handleStartCall(isVideoCall=false) // Start audio call
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
                    .replace(
                        R.id.fragment_container,
                        DocumentReaderFragment())
                    .addToBackStack(null)
                    .commit()
                Log.d(QUICK_MENU_TAG, "Executed: OCR")
            }
            R.id.ball_bk_note -> {
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(
                        R.id.fragment_container,
                        BrailleNoteFragment())
                    .addToBackStack(null)
                    .commit()
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
            TextToSpeechHelper.speak(requireContext(), "Photo capture failed. Camera is not ready.")
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
                    TextToSpeechHelper.speak(requireContext(), "Photo capture failed.")
                    Toast.makeText(requireContext(), "Photo capture failed.", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture successful. Saved to Gallery."
                    Log.d(QUICK_MENU_TAG, msg)
                    TextToSpeechHelper.speak(requireContext(), msg)
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
            screensaverHandler.doAutoScreensaver() // Start screensaver by default
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
                                TextToSpeechHelper.speak(safeContext, "Navigating to Profile Page")
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
                        context?.let { safeContext -> screensaverHandler.toggleScreenSaver() }
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
                            TextToSpeechHelper.speak(safeContext, "Emergency activation cancelled.")
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

        webRTCManager.releaseCamera()
        // Unregister the listener to prevent the call dialog from showing up
        webRTCManager.disconnectMainServiceListener()
        webRTCManager.releaseMediaPlayer()
    }

    //  Rebind camera once either:
    //      No calls incoming or not about to call
    override fun onResume() {
        super.onResume()
        // Re-register as the active listener for incoming calls
        webRTCManager.connectMainServiceListener()
        // TODO: Check if camera is already permitted (very unlikely, as this app should be running na, last TODO)
        context?.let {
            fragmentCameraBinding?.viewFinder?.post {
                cameraBindsHandler.setUpCamera()
            }
        }
        batteryReceiver = batteryHandler.getBatteryReceiver()
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
        }
        context?.registerReceiver(batteryReceiver, intentFilter)
        Log.d("battery", "finished adding battery intents")

        // Check initially for battery here
        batteryHandler.checkInitialBatteryStatus()
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()
        cameraExecutor.shutdown()
        webRTCManager.disconnectMainServiceListener()
    }

    @SuppressLint("MissingPermission", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize fully sharedPreferences for battery
        sharedPreferences = requireContext().getSharedPreferences(
            SHARED_PREFERENCES_NAME,
            Context.MODE_PRIVATE)

        // Bind to make WebRTC and screensaver work_fragmentCameraBinding = FragmentCameraBinding.bind(view
        _fragmentCameraBinding = FragmentCameraBinding.bind(view)

        // Init. managers and handlers.
        emergencyManager = EmergencyManager(
            context = requireContext(),
            sharedPreferences = sharedPreferences,
            releaseCamera = { webRTCManager.releaseCamera() }, // Pass the reference to the Fragment's private function
            isAdded = isAdded,
            didEmergencySequenceComplete = false
        )

        batteryHandler = BatteryHandler(
            sharedPreferences=sharedPreferences,
            context=requireContext(),
            activity=requireActivity(),
            isAdded=isAdded
        )

        screensaverHandler = ScreensaverHandler(
            context=requireContext(),
            activity = requireActivity(),
            fragmentCameraBinding = fragmentCameraBinding
        )

        cameraBindsHandler = CameraBindsHandler(this)
        webRTCManager = WebRTCManager(this)

        // Link Main Service listener and Main Repository
        webRTCManager.connectMainServiceListener()
        service = MainService.getInstance()
        mainRepository = MainRepository.getInstance(requireContext())

        // Start camera bind with object detector
        objectDetectorHelper = ObjectDetectorHelper(
            context = requireContext(),
            objectDetectorListener = this
        )
        cameraExecutor = Executors.newSingleThreadExecutor()
        fragmentCameraBinding?.viewFinder?.post {
            cameraBindsHandler.setUpCamera()
        }

        // Bind/set extra functionalities here
        screensaverHandler.toggleScreenSaver() // Begin screen saving
        bindTouchListener() // Set and start the binding. Do not remove.
        observeCaregiverUid() // Set for calling using Quick Menu

        // Jump to emergency mode if activated on startup
        if (emergencyManager.checkIfEmergencyMode()) emergencyManager.launchEmergencyMode()
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
        webRTCManager.releaseMediaPlayer() // Set to release media player
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
            TextToSpeechHelper.speak(safeContext, "$cameraName camera activated")
        }

        // Rebind the camera use cases with the new selector
        cameraBindsHandler.bindCameraUseCases()
    }

    /////////////////////////////////////////////////////////
    // END OF MAIN APP FLOW AND CAMERA SYSTEM
    /////////////////////////////////////////////////////////
}