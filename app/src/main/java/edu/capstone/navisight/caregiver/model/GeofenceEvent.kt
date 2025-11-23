package edu.capstone.navisight.caregiver.model

import com.google.firebase.Timestamp

data class GeofenceEvent(
    val viuUid: String = "",
    val geofenceId: String = "",
    val geofenceName: String = "",
    val eventType: String = "",
    val timestamp: Timestamp? = null
)