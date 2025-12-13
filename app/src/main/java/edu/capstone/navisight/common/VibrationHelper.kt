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
import edu.capstone.navisight.common.Constants.VIBRATE_DEFAULT_DURATION
import edu.capstone.navisight.viu.ui.profile.ViuSettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.lang.Thread.sleep
import kotlin.time.Duration
import kotlin.time.DurationUnit

private const val tag = "VibrationHelper"
private const val defaultVibrationMilliseconds = 500L // Modify default here
private const val defaultDelayMilliseconds = 500L // Modify default here
private const val DEBOUNCE_PERIOD_MS = 500L

object VibrationHelper {
    private val helperScope = CoroutineScope(Dispatchers.Main + Job())
    private val vibrationFlow = MutableSharedFlow<Pair<Context, List<HapticEvent>>>()

    init {
        helperScope.launch {
            vibrationFlow
                .debounce(DEBOUNCE_PERIOD_MS)
                .collect { (context, pattern) ->
                    performVibrationPattern(context, pattern)
                }
        }
    }

    // Vibrate regardless of setting
    fun emergencyVibrate(context: Context?, milliseconds:Long=defaultVibrationMilliseconds){
        val vibrator = context?.getSystemService(Vibrator::class.java)
        vibrator?.vibrate(
            VibrationEffect.createOneShot(milliseconds,
                VibrationEffect.DEFAULT_AMPLITUDE))
        Log.i(tag, "Vibrated for $milliseconds milliseconds")
    }

    // Vibrate - default func.
    fun vibrate(context: Context?, milliseconds: Long = defaultVibrationMilliseconds) {
        if (context == null || !ViuSettingsManager.getBoolean(
                context,
                ViuSettingsManager.KEY_VIBRATION,
                true)) {
            Log.i(tag, "Vibration is disabled in settings. Skipping vibration.")
            return
        }
        val singlePulse = listOf(HapticEvent(milliseconds, isVibration = true))
        helperScope.launch {
            vibrationFlow.emit(Pair(context, singlePulse))
            Log.d(tag, "Single pulse event emitted. Awaiting debounce.")
        }
    }

    // Overloaded default func. for handling kotlin durasyon
    fun vibrate(context: Context?, milliseconds: Duration = VIBRATE_DEFAULT_DURATION) {
        if (context == null || !ViuSettingsManager.getBoolean(
                context,
                ViuSettingsManager.KEY_VIBRATION,
                true)) {
            Log.i(tag, "Vibration is disabled in settings. Skipping vibration.")
            return
        }
        val singlePulse = listOf(HapticEvent(
            milliseconds.toLong(DurationUnit.MILLISECONDS), isVibration = true))
        helperScope.launch {
            vibrationFlow.emit(Pair(context, singlePulse))
            Log.d(tag, "Single pulse event emitted. Awaiting debounce.")
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

    fun vibratePattern(context: Context?, pattern: List<HapticEvent>) {
        if (context == null || !ViuSettingsManager.getBoolean(
                context,
                ViuSettingsManager.KEY_VIBRATION,
                true)) {
            Log.i(tag, "Vibration is disabled or context is null. Skipping pattern.")
            return
        }
        helperScope.launch {
            vibrationFlow.emit(Pair(context, pattern))
            Log.d(tag, "Pattern event emitted. Awaiting debounce.")
        }
    }

    private fun performVibrationPattern(context: Context, pattern: List<HapticEvent>) {
        val vibrator = context.getSystemService(Vibrator::class.java)
        val timings = pattern.map { it.durationMs }.toLongArray()
        val amplitudes = pattern.map {
            if (it.isVibration) VibrationEffect.DEFAULT_AMPLITUDE else 0
        }.toIntArray()
        // Log the pattern execution details
        Log.i(tag,
            "PATTERN TRIGGERED: Timings: ${timings.joinToString()}, " +
                    "Amplitudes: ${amplitudes.joinToString()}")
        vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }
}