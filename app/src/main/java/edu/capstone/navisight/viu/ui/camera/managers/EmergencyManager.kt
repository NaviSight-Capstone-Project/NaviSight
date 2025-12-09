package edu.capstone.navisight.viu.ui.camera.managers

import android.content.Intent
import androidx.core.content.edit
import edu.capstone.navisight.common.Constants.SP_IS_EMERGENCY_MODE_ACTIVE
import edu.capstone.navisight.common.TextToSpeechHelper
import edu.capstone.navisight.common.VibrationHelper
import edu.capstone.navisight.viu.ui.camera.CameraFragment
import edu.capstone.navisight.viu.ui.emergency.EmergencyActivity

class EmergencyManager (
    private val cameraFragment: CameraFragment,
    private val webRTCManager: WebRTCManager
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
        cameraFragment.sharedPreferences.edit { putBoolean(SP_IS_EMERGENCY_MODE_ACTIVE, true) }
    }

    fun checkIfEmergencyMode() : Boolean{
        return cameraFragment.sharedPreferences.getBoolean(SP_IS_EMERGENCY_MODE_ACTIVE, false)
    }

    fun launchEmergencyMode() {
        val intent = Intent(cameraFragment.context,
            EmergencyActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        cameraFragment.context?.startActivity(intent)
    }
}