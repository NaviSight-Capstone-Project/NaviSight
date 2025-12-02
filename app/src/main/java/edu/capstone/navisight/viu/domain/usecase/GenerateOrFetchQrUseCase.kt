package edu.capstone.navisight.viu.domain.usecase

import android.graphics.Bitmap
import edu.capstone.navisight.viu.model.QR
import edu.capstone.navisight.viu.data.remote.QrGenerator
import edu.capstone.navisight.viu.data.repository.ViuQrRepository
import java.util.UUID

class GenerateOrFetchQrUseCase(
    private val repository: ViuQrRepository = ViuQrRepository()
) {
    suspend operator fun invoke(viuUid: String, name: String): Pair<QR, Bitmap> {
        // Check if a QR already exists for this VIU
        val existing = repository.getQr(viuUid)
        if (existing != null && existing.qrCodeData.isNotEmpty()) {
            val bitmap = QrGenerator.generateQr(existing.qrCodeData)
            return Pair(existing, bitmap)
        }

        // Generate a new random ID for this QR
        val newQrId = UUID.randomUUID().toString()

        // Data that will be encoded inside the QR code
        val qrData = "qruid:$newQrId|viuUid:$viuUid|name:$name"

        // Build the model
        val newModel = QR(
            qrUid = newQrId,
            viuUid = viuUid,
            name = name,
            qrCodeData = qrData
        )

        // Save to Firestore
        repository.saveQr(newModel)

        // Generate the QR image bitmap
        val qrBitmap = QrGenerator.generateQr(qrData)

        return Pair(newModel, qrBitmap)
    }
}
