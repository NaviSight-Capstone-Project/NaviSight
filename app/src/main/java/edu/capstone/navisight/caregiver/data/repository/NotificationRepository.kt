package edu.capstone.navisight.caregiver.data.repository

import edu.capstone.navisight.caregiver.data.remote.NotificationDataSource
import edu.capstone.navisight.caregiver.model.GeofenceActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class NotificationRepository(
    private val remote: NotificationDataSource = NotificationDataSource()
) {

    // Logic: Paired Viu Events MINUS Dismissed IDs para ma ex ex siya yun siya anu HAAHHA BALIW
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
}