package edu.capstone.navisight.caregiver.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import edu.capstone.navisight.caregiver.model.GeofenceActivity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class TravelLogDataSource {
    private val db = FirebaseFirestore.getInstance()

    fun getTravelLogs(viuUid: String): Flow<List<GeofenceActivity>> = callbackFlow {
        Log.d("TravelLog", "Fetching logs for VIU: $viuUid")

        val listener = db.collection("geofence_events")
            .whereEqualTo("viuUid", viuUid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("TravelLog", "Firestore Error: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val events = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(GeofenceActivity::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            Log.e("TravelLog", "Error parsing doc: ${doc.id}", e)
                            null
                        }
                    }
                    Log.d("TravelLog", "Found ${events.size} events")
                    trySend(events)
                } else {
                    Log.d("TravelLog", "Snapshot empty or null")
                    trySend(emptyList())
                }
            }
        awaitClose { listener.remove() }
    }
}