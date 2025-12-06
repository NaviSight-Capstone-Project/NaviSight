package edu.capstone.navisight.viu.ui.emergency

/*

EmergencyActivity.kt

This activity triggers only if the VIU has fully completed the emergency sequence.
The app will lock down to only this activity unless any of the following is done:
    - For 5 seconds, hold:  volume up and down, and screen.
While this activity is enabled, this does the following:
    -

 */

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import edu.capstone.navisight.common.TextToSpeechHelper
import edu.capstone.navisight.common.webrtc.service.MainService
import edu.capstone.navisight.common.webrtc.service.MainServiceRepository
import edu.capstone.navisight.viu.data.remote.ViuDataSource

class EmergencyActivity : ComponentActivity(), MainService.EndAndDeniedCallListener {
    private lateinit var serviceRepository: MainServiceRepository
    private var target: String? = null
    private var isVideoCall: Boolean = false // Default to audio call first
    private var isCaller: Boolean = true
    private var viuRemoteDataSource = ViuDataSource()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Init.
        TextToSpeechHelper.speak(applicationContext, "Emergency mode is activated. ")

        serviceRepository = MainServiceRepository.getInstance(applicationContext)

        // Set the listener to handle remote end call signals
        MainService.endAndDeniedCallListener = this

        // Get initial intent data
        target = intent.getStringExtra("target")
        isVideoCall = intent.getBooleanExtra("isVideoCall", true)
        isCaller = intent.getBooleanExtra("isCaller", true)

        setContent {
            EmergencyScreen(
                target = target ?: "",
                isVideoCall = isVideoCall,
                isCaller = isCaller,
                serviceRepository = serviceRepository,
                onEndCall = {
                    serviceRepository.sendEndOrAbortCall()
                    finish()
                },
                viuDataSource = viuRemoteDataSource,
            )
        }
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
