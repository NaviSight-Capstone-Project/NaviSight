package edu.capstone.navisight.caregiver.model

import com.google.firebase.Timestamp

data class GeofenceActivity(
    val id: String = "",

    val viuUid: String = "",
    val viuName: String = "",
    val geofenceId: String = "",
    val geofenceName: String = "",
    val eventType: String = "",
    val timestamp: Timestamp? = null
)