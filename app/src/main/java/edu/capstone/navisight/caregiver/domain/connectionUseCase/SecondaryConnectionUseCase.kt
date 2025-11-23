package edu.capstone.navisight.caregiver.domain.connectionUseCase

import edu.capstone.navisight.caregiver.data.repository.ConnectionRepository
import edu.capstone.navisight.caregiver.model.SecondaryPairingRequest
import edu.capstone.navisight.caregiver.model.RequestStatus

class SecondaryConnectionUseCase(
    private val repository: ConnectionRepository = ConnectionRepository()
) {

    suspend fun sendRequest(
        requesterUid: String,
        viuUid: String,
        viuName: String
    ): RequestStatus {
        return repository.requestSecondaryPairing(requesterUid, viuUid, viuName)
    }

    fun getPendingRequests(caregiverUid: String) =
        repository.getSecondaryPendingRequests(caregiverUid)

    suspend fun approveRequest(request: SecondaryPairingRequest) =
        repository.approveSecondaryRequest(request)

    suspend fun denyRequest(requestId: String) =
        repository.denySecondaryRequest(requestId)
}