package edu.capstone.navisight.auth.domain

import edu.capstone.navisight.auth.data.repository.AuthService

class GetUserCollectionUseCase(
    private val repository: AuthService = AuthService()
) {
    suspend operator fun invoke(uid: String): String? {
        return repository.getUserCollection(uid)
    }
}