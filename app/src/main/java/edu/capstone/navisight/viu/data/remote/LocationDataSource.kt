package edu.capstone.navisight.viu.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import edu.capstone.navisight.viu.model.ViuLocation

class LocationDataSource {

    private val db = FirebaseDatabase.getInstance().getReference("viu_location")
    private val auth = FirebaseAuth.getInstance()

    private var connectedListener: ValueEventListener? = null

    suspend fun setupPresenceSystem() {
        val user = auth.currentUser ?: return
        val statusRef = db.child(user.uid).child("status")
        val connectionRef = FirebaseDatabase.getInstance().getReference(".info/connected")

        connectedListener = connectionRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false

                if (connected) {
                    Log.d("RTDB", "Device connected to Firebase. Arming presence.")

                    statusRef.child("state").onDisconnect().setValue("offline")
                    statusRef.child("last_seen").onDisconnect().setValue(ServerValue.TIMESTAMP)

                    statusRef.child("state").setValue("online")
                    statusRef.child("last_seen").setValue(ServerValue.TIMESTAMP)
                } else {
                    Log.d("RTDB", "Device disconnected from Firebase.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("RTDB", "Listener was cancelled: ${error.message}")
            }
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

        try {
            userRef.updateChildren(updateMap)
        } catch (e: Exception) {
            Log.e("RTDB", "Failed to update location: ${e.message}")
        }
    }

    suspend fun setUserOffline() {
        val user = auth.currentUser ?: return
        val statusRef = db.child(user.uid).child("status")

        statusRef.child("state").setValue("offline")
        statusRef.child("last_seen").setValue(ServerValue.TIMESTAMP)
    }
}