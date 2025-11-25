package edu.capstone.navisight.viu.data.repository

import edu.capstone.navisight.viu.model.ViuLocation
import edu.capstone.navisight.viu.model.GeofenceItem
import edu.capstone.navisight.viu.model.GeofenceEvent
import edu.capstone.navisight.viu.data.remote.LocationDataSource

class LocationRepository(
    private val remote: LocationDataSource = LocationDataSource()
) {
    private var cachedGeofences: List<GeofenceItem> = emptyList()

    suspend fun setupPresence() {
        remote.setupPresenceSystem()
    }

    suspend fun updateUserLocation(location: ViuLocation) {
        remote.updateUserLocation(location)
    }

    suspend fun setUserOffline() {
        remote.setUserOffline()
        remote.cleanup()
    }

    fun startGeofenceListener() {
        remote.listenToGeofences { newFences ->
            cachedGeofences = newFences
        }
    }

    fun getActiveGeofences(): List<GeofenceItem> {
        return cachedGeofences
    }

    suspend fun uploadGeofenceEvent(event: GeofenceEvent) {
        remote.uploadGeofenceEvent(event)
    }
}