package edu.capstone.navisight.viu.domain

import android.location.Location
import edu.capstone.navisight.viu.data.repository.LocationRepository
import edu.capstone.navisight.viu.model.GeofenceEvent
import com.google.firebase.auth.FirebaseAuth

class MonitorGeofenceUseCase(
    private val repository: LocationRepository
) {
    private val auth = FirebaseAuth.getInstance()

    private val fenceStateMap = mutableMapOf<String, Boolean>()

    fun startMonitoring() {
        repository.startGeofenceListener()
    }

    suspend operator fun invoke(lat: Double, lon: Double, viuName: String) {
        val currentUser = auth.currentUser ?: return
        val activeFences = repository.getActiveGeofences()

        val activeIds = activeFences.map { it.id }.toSet()
        fenceStateMap.keys.retainAll(activeIds)

        for (fence in activeFences) {
            val results = FloatArray(1)
            Location.distanceBetween(lat, lon, fence.location.latitude, fence.location.longitude, results)
            val distanceInMeters = results[0]

            val isCurrentlyInside = distanceInMeters <= fence.radius

            val wasInside = fenceStateMap[fence.id] ?: false

            if (isCurrentlyInside && !wasInside) {
                fenceStateMap[fence.id] = true
                sendEvent(currentUser.uid, viuName, fence.id, fence.name, "ENTER", lat, lon)
            }
            else if (!isCurrentlyInside && wasInside) {
                fenceStateMap[fence.id] = false
                sendEvent(currentUser.uid, viuName, fence.id, fence.name, "EXIT", lat, lon)
            }
        }
    }

    private suspend fun sendEvent(uid: String, name: String, fenceId: String, fenceName: String, type: String, lat: Double, lon: Double) {
        val event = GeofenceEvent(
            viuUid = uid,
            viuName = name,
            geofenceId = fenceId,
            geofenceName = fenceName,
            eventType = type,
            triggerLat = lat,
            triggerLng = lon
        )
        repository.uploadGeofenceEvent(event)
    }
}