package edu.capstone.navisight.guest

/*

GuestFragment.kt

This fragment is used primarily as a placeholder for non-registered users.
This fragment must default to object detection - camera mode.
This fragment will and only will be used if there is NO user logged in.

 */

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import edu.capstone.navisight.R
import edu.capstone.navisight.auth.AuthActivity
import edu.capstone.navisight.common.Constants.GUEST_LOCAL_SETTINGS
import edu.capstone.navisight.common.Constants.PREF_DELEGATE
import edu.capstone.navisight.common.Constants.PREF_MAX_RESULTS
import edu.capstone.navisight.common.Constants.PREF_THREADS
import edu.capstone.navisight.common.Constants.PREF_THRESHOLD
import edu.capstone.navisight.viu.detectors.ObjectDetection
import edu.capstone.navisight.common.objectdetection.ObjectDetectorHelper
import edu.capstone.navisight.common.TextToSpeechHelper
import edu.capstone.navisight.databinding.FragmentGuestBinding
import edu.capstone.navisight.viu.ui.braillenote.BrailleNoteFragment
import edu.capstone.navisight.viu.ui.ocr.DocumentReaderFragment
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

// TODO: Make this fragment's camera front facing on deployment time

class GuestFragment :
    Fragment(R.layout.fragment_guest),
    ObjectDetectorHelper.DetectorListener,
    GuestQuickMenuListener {

    private val TAG = "ObjectDetection"
    var imageCapture: ImageCapture? = null
    private var fragmentGuestBinding: FragmentGuestBinding? = null
    val fragmentCameraBinding get() = fragmentGuestBinding
    private lateinit var guestQuickMenuHandler : GuestQuickMenuHandler
    var guestQuickMenuFragment: GuestQuickMenuFragment? = null

    var isDetectionUiModeActive: Boolean = false
    private lateinit var detectionControlsHandler : GuestDetectionControlsHandler
    lateinit var guestDetectionSharedPreferences: SharedPreferences

    lateinit var objectDetectorHelper: ObjectDetectorHelper
    private lateinit var bitmapBuffer: Bitmap
    var preview: Preview? = null
    var imageAnalyzer: ImageAnalysis? = null
    var camera: Camera? = null
    var cameraProvider: ProcessCameraProvider? = null

    private var isScreensaverActive = false
    private var currentBrightness = 0.0F
    var currentCameraFacing: Int = CameraSelector.LENS_FACING_BACK
    lateinit var cameraBindsHandler : GuestCameraBindsHandler

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

    lateinit var cameraExecutor: ExecutorService

    override fun onDestroyView() {
        idleHandler.removeCallbacks(idleRunnable)
        fragmentGuestBinding = null
        super.onDestroyView()
        cameraExecutor.shutdown()
    }


    override fun onGuestQuickMenuAction(actionId: Int) {
        when(actionId) {
            R.id.ball_snap -> {
                guestQuickMenuHandler.takePicture()
            }
            R.id.ball_flip_camera -> {
                guestQuickMenuHandler.switchCamera()
            }
            R.id.ball_ocr -> {
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(
                        R.id.fragment_container,
                        DocumentReaderFragment())
                    .addToBackStack(null)
                    .commit()
            }
            R.id.ball_bk_note -> {
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(
                        R.id.fragment_container,
                        BrailleNoteFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    override fun onGuestQuickMenuDismissed() {
        if (isAdded) {
            childFragmentManager.commit {
                guestQuickMenuFragment?.let { remove(it) }
            }
        }
        guestQuickMenuFragment = null
        fragmentCameraBinding?.quickMenuContainer?.visibility = View.GONE
    }

    override fun onStop() {
        super.onStop()
        idleHandler.removeCallbacks(idleRunnable)
    }

    @SuppressLint("MissingPermission", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentGuestBinding = FragmentGuestBinding.bind(view)

        guestQuickMenuHandler = GuestQuickMenuHandler(this)
        cameraBindsHandler = GuestCameraBindsHandler(this)

        guestDetectionSharedPreferences = requireContext().getSharedPreferences(
            GUEST_LOCAL_SETTINGS,
            Context.MODE_PRIVATE)

        val savedThreshold = guestDetectionSharedPreferences.getFloat(
            PREF_THRESHOLD, 0.50f)
        val savedMaxResults = guestDetectionSharedPreferences.getInt(
            PREF_MAX_RESULTS, 3)
        val savedThreads = guestDetectionSharedPreferences.getInt(
            PREF_THREADS, 1)
        val savedDelegate = guestDetectionSharedPreferences.getInt(
            PREF_DELEGATE, 0)

        objectDetectorHelper = ObjectDetectorHelper(
            context = requireContext(),
            objectDetectorListener = this,
            threshold = savedThreshold,
            maxResults = savedMaxResults,
            numThreads = savedThreads,
            currentDelegate = savedDelegate
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

            detectionControlsHandler = GuestDetectionControlsHandler(this)
            detectionControlsHandler.initControlsAndListeners()
            detectionControlsHandler.synchronizeUiWithDetector()
        }

        // Gesture Detector


        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 != null && e1.x - e2.x > 100 && abs(velocityX) > 100) {
                    navigateToLogin()
                    return true
                }

                if (e1 != null && e1.y - e2.y > 100 && abs(velocityY) > 100) {
                    Log.d(TAG, "Swipe Up Detected: Executing action.")
                    toggleDetectionUiMode(true)
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
            if (isDetectionUiModeActive) {
                toggleDetectionUiMode(false) // Tap outside bottom sheet to exit
            } else {
                doAutoScreensaver()
            }
            gestureDetector.onTouchEvent(event)
        }

        fragmentGuestBinding?.previewModeHitbox.apply {
            this!!.setOnLongClickListener {
                startQuickMenuDrag(it)
                return@setOnLongClickListener true // Consumed
            }
        }
    }

    fun toggleDetectionUiMode(enable: Boolean) {
        TextToSpeechHelper.speak(
            requireContext(),
            "Object detection settings opened")
        if (!isAdded) return
        if (enable == isDetectionUiModeActive) return // Avoid redundant toggles
        isDetectionUiModeActive = enable

        if (enable) {
            fragmentCameraBinding?.previewModeOverlay?.visibility = View.INVISIBLE
            resetScreensaverBrightness()
            detectionControlsHandler.toggleBottomSheet(true)
            fragmentCameraBinding?.previewModeHitbox?.setOnLongClickListener(null)
        } else {
            changeScreenBrightness(0.0F)
            TextToSpeechHelper.speak(
                requireContext(),
                "Object detection settings closed")
            detectionControlsHandler.toggleBottomSheet(false) // Close
            fragmentCameraBinding?.previewModeOverlay?.visibility = View.VISIBLE
            fragmentCameraBinding?.previewModeHitbox?.setOnLongClickListener {
                // If Volume Key is held, or Detection UI is active, do NOT open menu
                if (isDetectionUiModeActive)
                    return@setOnLongClickListener true
                startQuickMenuDrag(it)
                return@setOnLongClickListener true // Consumed
            }
        }
    }

    private fun startQuickMenuDrag(view: View) {
        context?.let { safeContext ->
            // Show the drag fragment (the drop targets)
            guestQuickMenuHandler.showQuickMenuFragment()

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
            fragmentCameraBinding?.previewModeHitbox?.setOnTouchListener(null)

            // Start the drag operation
            view.startDragAndDrop(
                dragData,
                shadowBuilder,
                null,
                0
            )
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

    override fun onResume() {
        super.onResume()
        context?.let {
            fragmentCameraBinding?.viewFinder?.post {
                cameraBindsHandler.setUpCamera()
            }
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

    fun resetScreensaverBrightness(){
        changeScreenBrightness(currentBrightness)
    }

    fun changeScreenBrightness(screenBrightnessValue: Float) {
        val window = requireActivity().window
        val layoutParams = window.attributes
        layoutParams.screenBrightness = screenBrightnessValue
        window.attributes = layoutParams
    }
}