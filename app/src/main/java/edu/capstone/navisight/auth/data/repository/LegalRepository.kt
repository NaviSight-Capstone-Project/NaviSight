package edu.capstone.navisight.auth.data.repository

import edu.capstone.navisight.auth.data.remote.LegalRemoteDataSource
import edu.capstone.navisight.auth.model.LegalConsent

class LegalRepository {

    private val remoteDataSource = LegalRemoteDataSource()

    suspend fun submitConsent(
        uid: String,
        email: String,
        termsAgreed: Boolean,
        privacyAgreed: Boolean,
        version: String
    ): Result<Boolean> {

        val consentModel = LegalConsent(
            userUid = uid,
            email = email,
            termsAgreed = termsAgreed,
            privacyAgreed = privacyAgreed,
            documentVersion = version
        )

        return remoteDataSource.saveLegalConsent(consentModel)
    }
}