package edu.capstone.navisight.viu.ui.emergency

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.util.Log

class EmergencyStatusListener(
    private val onEmergencyDeactivated: () -> Unit
) {
    private val db = FirebaseDatabase.getInstance().getReference("viu_location")
    private val auth = FirebaseAuth.getInstance()
    private var listener: ValueEventListener? = null

    fun startListening() {
        val user = auth.currentUser ?: return
        val emergencyRef = db.child(user.uid).child("status").child("emergencyActivated")

        listener = emergencyRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isEmergencyActivated = snapshot.getValue(Boolean::class.java) ?: true

                if (!isEmergencyActivated) {
                    Log.d("EmergencyListener", "Realtime emergency flag set to false. Deactivating.")
                    onEmergencyDeactivated()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("EmergencyListener", "Database error: ${error.message}")
            }
        })
    }

    fun stopListening() {
        listener?.let {
            val user = auth.currentUser ?: return
            db.child(user.uid).child("status").child("emergencyActivated").removeEventListener(it)
        }
        listener = null
    }
}