package edu.capstone.navisight.caregiver.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import edu.capstone.navisight.caregiver.model.Viu
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class PairingDataSource() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var viuListener: ListenerRegistration? = null

    fun getAllPairedVius() = callbackFlow<List<Viu>> {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val relationshipsRef = firestore.collection("relationships")
            .whereEqualTo("caregiverUid", currentUid)

        val relationshipListener = relationshipsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val viuUids = snapshot?.documents?.mapNotNull { it.getString("viuUid") } ?: emptyList()

            if (viuUids.isEmpty()) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            viuListener?.remove()

            viuListener = firestore.collection("vius")
                .whereIn("uid", viuUids)
                .addSnapshotListener { viuSnap, viuErr ->
                    if (viuErr != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val viuList = viuSnap?.toObjects(Viu::class.java) ?: emptyList()
                    trySend(viuList)
                }
        }

        awaitClose {
            relationshipListener.remove()
            viuListener?.remove()
        }
    }
}