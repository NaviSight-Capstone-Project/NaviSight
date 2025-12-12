package edu.capstone.navisight.caregiver.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import edu.capstone.navisight.R
import edu.capstone.navisight.caregiver.domain.notificationUseCase.SaveAlertUseCase
import edu.capstone.navisight.caregiver.domain.viuUseCase.GetConnectedViuUidsUseCase
import edu.capstone.navisight.caregiver.domain.viuUseCase.GetViuByUidUseCase
import edu.capstone.navisight.caregiver.model.AlertNotification
import edu.capstone.navisight.caregiver.model.AlertType
import edu.capstone.navisight.caregiver.ui.emergency.EmergencySignal
import edu.capstone.navisight.caregiver.ui.emergency.EmergencyViewModel
import edu.capstone.navisight.common.NaviSightNotificationManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.util.UUID


const val ACTION_EMERGENCY_ALERT = "edu.capstone.navisight.EMERGENCY_ALERT"
const val EXTRA_VIU_ID = "viu_id"
const val EXTRA_VIU_NAME = "viu_name"
const val EXTRA_LOCATION = "last_location"
const val EXTRA_TIMESTAMP = "timestamp"

class ViuMonitorService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    // Dependencies (Instantiate them here, assuming default constructors work)
    private val getConnectedViuUidsUseCase = GetConnectedViuUidsUseCase()
    private val getViuByUidUseCase = GetViuByUidUseCase()
    private lateinit var naviSightNotificationManager: NaviSightNotificationManager
    private val saveAlertUseCase = SaveAlertUseCase()

    // Make it similar to MainService
    private var lastEmergencySignal: EmergencySignal? = null

    companion object {
        const val NOTIFICATION_ID = 101
        const val CHANNEL_ID = NaviSightNotificationManager.MONITORING_CHANNEL_ID
        var INSTANCE: ViuMonitorService? = null
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        naviSightNotificationManager = NaviSightNotificationManager(applicationContext)
    }

    fun getCurrentEmergencySignal(): EmergencySignal? {
        return lastEmergencySignal
    }

    override fun onDestroy() {
        super.onDestroy()
        INSTANCE = null // Clear the static instance when the service is destroyed
        serviceJob.cancel() // Cancel all running coroutines when the service is destroyed
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start as a Foreground Service (required for long-running background tasks)
        startForeground(NOTIFICATION_ID, buildForegroundNotification())

        // Start monitoring VIUs
        startViuMonitoring()

        // Ensures the service restarts if it's killed by the OS (useful for reliability)
        return START_STICKY
    }


    override fun onBind(intent: Intent?): IBinder? {
        // Not using binding for this type of service
        return null
    }

    private fun buildForegroundNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("VIU Monitoring Active")
        .setContentText("Listening for emergency and low battery alerts.")
        .setSmallIcon(R.drawable.ic_logo) // Use your app icon
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true) // Makes it non-dismissible
        .build()

    private fun startViuMonitoring() {
        serviceScope.launch {
            // Map to hold the monitoring Job for each VIU, keyed by VIU UID
            val viuMonitoringJobs = mutableMapOf<String, Job>()

            // Collect the real-time list of connected VIU UIDs
            getConnectedViuUidsUseCase().collectLatest { viuUids ->
                if (viuUids == null) return@collectLatest

                // Cancel jobs for VIUs that were removed
                val removedUids = viuMonitoringJobs.keys - viuUids.toSet()
                removedUids.forEach { uid ->
                    viuMonitoringJobs[uid]?.cancel()
                    viuMonitoringJobs.remove(uid)
                }

                // Start monitoring for new VIUs
                viuUids.forEach { viuUid ->
                    if (!viuMonitoringJobs.containsKey(viuUid)) {
                        // Start a new coroutine job for each VIU's status monitoring
                        val job = serviceScope.launch {
                            monitorSingleViuStatus(viuUid)
                        }
                        viuMonitoringJobs[viuUid] = job
                    }
                }
            }
        }
    }

    private suspend fun monitorSingleViuStatus(viuUid: String) {
        var lastEmergencyStatus = false
        var lastBatteryStatus = false

        getViuByUidUseCase(viuUid).collect { viu ->
            val status = viu?.status
            val currentViu = viu

            if (status != null && currentViu != null) {
                val lastLocation = currentViu.location?.let { "${it.latitude}, ${it.longitude}" } ?: "Location Unknown"

                if (status.emergencyActivated && !lastEmergencyStatus) {
                    // trigger sys alert

                    val alertTimestamp = System.currentTimeMillis()

                    val emergencyAlert = AlertNotification(
                        id = UUID.randomUUID().toString(), // Generates a unique document ID
                        title = "Emergency Mode Activated",
                        message = "${currentViu.firstName} has triggered an emergency alert.",
                        type = AlertType.EMERGENCY,
                        viu = currentViu // Pass the full Viu object
                    )
                    lastEmergencySignal = EmergencySignal(
                        viuId = currentViu.uid,
                        viuName = "${currentViu.firstName} ${currentViu.lastName}",
                        lastLocation = lastLocation,
                        timestamp = alertTimestamp
                    )

                    naviSightNotificationManager.showEmergencyAlert(currentViu, lastLocation)

                    val intent = Intent(ACTION_EMERGENCY_ALERT).apply {
                        putExtra(EXTRA_VIU_ID, currentViu.uid)
                        putExtra(EXTRA_VIU_NAME, currentViu.firstName)
                        putExtra(EXTRA_LOCATION, lastLocation)
                        putExtra(EXTRA_TIMESTAMP, alertTimestamp)
                    }

                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
                    serviceScope.launch {
                        saveAlertUseCase(emergencyAlert)
                    }
                    Log.d("Broadcast", "Broadcast emergency detected and alerts have been fired ($ACTION_EMERGENCY_ALERT)!!!!!!!!!!!")
                }

                // low bat
                if (status.isLowBattery && !lastBatteryStatus) {
                    // system Alert
                    naviSightNotificationManager.showLowBatteryAlert(currentViu)

                    val batteryAlert = AlertNotification(
                        id = UUID.randomUUID().toString(), // Generates a unique document ID
                        title = "Low Battery Warning",
                        message = "${currentViu.firstName}'s device is running low on battery.",
                        type = AlertType.LOW_BATTERY,
                        viu = currentViu // Pass the full Viu object
                    )
                    serviceScope.launch {
                        saveAlertUseCase(batteryAlert)
                    }
                }

                lastEmergencyStatus = status.emergencyActivated
                lastBatteryStatus = status.isLowBattery
            }
        }
    }
}