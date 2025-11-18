package edu.capstone.navisight.common.data.repository

import edu.capstone.navisight.common.data.remote.AuthDataSource

class AuthService(
    private val authDataSource: AuthDataSource = AuthDataSource()
) {
    fun getCurrentUserUid(): String? {
        return authDataSource.getCurrentUserUid()
    }
}