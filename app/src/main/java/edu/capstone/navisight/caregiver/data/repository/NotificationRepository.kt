package edu.capstone.navisight.caregiver.data.repository

import edu.capstone.navisight.caregiver.data.remote.NotificationDataSource
import edu.capstone.navisight.caregiver.model.AlertNotification
import edu.capstone.navisight.caregiver.model.GeofenceActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class NotificationRepository(
    private val remote: NotificationDataSource = NotificationDataSource()
) {

    // Logic: Paired Viu Events MINUS Dismissed IDs
    fun getActivityFeed(): Flow<List<GeofenceActivity>> {
        return combine(
            remote.getRelevantEventsFlow(),
            remote.getUserDismissedIdsFlow()
        ) { relevantEvents, dismissedIds ->
            relevantEvents.filter { event ->
                !dismissedIds.contains(event.id)
            }
        }
    }

    suspend fun dismissActivity(activityId: String) {
        remote.dismissEvent(activityId)
    }

    // Save Alert (ViuMonitorService ito)
    suspend fun saveAlert(alert: AlertNotification): Result<Unit> {
        return remote.saveAlert(alert)
    }

    // Flow of unread alerts for the NotificationViewModel
    fun getUnreadAlerts(): Flow<List<AlertNotification>> {
        return combine(
            remote.getAlertsFlow(), // Data source provides all active alerts
            remote.getDismissedAlertIdsFlow() // Data source provides all dismissed alert IDs
        ) { relevantAlerts, dismissedIds ->
            relevantAlerts.filter { alert ->
                !dismissedIds.contains(alert.id)
            }
        }.map { alerts ->
            // Sort by timestamp descending (newest first)
            alerts.sortedByDescending { it.timestamp?.time ?: 0 }
        }
    }

    // Allows the ViewModel to dismiss an alert
    suspend fun dismissAlert(alertId: String) {
        remote.dismissAlert(alertId)
    }
}