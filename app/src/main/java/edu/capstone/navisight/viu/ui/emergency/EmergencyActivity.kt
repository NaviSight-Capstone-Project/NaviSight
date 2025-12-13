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
import edu.capstone.navisight.common.Constants.GLOBAL_LOCAL_SETTINGS
import edu.capstone.navisight.common.TextToSpeechHelper
import edu.capstone.navisight.common.VibrationHelper
import edu.capstone.navisight.common.webrtc.service.MainService
import edu.capstone.navisight.common.webrtc.service.MainServiceRepository
import edu.capstone.navisight.viu.ViuHomeViewModel
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
    private lateinit var emergencyStatusListener: EmergencyStatusListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Init.
        TextToSpeechHelper.speak(applicationContext, "Emergency mode is active. ")
        VibrationHelper.emergencyVibrate(applicationContext, 1000L) // Long vibrate to show seriousness
        sayEmergencyModeDescription()

        serviceRepository = MainServiceRepository.getInstance(applicationContext)

        sharedPreferences = applicationContext.getSharedPreferences(
            GLOBAL_LOCAL_SETTINGS,
            MODE_PRIVATE)

        // Set the listener to handle remote end call signals
        MainService.endAndDeniedCallListener = this

        // Get initial intent data
        target = intent.getStringExtra("target")
        isVideoCall = intent.getBooleanExtra("isVideoCall", true)
        isCaller = intent.getBooleanExtra("isCaller", true)

        emergencyStatusListener = EmergencyStatusListener {
            // This is the callback when the flag is set to false remotely
            handleRemoteEmergencyDeactivation()
        }
        emergencyStatusListener.startListening()

        setContent {
            EmergencyScreen(
                target = target ?: "",
                isVideoCall = isVideoCall,
                isCaller = isCaller,
                serviceRepository = serviceRepository,
                onEndEmergencyMode = {
                    serviceRepository.sendEndOrAbortCall()
                    viuHomeViewModel?.removeUserEmergencyActivated()
                    TextToSpeechHelper.speak(
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

    private fun handleRemoteEmergencyDeactivation() {
        emergencyStatusListener.stopListening()
        serviceRepository.sendEndOrAbortCall()
        viuHomeViewModel?.removeUserEmergencyActivated()
        TextToSpeechHelper.speak(
            applicationContext,"Emergency mode deactivated by caregiver.")
        finish()
    }

    override fun onCallEnded() {
        emergencyStatusListener.stopListening()
        finish()
    }

    private fun sayEmergencyModeDescription(){
        TextToSpeechHelper.speak(applicationContext,
            emergencyModeDescription +
                    emergencyModeDescription2 )
    }

    override fun onCallDenied() {
        emergencyStatusListener.stopListening()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        emergencyStatusListener.stopListening()
        if (MainService.endAndDeniedCallListener == this) {
            MainService.endAndDeniedCallListener = null
        }
        MainService.remoteSurfaceView?.release()
        MainService.remoteSurfaceView = null
        MainService.localSurfaceView?.release()
        MainService.localSurfaceView = null
    }
}
