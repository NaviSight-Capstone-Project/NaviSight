package edu.capstone.navisight.viu.ui.camera

import android.annotation.SuppressLint
import android.content.ClipDescription
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.DragEvent
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import edu.capstone.navisight.R
import edu.capstone.navisight.common.TextToSpeechHelper
import edu.capstone.navisight.common.VibrationHelper
import edu.capstone.navisight.viu.ui.SpeechToTextHandler

/*

QuickMenuFragment.kt

 */

interface QuickMenuListener {
    fun onQuickMenuDismissed()
    fun onQuickMenuAction(actionId: Int)
}

private const val UNHIGHLIGHTED_ALPHA = 0.4f
private const val HIGHLIGHTED_ALPHA = 1.0f
private const val STT_GPS_TAG = "QuickMenu STT GPS"
private const val TAG = "QuickMenuFragment (Not inside CameraFragment)"

class QuickMenuFragment : Fragment(R.layout.dialog_quick_menu) {
    var dragListener: QuickMenuListener? = null

    // Map View IDs to their respective Views for easy access
    private lateinit var ballViews: Map<Int, View>

    lateinit var screensaverLockView: ImageView
    lateinit var automaticFlashView: ImageView
    lateinit var ttsToggleView: ImageView

    private lateinit var cameraFragment: CameraFragment

    // Init. Speech to Text
    private lateinit var speechToTextHelper: SpeechToTextHandler

