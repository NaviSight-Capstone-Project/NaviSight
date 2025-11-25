package edu.capstone.navisight.viu.model

import com.google.firebase.firestore.GeoPoint
import java.util.Date

data class GeofenceItem(
    val id: String = "",
    val name: String = "",
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val radius: Double = 0.0,
    val viuUid: String = ""
)

data class GeofenceEvent(
    val viuUid: String,
    val viuName: String,
    val geofenceId: String,
    val geofenceName: String,
    val eventType: String,
    val triggerLat: Double,
    val triggerLng: Double,
    val timestamp: Date = Date()
)