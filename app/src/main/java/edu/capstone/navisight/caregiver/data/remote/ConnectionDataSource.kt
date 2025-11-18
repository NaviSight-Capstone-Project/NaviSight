package edu.capstone.navisight.caregiver.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import edu.capstone.navisight.caregiver.model.Viu
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class ConnectionDataSource(

    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

) {



    fun getAllPairedVius() = callbackFlow<List<Viu>> {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        var viuListener: ListenerRegistration? = null

        val relationshipListener = firestore.collection("relationships")
            .whereEqualTo("caregiverUid", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val viuUids = snapshot?.documents?.mapNotNull { it.getString("viuUid") } ?: emptyList()

                viuListener?.remove()

                if (viuUids.isEmpty()) {
                    trySend(emptyList())
                } else {
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
            }

        awaitClose {
            relationshipListener.remove()
            viuListener?.remove()
        }
    }
}