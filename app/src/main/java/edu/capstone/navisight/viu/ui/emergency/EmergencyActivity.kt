package edu.capstone.navisight.viu.ui.emergency

/*

EmergencyDialog.kt

This activity triggers only if the VIU has fully completed the emergency sequence.
The app will lock down to only this activity unless any of the following is done:
    - For 5 seconds, hold:  volume up and down, and screen.
While this activity is enabled, this does the following:
    -

 */

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.edit
import edu.capstone.navisight.common.Constants.SHARED_PREFERENCES_NAME
import edu.capstone.navisight.common.Constants.SP_IS_EMERGENCY_MODE_ACTIVE
import edu.capstone.navisight.common.TextToSpeechHelper
import edu.capstone.navisight.common.webrtc.service.MainService
import edu.capstone.navisight.common.webrtc.service.MainServiceRepository
import edu.capstone.navisight.viu.ViuHomeViewModel
import edu.capstone.navisight.viu.data.remote.RealtimeDataSource
import edu.capstone.navisight.viu.data.remote.ViuDataSource

private const val EMERGENCY_MODE_ACTIVATED_TAG = "isEmergencyModeActive"

class EmergencyActivity : ComponentActivity(), MainService.EndAndDeniedCallListener {
    private lateinit var serviceRepository: MainServiceRepository
    private var target: String? = null
    private var isVideoCall: Boolean = false // Default to audio call first
    private var isCaller: Boolean = true
    private var viuRemoteDataSource = ViuDataSource()
    private val viuHomeViewModel: ViuHomeViewModel? = ViuHomeViewModel.getInstance()
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Init.
        TextToSpeechHelper.speak(applicationContext, "Emergency mode is active. ")
        sayEmergencyModeDescription()


        serviceRepository = MainServiceRepository.getInstance(applicationContext)

        sharedPreferences = applicationContext.getSharedPreferences(
            SHARED_PREFERENCES_NAME,
            MODE_PRIVATE)

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
                onEndEmergencyMode = {
                    serviceRepository.sendEndOrAbortCall()
                    viuHomeViewModel?.removeUserEmergencyActivated()
                    TextToSpeechHelper.queueSpeak(
                        applicationContext,"Emergency mode deactivated")
                    finish()
                },
                viuDataSource = viuRemoteDataSource,
            )
        }

        onBackPressedDispatcher.addCallback(this,
            object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(
                    applicationContext,
                    "App is locked in Emergency Mode.",
                    Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onCallEnded() {
        finish()
    }



    private fun sayEmergencyModeDescription(){
        TextToSpeechHelper.speak(applicationContext,
            emergencyModeDescription +
                    emergencyModeDescription2 )
//                    emergencyModeDescription3
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
