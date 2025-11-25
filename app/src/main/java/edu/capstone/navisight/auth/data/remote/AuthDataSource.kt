package edu.capstone.navisight.auth.data.remote

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class FirebaseAuthDataSource(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun login(email: String, password: String): FirebaseUser? {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        return result.user
    }
    suspend fun sendPasswordResetEmail(email: String) {
        // .await() suspends until success or throws an exception on failure
        firebaseAuth.sendPasswordResetEmail(email).await()
    }
}

