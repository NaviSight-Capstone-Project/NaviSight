package edu.capstone.navisight.viu.ui.camera.managers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import edu.capstone.navisight.R
import edu.capstone.navisight.common.Constants.SP_IS_USER_WARNED_OF_LOWBAT
import edu.capstone.navisight.common.TextToSpeechHelper
import edu.capstone.navisight.viu.utils.BatteryAlertListener
import edu.capstone.navisight.viu.utils.BatteryStateReceiver

private const val BATTERY_HANDLER_TAG = "BatteryHandler"
private var HAS_BATTERY_BEEN_DETECTED_ONCE = false

class BatteryHandler (
    private val sharedPreferences: SharedPreferences,
    private val context : Context,
    private val activity : Activity,
    private val isAdded : Boolean,
    private var batteryAlert: AlertDialog? = null
) : BatteryAlertListener {
    private val batteryReceiver: BatteryStateReceiver = BatteryStateReceiver(this)

    fun getBatteryReceiver(): BatteryStateReceiver {
        return batteryReceiver
    }

    override fun onBatteryLow() {
        if (sharedPreferences.getBoolean(
                SP_IS_USER_WARNED_OF_LOWBAT,
                false) || !HAS_BATTERY_BEEN_DETECTED_ONCE) {
            activity?.runOnUiThread {
                showLowBatteryAlert()
                saveBatteryWarnFlag()
                HAS_BATTERY_BEEN_DETECTED_ONCE = true
            }
        }
    }

    override fun onBatteryOkay() {
        activity?.runOnUiThread {
            if (batteryAlert?.isShowing == true) {
                batteryAlert?.dismiss()
                batteryAlert = null
                removeBatteryWarnFlag()
            }
        }
    }

     fun showLowBatteryAlert() {
        if (batteryAlert?.isShowing == true || !isAdded) {
            Log.w(BATTERY_HANDLER_TAG, "Battery alert already visible or fragment is not added. Ignoring.")
            return
        }

        TextToSpeechHelper.speak(context, "Warning! Battery is low. Please charge your device.")

        try {
            val inflater = activity.layoutInflater
            val customLayout = inflater.inflate(R.layout.dialog_battery_alert, null)
            val btnAccept = customLayout.findViewById<Button>(R.id.ok)
            batteryAlert = AlertDialog.Builder(activity)
                .setView(customLayout)
                .setCancelable(true)
                .create()

            // Dismiss the dialog when the user taps any part of the custom layout's background
            customLayout.setOnClickListener {
                batteryAlert?.dismiss()
            }
            batteryAlert?.setCanceledOnTouchOutside(true)
            batteryAlert?.window?.setBackgroundDrawableResource(R.drawable.bg_popup_rounded)

            // Dismiss action for both buttons
            val dismissAction: () -> Unit = {
                batteryAlert?.dismiss()
                batteryAlert = null
            }
            btnAccept.setOnClickListener { dismissAction() }
            batteryAlert?.show()

        } catch (e: Exception) {
            Log.e(BATTERY_HANDLER_TAG, "Error showing battery alert: ${e.message}", e)
            Toast.makeText(context, "Error showing battery alert.", Toast.LENGTH_LONG).show()
        }
    }

     fun checkInitialBatteryStatus() {
        val context = context ?: return

        // Get the sticky Intent that holds the current battery state
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let {
                filter -> context.registerReceiver(null, filter)
        }

        // Extract the percent and the scale (max level, always 100)
        val level: Int = batteryStatus?.getIntExtra(
            android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(
            android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1

        // Calculate the percentage
        val batteryPct: Float = if (level != -1 && scale != -1 && scale != 0) {
            level / scale.toFloat() * 100
        } else {
            0f
        }

        // Define the low battery threshold (default is 15)
        // TODO: Unify thresholds in a combined file?
        val lowThreshold = 15

        // Check if the battery percentage is below the threshold
        if (batteryPct <= lowThreshold) {
            Log.w(BATTERY_HANDLER_TAG, "Initial battery check found battery at $batteryPct%. Triggering alert.")
            activity?.runOnUiThread {
                onBatteryLow()
            }
        } else {
            Log.d(BATTERY_HANDLER_TAG, "Initial battery check found battery at $batteryPct%. Status is OK.")
            activity?.runOnUiThread {
                if (batteryAlert?.isShowing == true) {
                    onBatteryOkay()
                }
            }
        }
    }

     fun saveBatteryWarnFlag() {
        sharedPreferences.edit { putBoolean(SP_IS_USER_WARNED_OF_LOWBAT, true) }
    }

     fun removeBatteryWarnFlag() {
        sharedPreferences.edit { putBoolean(SP_IS_USER_WARNED_OF_LOWBAT, false) }
    }
}