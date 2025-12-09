package edu.capstone.navisight.caregiver.model

data class ViuStatus(
    val state: String = "offline",
    val last_seen: Long = 0,

    @JvmField
    val isLowBattery: Boolean = false,
    @JvmField
    val emergencyActivated: Boolean = false
)