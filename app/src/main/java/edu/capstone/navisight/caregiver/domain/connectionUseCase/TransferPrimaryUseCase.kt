package edu.capstone.navisight.caregiver.domain.connectionUseCase

import edu.capstone.navisight.caregiver.data.repository.ConnectionRepository
import edu.capstone.navisight.caregiver.model.RequestStatus
import edu.capstone.navisight.caregiver.model.TransferPrimaryRequest
import kotlinx.coroutines.flow.Flow

class TransferPrimaryUseCase(
    private val repository: ConnectionRepository = ConnectionRepository()
) {

    // Used by Sender (Edit Profile Screen) to find people to transfer to
    suspend fun getCandidates(viuUid: String, currentUid: String): List<TransferPrimaryRequest> {
        return repository.getTransferCandidates(viuUid, currentUid)
    }

    // Used by Sender to initiate the transfer
    suspend fun sendRequest(request: TransferPrimaryRequest): RequestStatus {
        return repository.sendTransferRequest(request)
    }

    // Used by Receiver (Notification Screen) to listen for incoming requests
    fun getIncomingRequests(myUid: String): Flow<List<TransferPrimaryRequest>> {
        return repository.getIncomingTransferRequests(myUid)
    }

    // Used by Receiver to Accept the transfer
    suspend fun approveRequest(request: TransferPrimaryRequest): RequestStatus {
        return repository.approveTransferRequest(request)
    }

    // Used by Receiver to Decline the transfer
    suspend fun denyRequest(requestId: String): RequestStatus {
        return repository.denyTransferRequest(requestId)
    }

    suspend fun getCurrentCaregiverName(uid: String): String {
        return repository.getCaregiverName(uid)
    }
}