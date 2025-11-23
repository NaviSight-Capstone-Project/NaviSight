package edu.capstone.navisight.caregiver.domain.connectionUseCase

import edu.capstone.navisight.caregiver.data.repository.ConnectionRepository
import edu.capstone.navisight.caregiver.model.RequestStatus

class SendSecondaryPairingRequestUseCase(
    private val repository: ConnectionRepository = ConnectionRepository()
) {
    suspend operator fun invoke(
        requesterUid: String,
        viuUid: String,
        viuName: String
    ): RequestStatus {
        return repository.requestSecondaryPairing(requesterUid, viuUid, viuName)
    }
}