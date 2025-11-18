package edu.capstone.navisight.caregiver.domain.viuUseCase


import edu.capstone.navisight.caregiver.model.Viu
import edu.capstone.navisight.caregiver.data.repository.ViuRepository
import kotlinx.coroutines.flow.Flow

class GetViuByUidUseCase(
    private val viuRepository: ViuRepository = ViuRepository()
) {
    operator fun invoke(uid: String): Flow<Viu?> {
        return viuRepository.getViuByUid(uid)
    }
}