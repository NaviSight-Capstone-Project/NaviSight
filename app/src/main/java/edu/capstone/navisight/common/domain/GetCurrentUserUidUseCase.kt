package edu.capstone.navisight.common.domain.usecase

import edu.capstone.navisight.common.data.repository.AuthService

class GetCurrentUserUidUseCase(
    private val authService: AuthService = AuthService()
) {
    operator fun invoke(): String? {
        return authService.getCurrentUserUid()
    }
}