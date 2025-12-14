package edu.capstone.navisight.viu.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import edu.capstone.navisight.R
import edu.capstone.navisight.common.Constants.PREF_DELEGATE
import edu.capstone.navisight.common.Constants.PREF_MAX_RESULTS
import edu.capstone.navisight.common.Constants.PREF_THREADS
import edu.capstone.navisight.common.Constants.PREF_THRESHOLD
import edu.capstone.navisight.common.Constants.VIBRATE_SUCCESS
import edu.capstone.navisight.common.Constants.VIU_LOCAL_SETTINGS
import edu.capstone.navisight.databinding.FragmentCameraBinding
import edu.capstone.navisight.viu.detectors.ObjectDetection
import edu.capstone.navisight.viu.ui.profile.ProfileFragment
import edu.capstone.navisight.common.objectdetection.ObjectDetector
import edu.capstone.navisight.common.TextToSpeechHelper
import edu.capstone.navisight.common.VibrationHelper
import edu.capstone.navisight.common.webrtc.repository.MainRepository
import edu.capstone.navisight.common.webrtc.service.MainService
import edu.capstone.navisight.viu.ViuHomeViewModel
import edu.capstone.navisight.viu.data.remote.ViuDataSource
import edu.capstone.navisight.viu.ui.profile.ProfileViewModel
import edu.capstone.navisight.viu.utils.BatteryStateReceiver
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.apply
import edu.capstone.navisight.viu.ui.braillenote.BrailleNoteFragment
import edu.capstone.navisight.viu.ui.camera.managers.BatteryHandler
import edu.capstone.navisight.viu.ui.camera.managers.CameraBindsHandler
import edu.capstone.navisight.viu.ui.camera.managers.DetectionControlsHandler
import edu.capstone.navisight.viu.ui.camera.managers.EmergencyManager
import edu.capstone.navisight.viu.ui.camera.managers.ForceDetectionHandler
import edu.capstone.navisight.viu.ui.camera.managers.QuickMenuHandler
import edu.capstone.navisight.viu.ui.camera.managers.ScreensaverHandler
import edu.capstone.navisight.viu.ui.camera.managers.WebRTCManager
import edu.capstone.navisight.viu.ui.ocr.DocumentReaderFragment
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val TAG = "CameraFragment"
private const val QUICK_MENU_TAG = "QuickMenu"
private const val SHARED_PREFERENCES_NAME = "NaviData"
private const val EMERGENCY_SYS_TAG = "EmergencySystem"

