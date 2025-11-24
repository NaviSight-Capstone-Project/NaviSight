package edu.capstone.navisight.auth.model

data class Viu(
    val uid: String = "",
    val email: String = "",
    val firstName: String = "",
    var middleName: String = "",
    var lastName: String = "",
    val phone: String = "",
    var profileImageUrl: String? = null,
    val category : String? = null,
    val address: String? = null
)