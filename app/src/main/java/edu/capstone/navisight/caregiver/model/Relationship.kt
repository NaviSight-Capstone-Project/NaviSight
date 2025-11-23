package edu.capstone.navisight.caregiver.model

import com.google.firebase.Timestamp


data class Relationship(
    val createdAt: Timestamp = Timestamp.now(),
    val caregiverUid: String = "",
    val viuUid: String = "",
    val primaryCaregiver: Boolean = false
)