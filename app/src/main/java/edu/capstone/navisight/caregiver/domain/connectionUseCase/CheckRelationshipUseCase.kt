package edu.capstone.navisight.caregiver.domain.connectionUseCase

import edu.capstone.navisight.caregiver.data.repository.ConnectionRepository

class CheckRelationshipUseCase(
    private val repository: ConnectionRepository = ConnectionRepository()
) {
    suspend operator fun invoke(caregiverUid: String, viuUid: String): Boolean {
        return repository.checkIfPaired(caregiverUid, viuUid)
    }
}