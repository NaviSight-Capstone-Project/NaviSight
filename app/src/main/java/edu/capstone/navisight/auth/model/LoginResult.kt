package edu.capstone.navisight.auth.model

sealed class LoginResult {
     data class Success(val collection: String) : LoginResult()

     object UserNotFoundInCollection : LoginResult()

     object InvalidCredentials : LoginResult()

    data class Error(val message: String) : LoginResult()
}