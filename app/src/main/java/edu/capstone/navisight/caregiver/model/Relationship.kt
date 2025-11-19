package edu.capstone.navisight.caregiver.model



data class Relationship(
    val caregiverUid: String = "",
    val viuUid: String = "",
    val primaryCaregiver: Boolean = false
)