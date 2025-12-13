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
        val singlePulse = listOf(HapticEvent(milliseconds, isVibration = true))
        vibratePattern(context, singlePulse)
    }

    fun vibrate(context: Context?) {
        if (context == null || !ViuSettingsManager.getBoolean(
                context,
                ViuSettingsManager.KEY_VIBRATION,
                true)) {
            Log.i(tag, "Vibration is disabled in settings. Skipping vibration.")
            return
        }
        val singlePulse = listOf(HapticEvent(defaultVibrationMilliseconds,
            isVibration = true))
        vibratePattern(context, singlePulse)
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
        vibratePattern(context, singlePulse)
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
        vibratePattern(context, singlePulse)
    }

    fun vibratePattern(context: Context?, pattern: List<HapticEvent>) {
        if (context == null || !ViuSettingsManager.getBoolean(
                context,
                ViuSettingsManager.KEY_VIBRATION,
                true)) {
            Log.i(tag, "Vibration is disabled or context is null. Skipping pattern.")
            return
        }

        // Debounce Bypass Logic
        val isShortSinglePulse = pattern.size == 1 &&
                pattern.first().isVibration &&
                pattern.first().durationMs < 100L // < 100ms is a safe UI tap threshold

        if (isShortSinglePulse) {
            // Execute immediately, bypassing the flow/debounce
            performVibrationPattern(context, pattern)
            Log.d(tag, "Single pulse executed immediately (bypassed debounce).")
        } else {
            // Use the debounced flow for complex or longer patterns
            helperScope.launch {
                vibrationFlow.emit(Pair(context, pattern))
                Log.d(tag, "Pattern event emitted to debounced flow.")
            }
        }
    }

    @Suppress("Deprecation")
    private fun performVibrationPattern(context: Context, pattern: List<HapticEvent>) {
        // Service Lookup
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // Modern, type-safe lookup for Android 12+
            context.getSystemService(Vibrator::class.java)
        } else {
            // Old, reliable string constant lookup for older/problematic OEMs
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        if (vibrator == null) {
            Log.e(tag, "Vibrator service not available on this device.")
            return
        }

        val timings = pattern.map { it.durationMs }.toLongArray()
        val amplitudes = pattern.map {
            if (it.isVibration) VibrationEffect.DEFAULT_AMPLITUDE else 0
        }.toIntArray()

        // Log the pattern execution details
        Log.i(tag,
            "PATTERN TRIGGERED: Timings: ${timings.joinToString()}, " +
                    "Amplitudes: ${amplitudes.joinToString()}")
        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }
}