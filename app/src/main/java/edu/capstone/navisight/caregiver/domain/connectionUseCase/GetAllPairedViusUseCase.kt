package edu.capstone.navisight.caregiver.domain.connectionUseCase

import edu.capstone.navisight.caregiver.data.repository.ConnectionRepository
import edu.capstone.navisight.caregiver.model.Viu
import kotlinx.coroutines.flow.Flow

class GetAllPairedViusUseCase(
    private val connectionRepository: ConnectionRepository = ConnectionRepository()
) {
    operator fun invoke(caregiverUid: String): Flow<List<Viu>> {
        return connectionRepository.getAllPairedVius(caregiverUid)
    }
}