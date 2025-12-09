package edu.capstone.navisight.viu.ui.camera.managers

import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import edu.capstone.navisight.R
import edu.capstone.navisight.common.Constants.SP_IS_USER_WARNED_OF_LOWBAT
import edu.capstone.navisight.common.TextToSpeechHelper
import edu.capstone.navisight.viu.ViuHomeViewModel
import edu.capstone.navisight.viu.ui.camera.CameraFragment
import edu.capstone.navisight.viu.utils.BatteryAlertListener
import edu.capstone.navisight.viu.utils.BatteryStateReceiver

private const val BATTERY_HANDLER_TAG = "BatteryHandler"
private var HAS_BATTERY_BEEN_DETECTED_ONCE = false

class BatteryHandler (
    private val cameraFragment: CameraFragment,
    private val realTimeViewModel : ViuHomeViewModel
) : BatteryAlertListener {
    private val batteryReceiver: BatteryStateReceiver = BatteryStateReceiver(this)

    fun getBatteryReceiver(): BatteryStateReceiver {
        return batteryReceiver
    }

    override fun onBatteryLow() {
        showLowBatteryAlert()
        saveBatteryWarnFlag()
    }

    override fun onBatteryOkay() {
        cameraFragment.activity?.runOnUiThread {
            if (cameraFragment.batteryAlert?.isShowing == true) {
                cameraFragment.batteryAlert?.dismiss()
                cameraFragment.batteryAlert = null
            }
        }
        removeBatteryWarnFlag()
    }

     fun showLowBatteryAlert() {
        if (cameraFragment.batteryAlert?.isShowing == true || !cameraFragment.isAdded) {
            Log.w(BATTERY_HANDLER_TAG, "Battery alert already visible or fragment is not added. Ignoring.")
            return
        }

        TextToSpeechHelper.speak(cameraFragment.requireContext(), "Warning! Battery is low. Please charge your device.")

        try {
            val inflater = cameraFragment.requireActivity().layoutInflater
            val customLayout = inflater.inflate(R.layout.dialog_battery_alert, null)
            val btnAccept = customLayout.findViewById<Button>(R.id.ok)
            cameraFragment.batteryAlert = AlertDialog.Builder(cameraFragment.requireContext())
                .setView(customLayout)
                .setCancelable(true)
                .create()

            // Dismiss the dialog when the user taps any part of the custom layout's background
            customLayout.setOnClickListener {
                cameraFragment.batteryAlert?.dismiss()
            }
            cameraFragment.batteryAlert?.setCanceledOnTouchOutside(true)
            cameraFragment.batteryAlert?.window?.setBackgroundDrawableResource(R.drawable.bg_popup_rounded)

            // Dismiss action for both buttons
            val dismissAction: () -> Unit = {
                cameraFragment.batteryAlert?.dismiss()
                cameraFragment.batteryAlert = null
            }
            btnAccept.setOnClickListener { dismissAction() }
            cameraFragment.batteryAlert?.show()

        } catch (e: Exception) {
            Log.e(BATTERY_HANDLER_TAG, "Error showing battery alert: ${e.message}", e)
            Toast.makeText(cameraFragment.context, "Error showing battery alert.", Toast.LENGTH_LONG).show()
        }
    }

     fun checkInitialBatteryStatus() {
        val context = cameraFragment.context ?: return

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
            cameraFragment.activity?.runOnUiThread {
                onBatteryLow()
            }
        } else {
            Log.d(BATTERY_HANDLER_TAG, "Initial battery check found battery at $batteryPct%. Status is OK.")
            cameraFragment.activity?.runOnUiThread {
                if (cameraFragment.batteryAlert?.isShowing == true) {
                    onBatteryOkay()
                }
            }
        }
    }

     fun saveBatteryWarnFlag() {
         cameraFragment.sharedPreferences.edit { putBoolean(SP_IS_USER_WARNED_OF_LOWBAT, true) }
         realTimeViewModel.setUserLowBatteryDetected()
    }

     fun removeBatteryWarnFlag() {
         cameraFragment.sharedPreferences.edit { putBoolean(SP_IS_USER_WARNED_OF_LOWBAT, false) }
         realTimeViewModel.removeUserLowBatteryDetected()
    }
}