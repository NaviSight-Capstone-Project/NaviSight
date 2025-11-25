package edu.capstone.navisight.caregiver.domain.notificationUseCase

import edu.capstone.navisight.caregiver.data.repository.NotificationRepository
import edu.capstone.navisight.caregiver.model.GeofenceActivity
import kotlinx.coroutines.flow.Flow

class GetActivityFeedUseCase(
    private val repository: NotificationRepository = NotificationRepository()
) {
    operator fun invoke(): Flow<List<GeofenceActivity>> {
        return repository.getActivityFeed()
    }
}