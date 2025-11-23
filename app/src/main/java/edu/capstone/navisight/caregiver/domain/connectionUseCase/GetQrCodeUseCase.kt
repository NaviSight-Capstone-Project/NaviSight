package edu.capstone.navisight.caregiver.domain.connectionUseCase

import edu.capstone.navisight.caregiver.data.repository.ConnectionRepository
import edu.capstone.navisight.caregiver.model.QRModel

class GetQrCodeUseCase(
    private val connectionRepository: ConnectionRepository = ConnectionRepository()
) {
    suspend operator fun invoke(qrUid: String): QRModel? {
        return connectionRepository.getQrCode(qrUid)
    }
}