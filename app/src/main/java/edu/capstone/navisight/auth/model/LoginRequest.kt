package edu.capstone.navisight.auth.model

data class LoginRequest(
    val uid: String = "",
    val email: String = ""
)