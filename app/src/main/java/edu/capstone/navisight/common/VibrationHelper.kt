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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.lang.Thread.sleep

private const val tag = "VibrationHelper"
private const val defaultVibrationMilliseconds = 500L // Modify default here
private const val defaultDelayMilliseconds = 500L // Modify default here
private const val DEBOUNCE_PERIOD_MS = 500L

object VibrationHelper {
    private val helperScope = CoroutineScope(Dispatchers.Main + Job())
    private val vibrationFlow = MutableSharedFlow<Pair<Context, Long>>()

    init {
        helperScope.launch {
            vibrationFlow
                // Ignore all events that arrive within the debounce period
                .debounce(DEBOUNCE_PERIOD_MS)
                .collect { (context, milliseconds) ->
                    performVibration(context, milliseconds)
                }
        }
    }

    fun vibrateQuick(context: Context?, milliseconds:Long=defaultVibrationMilliseconds){
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

    fun vibrate(context: Context?, milliseconds: Long = defaultVibrationMilliseconds) {
        if (context != null && !ViuSettingsManager.getBoolean(
                context,
                ViuSettingsManager.KEY_VIBRATION,
                true)) {
            Log.i(tag, "Vibration is disabled in settings. Skipping vibration.")
            return
        }
        helperScope.launch {
            vibrationFlow.emit(Pair(context!!, milliseconds))
            Log.d(tag, "Vibration event emitted. Awaiting debounce.")
        }
    }

    private fun performVibration(context: Context, milliseconds: Long) {
        val vibrator = context.getSystemService(Vibrator::class.java)
        vibrator?.vibrate(
            VibrationEffect.createOneShot(milliseconds,
                VibrationEffect.DEFAULT_AMPLITUDE))
        Log.i(tag, "VIBRATION TRIGGERED. Time since last trigger > $DEBOUNCE_PERIOD_MS ms.")
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