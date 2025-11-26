package edu.capstone.navisight.caregiver.domain.connectionUseCase

import edu.capstone.navisight.caregiver.data.repository.ConnectionRepository
import edu.capstone.navisight.caregiver.model.RequestStatus

class UnpairViuUseCase(
    private val repository: ConnectionRepository = ConnectionRepository()
) {
    suspend fun invoke(caregiverUid: String, viuUid: String): RequestStatus {
        return repository.unpairViu(caregiverUid, viuUid)
    }
}