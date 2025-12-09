package edu.capstone.navisight.viu.ui.camera.managers

import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import edu.capstone.navisight.common.Constants.SP_IS_EMERGENCY_MODE_ACTIVE
import edu.capstone.navisight.common.TextToSpeechHelper
import edu.capstone.navisight.common.VibrationHelper
import edu.capstone.navisight.viu.ViuHomeViewModel
import edu.capstone.navisight.viu.ui.camera.CameraFragment
import edu.capstone.navisight.viu.ui.emergency.EmergencyActivity

class EmergencyManager (
    private val cameraFragment: CameraFragment,
    private val webRTCManager: WebRTCManager,
    private val realTimeViewModel : ViuHomeViewModel
){
    fun initiateEmergencyModeSequence() {
        // Todo: FIX TIMING, PERHAPS MAKE DEDICATED queueSpeakEmergency (then combine with vibration)
        VibrationHelper.vibrate(cameraFragment.context)
        TextToSpeechHelper.queueSpeak(cameraFragment.context, "Emergency mode initiating now." +
                "Please hold for 3 seconds to continue.")
        cameraFragment.didEmergencySequenceComplete = true // Set flag TODO: DO NOT FORGET TO ADD FALSE ONCE EMERGENCY IS COMPLETE
        setEmergencyModeFlag()
        webRTCManager.releaseCamera {
            if (cameraFragment.isAdded) {
                launchEmergencyMode()
            }
        }
    }

    fun setEmergencyModeFlag() {
        realTimeViewModel.setUserEmergencyActivated()
    }

    suspend fun checkIfEmergencyMode(): Boolean {
        val isEmergencyActivated = realTimeViewModel.getUserEmergencyActivated() ?: false
        return isEmergencyActivated
    }

    fun launchEmergencyMode() {
        val intent = Intent(cameraFragment.context,
            EmergencyActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        cameraFragment.context?.startActivity(intent)
    }
}