package edu.capstone.navisight.caregiver.domain.geofenceUseCase

import edu.capstone.navisight.caregiver.data.repository.GeofenceRepository

class DeleteGeofenceUseCase(
    private val geofenceRepository: GeofenceRepository = GeofenceRepository()
) {
    suspend operator fun invoke(geofenceId: String) {
        geofenceRepository.deleteGeofence(geofenceId)
    }
}