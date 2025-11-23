package edu.capstone.navisight.caregiver.data.repository

import edu.capstone.navisight.caregiver.data.remote.ConnectionDataSource
import edu.capstone.navisight.caregiver.model.QRModel
import edu.capstone.navisight.caregiver.model.RequestStatus
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

    suspend fun getQrCode(qrUid: String): QRModel? {
        return connectionDataSource.getQrByUid(qrUid)
    }

    suspend fun requestSecondaryPairing(
        requesterUid: String,
        viuUid: String,
        viuName: String
    ): RequestStatus {
        return connectionDataSource.sendSecondaryPairingRequest(requesterUid, viuUid, viuName)
    }
}