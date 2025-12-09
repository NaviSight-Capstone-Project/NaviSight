package edu.capstone.navisight.viu.ui.camera.managers
//
//import android.app.Activity
//import android.content.Context
//import android.content.Intent
//import android.media.MediaPlayer
//import android.media.RingtoneManager
//import android.net.Uri
//import android.util.Log
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AlertDialog
//import androidx.appcompat.app.AppCompatActivity
//import androidx.appcompat.widget.AppCompatImageButton
//import edu.capstone.navisight.R
//import edu.capstone.navisight.common.webrtc.model.DataModel
//import edu.capstone.navisight.common.webrtc.model.DataModelType
//import edu.capstone.navisight.common.webrtc.repository.MainRepository
//import edu.capstone.navisight.common.webrtc.mainService.MainService
//import edu.capstone.navisight.common.webrtc.service.MainService
//import edu.capstone.navisight.common.webrtc.utils.getCameraAndMicPermission
//import edu.capstone.navisight.viu.ui.call.CallActivity
//import edu.capstone.navisight.viu.ui.camera.TAG
//
//class CallHandler (
//    private val context: Context,
//    private val activity: Activity,
//    private val caregiverUid : String,
//    private val mainRepository: MainRepository,
//    private val mainService : MainService
//) {
//    fun handleStartCall(isVideoCall: Boolean) {
//        val targetUid = caregiverUid // Get the UID from the observed StateFlow
//
//        if (targetUid.isNullOrEmpty()) {
//            Toast.makeText(requireContext(), "Caregiver not found. Cannot start call.", Toast.LENGTH_LONG).show()
//            Log.e("CallSignal", "Caregiver UID is null or empty.")
//            return
//        }
//
//        (activity as AppCompatActivity).getCameraAndMicPermission {
//            mainRepository.sendConnectionRequest(targetUid, isVideoCall) { success ->
//                if (success) {
//                    mainService.startCallTimeoutTimer()
//                    val intent = Intent(requireActivity(), CallActivity::class.java).apply {
//                        putExtra("target", targetUid)
//                        putExtra("isVideoCall", isVideoCall)
//                        putExtra("isCaller", true)
//                    }
//                    callActivityLauncher.launch(intent)
//                } else {
//                    Log.e("CallSignal", "Failed to send call request.")
//                    Toast.makeText(requireContext(), "Failed to send call request.", Toast.LENGTH_LONG).show()
//                }
//            }
//        }
//    }
//
//    override fun onCallReceived(model: DataModel) {
//        // Ensure switch to the Main thread to handle UI
//        activity?.runOnUiThread {
//            beginCall(model)
//        }
//    }
//
//    override fun onCallAborted() {
//        activity?.runOnUiThread {
//            Log.d("abortcall", "onCallAborted launching")
//
//            // Check if the dialog is currently showing and dismiss it
//            if (callRequestDialog?.isShowing == true) {
//                callRequestDialog?.dismiss()
//
//                // Show a toast message to the user
//                Toast.makeText(
//                    context,
//                    "Caregiver aborted their call.",
//                    Toast.LENGTH_LONG
//                ).show()
//
//                // Stop the ringing
//                releaseMediaPlayer()
//                Log.d(TAG, "Incoming call aborted, dialog dismissed and ringing stopped.")
//            } else {
//                Log.d(TAG, "Call aborted, but no dialog was active.")
//            }
//        }
//    }
//
//    override fun onCallMissed(senderId: String) {
//        activity?.runOnUiThread {
//            // Check if the dialog is currently showing and dismiss it
//            if (callRequestDialog?.isShowing == true) {
//                callRequestDialog?.dismiss()
//
//                // Show a toast message to the user
//                Toast.makeText(
//                    context,
//                    "You've missed your caregiver's call!",
//                    Toast.LENGTH_LONG
//                ).show()
//
//                // Stop the ringing
//                releaseMediaPlayer()
//                Log.d(TAG, "Incoming call missed, dialog dismissed and ringing stopped.")
//            } else {
//                Log.d(TAG, "Call missed, but no dialog was active.")
//            }
//        }
//    }
//
//
//    fun beginCall(model: DataModel) {
//        Log.d(TAG, "DETECTED AN INCOMING CALL: ${model.sender}")
//
//        val isVideoCall = model.type == DataModelType.StartVideoCall
//        val callMessage = "Your caregiver is requesting a call. Accept?"
//
//        // Non-repeating
//        if (callRequestDialog?.isShowing == true) {
//            Log.w(TAG, "Call received but dialog is already visible. Ignoring.")
//            return
//        }
//
//        try {
//            // For ringtone
//            incomingMediaPlayer?.stop()
//            incomingMediaPlayer?.release()
//            incomingMediaPlayer = null
//            val notificationUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
//            incomingMediaPlayer = MediaPlayer.create(requireActivity().applicationContext, notificationUri)?.apply {
//                isLooping = true
//                start()
//            }
//
//
//            // Inflate the custom layout
//            val inflater = requireActivity().layoutInflater
//            val customLayout = inflater.inflate(R.layout.popup_call_request, null)
//
//            // Find and set the dynamic content on the custom views
//            val messageTitle = customLayout.findViewById<TextView>(R.id.messageTitle)
//            val btnAccept = customLayout.findViewById<AppCompatImageButton>(R.id.btnAccept)
//            val btnDecline = customLayout.findViewById<AppCompatImageButton>(R.id.btnDecline)
//            messageTitle.text = if (isVideoCall) "Incoming Video Call" else "Incoming Audio Call"
//
//            // Create the custom AlertDialog, injecting the view
//            callRequestDialog = AlertDialog.Builder(requireActivity())
//                .setView(customLayout)
//                .setCancelable(false) // Force user to choose
//                .create()
//
//            callRequestDialog?.window?.setBackgroundDrawableResource(R.drawable.bg_popup_rounded)
//
//            // Stop and release when the dialog is dismissed for any reason
//            callRequestDialog?.setOnDismissListener { // Use safe call
//                releaseMediaPlayer()
//                callRequestDialog = null // Clear the reference when dismissed
//            }
//
//
//            // Set up the ACCEPT button click listener
//            btnAccept.setOnClickListener {
//                releaseMediaPlayer() // Stop ringtone upon user action
//
//                // Stop timer on end/miss call timeout
//                mainService.stopCallTimeoutTimer()
//
//                Log.w(TAG, "Getting reference for CallActivity...")
//                val activity = requireActivity() as? AppCompatActivity
//                Log.w(TAG, "Reference for CallActivity retrieved.")
//
//                Log.w(TAG, "Requesting permissions for camera and mic...")
//                (requireActivity() as AppCompatActivity).getCameraAndMicPermission {
//                    // This code runs AFTER permissions are granted.
//                    Log.w(TAG, "Mic and Camera permissions retrieved. Proceeding to starting activity.")
//                    Log.w(TAG, "Mic and Camera permissions retrieved. Releasing camera...")
//                    releaseCamera {
//                        // Launch CallActivity using the launcher instead of startActivity
//                        val intent = Intent(requireActivity(), CallActivity::class.java).apply {
//                            putExtra("target", model.sender)
//                            putExtra("isVideoCall", isVideoCall)
//                            putExtra("isCaller", false)
//                            putExtra("sender_id", model.sender)
//                        }
//                        callActivityLauncher.launch(intent) // <--- IMPORTANT CHANGE HERE
//                        Log.w(TAG, "Call activity started via launcher.")
//                    }
//                } ?: run {
//                    // Handle the case where the Activity is not an AppCompatActivity (shouldn't happen)
//                    Log.e(TAG, "Failed requesting camera and mic permissions for CallActivity.")
//                    Toast.makeText(context, "Cannot request permissions. Activity is missing.", Toast.LENGTH_LONG).show()
//                }
//
//                callRequestDialog?.dismiss()
//            }
//
//            // Set DECLINE button click listener
//            btnDecline.setOnClickListener {
//                releaseMediaPlayer() // Stop ringtone upon user action
//                callRequestDialog?.dismiss()
//                Log.d(TAG, "Call declined by user.")
//                MainService.getMainRepository()?.sendDeniedCall(model.sender)
//            }
//
//            // Show the custom dialog
//            callRequestDialog?.show()
//
//        } catch (e: Exception) {
//            Log.e(TAG, "Failed to show custom AlertDialog or play ringtone. Error: ${e.message}")
//            Toast.makeText(context, "Error showing call prompt.", Toast.LENGTH_LONG).show()
//            releaseMediaPlayer() // Cleanup even if an error occurs
//        }
//    }
//
//
//    // Release media player if needed
//    fun releaseMediaPlayer() {
//        incomingMediaPlayer?.stop()
//        incomingMediaPlayer?.release()
//        incomingMediaPlayer = null
//    }
//
//    fun releaseCamera(onReleased: (() -> Unit)? = null) {
//        try {
//            cameraProvider?.unbindAll()
//            camera = null
//            preview = null
//            imageAnalyzer = null
//            Log.d(TAG, "Camera released successfully.")
//
//            // Wait a bit for CameraX to release hardware resources
//            cameraReleaseHandler.postDelayed({
//                Log.d(TAG, "Camera release delay complete. Proceeding.")
//                if (isAdded) {
//                    onReleased?.invoke()
//                } else {
//                    Log.w(TAG, "Fragment detached before camera release callback completed. Aborting onReleased.")
//                }
//            }, 500) // 0.5 second delay (tweakable)
//        } catch (e: Exception) {
//            Log.e(TAG, "Error releasing camera: ${e.message}", e)
//            onReleased?.invoke()
//        }
//    }
//}