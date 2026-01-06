package edu.capstone.navisight.viu.ui.camera.managers

import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import edu.capstone.navisight.R
import edu.capstone.navisight.common.webrtc.model.DataModel
import edu.capstone.navisight.common.webrtc.model.DataModelType
import edu.capstone.navisight.common.webrtc.service.MainService
import edu.capstone.navisight.common.webrtc.utils.getCameraAndMicPermission
import edu.capstone.navisight.viu.ui.call.CallActivity
import edu.capstone.navisight.viu.ui.camera.CameraFragment

private const val TAG = "WebRTCManager - CameraFragment"

class WebRTCManager(
    private val cameraFragment : CameraFragment
) : MainService.Listener {

    private var callRequestDialog: AlertDialog? = null

    fun disconnectMainServiceListener() {
        if (MainService.listener == this) {
            MainService.listener = null
        }
    }

    fun connectMainServiceListener() {
        MainService.listener = this
    }

    fun handleStartCall(isVideoCall: Boolean) {
        val targetUid = cameraFragment.caregiverUid // Get the UID from the observed StateFlow

        if (targetUid.isNullOrEmpty()) {
            Toast.makeText(cameraFragment.requireContext(), "Companion not found. Cannot start call.", Toast.LENGTH_LONG).show()
            Log.e("CallSignal", "Caregiver UID is null or empty.")
            return
        }

        (cameraFragment.requireActivity() as AppCompatActivity).getCameraAndMicPermission {
            cameraFragment.mainRepository.sendConnectionRequest(targetUid, isVideoCall) { success ->
                if (success) {
                    cameraFragment.service.startCallTimeoutTimer()
                    val intent = Intent(cameraFragment.requireActivity(), CallActivity::class.java).apply {
                        putExtra("target", targetUid)
                        putExtra("isVideoCall", isVideoCall)
                        putExtra("isCaller", true)
                    }
                    cameraFragment.callActivityLauncher.launch(intent)
                } else {
                    Log.e("CallSignal", "Failed to send call request.")
                    Toast.makeText(cameraFragment.requireContext(), "Failed to send call request.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCallReceived(model: DataModel) {
        // Ensure switch to the Main thread to handle UI
        Log.d("MainRepository", "Non-emergency CALL RECEIVED SUCCESSFULLY, BEGINNING CALL NOW. STARTING WITH TYPE: ${model.type}")
        cameraFragment.activity?.runOnUiThread {
            beginCall(model, false)
        }
    }

    override fun onEmergencyCallReceived(model: DataModel) {
        // Ensure switch to the Main thread to handle UI
        Log.d("MainRepository", "EMERGENCY CALL RECEIVED SUCCESSFULLY, BEGINNING CALL NOW. STARTING WITH TYPE: ${model.type}")
        cameraFragment.activity?.runOnUiThread {
            beginCall(model, true)
        }
    }

    override fun onCallAborted() {
        cameraFragment.activity?.runOnUiThread {
            Log.d("abortcall", "onCallAborted launching")

            // Check if the dialog is currently showing and dismiss it
            if (callRequestDialog?.isShowing == true) {
                callRequestDialog?.dismiss()

                // Show a toast message to the user
                Toast.makeText(
                    cameraFragment.context,
                    "Companion aborted their call.",
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
        cameraFragment.activity?.runOnUiThread {
            // Check if the dialog is currently showing and dismiss it
            if (callRequestDialog?.isShowing == true) {
                callRequestDialog?.dismiss()

                // Show a toast message to the user
                Toast.makeText(
                    cameraFragment.context,
                    "You've missed your companion's call!",
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


    fun beginCall(model: DataModel, isEmergency: Boolean) {
        Log.d(TAG, "begin call has been pass thru")
        if (isEmergency) {
            Log.d(TAG, "DETECTED AN EMERGENCY INCOMING CALL: ${model.sender}")
            reallyBeginCall(model, true)
        }
        else {
            Log.d(TAG, "DETECTED AN INCOMING CALL: ${model.sender}")
            // Non-repeating
            if (callRequestDialog?.isShowing == true) {
                Log.w(TAG, "Call received but dialog is already visible. Ignoring.")
                return
            }

            try {
                // For ringtone

                cameraFragment.incomingMediaPlayer?.stop()
                cameraFragment.incomingMediaPlayer?.release()
                cameraFragment.incomingMediaPlayer = null
                val notificationUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                cameraFragment.incomingMediaPlayer = MediaPlayer.create(cameraFragment.requireActivity().applicationContext, notificationUri)?.apply {
                    isLooping = true
                    start()
                }


                // Inflate the custom layout
                val inflater = cameraFragment.requireActivity().layoutInflater
                val customLayout = inflater.inflate(R.layout.popup_call_request, null)

                // Find and set the dynamic content on the custom views
                val messageTitle = customLayout.findViewById<TextView>(R.id.messageTitle)
                val btnAccept = customLayout.findViewById<AppCompatImageButton>(R.id.btnAccept)
                val btnDecline = customLayout.findViewById<AppCompatImageButton>(R.id.btnDecline)
                val isVideoCall = model.type == DataModelType.StartVideoCall
                messageTitle.text = if (isVideoCall) "Incoming Video Call" else "Incoming Audio Call"

                // Create the custom AlertDialog, injecting the view
                callRequestDialog = AlertDialog.Builder(cameraFragment.requireActivity())
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
                    cameraFragment.service.stopCallTimeoutTimer()

                    Log.w(TAG, "Getting reference for CallActivity...")
                    val activity = cameraFragment.requireActivity() as? AppCompatActivity
                    Log.w(TAG, "Reference for CallActivity retrieved.")

                    Log.w(TAG, "Requesting permissions for camera and mic...")
                    reallyBeginCall(model, isEmergency)
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
                Toast.makeText(cameraFragment.context, "Error showing call prompt.", Toast.LENGTH_LONG).show()
                releaseMediaPlayer() // Cleanup even if an error occurs
            }
        }
    }

    fun reallyBeginCall(model: DataModel, isEmergency: Boolean = false) {
        (cameraFragment.requireActivity() as AppCompatActivity).getCameraAndMicPermission {
            // This code runs AFTER permissions are granted.
            Log.w(TAG, "Mic and Camera permissions retrieved. Proceeding to starting activity.")
            Log.w(TAG, "Mic and Camera permissions retrieved. Releasing camera...")
            releaseCamera {
                var isVideoCall = model.type == DataModelType.StartVideoCall
                if (isEmergency) isVideoCall = model.type == DataModelType.EmergencyStartVideoCall

                // Launch CallActivity using the launcher instead of startActivity
                val intent = Intent(cameraFragment.requireActivity(),
                    CallActivity::class.java).apply {
                    putExtra("target", model.sender)
                    putExtra("isVideoCall", isVideoCall)
                    putExtra("isCaller", false)
                    putExtra("sender_id", model.sender)
                }
                cameraFragment.callActivityLauncher.launch(intent)
                Log.w(TAG, "Call activity started via launcher.")
            }
        } ?: run {
            // Handle the case where the Activity is not an AppCompatActivity (shouldn't happen)
            Log.e(TAG, "Failed requesting camera and mic permissions for CallActivity.")
            Toast.makeText(cameraFragment.context, "Cannot request permissions. Activity is missing.", Toast.LENGTH_LONG).show()
        }
    }


    // Release media player if needed
    fun releaseMediaPlayer() {
        cameraFragment.incomingMediaPlayer?.stop()
        cameraFragment.incomingMediaPlayer?.release()
        cameraFragment.incomingMediaPlayer = null
    }

    fun releaseCamera(onReleased: (() -> Unit)? = null) {
        try {
            cameraFragment.cameraProvider?.unbindAll()
            cameraFragment.camera = null
            cameraFragment.preview = null
            cameraFragment.imageAnalyzer = null
            Log.d(TAG, "Camera released successfully.")

            // Wait a bit for CameraX to release hardware resources
            cameraFragment.cameraReleaseHandler.postDelayed({
                Log.d(TAG, "Camera release delay complete. Proceeding.")
                if (cameraFragment.isAdded) {
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
}