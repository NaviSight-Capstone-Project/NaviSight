package edu.capstone.navisight.caregiver.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import edu.capstone.navisight.caregiver.model.Geofence
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class GeofenceDataSource (
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
){

    suspend fun addGeofence(geofence: Geofence) {
        try {
            firestore.collection("geofences").add(geofence).await()
        } catch (e: Exception) {
        }
    }

    suspend fun deleteGeofence(geofenceId: String) {
        if (geofenceId.isEmpty()) return

        try {
            firestore.collection("geofences").document(geofenceId).delete().await()
        } catch (e: Exception) {
        }
    }

    fun getGeofencesForViu(viuUid: String): Flow<List<Geofence>> = callbackFlow {
        val query = FirebaseFirestore.getInstance()
            .collection("geofences")
            .whereEqualTo("viuUid", viuUid)


        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val geofences = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Geofence::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(geofences)
        }

        awaitClose { listener.remove() }
    }

}