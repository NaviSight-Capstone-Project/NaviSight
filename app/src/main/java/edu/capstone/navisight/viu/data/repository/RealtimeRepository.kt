package edu.capstone.navisight.viu.data.repository

/*

RealtimeRepository.kt

Formerly LocationRepository.kt
Updated to fit RealtimeDataSource changes

 */


import edu.capstone.navisight.viu.model.ViuLocation
import edu.capstone.navisight.viu.model.GeofenceItem
import edu.capstone.navisight.viu.model.GeofenceEvent
import edu.capstone.navisight.viu.data.remote.RealtimeDataSource

class RealtimeRepository(
    private val remote: RealtimeDataSource = RealtimeDataSource()
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

    suspend fun setUserEmergencyActivated() {
        remote.setUserEmergencyActivated()
    }

    suspend fun removeUserEmergencyActivated() {
        remote.removeUserEmergencyActivated()
    }

    suspend fun setUserLowBatteryDetected() {
        remote.setUserLowBatteryDetected()
    }

    suspend fun removeUserLowBatteryDetected() {
        remote.removeUserLowBatteryDetected()
    }

    suspend fun getUserLowBatteryDetected() : Boolean? {
        return remote.getUserLowBatteryDetected()
    }

    suspend fun getUserEmergencyActivated() : Boolean? {
        return remote.getUserEmergencyActivated()
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