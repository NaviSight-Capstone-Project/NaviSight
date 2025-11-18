package edu.capstone.navisight.common.data.remote

import com.google.firebase.auth.FirebaseAuth

class AuthDataSource(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    fun getCurrentUserUid(): String? {
        return auth.currentUser?.uid
    }
}