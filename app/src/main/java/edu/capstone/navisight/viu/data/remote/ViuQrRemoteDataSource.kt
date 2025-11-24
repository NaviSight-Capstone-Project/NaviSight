package edu.capstone.navisight.viu.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import edu.capstone.navisight.viu.model.QR
import kotlinx.coroutines.tasks.await
import kotlin.collections.firstOrNull
import kotlin.jvm.java
import kotlin.text.ifEmpty

class ViuQrRemoteDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val qrCollection = firestore.collection("QR_Code")

    // Fetch QR by the VIU’s UID (not document ID)
    suspend fun getQrByViuUid(viuUid: String): QR? {
        val querySnapshot = qrCollection
            .whereEqualTo("viuUid", viuUid)
            .limit(1)
            .get()
            .await()

        return querySnapshot.documents.firstOrNull()?.toObject(QR::class.java)
    }

    // Save QR — use its qruid as Firestore document ID (random unique)
    suspend fun saveQr(qr: QR) {
        val docId = qr.QrUid.ifEmpty { qrCollection.document().id }
        qrCollection.document(docId).set(qr.copy(QrUid = docId)).await()
    }
}
