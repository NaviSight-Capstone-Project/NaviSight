package edu.capstone.navisight.caregiver.model

/*

AlertNotification.kt
If you have new ideas on how to structure new notifications, tapon lang dito.

 */

import java.util.Date

data class AlertNotification(
    val id: String,
    val title: String, // e.g., "Emergency Mode Activated"
    val message: String, // e.g., "VIU Juan has triggered an emergency alert."
    val type: AlertType, // e.g., AlertType.EMERGENCY
    val isViewed: Boolean = false,
    val viu: Viu = Viu(),
    val timestamp: Date? = Date(),
    val extraDetails: Map<String, Any?> = emptyMap()
)

enum class AlertType {
    EMERGENCY,
    LOW_BATTERY
}