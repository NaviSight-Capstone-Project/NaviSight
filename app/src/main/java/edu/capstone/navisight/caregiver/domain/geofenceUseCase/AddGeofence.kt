package edu.capstone.navisight.caregiver.domain.geofenceUseCase

import edu.capstone.navisight.caregiver.data.repository.GeofenceRepository
import edu.capstone.navisight.caregiver.model.Geofence

class AddGeofenceUseCase(
    private val geofenceRepository: GeofenceRepository = GeofenceRepository()
) {
    suspend operator fun invoke(geofence: Geofence) {
        geofenceRepository.addGeofence(geofence)
    }
}