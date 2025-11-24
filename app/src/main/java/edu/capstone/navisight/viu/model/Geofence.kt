package edu.capstone.navisight.viu.model

import com.google.firebase.firestore.GeoPoint

data class Geofence(
    val id: String = "",
    val viuUid: String = "",
    val name: String = "",
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val radius: Double = 0.0
)
