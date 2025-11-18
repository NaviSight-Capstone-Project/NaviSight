package edu.capstone.navisight.caregiver.model

import com.google.firebase.firestore.GeoPoint

data class Geofence(
    val id: String = "",
    val viuUid: String = "",
    val name: String = "",
    val location: GeoPoint? = null,
    val radius: Double = 0.0
)