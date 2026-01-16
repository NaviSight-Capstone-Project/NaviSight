package edu.capstone.navisight.caregiver.ui.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import edu.capstone.navisight.R
import edu.capstone.navisight.caregiver.data.remote.ViuDataSource
import edu.capstone.navisight.common.Constants.BR_ACTION_DENIED_CALL
import edu.capstone.navisight.common.Constants.BR_ACTION_MISSED_CALL
import edu.capstone.navisight.common.Constants.BR_CONNECTION_ESTABLISHED
import edu.capstone.navisight.common.Constants.BR_CONNECTION_FAILURE
import edu.capstone.navisight.common.webrtc.service.MainService
import edu.capstone.navisight.common.webrtc.service.MainServiceRepository

class CaregiverCallActivity : ComponentActivity(), MainService.EndAndDeniedCallListener {

    private lateinit var serviceRepository: MainServiceRepository
    private var target: String? = null
    private var isConnectedState = mutableStateOf(false)
    private var isVideoCall: Boolean = true
    private var isCaller: Boolean = true
    private var isScreenCasting = false
    private var viuRemoteDataSource = ViuDataSource()

    // Declare the ActivityResultLauncher for screen capture
    private lateinit var requestScreenCaptureLauncher: ActivityResultLauncher<Intent>

    private var isConnectionFailed = mutableStateOf(false)
    private var failureMessage = mutableStateOf("Connection failed. Try again.") // Default message
    private var mediaPlayer: MediaPlayer? = null

    private fun playCallingSound() {
        Log.d("Calling", "Playing sound")
        if (mediaPlayer == null) {
            // Ensure you have a 'calling_tone.mp3' in res/raw
            mediaPlayer = MediaPlayer.create(this, R.raw.calling_tone).apply {
                isLooping = true
                start()
            }
            Log.d("Calling", "looped")
        }
    }

    private fun stopCallingSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private val finishReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BR_ACTION_MISSED_CALL) {
                Toast.makeText(context,
                    "Your VIU missed your call. Try again?",
                    Toast.LENGTH_LONG).show()
                stopAndCleanUp()
            }
            if (intent?.action == BR_ACTION_DENIED_CALL) {
                Toast.makeText(context,
                    "Your VIU declined your call. Try again?",
                    Toast.LENGTH_LONG).show()
                stopAndCleanUp()
            }
            if (intent?.action == BR_CONNECTION_FAILURE) {
                isConnectedState.value = false
                isConnectionFailed.value = true

            }
        }
    }

    private val connectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BR_CONNECTION_ESTABLISHED) {
                isConnectedState.value = true
                stopCallingSound()
            }
        }
    }

    private fun stopAndCleanUp(){
        stopCallingSound()
        serviceRepository.sendEndOrAbortCall()
        MainService.remoteSurfaceView?.release()
        MainService.remoteSurfaceView = null
        MainService.localSurfaceView?.release()
        MainService.localSurfaceView = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        serviceRepository = MainServiceRepository.getInstance(applicationContext)

        // Set the listener to handle remote end call signals
        MainService.endAndDeniedCallListener = this

        val finishFilter = IntentFilter().apply {
            addAction(BR_ACTION_MISSED_CALL)
            addAction(BR_ACTION_DENIED_CALL)
            addAction(BR_CONNECTION_FAILURE)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(
            finishReceiver, finishFilter)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(
                connectionReceiver, IntentFilter(BR_CONNECTION_ESTABLISHED))

        // Screen share var init.
        setupScreenCaptureLauncher()

        // Get initial intent data
        target = intent.getStringExtra("target")
        isVideoCall = intent.getBooleanExtra("isVideoCall", true)
        isCaller = intent.getBooleanExtra("isCaller", true)

        if (isCaller) {
            playCallingSound()
        }

        setContent {
            CallScreen(
                target = target ?: "",
                isVideoCall = isVideoCall,
                isCaller = isCaller,
                serviceRepository = serviceRepository,
                onEndCall = {
                    serviceRepository.sendEndOrAbortCall()
                    finish()
                },
                // Pass the function to trigger the screen capture request
                onRequestScreenCapture = { startScreenCapture() },
                viuRemoteDataSource = viuRemoteDataSource,
                isConnectionFailed = isConnectionFailed.value,
                failureMessage = failureMessage.value,
                isConnected = isConnectedState.value
            )
        }
    }

    private fun setupScreenCaptureLauncher() {
        requestScreenCaptureLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val intent = result.data

                // Store the permission intent for the Service
                MainService.screenPermissionIntent = intent

                isScreenCasting = true

                // Tell the service to start streaming the screen
                serviceRepository.toggleScreenShare(true)
            } else {
                Toast.makeText(this, "Screen sharing permission denied.", Toast.LENGTH_SHORT).show()
                isScreenCasting = false
            }
        }
    }

    // Function to start the system permission dialog
    private fun startScreenCapture() {
        val mediaProjectionManager = application.getSystemService(
            MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager

        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        requestScreenCaptureLauncher.launch(captureIntent)
    }

    override fun onCallEnded() {
        finish()
    }

    override fun onCallDenied() {
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCallingSound()
        if (MainService.endAndDeniedCallListener == this) {
            MainService.endAndDeniedCallListener = null
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(finishReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(connectionReceiver)
        MainService.remoteSurfaceView?.release()
        MainService.remoteSurfaceView = null
        MainService.localSurfaceView?.release()
        MainService.localSurfaceView = null
    }
}

