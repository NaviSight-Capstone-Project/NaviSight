package edu.capstone.navisight.viu.data.repository

import edu.capstone.navisight.auth.data.remote.FirebaseAuthDataSource
import edu.capstone.navisight.viu.model.Viu

class AuthRepository(
    private val remote: FirebaseAuthDataSource = FirebaseAuthDataSource()
) {
    suspend fun login(email: String, password: String): Viu? {
        val user = remote.login(email, password)
        return user?.let { Viu(it.uid, it.email ?: "") }
    }
}