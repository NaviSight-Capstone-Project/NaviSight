package edu.capstone.navisight.common

/*

VibrationHelper.kt

Set all vibration properties here.
Defaults to 500 milliseconds (0.5)

 */

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import edu.capstone.navisight.viu.ui.profile.ViuSettingsManager
import java.lang.Thread.sleep

private const val tag = "VibrationHelper"
private const val defaultVibrationMilliseconds = 500L // Modify default here
private const val defaultDelayMilliseconds = 500L // Modify default here

object VibrationHelper {
    fun vibrate(context: Context?, milliseconds:Long=defaultVibrationMilliseconds){
        if (context != null && !ViuSettingsManager.getBoolean(
                context,
                ViuSettingsManager.KEY_VIBRATION,
                true)) {
            Log.i(tag, "Vibration is disabled in settings. Skipping vibration.")
            return
        }
        val vibrator = context?.getSystemService(Vibrator::class.java)
        vibrator?.vibrate(
            VibrationEffect.createOneShot(milliseconds,
                VibrationEffect.DEFAULT_AMPLITUDE))
        Log.i(tag, "Vibrated for $milliseconds milliseconds")
    }

    fun vibrateAfterDelay(
        context: Context?,
        delayMilliseconds:Long=defaultDelayMilliseconds,
        vibrationMilliseconds:Long=defaultVibrationMilliseconds)
    {
        if (context != null && !ViuSettingsManager.getBoolean(
                context,
                ViuSettingsManager.KEY_VIBRATION,
                true)) {
            Log.i(tag, "Vibration is disabled in settings. Skipping vibration.")
            return
        }
        sleep(delayMilliseconds)
        vibrate(context, vibrationMilliseconds)
    }
}