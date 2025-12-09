package edu.capstone.navisight.caregiver.model

data class ViuStatus(
    val state: String = "offline",
    val lowBattery: Boolean = false,
    val emergencyActivated: Boolean = false,
    val last_seen: Long = 0
)