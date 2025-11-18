package edu.capstone.navisight.caregiver.domain.geofenceUseCase

import edu.capstone.navisight.caregiver.data.repository.GeofenceRepository
import edu.capstone.navisight.caregiver.model.Geofence
import kotlinx.coroutines.flow.Flow


class GetGeofencesByViuUseCase(
    private val geofenceRepository: GeofenceRepository = GeofenceRepository()
) {
    operator fun invoke(viuUid: String): Flow<List<Geofence>> {
        return geofenceRepository.getGeofencesForViu(viuUid)
    }
}