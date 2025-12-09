package edu.capstone.navisight.caregiver.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import edu.capstone.navisight.R
import edu.capstone.navisight.caregiver.domain.notificationUseCase.SaveAlertUseCase
import edu.capstone.navisight.caregiver.domain.viuUseCase.GetConnectedViuUidsUseCase
import edu.capstone.navisight.caregiver.domain.viuUseCase.GetViuByUidUseCase
import edu.capstone.navisight.caregiver.model.AlertNotification
import edu.capstone.navisight.caregiver.model.AlertType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.util.UUID

class ViuMonitorService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    // Dependencies (Instantiate them here, assuming default constructors work)
    private val getConnectedViuUidsUseCase = GetConnectedViuUidsUseCase()
    private val getViuByUidUseCase = GetViuByUidUseCase()
    private lateinit var notificationService: SystemNotificationService // Will be initialized later
    private val saveAlertUseCase = SaveAlertUseCase()

    companion object {
        const val NOTIFICATION_ID = 101
        const val CHANNEL_ID = "viu_monitor_channel"
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize the SystemNotificationService with the service context
        notificationService = SystemNotificationService(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start as a Foreground Service (required for long-running background tasks)
        startForeground(NOTIFICATION_ID, buildForegroundNotification())

        // Start monitoring VIUs
        startViuMonitoring()

        // Ensures the service restarts if it's killed by the OS (useful for reliability)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel() // Cancel all running coroutines when the service is destroyed
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
                    notificationService.showEmergencyAlert(currentViu, lastLocation)
                    val emergencyAlert = AlertNotification(
                        id = UUID.randomUUID().toString(), // Generates a unique document ID
                        title = "Emergency Mode Activated",
                        message = "${currentViu.firstName} has triggered an emergency alert.",
                        type = AlertType.EMERGENCY,
                        viu = currentViu // Pass the full Viu object
                    )
                    serviceScope.launch {
                        saveAlertUseCase(emergencyAlert)
                    }
                }

                // low bat
                if (status.isLowBattery && !lastBatteryStatus) {
                    // system Alert
                    notificationService.showLowBatteryAlert(currentViu)

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