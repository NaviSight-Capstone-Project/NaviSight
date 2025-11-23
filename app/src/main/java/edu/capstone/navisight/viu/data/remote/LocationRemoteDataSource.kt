package edu.capstone.navisight.viu.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import edu.capstone.navisight.viu.data.model.UserLocation

class LocationRemoteDataSource {

    private val db = FirebaseDatabase.getInstance().getReference("viu_location")
    private val auth = FirebaseAuth.getInstance()

    suspend fun updateUserLocation(location: UserLocation) {
        val user = auth.currentUser
        if (user == null) {
            Log.w("RTDB", "No logged-in user. Skipping location update.")
            return
        }

        val userId = user.uid
        val userRef = db.child(userId).child("location")

        try {
            userRef.setValue(location)
            Log.d("RTDB", "Updated location in RTDB: $location")
        } catch (e: Exception) {
            Log.e("RTDB", "Failed to update location: ${e.message}", e)
        }
    }
}
