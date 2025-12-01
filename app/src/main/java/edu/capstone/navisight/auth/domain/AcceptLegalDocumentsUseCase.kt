package edu.capstone.navisight.auth.domain.usecase

import edu.capstone.navisight.auth.data.repository.LegalRepository

class AcceptLegalDocumentsUseCase {

    private val repository = LegalRepository()

    suspend operator fun invoke(
        uid: String,
        email: String,
        termsAccepted: Boolean,
        privacyAccepted: Boolean,
        version: String
    ): Result<Boolean> {

        if (!termsAccepted || !privacyAccepted) {
            return Result.failure(Exception("User must accept both Terms and Privacy Policy."))
        }

        return repository.submitConsent(
            uid = uid,
            email = email,
            termsAgreed = termsAccepted,
            privacyAgreed = privacyAccepted,
            version = version
        )
    }
}