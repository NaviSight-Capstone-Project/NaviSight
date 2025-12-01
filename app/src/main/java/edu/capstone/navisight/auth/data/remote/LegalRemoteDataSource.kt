package edu.capstone.navisight.auth.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import edu.capstone.navisight.auth.model.LegalConsent
import kotlinx.coroutines.tasks.await

class LegalRemoteDataSource {

    private val firestore = FirebaseFirestore.getInstance()
    private val collectionName = "legal_documents"

    suspend fun saveLegalConsent(consent: LegalConsent): Result<Boolean> {
        return try {
            firestore.collection(collectionName)
                .document(consent.userUid)
                .set(consent)
                .await()
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}