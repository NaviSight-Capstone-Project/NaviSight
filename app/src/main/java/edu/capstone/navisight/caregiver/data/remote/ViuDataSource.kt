package edu.capstone.navisight.caregiver.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import edu.capstone.navisight.caregiver.model.Viu
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ViuDataSource(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val viusCollection = db.collection("vius")

    fun getViuDetails(viuUid: String): Flow<Viu?> = callbackFlow {
        val viuRef = viusCollection.document(viuUid)
        val listener = viuRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                close(error)
                return@addSnapshotListener
            }

            if (snapshot == null || !snapshot.exists()) {
                trySend(null)
                return@addSnapshotListener
            }
            val viu = snapshot.data?.let { data ->
                Viu(
                    uid = snapshot.getString("uid") ?: "",
                    firstName = snapshot.getString("firstName") ?: "",
                    middleName = snapshot.getString("middleName") ?: "",
                    lastName = snapshot.getString("lastName") ?: "",
                    birthday = snapshot.getString("birthday") ?: "",
                    sex = snapshot.getString("sex") ?: "",
                    email = snapshot.getString("email") ?: "",
                    phone = snapshot.getString("phone") ?: "",
                    location = null,
                    profileImageUrl = snapshot.getString("profileImageUrl"),
                    status = snapshot.getString("status"),
                    address = snapshot.getString("address")
                )
            }
            trySend(viu)
        }
        awaitClose { listener.remove() }
    }
}