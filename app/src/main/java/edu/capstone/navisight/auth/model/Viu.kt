package edu.capstone.navisight.auth.model

data class Viu(
    val uid: String = "",
    val email: String = "",
    val firstName: String = "",
    var middleName: String = "",
    var lastName: String = "",
    val birthday: String = "",
    val phone: String = "",
    var profileImageUrl: String? = null,
    val category : String? = null,
    val address: String? = null,
    val isEmailVerified: Boolean = false, // Defaults to false
    val caregiverId: String? = null, // To store the ID of their primary caregiver
    var sex: String = "",
    var province: String = "",
    var city: String = "",
    val country : String = "Philippines"
)