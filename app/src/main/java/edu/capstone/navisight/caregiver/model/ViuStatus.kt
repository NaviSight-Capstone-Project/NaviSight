package edu.capstone.navisight.caregiver.model

data class ViuStatus(
    val state: String = "offline",
    val last_seen: Long = 0
)