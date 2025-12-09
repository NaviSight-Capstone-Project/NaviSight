package edu.capstone.navisight.caregiver.domain.notificationUseCase

import edu.capstone.navisight.caregiver.data.repository.NotificationRepository
import edu.capstone.navisight.caregiver.model.AlertNotification

class SaveAlertUseCase(
    private val notificationRepository: NotificationRepository = NotificationRepository() // Assuming default constructor
) {
    suspend operator fun invoke(alert: AlertNotification): Result<Unit> {
        return notificationRepository.saveAlert(alert)
    }
}