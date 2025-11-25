package edu.capstone.navisight.auth.domain

import edu.capstone.navisight.auth.data.repository.AuthService

class ForgotPasswordUseCase(
    private val repository: AuthService = AuthService()
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        return try {
            if (email.isBlank()) {
                return Result.failure(Exception("Please enter your email address."))
            }
            // Basic regex check before hitting the network
            val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
            if (!emailRegex.matches(email)) {
                return Result.failure(Exception("Invalid email format."))
            }

            repository.resetPassword(email)
            Result.success(Unit)
        } catch (e: Exception) {
            // Firebase usually throws specific errors, we catch generic here for safety
            Result.failure(e)
        }
    }
}