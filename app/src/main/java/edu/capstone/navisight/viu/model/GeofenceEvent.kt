package edu.capstone.navisight.viu.model

import com.google.firebase.Timestamp

data class GeofenceEvent(
    val viuUid: String = "",
    val geofenceId: String = "",
    val geofenceName: String = "",
    val eventType: String = "",
    val timestamp: Timestamp = Timestamp.now()
)
