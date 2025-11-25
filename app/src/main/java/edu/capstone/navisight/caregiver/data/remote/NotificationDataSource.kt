package edu.capstone.navisight.caregiver.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import edu.capstone.navisight.caregiver.model.GeofenceActivity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NotificationDataSource {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Fetches events ONLY for Vius paired with this Caregiver
    fun getRelevantEventsFlow(): Flow<List<GeofenceActivity>> = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        var eventsListener: ListenerRegistration? = null

        // 1. Get paired Viu UIDs from Relationships
        val relationshipListener = db.collection("relationships")
            .whereEqualTo("caregiverUid", currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val pairedViuUids = snapshot?.documents?.mapNotNull { it.getString("viuUid") } ?: emptyList()

                eventsListener?.remove()

                if (pairedViuUids.isEmpty()) {
                    trySend(emptyList())
                } else {
                    // 2. Query events for these Vius (Limit 10 due to Firestore IN query constraint)
                    eventsListener = db.collection("geofence_events")
                        .whereIn("viuUid", pairedViuUids.take(10))
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(50)
                        .addSnapshotListener { eventSnap, eventError ->
                            if (eventError != null) {
                                trySend(emptyList())
                                return@addSnapshotListener
                            }

                            val events = eventSnap?.documents?.mapNotNull { doc ->
                                doc.toObject(GeofenceActivity::class.java)?.copy(id = doc.id)
                            } ?: emptyList()

                            trySend(events)
                        }
                }
            }

        awaitClose {
            relationshipListener.remove()
            eventsListener?.remove()
        }
    }

    // Listen to personal dismissed list
    fun getUserDismissedIdsFlow(): Flow<Set<String>> = callbackFlow {
        val currentUser = auth.currentUser ?: return@callbackFlow

        val listener = db.collection("users").document(currentUser.uid)
            .collection("dismissed_events")
            .addSnapshotListener { snapshot, _ ->
                val ids = snapshot?.documents?.map { it.id }?.toSet() ?: emptySet()
                trySend(ids)
            }
        awaitClose { listener.remove() }
    }

    // Add ID to personal blacklist
    suspend fun dismissEvent(eventId: String) {
        val currentUser = auth.currentUser ?: return
        val data = hashMapOf("timestamp" to com.google.firebase.Timestamp.now())

        try {
            db.collection("users").document(currentUser.uid)
                .collection("dismissed_events")
                .document(eventId)
                .set(data)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}