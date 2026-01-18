package edu.capstone.navisight.caregiver.model

data class Viu(
    val uid: String = "",
    var firstName: String = "",
    var middleName: String = "",
    var lastName: String = "",
    val birthday: String = "",
    val sex: String = "",
    var email: String = "",
    val phone: String = "",
    var location: ViuLocation? = null,
    var profileImageUrl: String? = null,
    val category : String? = null,
    var address: String? = null,
    val status: ViuStatus? = null,
    var caregiverType : String = "",
    val country : String = "Philippines",
    var province: String = "",
    var city: String = "",
    )


data class ViuLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = 0L
)