class CameraFragment (private val realTimeViewModel : ViuHomeViewModel):
    Fragment(R.layout.fragment_camera),
    ObjectDetector.DetectorListener,  QuickMenuListener, SensorEventListener {

    // Init. battery receivers and related
    lateinit var sharedPreferences: SharedPreferences
    private lateinit var batteryReceiver: BatteryStateReceiver
    var batteryAlert: AlertDialog? = null

    lateinit var automaticFlashView: ImageView
    lateinit var forceDetectionView: ImageView
    var isAutomaticFlashViewOn = true

    // Init. WebRTC and pop-up call and quick menu action vars
    lateinit var service: MainService
    lateinit var mainRepository : MainRepository

    var isPreviewLocked = false
    var isTTSSilenced = false
    var isAutomaticFlashOn = false

    // Auto flash
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private val LIGHT_THRESHOLD = 5.0f // Example: in lux (5 lux is quite dim, like a dark room)

    // Flag to ensure we only turn the flashlight on/off once per state change
    private var isFlashlightActive = false

    // Init. camera system vars
    var imageCapture: ImageCapture? = null
    var camera: Camera? = null
    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    var currentCameraFacing: Int = CameraSelector.LENS_FACING_BACK
    val fragmentCameraBinding get() = _fragmentCameraBinding
    val cameraReleaseHandler = Handler(Looper.getMainLooper())
    lateinit var cameraExecutor: ExecutorService
    lateinit var objectDetectorHelper: ObjectDetector
    var preview: Preview? = null
    var imageAnalyzer: ImageAnalysis? = null
    var cameraProvider: ProcessCameraProvider? = null

    // Detection settings
    lateinit var detectionSharedPreferences: SharedPreferences

    private lateinit var emergencyManager : EmergencyManager
    private lateinit var forceDetectionHandler: ForceDetectionHandler
    private lateinit var batteryHandler: BatteryHandler
    lateinit var screensaverHandler : ScreensaverHandler
    lateinit var cameraBindsHandler : CameraBindsHandler
    private lateinit var quickMenuHandler : QuickMenuHandler
    private lateinit var webRTCManager : WebRTCManager
    private lateinit var detectionControlsHandler : DetectionControlsHandler
    var isDetectionUiModeActive: Boolean = false

    // Init. screensaver vars
    private val idleHandler = Handler(Looper.getMainLooper())
    private lateinit var gestureDetector: GestureDetectorCompat

    // Init. emergency mode vars
    private val volumeKeyResetHandler = Handler(Looper.getMainLooper())
    private val volumeKeyResetDelay = 500L
    private val volumeKeyResetRunnable = Runnable {
        isVolumeKeyPressed = false
    }
    var didEmergencySequenceComplete = false
    private val emergencyHoldDuration = 1500L
    private val emergencyHoldHandler = Handler(Looper.getMainLooper())
    private var isVolumeKeyPressed: Boolean = false
    private val emergencyHoldRunnable = Runnable {
        if (isAdded) {
            emergencyManager.initiateEmergencyModeSequence()
        }
        isVolumeKeyPressed = false
        volumeKeyResetHandler.removeCallbacks(volumeKeyResetRunnable)
    }

    //  Init. variables for menu activation
    private val viuDataSource: ViuDataSource by lazy { ViuDataSource.getInstance() }
    val profileViewModel: ProfileViewModel by activityViewModels {
        ProfileViewModel.provideFactory(
            remoteDataSource = viuDataSource
        )
    }
    var caregiverUid: String? = null
    var quickMenuFragment: QuickMenuFragment? = null

    // For ringtone
    var incomingMediaPlayer: MediaPlayer? = null

    // Prepare activity for re-instantiation after WebRTC
    val callActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            Log.d(TAG, "CallActivity finished. Re-binding camera use cases.")
            cameraBindsHandler.setUpCamera()
            screensaverHandler.startAutoScreenSaver()
        } else {
            Log.d(TAG, "CallActivity finished with result code ${result.resultCode}. Re-binding camera use cases anyway.")
            cameraBindsHandler.setUpCamera()
            screensaverHandler.startAutoScreenSaver()
        }
    }


    fun turnFlashlightOn() {
        // Check if the CameraX instance is ready and the torch is not already on
        if (camera != null && !isFlashlightActive) {
            // CameraX control to enable torch
            camera?.cameraControl?.enableTorch(true)
            isFlashlightActive = true
            Log.d("FLASHLIGHT", "Flashlight ON via Ambient Light Sensor")
        }
    }

    fun turnFlashlightOff() {
        // Check if the CameraX instance is ready and the torch is currently on
        if (camera != null && isFlashlightActive) {
            // CameraX control to disable torch
            camera?.cameraControl?.enableTorch(false)
            isFlashlightActive = false
            Log.d("FLASHLIGHT", "Flashlight OFF via Ambient Light Sensor")
        }
    }

    //////////////////////////////////////////////////
    // END OF INITIALIZATIONS
    //////////////////////////////////////////////////

    override fun onQuickMenuAction(actionId: Int) {
        when(actionId) {
            R.id.ball_video_call -> {
                TextToSpeechHelper.speak(requireContext(), "Video calling your caregiver")
                webRTCManager.handleStartCall(isVideoCall=true)
                Log.d(QUICK_MENU_TAG, "Executed: Video Calling Primary Caregiver")
            }
            R.id.ball_audio_call -> {
                TextToSpeechHelper.speak(requireContext(), "Audio calling your caregiver")
                webRTCManager.handleStartCall(isVideoCall=false)
                Log.d(QUICK_MENU_TAG, "Executed: Audio Calling Primary Caregiver")
            }
            R.id.ball_snap -> {
                quickMenuHandler.takePicture()
                Log.d(QUICK_MENU_TAG, "Executed: Quick Action #1")
            }
            R.id.ball_flip_camera -> {
                quickMenuHandler.switchCamera()
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
                onQuickMenuDismissed() // force quit
            }
            R.id.ball_bk_note -> {
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(
                        R.id.fragment_container,
                        BrailleNoteFragment())
                    .addToBackStack(null)
                    .commit()
                Log.d(QUICK_MENU_TAG, "Executed: Braille Keyboard note app")
                onQuickMenuDismissed() // force quit
            }
            R.id.ball_screensaver_lock -> {
                quickMenuHandler.lockPreviewMode()
            }
            R.id.ball_toggle_TTS_temporarily -> {
                quickMenuHandler.toggleTTSTemporarily()
            }
        }
    }

    override fun onQuickMenuDismissed() {
        if (isAdded) {
            childFragmentManager.commit {
                quickMenuFragment?.let { remove(it) }
            }
        }
        quickMenuFragment = null
        toggleAutomaticFlashView()
        fragmentCameraBinding?.quickMenuContainer?.visibility = View.GONE

        // Ensure input is still listening
        setupInputListeners()
        Log.d(TAG, "Quick Menu Drag ended and fragment removed.")
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupInputListeners() {
        val touchInterceptor = fragmentCameraBinding?.touchInterceptorView ?: return

        // Initialize Gesture Detector for Swipes (Profile Navigation)
        gestureDetector = GestureDetectorCompat(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onDown(e: MotionEvent): Boolean {
                // Return false so we don't consume the click, allowing onClickListener to fire
                return false
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                // Check for Slide Left
                if (e1 != null && e1.x - e2.x > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    navigateToProfile() // Navigate to profile
                    return true
                }
                // Check for Swipe Up
                if (e1 != null && e1.y - e2.y > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    Log.d(TAG, "Swipe Up Detected: Executing action.")
                    toggleDetectionUiMode(true)
                    return true
                }
                return false
            }
        })

        touchInterceptor.apply {
            isFocusableInTouchMode = true
            requestFocus()

            // Set Accessibility Delegate for "TalkBack Slide" (Optional: Adds "Navigate Profile" to menu)
            // But relies on GestureDetector for 2-finger swipe in TalkBack

            // KEY LISTENER: Volume Buttons for Emergency
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    when (event.action) {
                        KeyEvent.ACTION_DOWN -> {
                            volumeKeyResetHandler.removeCallbacks(volumeKeyResetRunnable)
                            if (!isVolumeKeyPressed) {
                                isVolumeKeyPressed = true
                                // If holding volume, we might be starting emergency
                                checkEmergencyStart()
                            }
                            return@setOnKeyListener true
                        }
                        KeyEvent.ACTION_UP -> {
                            volumeKeyResetHandler.postDelayed(
                                volumeKeyResetRunnable, volumeKeyResetDelay)

                            // If released, cancel emergency if not yet complete
                            if (!didEmergencySequenceComplete) {
                                emergencyHoldHandler.removeCallbacks(emergencyHoldRunnable)
                            }
                            return@setOnKeyListener true
                        }
                    }
                }
                false
            }

            // CLICK LISTENER: Screensaver Toggle
            // Standard: 1 Tap | TalkBack: Double Tap
            setOnClickListener {
                if (isDetectionUiModeActive) {
                    toggleDetectionUiMode(false) // Tap outside bottom sheet to exit
                } else {
                    screensaverHandler.toggleScreenSaver()
                    screensaverHandler.startAutoScreenSaver()
                }
            }

            // LONG CLICK LISTENER: Quick Menu
            // Standard: Hold | TalkBack: Double Tap & Hold
            setOnLongClickListener {
                // If Volume Key is held, do NOT open menu (Priority to Emergency)
                if (isVolumeKeyPressed) return@setOnLongClickListener true
                startQuickMenuDrag(it)
                return@setOnLongClickListener true // Consumed
            }

            //  TOUCH LISTENER: Feeds the Gesture Detector
            setOnTouchListener { _, event ->
                screensaverHandler.startAutoScreenSaver() // Reset screensaver timer on any interaction

                // Pass event to GestureDetector (Handles Slide Left)
                if (gestureDetector.onTouchEvent(event)) {
                    return@setOnTouchListener true // Consumed by Swipe
                }

                // Let the event bubble up to trigger onClick or onLongClick if not a swipe
                return@setOnTouchListener false
            }
        }
    }

    fun toggleDetectionUiMode(enable: Boolean) {
        if (!isAdded) return
        if (enable == isDetectionUiModeActive) return // Avoid redundant toggles
        isDetectionUiModeActive = enable

        if (enable) {
            TextToSpeechHelper.speak(
                requireContext(),
                "Object detection settings opened")
            VibrationHelper.vibratePattern(requireContext(), VIBRATE_SUCCESS)
            screensaverHandler.resetScreensaverBrightness()
            fragmentCameraBinding?.previewModeOverlay?.visibility = View.INVISIBLE
            detectionControlsHandler.toggleBottomSheet(true)
            fragmentCameraBinding?.touchInterceptorView?.setOnLongClickListener(null)
        } else {
            screensaverHandler.startAutoScreenSaver()
            TextToSpeechHelper.speak(
                requireContext(),
                "Object detection settings closed")
            VibrationHelper.vibratePattern(requireContext(), VIBRATE_SUCCESS)
            detectionControlsHandler.toggleBottomSheet(false) // Close
            fragmentCameraBinding?.previewModeOverlay?.visibility = View.VISIBLE
            fragmentCameraBinding?.touchInterceptorView?.setOnLongClickListener {
                // If Volume Key is held, or Detection UI is active, do NOT open menu
                if (isVolumeKeyPressed || isDetectionUiModeActive)
                    return@setOnLongClickListener true
                startQuickMenuDrag(it)
                return@setOnLongClickListener true // Consumed
            }
        }
    }

    private fun checkEmergencyStart() {
        // If volume key is pressed, start emergency timer immediately
        // Note: Logic simplified from original to rely on KeyDown events
        emergencyHoldHandler.removeCallbacks(emergencyHoldRunnable)
        emergencyHoldHandler.postDelayed(emergencyHoldRunnable, emergencyHoldDuration)
    }

    private fun navigateToProfile() {
        screensaverHandler.resetScreensaverBrightness()
        context?.let { safeContext ->
            if (isAdded) {
                TextToSpeechHelper.clearQueue() // Clear
                TextToSpeechHelper.speak(safeContext, "Navigating to Profile Page")
                VibrationHelper.vibratePattern(requireContext(), VIBRATE_SUCCESS)
                requireActivity().supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace(
                        R.id.fragment_container,
                        ProfileFragment(realTimeViewModel))
                    addToBackStack(null)
                }
            }
        }
    }

    private fun startQuickMenuDrag(view: View) {
        context?.let { safeContext ->
            // Show the drag fragment (the drop targets)
            quickMenuHandler.showQuickMenuFragment()

            toggleAutomaticFlashView()

            fragmentCameraBinding?.quickMenuContainer?.visibility = View.VISIBLE

            // Prepare the View for the Drag Shadow
            val shadowView = View(safeContext).apply {
                layoutParams = ViewGroup.LayoutParams(50, 50)
                setBackgroundResource(R.drawable.quick_menu_drag_shadow)
            }

            val widthSpec = View.MeasureSpec.makeMeasureSpec(50, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(50, View.MeasureSpec.EXACTLY)
            shadowView.measure(widthSpec, heightSpec)
            shadowView.layout(0, 0, shadowView.measuredWidth, shadowView.measuredHeight)

            val clipItem = ClipData.Item("Quick Menu Drag")
            val dragData = ClipData(
                "Quick Menu Drag",
                arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
                clipItem
            )

            val shadowBuilder = View.DragShadowBuilder(shadowView)

            // Temporarily remove listener to prevent conflicts during drag
            fragmentCameraBinding?.touchInterceptorView?.setOnTouchListener(null)

            // Start the drag operation
            view.startDragAndDrop(
                dragData,
                shadowBuilder,
                null,
                0
            )
        }
    }

    //////////////////////////////////////////////
    // END OF BIND/TOUCH LISTENER
    //////////////////////////////////////////////

    override fun onPause() {
        super.onPause()

        if (::sensorManager.isInitialized) {
            sensorManager.unregisterListener(this)
            Log.d("FLASHLIGHT", "Sensor Registration: Light sensor listener unregistered.") // LOG ADDED
        }

        cameraReleaseHandler.removeCallbacksAndMessages(null)
        idleHandler.removeCallbacksAndMessages(null)
        // Removed old handlers cleanup
        context?.unregisterReceiver(batteryReceiver)
        Log.d("battery", "finished adding battery intents")

        webRTCManager.releaseCamera()
        webRTCManager.disconnectMainServiceListener()
        webRTCManager.releaseMediaPlayer()
    }

    override fun onResume() {
        super.onResume()
        webRTCManager.connectMainServiceListener()
        context?.let {
            fragmentCameraBinding?.viewFinder?.post {
                cameraBindsHandler.setUpCamera()
            }
        }

        if (::sensorManager.isInitialized && ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED) {
            lightSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                Log.d("FLASHLIGHT", "Sensor Registration: Light sensor listener registered.") // LOG ADDED
            } ?: Log.d("FLASHLIGHT", "Sensor Registration: Light sensor is NULL.")
        }

        batteryReceiver = batteryHandler.getBatteryReceiver()
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
        }
        context?.registerReceiver(batteryReceiver, intentFilter)

        batteryHandler.checkInitialBatteryStatus()

        // Ensure touch interceptor has focus for Volume Keys
        fragmentCameraBinding?.touchInterceptorView?.requestFocus()

        screensaverHandler.toggleScreenSaver(true)

        // For emergency mode statuses. This thing is pretty slow to startup and needs to recheck
        // the RTDB. Could also serve na rin as a redundant checker for high-stakes cases
        // TODO: Remove this if found a better way
        mainRepository.reFetchLatestEvent()
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

        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        Log.d("FLASHLIGHT", "Sensor Setup: Initialized SensorManager. Light Sensor available: ${lightSensor != null}") // LOG ADDED

        sharedPreferences = requireContext().getSharedPreferences(
            SHARED_PREFERENCES_NAME,
            Context.MODE_PRIVATE)

        detectionSharedPreferences = requireContext().getSharedPreferences(
            VIU_LOCAL_SETTINGS,
            Context.MODE_PRIVATE)

        _fragmentCameraBinding = FragmentCameraBinding.bind(view)

        batteryHandler = BatteryHandler(this, realTimeViewModel)
        screensaverHandler = ScreensaverHandler(this)
        cameraBindsHandler = CameraBindsHandler(this)
        webRTCManager = WebRTCManager(this)
        emergencyManager = EmergencyManager(this, webRTCManager, realTimeViewModel)
        quickMenuHandler = QuickMenuHandler(this)
        forceDetectionHandler = ForceDetectionHandler(this)

        webRTCManager.connectMainServiceListener()
        service = MainService.getInstance()
        mainRepository = MainRepository.getInstance(requireContext())

        val savedThreshold = detectionSharedPreferences.getFloat(PREF_THRESHOLD, 0.50f)
        val savedMaxResults = detectionSharedPreferences.getInt(PREF_MAX_RESULTS, 3)
        val savedThreads = detectionSharedPreferences.getInt(PREF_THREADS, 2)
        val savedDelegate = detectionSharedPreferences.getInt(PREF_DELEGATE, 0)

        forceDetectionView = view.findViewById(R.id.ball_force_detect)
        forceDetectionView.setOnClickListener {
            forceDetectionHandler.forceDetection()
        }

        // state for Automatic Flash
        automaticFlashView = view.findViewById(R.id.ball_automatic_flash)
        automaticFlashView.setOnClickListener {
            toggleAutomaticFlash()
        }

        objectDetectorHelper = ObjectDetector(
            context = requireContext(),
            objectDetectorListener = this,
            threshold = savedThreshold,
            maxResults = savedMaxResults,
            numThreads = savedThreads,
            currentDelegate = savedDelegate
        )
        cameraExecutor = Executors.newSingleThreadExecutor()
        fragmentCameraBinding?.viewFinder?.post {
            cameraBindsHandler.setUpCamera()
        }

        setupInputListeners()
        quickMenuHandler.observeCaregiverUid()

        // Init. detection controls
        detectionControlsHandler = DetectionControlsHandler(this)
        detectionControlsHandler.initControlsAndListeners()
        detectionControlsHandler.synchronizeUiWithDetector()

        viewLifecycleOwner.lifecycleScope.launch {
            if (emergencyManager.checkIfEmergencyMode()) {
                emergencyManager.launchEmergencyMode()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation =
            fragmentCameraBinding?.viewFinder?.display?.rotation ?: Surface.ROTATION_0
    }

    fun toggleAutomaticFlashView(){
        if (isAutomaticFlashViewOn) {
            isAutomaticFlashViewOn = false
            automaticFlashView.visibility = View.GONE
        } else {
            isAutomaticFlashViewOn = true
            automaticFlashView.visibility = View.VISIBLE
        }
    }


    fun toggleAutomaticFlash(){
        if (isAutomaticFlashOn) {
            isAutomaticFlashOn = false
            automaticFlashView.setImageResource(R.drawable.ic_automatic_flash)
            TextToSpeechHelper.speak(requireContext(), "Automatic flashlight is off")
            turnFlashlightOff()
        } else {
            isAutomaticFlashOn = true
            automaticFlashView.setImageResource(R.drawable.ic_automatic_flash_off)
            TextToSpeechHelper.speak(requireContext(), "Automatic flashlight is on")
        }
    }

    override fun onResults(
        results: List<ObjectDetection>,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        activity?.runOnUiThread {
            fragmentCameraBinding?.bottomSheetLayout?.inferenceTimeVal?.text =
                String.format("%d ms", inferenceTime)
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
        screensaverHandler.cleanupScreensaverHandler()
        webRTCManager.releaseMediaPlayer()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        Log.d(
            "FLASHLIGHT",
            "HITTING ON SENSOR CHANGED"
        )
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            val ambientLightLux = event.values[0]

            // LOG MODIFIED: Logs the precise lux value, the current threshold,
            // and the master state every time a reading is received.
            Log.d(
                "FLASHLIGHT",
                "SENSOR LUX CHECK: Lux=$ambientLightLux | Threshold=$LIGHT_THRESHOLD | AutoFlashEnabled=$isAutomaticFlashOn"
            )

            // Only run the logic if the user has enabled automatic flash via Quick Menu
            if (!isAutomaticFlashOn) {
                turnFlashlightOff()
                return
            }

            // Low Light: Below the threshold (turn ON flash)
            if (ambientLightLux < LIGHT_THRESHOLD) {
                Log.d(
                    "FLASHLIGHT",
                    "AUTO FLASH DECISION: Lux $ambientLightLux < $LIGHT_THRESHOLD. Attempting to turn ON flash."
                )
                turnFlashlightOn()
            }
            // Sufficient Light: Above the threshold (turn OFF flash)
            else {
                Log.d(
                    "FLASHLIGHT",
                    "AUTO FLASH DECISION: Lux $ambientLightLux >= $LIGHT_THRESHOLD. Attempting to turn OFF flash."
                )
                turnFlashlightOff()
            }
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Required SensorEventListener method, no implementation needed here
    }
}