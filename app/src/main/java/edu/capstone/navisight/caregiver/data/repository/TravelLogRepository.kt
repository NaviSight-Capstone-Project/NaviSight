package edu.capstone.navisight.caregiver.data.repository

import edu.capstone.navisight.caregiver.data.remote.TravelLogDataSource
import edu.capstone.navisight.caregiver.model.GeofenceActivity
import kotlinx.coroutines.flow.Flow

class TravelLogRepository(
    private val remote: TravelLogDataSource = TravelLogDataSource()
) {
    fun getLogs(viuUid: String): Flow<List<GeofenceActivity>> {
        return remote.getTravelLogs(viuUid)
    }
}