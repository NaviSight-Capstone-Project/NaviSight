package edu.capstone.navisight.auth.domain

import edu.capstone.navisight.auth.data.repository.AuthService
import edu.capstone.navisight.auth.model.LoginRequest


class LoginUseCase(
    private val repository: AuthService = AuthService()
) {
    suspend operator fun invoke(email: String, password: String): LoginRequest? {
        return repository.login(email, password)
    }

    suspend fun getUserCollection(uid: String): String? {
        return repository.getUserCollection(uid)
    }
}

