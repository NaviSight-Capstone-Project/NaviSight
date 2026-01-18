package edu.capstone.navisight.caregiver.data.repository

import edu.capstone.navisight.caregiver.data.remote.ConnectionDataSource
import edu.capstone.navisight.caregiver.model.QRModel
import edu.capstone.navisight.caregiver.model.RequestStatus
import edu.capstone.navisight.caregiver.model.SecondaryPairingRequest
import edu.capstone.navisight.caregiver.model.TransferPrimaryRequest
import edu.capstone.navisight.caregiver.model.Viu
import kotlinx.coroutines.flow.Flow

class ConnectionRepository(
    private val connectionDataSource: ConnectionDataSource = ConnectionDataSource()
) {

    fun getAllPairedVius(caregiverUid: String): Flow<List<Viu>> {
        return connectionDataSource.getAllPairedVius(caregiverUid)
    }

    fun isPrimaryCaregiver(caregiverUid: String, viuUid: String): Flow<Boolean> {
        return connectionDataSource.isPrimaryCaregiver(caregiverUid, viuUid)
    }
    suspend fun isPrimaryForAny(uid: String) = connectionDataSource.isPrimaryForAny(uid)
    suspend fun removeAllRelationships(uid: String) = connectionDataSource.removeAllRelationships(uid)

    suspend fun getQrCode(qrUid: String): QRModel? {
        return connectionDataSource.getQrByUid(qrUid)
    }

    suspend fun checkIfPaired(caregiverUid: String, viuUid: String): Boolean {
        return connectionDataSource.checkIfRelationshipExists(caregiverUid, viuUid)
    }
    suspend fun requestSecondaryPairing(
        requesterUid: String,
        viuUid: String,
        viuName: String
    ): RequestStatus {
        return connectionDataSource.sendSecondaryPairingRequest(requesterUid, viuUid, viuName)
    }

    fun getSecondaryPendingRequests(caregiverUid: String) =
        connectionDataSource.getSecondaryPendingRequestsForCaregiver(caregiverUid)

    suspend fun approveSecondaryRequest(request: SecondaryPairingRequest): RequestStatus =
        connectionDataSource.approveSecondaryRequest(request)

    suspend fun denySecondaryRequest(requestId: String): RequestStatus =
        connectionDataSource.denySecondaryRequest(requestId)

    suspend fun getTransferCandidates(viuUid: String, currentUid: String): List<TransferPrimaryRequest> =
        connectionDataSource.getTransferCandidates(viuUid, currentUid)

    suspend fun sendTransferRequest(request: TransferPrimaryRequest): RequestStatus =
        connectionDataSource.sendTransferRequest(request)

    fun getIncomingTransferRequests(myUid: String): Flow<List<TransferPrimaryRequest>> =
        connectionDataSource.getIncomingTransferRequests(myUid)

    suspend fun approveTransferRequest(request: TransferPrimaryRequest): RequestStatus =
        connectionDataSource.approveTransferRequest(request)

    suspend fun denyTransferRequest(requestId: String): RequestStatus =
        connectionDataSource.denyTransferRequest(requestId)

    suspend fun getCaregiverName(uid: String): String {
        return connectionDataSource.getCaregiverName(uid)
    }
    suspend fun unpairViu(caregiverUid: String, viuUid: String): RequestStatus {
        return connectionDataSource.unpairViu(caregiverUid, viuUid)}
}