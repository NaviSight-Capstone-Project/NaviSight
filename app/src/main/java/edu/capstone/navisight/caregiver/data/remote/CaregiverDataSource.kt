package edu.capstone.navisight.caregiver.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import edu.capstone.navisight.caregiver.model.Caregiver
import kotlinx.coroutines.tasks.await
import kotlin.text.get

class CaregiverDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val usersCollection = firestore.collection("caregivers")

    suspend fun getProfile(uid: String): Caregiver? {
        val doc = usersCollection.document(uid).get().await()
        return doc.toObject(Caregiver::class.java)
    }
}

