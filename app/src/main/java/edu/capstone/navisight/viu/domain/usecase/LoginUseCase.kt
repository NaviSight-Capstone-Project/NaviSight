package edu.capstone.navisight.viu.domain.usecase

import edu.capstone.navisight.viu.model.Viu
import edu.capstone.navisight.viu.data.repository.AuthRepository

class LoginUseCase(
    private val repository: AuthRepository = AuthRepository()
) {
    suspend operator fun invoke(email: String, password: String): Viu? {
        return repository.login(email, password)
    }
}