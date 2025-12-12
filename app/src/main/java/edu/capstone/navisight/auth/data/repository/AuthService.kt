package edu.capstone.navisight.auth.data.repository

import edu.capstone.navisight.auth.model.LoginRequest
import com.google.firebase.firestore.FirebaseFirestore
import edu.capstone.navisight.auth.data.remote.FirebaseAuthDataSource
import edu.capstone.navisight.common.Constants.USER_TYPE_CAREGIVER
import edu.capstone.navisight.common.Constants.USER_TYPE_VIU
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
        val viuDoc = firestore.collection(USER_TYPE_VIU).document(uid).get().await()
        if (viuDoc.exists()) return USER_TYPE_VIU

        val caregiverDoc = firestore.collection(USER_TYPE_CAREGIVER).document(uid).get().await()
        if (caregiverDoc.exists()) return USER_TYPE_CAREGIVER

        return null
    }
    suspend fun resetPassword(email: String) {
        remote.sendPasswordResetEmail(email)
    }
}