package edu.capstone.navisight.auth.domain

import com.google.firebase.FirebaseNetworkException
import edu.capstone.navisight.auth.data.repository.AuthService
import edu.capstone.navisight.auth.model.LoginResult

class LoginUseCase(
    private val repository: AuthService = AuthService()
) {

    suspend operator fun invoke(email: String, password: String): LoginResult {
        return try {

            val user = repository.login(email, password)

            if (user == null) {
                  LoginResult.InvalidCredentials
            } else {
                   val collection = repository.getUserCollection(user.uid)
                if (collection != null) {
                    LoginResult.Success(collection)
                } else {
                    LoginResult.UserNotFoundInCollection //Since auth ang gamit baka na sa ibang collection
                }
            }
        } catch (e: FirebaseNetworkException) {
            LoginResult.Error("No internet connection. Please try again.")
        }

        catch (e: Exception) {
            LoginResult.Error(e.message ?: "An unknown error occurred")
        }
    }
}