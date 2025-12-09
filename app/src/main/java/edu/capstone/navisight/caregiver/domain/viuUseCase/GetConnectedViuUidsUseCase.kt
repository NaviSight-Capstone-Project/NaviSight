package edu.capstone.navisight.caregiver.domain.viuUseCase

import edu.capstone.navisight.caregiver.data.repository.ViuRepository // Assume this exists
import kotlinx.coroutines.flow.Flow

class GetConnectedViuUidsUseCase(
    private val viuRepository: ViuRepository = ViuRepository()
) {
    operator fun invoke(): Flow<List<String>?> {
        return viuRepository.getConnectedViuUids()
    }
}