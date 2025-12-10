package edu.capstone.navisight.caregiver.ui.emergency

import com.google.firebase.database.FirebaseDatabase

object ViuEmergencyDataSource {
    private val db = FirebaseDatabase.getInstance().getReference("viu_location")
    fun removeUserEmergencyActivated(uid: String) {
        db.child(uid).child("status")
            .child("emergencyActivated").setValue(false)
    }
}