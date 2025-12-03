package edu.capstone.navisight.viu.ui.camera

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
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
import android.view.ViewGroup
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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import edu.capstone.navisight.R
import edu.capstone.navisight.databinding.FragmentCameraBinding
import edu.capstone.navisight.viu.detectors.ObjectDetection
import edu.capstone.navisight.viu.ui.call.ViuCallActivity
import edu.capstone.navisight.viu.ui.profile.ProfileFragment
import edu.capstone.navisight.viu.utils.ObjectDetectorHelper
import edu.capstone.navisight.common.TTSHelper
import edu.capstone.navisight.common.webrtc.model.DataModel
import edu.capstone.navisight.common.webrtc.model.DataModelType
import edu.capstone.navisight.common.webrtc.repository.MainRepository
import edu.capstone.navisight.common.webrtc.service.MainService
import edu.capstone.navisight.common.webrtc.utils.getCameraAndMicPermission
import edu.capstone.navisight.viu.data.remote.ViuDataSource
import edu.capstone.navisight.viu.ui.profile.ProfileViewModel
import kotlinx.coroutines.flow.collectLatest
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


private const val TAG = "CameraFragment"
private const val QUICK_MENU_TAG = "QuickMenu"


// TODO: Make this fragment's camera front facing on deployment time

class CameraFragment : Fragment(R.layout.fragment_camera),
    ObjectDetectorHelper.DetectorListener, MainService.Listener, QuickMenuListener {


    // Init. WebRTC and pop-up call and quick menu action vars
    private lateinit var service: MainService
    private var callRequestDialog: AlertDialog? = null
    private lateinit var mainRepository : MainRepository

    // Init. camera system vars
    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private var currentCameraFacing: Int = CameraSelector.LENS_FACING_BACK // Default to back camera
    private val fragmentCameraBinding get() = _fragmentCameraBinding
    private val cameraReleaseHandler = Handler(Looper.getMainLooper())
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private lateinit var bitmapBuffer: Bitmap
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
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
            R.id.ball_top -> {
                handleStartCall(isVideoCall=true)
                Log.d(QUICK_MENU_TAG, "Executed: Video Calling Primary Caregiver")
            }
            R.id.ball_bottom -> {
                handleStartCall(isVideoCall=false)
                Log.d(QUICK_MENU_TAG, "Executed: Audio Calling Primary Caregiver")
            }
            R.id.ball_left -> {
                // TODO: Replace with something much better
                TTSHelper.queueSpeak(requireContext(), "Hello World")
                Log.d(QUICK_MENU_TAG, "Executed: Quick Action #1")
            }
            R.id.ball_right -> {
                switchCamera()
                Log.d(QUICK_MENU_TAG, "Executed: Switch Camera")
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

    ////////////////////////////////////////////////////
    // END OF QUICK MENU
    ////////////////////////////////////////////////////


    // Adjust binds here
    @SuppressLint("ClickableViewAccessibility")
    private fun bindTouchListener() {
        fragmentCameraBinding?.previewModeHitbox?.setOnTouchListener { _, event ->
            doAutoScreensaver()
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
            Toast.makeText(requireContext(), "Error: Cannot switch camera. Device may lack the selected camera.", Toast.LENGTH_LONG).show()
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
                    val intent = Intent(requireActivity(), ViuCallActivity::class.java).apply {
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
}