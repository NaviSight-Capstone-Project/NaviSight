package edu.capstone.navisight.viu.domain.usecase

import com.google.firebase.firestore.GeoPoint
import edu.capstone.navisight.viu.model.Geofence
import kotlin.math.*

class CheckGeofenceStatusUseCase {

    fun isInsideGeofence(currentLocation: GeoPoint, geofence: Geofence): Boolean {
        val distance = distanceBetween(
            currentLocation.latitude,
            currentLocation.longitude,
            geofence.location.latitude,
            geofence.location.longitude
        )
        return distance <= geofence.radius
    }

    private fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
}
