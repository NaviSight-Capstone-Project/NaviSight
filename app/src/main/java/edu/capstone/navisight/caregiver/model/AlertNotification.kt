package edu.capstone.navisight.caregiver.model

/*

AlertNotification.kt
If you have new ideas on how to structure new notifications, tapon lang dito.

 */

import java.util.Date

data class AlertNotification(
    // Change to var and provide default values
    var id: String = "",
    var title: String = "",
    var message: String = "",
    var type: AlertType = AlertType.EMERGENCY, // Provide a default AlertType
    var isViewed: Boolean = false,
    var viu: Viu = Viu(),
    var timestamp: Date? = Date(), // Can be nullable or have a default value
    var extraDetails: Map<String, Any?> = emptyMap()
)

enum class AlertType {
    EMERGENCY,
    LOW_BATTERY
}