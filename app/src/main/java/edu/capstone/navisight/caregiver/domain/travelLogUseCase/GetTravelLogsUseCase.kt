package edu.capstone.navisight.caregiver.domain.travelLogUseCase

import edu.capstone.navisight.caregiver.data.repository.TravelLogRepository
import edu.capstone.navisight.caregiver.model.GeofenceActivity
import kotlinx.coroutines.flow.Flow

class GetTravelLogsUseCase(
    private val repository: TravelLogRepository = TravelLogRepository()
) {
    operator fun invoke(viuUid: String): Flow<List<GeofenceActivity>> {
        return repository.getLogs(viuUid)
    }
}