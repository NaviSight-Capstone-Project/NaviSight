package edu.capstone.navisight.caregiver.ui.call

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import edu.capstone.navisight.caregiver.data.remote.ViuDataSource
import edu.capstone.navisight.common.webrtc.service.MainService
import edu.capstone.navisight.common.webrtc.service.MainServiceRepository

class CaregiverCallActivity : ComponentActivity(), MainService.EndAndDeniedCallListener {

    private lateinit var serviceRepository: MainServiceRepository
    private var target: String? = null
    private var isVideoCall: Boolean = true
    private var isCaller: Boolean = true

    private var isMicrophoneMuted = false
    private var isCameraMuted = false
    private var isSpeakerMode = true
    private var isScreenCasting = false
    private var viuRemoteDataSource = ViuDataSource()

    // Declare the ActivityResultLauncher for screen capture
    private lateinit var requestScreenCaptureLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        serviceRepository = MainServiceRepository.getInstance(applicationContext)

        // Set the listener to handle remote end call signals
        MainService.endAndDeniedCallListener = this

        // Screen share var init.
        setupScreenCaptureLauncher()

        // Get initial intent data
        target = intent.getStringExtra("target")
        isVideoCall = intent.getBooleanExtra("isVideoCall", true)
        isCaller = intent.getBooleanExtra("isCaller", true)

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
                viuRemoteDataSource = viuRemoteDataSource
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
        if (MainService.endAndDeniedCallListener == this) {
            MainService.endAndDeniedCallListener = null
        }
        MainService.remoteSurfaceView?.release()
        MainService.remoteSurfaceView = null
        MainService.localSurfaceView?.release()
        MainService.localSurfaceView = null
    }
}

