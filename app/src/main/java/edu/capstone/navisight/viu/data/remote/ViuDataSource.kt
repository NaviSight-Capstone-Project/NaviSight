package edu.capstone.navisight.viu.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.capstone.navisight.viu.model.Caregiver
import edu.capstone.navisight.viu.model.Viu
import kotlinx.coroutines.tasks.await
import kotlin.collections.firstOrNull
import kotlin.jvm.java

class ViuDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    suspend fun getCurrentViuProfile(): Viu {
        val currentUser = auth.currentUser ?: throw kotlin.Exception("User not logged in")
        val uid = currentUser.uid

        val doc = firestore.collection("vius").document(uid).get().await()
        if (!doc.exists()) throw kotlin.Exception("VIU profile not found")

        return doc.toObject(Viu::class.java) ?: throw kotlin.Exception("Invalid VIU data")
    }

    suspend fun getRegisteredCaregiver(): Caregiver {
        val relationshipsCollection = firestore.collection("relationships")
        val caregiversCollection = firestore.collection("caregivers")
        val viuUid = getCurrentViuProfile().uid
        val querySnapshot = relationshipsCollection
            .whereEqualTo("viuUid", viuUid)
            .limit(1) // Assuming one VIU has one primary caregiver relationship
            // TODO: add more relationships
            .get()
            .await()
        val relationshipDoc = querySnapshot.documents.firstOrNull()
            ?: throw kotlin.Exception("No relationship found for VIU: $viuUid")
        val caregiverUid = relationshipDoc.getString("caregiverUid")
            ?: throw kotlin.Exception("Caregiver UID not found in relationship document")
        val caregiverDoc = caregiversCollection.document(caregiverUid).get().await()
        if (!caregiverDoc.exists()) {
            throw kotlin.Exception("Caregiver profile not found for UID: $caregiverUid")
        }
        return caregiverDoc.toObject(Caregiver::class.java)
            ?: throw kotlin.Exception("Invalid Caregiver data for UID: $caregiverUid")
    }
}
