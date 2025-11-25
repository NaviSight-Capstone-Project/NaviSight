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
    suspend fun sendPasswordResetEmail(email: String) = withContext(Dispatchers.IO) {
        // 1. Get the Public API Key
        val apiKey = FirebaseApp.getInstance().options.apiKey

        // 2. Setup the Connection
        val url = URL("https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=$apiKey")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            // 3. Construct JSON: { "requestType": "PASSWORD_RESET", "email": "..." }
            val jsonBody = JSONObject().apply {
                put("requestType", "PASSWORD_RESET")
                put("email", email)
            }

            // 4. Send Request
            val outputStream = OutputStreamWriter(connection.outputStream)
            outputStream.write(jsonBody.toString())
            outputStream.flush()
            outputStream.close()

            // 5. Check Response
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Success!
                return@withContext
            } else {
                // Parse the specific error from Google
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw Exception("Server Error $responseCode: $errorStream")
            }
        } finally {
            connection.disconnect()
        }
    }
}

