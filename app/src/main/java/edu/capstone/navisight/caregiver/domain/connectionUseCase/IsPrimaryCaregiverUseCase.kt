package edu.capstone.navisight.caregiver.domain.connectionUseCase

import edu.capstone.navisight.caregiver.data.repository.ConnectionRepository
import kotlinx.coroutines.flow.Flow

class IsPrimaryCaregiverUseCase(
    private val connectionRepository: ConnectionRepository = ConnectionRepository()
) {
    operator fun invoke(caregiverUid: String, viuUid: String): Flow<Boolean> {
        return connectionRepository.isPrimaryCaregiver(caregiverUid, viuUid)
    }
}

