package edu.capstone.navisight.viu.ui.camera.managers

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.edit
import edu.capstone.navisight.common.Constants.SP_IS_EMERGENCY_MODE_ACTIVE
import edu.capstone.navisight.common.TextToSpeechHelper
import edu.capstone.navisight.common.VibrationHelper
import edu.capstone.navisight.viu.ui.emergency.EmergencyActivity

class EmergencyManager (
    private val context: Context,
    private val sharedPreferences: SharedPreferences,
    private val releaseCamera: ((onReleased: (() -> Unit)?) -> Unit),
    private val isAdded : Boolean,
    private var didEmergencySequenceComplete: Boolean){

    fun initiateEmergencyModeSequence() {
        // Todo: FIX TIMING, PERHAPS MAKE DEDICATED queueSpeakEmergency (then combine with vibration)
        VibrationHelper.vibrate(context)
        TextToSpeechHelper.queueSpeak(context, "Emergency mode initiating now." +
                "Please hold for 3 seconds to continue.")
        didEmergencySequenceComplete = true // Set flag TODO: DO NOT FORGET TO ADD FALSE ONCE EMERGENCY IS COMPLETE
        setEmergencyModeFlag()
        releaseCamera {
            if (isAdded) {
                launchEmergencyMode()
            }
        }
    }

    fun setEmergencyModeFlag() {
        sharedPreferences.edit { putBoolean(SP_IS_EMERGENCY_MODE_ACTIVE, true) }
    }

    fun checkIfEmergencyMode() : Boolean{
        return sharedPreferences.getBoolean(SP_IS_EMERGENCY_MODE_ACTIVE, false)
    }

    fun launchEmergencyMode() {
        val intent = Intent(context, EmergencyActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}