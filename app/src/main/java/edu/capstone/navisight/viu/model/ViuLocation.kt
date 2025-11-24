package edu.capstone.navisight.viu.model

data class ViuLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)
