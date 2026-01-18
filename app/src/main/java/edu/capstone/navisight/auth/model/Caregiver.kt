package edu.capstone.navisight.auth.model

import com.google.firebase.Timestamp

data class Caregiver(
    val uid: String = "",
    var firstName: String = "",
    var middleName: String = "",
    var lastName: String = "",
    var email: String = "",
    var phoneNumber: String = "",
    var profileImageUrl: String? = null,
    var geofencingAlerts: Boolean = true,
    var locationSharing: Boolean = true,
    var address: String = "",
    var birthday: Timestamp = Timestamp.now(),
    var sex: String = "",
    val isEmailVerified: Boolean = false, // Defaults to false
    val viuIds: List<String> = emptyList(),
    val country : String = "Philippines",
    var province: String = "",
    var city: String = "",

)