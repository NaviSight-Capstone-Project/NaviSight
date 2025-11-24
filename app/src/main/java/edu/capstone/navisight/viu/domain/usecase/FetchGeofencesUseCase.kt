package edu.capstone.navisight.viu.domain.usecase

import edu.capstone.navisight.viu.data.repository.GeofenceRepository
import edu.capstone.navisight.viu.model.Geofence

class FetchGeofencesUseCase(
    private val repository: GeofenceRepository = GeofenceRepository()
) {
    suspend operator fun invoke(viuUid: String): List<Geofence> {
        return repository.getGeofences(viuUid)
    }
}
