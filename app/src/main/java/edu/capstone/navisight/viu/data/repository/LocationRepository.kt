package edu.capstone.navisight.viu.data.repository

import edu.capstone.navisight.viu.model.ViuLocation
import edu.capstone.navisight.viu.data.remote.LocationDataSource

class LocationRepository(
    private val remote: LocationDataSource = LocationDataSource()
) {
    suspend fun setupPresence() {
        remote.setupPresenceSystem()
    }

    suspend fun updateUserLocation(location: ViuLocation) {
        remote.updateUserLocation(location)
    }

    suspend fun setUserOffline() {
        remote.setUserOffline()
    }
}