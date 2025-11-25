package edu.capstone.navisight.caregiver.domain.notificationUseCase

import edu.capstone.navisight.caregiver.data.repository.NotificationRepository

class DismissActivityUseCase(
    private val repository: NotificationRepository = NotificationRepository()
) {
    suspend operator fun invoke(activityId: String) {
        repository.dismissActivity(activityId)
    }
}