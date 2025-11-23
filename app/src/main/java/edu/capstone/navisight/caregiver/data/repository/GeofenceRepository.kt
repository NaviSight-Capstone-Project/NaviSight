package edu.capstone.navisight.caregiver.data.repository

import edu.capstone.navisight.caregiver.data.remote.GeofenceDataSource
import edu.capstone.navisight.caregiver.model.Geofence
import kotlinx.coroutines.flow.Flow

class GeofenceRepository (
    private val geofenceDataSource: GeofenceDataSource = GeofenceDataSource()
){

    suspend fun addGeofence(geofence: Geofence) {
        geofenceDataSource.addGeofence(geofence)
    }

    suspend fun deleteGeofence(geofenceId: String) {
        geofenceDataSource.deleteGeofence(geofenceId)
    }

    fun getGeofencesForViu(viuUid: String) = geofenceDataSource.getGeofencesForViu(viuUid)
}