    // Init. STT get location
    private var locationManager: LocationManager? = null
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            speechToTextHelper.currentLat = location.latitude
            speechToTextHelper.currentLon = location.longitude
        }
        override fun onProviderEnabled(provider: String) {
            Log.d(STT_GPS_TAG, "GPS Provider Enabled: $provider") }
        override fun onProviderDisabled(provider: String) {
            Log.d(STT_GPS_TAG, "GPS Provider Disabled: $provider") }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        try {
            Log.d(STT_GPS_TAG, "Requesting GPS updates...")

            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000L,
                5f,
                locationListener
            )

            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                5000L,
                5f,
                locationListener
            )

        } catch (e: Exception) {
            Log.d(STT_GPS_TAG, "Error starting location: ${e.message}")
        }
    }

    // Static variable to track the last highlighted view ID during a drag operation
    private var currentHighlightedId: Int? = null

    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        // Ensure the hosting fragment/activity implements the listener
        dragListener = parentFragment as? QuickMenuListener
            ?: throw IllegalStateException("Parent must implement QuickMenuListener")

        // *** ADDED: Get a reference to the parent CameraFragment ***
        cameraFragment = parentFragment as? CameraFragment
            ?: throw IllegalStateException("Parent fragment must be CameraFragment")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechToTextHelper.stopAndCleanup()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        screensaverLockView = view.findViewById(R.id.ball_screensaver_lock)
        automaticFlashView = view.findViewById(R.id.ball_automatic_flash)
        ttsToggleView = view.findViewById(R.id.ball_toggle_TTS_temporarily)

        // Alert user that quick menu is active
        TextToSpeechHelper.speak(requireContext(),
            "Quick Menu activated. Say 'where am I' or 'time'.")

        // Assume GPS and other stuff is activated before app is continued
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        startLocationUpdates()

        // Vibrate on start
        VibrationHelper.vibrate(requireContext())

        // Activate STT
        speechToTextHelper = SpeechToTextHandler(requireContext(), viewLifecycleOwner.lifecycleScope)
        speechToTextHelper.initialize()
        speechToTextHelper.startListeningForCommand()

        val lockDrawableRes = if (cameraFragment.isPreviewLocked) R.drawable.ic_lock else R.drawable.ic_screensaver_eye
        screensaverLockView.setImageResource(lockDrawableRes)

        // Set initial state for Automatic Flash
        val flashDrawableRes = if (cameraFragment.isAutomaticFlashOn) R.drawable.ic_automatic_flash_off else R.drawable.ic_automatic_flash
        automaticFlashView.setImageResource(flashDrawableRes)

        val ttsDrawableRes = if (TextToSpeechHelper.isSilenced) R.drawable.ic_mute else R.drawable.ic_tts_active
        ttsToggleView.setImageResource(ttsDrawableRes)

        ballViews = mapOf(
            R.id.ball_video_call to view.findViewById(R.id.ball_video_call),
            R.id.ball_audio_call to view.findViewById(R.id.ball_audio_call),
            R.id.ball_snap to view.findViewById(R.id.ball_snap),
            R.id.ball_flip_camera to view.findViewById(R.id.ball_flip_camera),
            R.id.ball_ocr to view.findViewById(R.id.ball_ocr),
            R.id.ball_bk_note to view.findViewById(R.id.ball_bk_note),
            R.id.ball_screensaver_lock to view.findViewById(R.id.ball_screensaver_lock),
            R.id.ball_automatic_flash to view.findViewById(R.id.ball_automatic_flash),
            R.id.ball_force_detect to view.findViewById(R.id.ball_force_detect),
            R.id.ball_toggle_TTS_temporarily to view.findViewById(R.id.ball_toggle_TTS_temporarily),
        )

        // Set the drag listener on the entire root view of the fragment
        // and also explicitly on each drop target.
        view.setOnDragListener(menuDragListener)
        ballViews.values.forEach { it.setOnDragListener(menuDragListener) }

        resetHighlights()
    }

    private fun resetHighlights() {
        ballViews.values.forEach { it.alpha = UNHIGHLIGHTED_ALPHA }
        currentHighlightedId = null
    }

    // Edit and set custom TTS statements when the ball is highlighted here
    private fun getIdTTSDescription(viewId:String, isVerbose: Boolean = false): String {
        when (viewId) {
            "ball_video_call" -> return if (isVerbose) "Start a Video call" else "Video call"
            "ball_audio_call" -> return if (isVerbose) "Stat a Audio call" else "Audio call"
            "ball_flip_camera" -> return if (isVerbose) "Switch your Camera" else "Switch camera"
            "ball_snap" -> return "Take a picture"
            "ball_bk_note" -> return if (isVerbose)
                "Take notes with your keyboard" else "Quick Notes"
            "ball_ocr" -> return if (isVerbose) "Use OCR" else "OCR"
            "ball_screensaver_lock" -> return "Toggle Preview Mode Lock"
            "ball_automatic_flash" -> return "Toggle automatic flashlight"
            "ball_force_detect" -> return "Detect immediately"
            "ball_toggle_TTS_temporarily" -> return "Toggle TTS Sound"
        }
        throw IllegalArgumentException(
            "viewId used is invalid. Choose a correct ID")
    }

    // Actual Drag Listener Implementation
    private val menuDragListener = View.OnDragListener { v, event ->
        val targetId = v.id // The ID of the View receiving the event (root or a ball)

        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                if (event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                    // Indicate that this View can accept the drag data
                    return@OnDragListener true
                }
                return@OnDragListener false
            }

            // Track which ball the shadow is over
            DragEvent.ACTION_DRAG_ENTERED, DragEvent.ACTION_DRAG_LOCATION -> {
                // Only process these events if the target is one of the four balls
                if (ballViews.containsKey(targetId)) {
                    if (targetId != currentHighlightedId) {
                        // Un-highlight old view
                        currentHighlightedId?.let { ballViews[it]?.alpha = UNHIGHLIGHTED_ALPHA }

                        // Highlight new view
                        ballViews[targetId]?.alpha = HIGHLIGHTED_ALPHA
                        currentHighlightedId = targetId

                        // Vibrate on highlight.
                        VibrationHelper.vibrate(requireContext())

                        // TTS depending on action.
                        val ballId = v.resources.getResourceEntryName(targetId)
                        TextToSpeechHelper.speak(
                            requireContext(),
                            getIdTTSDescription(ballId))

                        Log.d(TAG, "Highlighted: $ballId")
                    }
                }
                return@OnDragListener true
            }

            // Remove highlight when the shadow leaves a ball
            DragEvent.ACTION_DRAG_EXITED -> {
                if (ballViews.containsKey(targetId)) {
                    ballViews[targetId]?.alpha = UNHIGHLIGHTED_ALPHA
                    currentHighlightedId = null

                }
                return@OnDragListener true
            }

            // Drag released over a target
            DragEvent.ACTION_DROP -> {
                // If a ball was highlighted upon drop, execute the action
                if (currentHighlightedId != null) {
                    dragListener?.onQuickMenuAction(currentHighlightedId!!)
                    return@OnDragListener true
                }
                // The drag operation will end automatically after ACTION_DROP
                return@OnDragListener true
            }

            // Drag operation finished (successful drop or release elsewhere)
            DragEvent.ACTION_DRAG_ENDED -> {
                if (currentHighlightedId == null) { // Check if no consumption nangyari
                    // Make TTS say "cancel" to alert user
                    TextToSpeechHelper.speak(requireContext(), "Quick Menu cancelled")
                }
                dragListener?.onQuickMenuDismissed()
                return@OnDragListener true
            }

            else -> {
                return@OnDragListener false}
        }
    }
}