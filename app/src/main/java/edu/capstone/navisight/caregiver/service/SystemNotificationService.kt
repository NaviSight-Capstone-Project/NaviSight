package edu.capstone.navisight.caregiver.service

import android.content.Context
import edu.capstone.navisight.caregiver.model.Viu
import edu.capstone.navisight.common.NaviSightNotificationManager

class SystemNotificationService(private val context: Context) {
    private val naviSightNotificationManager = NaviSightNotificationManager(context)

    fun showEmergencyAlert(viu: Viu, lastLocation: String) {
        naviSightNotificationManager.showEmergencyAlert(viu, lastLocation)
    }

    fun showLowBatteryAlert(viu: Viu) {
        naviSightNotificationManager.showLowBatteryAlert(viu)
    }
}