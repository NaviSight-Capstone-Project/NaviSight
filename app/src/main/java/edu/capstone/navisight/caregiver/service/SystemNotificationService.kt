package edu.capstone.navisight.caregiver.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import edu.capstone.navisight.R
import edu.capstone.navisight.caregiver.model.Viu

class SystemNotificationService(private val context: Context) {

    private val EMERGENCY_CHANNEL_ID = "emergency_alerts"
    private val BATTERY_CHANNEL_ID = "battery_alerts"

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Channel for critical alerts (e.g., Emergency)
        val emergencyChannel = NotificationChannel(
            EMERGENCY_CHANNEL_ID,
            "Emergency Alerts",
            NotificationManager.IMPORTANCE_HIGH // Use HIGH for a loud/banner alert
        )
        manager.createNotificationChannel(emergencyChannel)

        // Channel for non-critical alerts (e.g., Low Battery)
        val batteryChannel = NotificationChannel(
            BATTERY_CHANNEL_ID,
            "Battery Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(batteryChannel)
    }

    fun showEmergencyAlert(viu: Viu, lastLocation: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, EMERGENCY_CHANNEL_ID)
            .setContentTitle("🚨 EMERGENCY: ${viu.firstName}")
            .setContentText("Emergency Activated! Last known location: $lastLocation")
            .setSmallIcon(R.drawable.ic_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(viu.uid.hashCode(), notification)
    }

    fun showLowBatteryAlert(viu: Viu) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, BATTERY_CHANNEL_ID)
            .setContentTitle("🔋 Low Battery: ${viu.firstName}")
            .setContentText("${viu.firstName}'s phone is running low on battery.")
            .setSmallIcon(R.drawable.ic_battery_alert)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        // Use a unique ID (VIU UID + 1)
        notificationManager.notify(viu.uid.hashCode() + 1, notification)
    }
}