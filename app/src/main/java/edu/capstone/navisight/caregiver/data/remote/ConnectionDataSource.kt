package edu.capstone.navisight.caregiver.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import edu.capstone.navisight.caregiver.model.Viu
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val TAG = "ConnectionDataSource"

class ConnectionDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun getAllPairedVius(caregiverUid: String): Flow<List<Viu>> = callbackFlow {

        var viuListener: ListenerRegistration? = null

        val relationshipListener = firestore.collection("relationships")
            .whereEqualTo("caregiverUid", caregiverUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching relationships", error)
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
                                Log.e(TAG, "Error fetching VIU profiles", viuErr)
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