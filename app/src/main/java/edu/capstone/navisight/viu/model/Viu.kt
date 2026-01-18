package edu.capstone.navisight.viu.model

data class Viu(
    val uid: String = "",
    val email: String = "",
    val firstName: String = "",
    var middleName: String = "",
    var lastName: String = "",
    val phone: String = "",
    var profileImageUrl: String? = null,
    val status : String? = null,
    val address: String? = null,
    val country : String = "Philippines",
    var province: String = "",
    var city: String = "",
)