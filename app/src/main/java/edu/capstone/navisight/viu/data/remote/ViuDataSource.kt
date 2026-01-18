package edu.capstone.navisight.viu.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.capstone.navisight.common.Constants.USER_TYPE_CAREGIVER
import edu.capstone.navisight.common.Constants.USER_TYPE_VIU
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

        val doc = firestore.collection(USER_TYPE_VIU).document(uid).get().await()
        if (!doc.exists()) throw kotlin.Exception("VIU profile not found")

        return doc.toObject(Viu::class.java) ?: throw kotlin.Exception("Invalid VIU data")
    }

    suspend fun getRegisteredCaregiver(): Caregiver {
        val relationshipsCollection = firestore.collection("relationships")
        val caregiversCollection = firestore.collection(USER_TYPE_CAREGIVER)
        val viuUid = getCurrentViuProfile().uid

        // Look only for primaryCaregiver
        val querySnapshot = relationshipsCollection
            .whereEqualTo("viuUid", viuUid)
            .whereEqualTo("primaryCaregiver", true)
            .limit(1)
            .get()
            .await()

        val relationshipDoc = querySnapshot.documents.firstOrNull()
            ?: throw kotlin.Exception("No primary caregiver relationship found for VIU: $viuUid")

        val caregiverUid = relationshipDoc.getString("caregiverUid")
            ?: throw kotlin.Exception("Caregiver UID not found in relationship document")

        val caregiverDoc = caregiversCollection.document(caregiverUid).get().await()
        if (!caregiverDoc.exists()) {
            throw kotlin.Exception("Caregiver profile not found for UID: $caregiverUid")
        }

        return caregiverDoc.toObject(Caregiver::class.java)
            ?: throw kotlin.Exception("Invalid Caregiver data for UID: $caregiverUid")
    }

    companion object {
        @Volatile
        private var INSTANCE: ViuDataSource? = null

        /**
         * Returns the singleton instance of ViuDataSource, creating it if necessary.
         */
        fun getInstance(
            firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
            auth: FirebaseAuth = FirebaseAuth.getInstance()
        ): ViuDataSource {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ViuDataSource(firestore, auth).also { INSTANCE = it }
            }
        }
    }
}
