package edu.capstone.navisight.viu.domain.locationUseCase

// Updated variables of locationRepository to realTimeRepository (9/12/2025)


import android.location.Location
import com.google.firebase.auth.FirebaseAuth
import edu.capstone.navisight.viu.data.repository.RealtimeRepository
import edu.capstone.navisight.viu.data.repository.ViuRepository
import edu.capstone.navisight.viu.model.GeofenceEvent
import edu.capstone.navisight.viu.model.GeofenceItem

class MonitorGeofenceUseCase(
    private val realTimeRepository: RealtimeRepository = RealtimeRepository(),
    private val viuRepository: ViuRepository = ViuRepository()
) {
    private val auth = FirebaseAuth.getInstance()
    private val fenceStateMap = mutableMapOf<String, Boolean>()

    private var cachedViuName: String? = null

    fun startMonitoring() {
        realTimeRepository.startGeofenceListener()
    }

    suspend operator fun invoke(lat: Double, lon: Double) {
        val currentUser = auth.currentUser ?: return

        if (cachedViuName == null) {
            try {
                val profile = viuRepository.getCurrentViuProfile()
                cachedViuName = "${profile.firstName} ${profile.lastName}"
            } catch (e: Exception) {
            }
        }
        val finalName = cachedViuName ?: "Viu User"

        val activeFences = realTimeRepository.getActiveGeofences()

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
                sendEvent(currentUser.uid, finalName, fence, "ENTER", lat, lon)
            }
            else if (!isCurrentlyInside && wasInside) {
                fenceStateMap[fence.id] = false
                sendEvent(currentUser.uid, finalName, fence, "EXIT", lat, lon)
            }
        }
    }

    private suspend fun sendEvent(uid: String, name: String, fence: GeofenceItem, type: String, lat: Double, lon: Double) {
        val event = GeofenceEvent(
            viuUid = uid,
            viuName = name,
            geofenceId = fence.id,
            geofenceName = fence.name,
            eventType = type,
            triggerLat = lat,
            triggerLng = lon
        )
        realTimeRepository.uploadGeofenceEvent(event)
    }
}