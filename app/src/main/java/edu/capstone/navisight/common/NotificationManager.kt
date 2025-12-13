package edu.capstone.navisight.common

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.net.Uri
import androidx.core.app.NotificationCompat
import edu.capstone.navisight.R
import edu.capstone.navisight.caregiver.model.Viu
import edu.capstone.navisight.caregiver.ui.feature_settings.CaregiverSettingsManager
import edu.capstone.navisight.viu.ui.profile.ViuSettingsManager


class NaviSightNotificationManager(private val context: Context) {
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Channel IDs (Public access for services/activities that use them)
    companion object {
        // For ViuMonitorService (Low Importance)
        const val MONITORING_CHANNEL_ID = "viu_monitoring_alerts"

        // For Caregiver System Alerts (High Importance)
        const val EMERGENCY_CHANNEL_ID = "emergency_alerts"

        // For Caregiver System Alerts (Default Importance)
        const val BATTERY_CHANNEL_ID = "battery_alerts"

        // For WebRTC MainService (High/Default Importance, depends on context)
        const val WEBRTC_CALL_CHANNEL_ID = "webrtc_call_alerts"
    }

    init {
        createAllNotificationChannels()
    }

    private fun isViuSoundAlertEnabled(): Boolean {
        return ViuSettingsManager.getBoolean(context,
            ViuSettingsManager.KEY_SOUND_ALERT, true)
    }

    private fun isCaregiverSoundAlertEnabled(): Boolean {
        return CaregiverSettingsManager.getBoolean(context,
            CaregiverSettingsManager.KEY_SOUND_ALERT, true)
    }

    private fun createAllNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val isViuSoundEnabled = isViuSoundAlertEnabled()
            val isCaregiverSoundEnabled = isCaregiverSoundAlertEnabled()

            // VIU Monitoring Service Channel (Low Priority)
            val monitoringChannel = NotificationChannel(
                MONITORING_CHANNEL_ID,
                "VIU Monitoring",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Continuous monitoring for emergency and low battery status of connected VIUs."
                setSound(null, null)
                enableVibration(false)
            }

            // Emergency Alerts Channel (High Priority)
            val emergencyChannel = NotificationChannel(
                EMERGENCY_CHANNEL_ID,
                "Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                if (!isCaregiverSoundEnabled) {
                    setSound(null, null)
                    enableVibration(false)
                }
            }

            // Battery Alerts Channel (Default Priority)
            val batteryChannel = NotificationChannel(
                BATTERY_CHANNEL_ID,
                "Battery Alerts",
                NotificationManager.IMPORTANCE_DEFAULT 
            ).apply {
                if (!isCaregiverSoundEnabled) {
                    setSound(null, null)
                    enableVibration(false)
                }
            }

            // WebRTC Call Service Channel (High Priority for foreground call service)
            val webrtcChannel = NotificationChannel(
                WEBRTC_CALL_CHANNEL_ID,
                "Ongoing Call Service",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notification for active video/audio calls."
                if (!isViuSoundEnabled) {
                    setSound(null, null)
                    enableVibration(false)
                    Log.d("NotificationManager", "WebRTC Call Channel created silently due to VIU user settings.")
                }
            }

            notificationManager.createNotificationChannel(monitoringChannel)
            notificationManager.createNotificationChannel(emergencyChannel)
            notificationManager.createNotificationChannel(batteryChannel)
            notificationManager.createNotificationChannel(webrtcChannel)
        }
    }

    private fun isViuAppNotificationEnabled(): Boolean {
        val isEnabled = ViuSettingsManager.getBoolean(
            context, ViuSettingsManager.KEY_APP_NOTIFICATION, true)
        if (!isEnabled) {
            Log.d(
                "NotificationManager",
                "VIU app notifications are disabled in user settings.")
        }
        return isEnabled
    }

    private fun isCaregiverAppNotificationEnabled(): Boolean {
        val isEnabled = CaregiverSettingsManager.getBoolean(
            context, CaregiverSettingsManager.KEY_APP_NOTIFICATION, true)
        if (!isEnabled) {
            Log.d(
                "NotificationManager",
                "Caregiver app notifications are disabled in user settings.")
        }
        return isEnabled
    }

    fun showEmergencyAlert(viu: Viu, lastLocation: String) {
        if (!isViuAppNotificationEnabled() && !isCaregiverAppNotificationEnabled()) return
        val notification = NotificationCompat.Builder(context, EMERGENCY_CHANNEL_ID)
            .setContentTitle("🚨 EMERGENCY: ${viu.firstName}")
            .setContentText("Emergency Activated! Last known location: $lastLocation")
            .setSmallIcon(R.drawable.ic_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        // Use VIU UID hash code for a unique ID
        notificationManager.notify(viu.uid.hashCode(), notification)
    }

    fun showLowBatteryAlert(viu: Viu) {
        if (!isViuAppNotificationEnabled() && !isCaregiverAppNotificationEnabled()) return
        val notification = NotificationCompat.Builder(context, BATTERY_CHANNEL_ID)
            .setContentTitle("🔋 Low Battery: ${viu.firstName}")
            .setContentText("${viu.firstName}'s phone is running low on battery.")
            .setSmallIcon(R.drawable.ic_battery_alert)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        // Use a unique ID (VIU UID hash code + 1)
        notificationManager.notify(viu.uid.hashCode() + 1, notification)
    }

    // Foreground Service Notification Builders (Used by MainActivity & MainService)
    fun buildWebrtcServiceStartNotification(): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(context, WEBRTC_CALL_CHANNEL_ID)
        builder.setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Welcome to NaviSight")
            .setContentText("NaviSight successfully booted! Live calling is ready for use.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
        if (!isCaregiverSoundAlertEnabled() || !isViuSoundAlertEnabled()) {
            builder.setSound(null)
                .setVibrate(null)
                .setSilent(true)
        }
        return builder
    }

    fun buildViuMonitoringNotification(): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(context, MONITORING_CHANNEL_ID)
        builder.setSmallIcon(R.drawable.ic_logo)
            .setContentTitle("VIU Monitoring Active")
            .setContentText("Monitoring status of linked VIUs.")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        if (!isCaregiverSoundAlertEnabled()) {
            builder.setSound(null)
                .setVibrate(null)
                .setSilent(true)
        }
        return builder
    }
}