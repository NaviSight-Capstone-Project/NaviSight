package edu.capstone.navisight.viu.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

private const val BATTERY_STATE_TAG = "BatteryState"

interface BatteryAlertListener {
    fun onBatteryLow()
    fun onBatteryOkay()
}

class BatteryStateReceiver (private val listener: BatteryAlertListener): BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BATTERY_LOW -> {
                Log.e(BATTERY_STATE_TAG, "Action: Battery Low Received")
                listener.onBatteryLow()
            }
            Intent.ACTION_BATTERY_OKAY -> {
                Log.d(BATTERY_STATE_TAG, "Action: Battery Okay Received")
                listener.onBatteryOkay()
            }
        }
    }
}