package edu.capstone.navisight.viu.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import edu.capstone.navisight.viu.data.model.Geofence
import edu.capstone.navisight.viu.data.model.GeofenceEvent
import kotlinx.coroutines.tasks.await
import kotlin.collections.mapNotNull
import kotlin.jvm.java

class GeofenceRemoteDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val geofenceCollection = firestore.collection("geofences")
    private val geofenceEventsCollection = firestore.collection("geofence_events")

    suspend fun getGeofencesByViuUid(viuUid: String): List<Geofence> {
        val snapshot = geofenceCollection
            .whereEqualTo("viuUid", viuUid)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Geofence::class.java)?.copy(id = doc.id)
        }
    }

    suspend fun addGeofenceEvent(event: GeofenceEvent) {
        geofenceEventsCollection.add(event).await()
    }
}
