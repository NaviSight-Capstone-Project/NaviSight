package edu.capstone.navisight.viu.domain.usecase

import edu.capstone.navisight.viu.data.model.Viu
import edu.capstone.navisight.viu.data.repository.ViuRepository

class GetViuProfileUseCase(
    private val repository: ViuRepository = ViuRepository()
) {
    suspend operator fun invoke(): Viu {
        return repository.getCurrentViuProfile()
    }
}
