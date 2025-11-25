package edu.capstone.navisight.viu.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import edu.capstone.navisight.viu.model.GeofenceEvent
import edu.capstone.navisight.viu.model.GeofenceItem
import edu.capstone.navisight.viu.model.ViuLocation
import kotlinx.coroutines.tasks.await

class LocationDataSource {

    private val db = FirebaseDatabase.getInstance().getReference("viu_location")
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var connectedListener: ValueEventListener? = null
    private var geofenceListener: ListenerRegistration? = null

    suspend fun setupPresenceSystem() {
        val user = auth.currentUser ?: return
        val statusRef = db.child(user.uid).child("status")
        val connectionRef = FirebaseDatabase.getInstance().getReference(".info/connected")

        connectedListener = connectionRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    statusRef.child("state").onDisconnect().setValue("offline")
                    statusRef.child("last_seen").onDisconnect().setValue(ServerValue.TIMESTAMP)
                    statusRef.child("state").setValue("online")
                    statusRef.child("last_seen").setValue(ServerValue.TIMESTAMP)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    suspend fun updateUserLocation(location: ViuLocation) {
        val user = auth.currentUser ?: return
        val userRef = db.child(user.uid)
        val updateMap = mapOf(
            "location/latitude" to location.latitude,
            "location/longitude" to location.longitude,
            "location/timestamp" to location.timestamp,
            "status/last_seen" to ServerValue.TIMESTAMP,
            "status/state" to "online"
        )
        try { userRef.updateChildren(updateMap).await() } catch (e: Exception) { }
    }

    suspend fun setUserOffline() {
        val user = auth.currentUser ?: return
        db.child(user.uid).child("status").child("state").setValue("offline")
    }

    fun listenToGeofences(onGeofencesUpdated: (List<GeofenceItem>) -> Unit) {
        val user = auth.currentUser ?: return
        val query = firestore.collection("geofences").whereEqualTo("viuUid", user.uid)

        geofenceListener = query.addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener

            val fences = snapshot.documents.mapNotNull { doc ->
                doc.toObject(GeofenceItem::class.java)?.copy(id = doc.id)
            }
            onGeofencesUpdated(fences)
        }
    }
    suspend fun uploadGeofenceEvent(event: GeofenceEvent) {
        try {
            firestore.collection("geofence_events").add(event).await()
            Log.d("Remote", "Geofence Event Sent: ${event.eventType}")
        } catch (e: Exception) {
            Log.e("Remote", "Failed to send event: ${e.message}")
        }
    }

    fun cleanup() {
        geofenceListener?.remove()
        connectedListener?.let { /* remove RTDB listener if needed */ }
    }
}