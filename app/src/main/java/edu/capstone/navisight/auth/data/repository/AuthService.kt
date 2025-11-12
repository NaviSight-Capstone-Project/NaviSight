package edu.capstone.navisight.auth.data.repository

import edu.capstone.navisight.auth.model.LoginRequest
import com.google.firebase.firestore.FirebaseFirestore
import edu.capstone.navisight.auth.data.remote.FirebaseAuthDataSource
import kotlinx.coroutines.tasks.await

class AuthService(
    private val remote: FirebaseAuthDataSource = FirebaseAuthDataSource(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun login(email: String, password: String): LoginRequest? {
        val firebaseUser = remote.login(email, password)
        return firebaseUser?.let { LoginRequest(it.uid, it.email ?: "") }
    }

    suspend fun getUserCollection(uid: String): String? {
        val viuDoc = firestore.collection("vius").document(uid).get().await()
        if (viuDoc.exists()) return "vius"

        val caregiverDoc = firestore.collection("caregivers").document(uid).get().await()
        if (caregiverDoc.exists()) return "caregivers"

        return null
    }
}