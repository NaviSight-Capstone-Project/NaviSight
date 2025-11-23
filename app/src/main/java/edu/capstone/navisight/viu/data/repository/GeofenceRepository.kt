package edu.capstone.navisight.viu.data.repository

import edu.capstone.navisight.viu.data.model.Geofence
import edu.capstone.navisight.viu.data.model.GeofenceEvent
import edu.capstone.navisight.viu.data.remote.GeofenceRemoteDataSource

class GeofenceRepository(
    private val remoteDataSource: GeofenceRemoteDataSource = GeofenceRemoteDataSource()
) {
    suspend fun getGeofences(viuUid: String): List<Geofence> =
        remoteDataSource.getGeofencesByViuUid(viuUid)

    suspend fun recordGeofenceEvent(event: GeofenceEvent) =
        remoteDataSource.addGeofenceEvent(event)
}
