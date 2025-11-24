package edu.capstone.navisight.viu.data.repository

import edu.capstone.navisight.viu.model.QR
import edu.capstone.navisight.viu.data.remote.ViuQrRemoteDataSource

class ViuQrRepository(
    private val remote: ViuQrRemoteDataSource = ViuQrRemoteDataSource()
) {
    suspend fun getQr(uid: String): QR? = remote.getQrByViuUid(uid)
    suspend fun saveQr(qr: QR) = remote.saveQr(qr)
}
