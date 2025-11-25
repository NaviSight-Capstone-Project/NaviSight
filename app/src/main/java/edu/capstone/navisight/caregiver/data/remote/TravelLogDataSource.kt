package edu.capstone.navisight.caregiver.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import edu.capstone.navisight.caregiver.model.GeofenceActivity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class TravelLogDataSource {
    private val db = FirebaseFirestore.getInstance()

    fun getTravelLogs(viuUid: String): Flow<List<GeofenceActivity>> = callbackFlow {
        val listener = db.collection("geofence_events")
            .whereEqualTo("viuUid", viuUid) // Filter by specific VIU
            .orderBy("timestamp", Query.Direction.DESCENDING) // Newest first
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val events = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(GeofenceActivity::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(events)
            }
        awaitClose { listener.remove() }
    }
}