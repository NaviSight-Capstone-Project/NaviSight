package edu.capstone.navisight.viu.domain.usecase

import edu.capstone.navisight.viu.data.model.GeofenceEvent
import edu.capstone.navisight.viu.data.repository.GeofenceRepository

class RecordGeofenceEventUseCase(
    private val repository: GeofenceRepository = GeofenceRepository()
) {
    suspend operator fun invoke(event: GeofenceEvent) {
        repository.recordGeofenceEvent(event)
    }
}
