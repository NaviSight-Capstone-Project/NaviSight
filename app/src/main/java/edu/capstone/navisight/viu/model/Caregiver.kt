package edu.capstone.navisight.viu.model

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
    var sex: String = ""

    //wag na lang pakealaman yung order para di nakakasakit ng ulo idebug
)