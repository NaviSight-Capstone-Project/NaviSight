package edu.capstone.navisight.auth.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class LegalConsent(
    val userUid: String = "",
    val email: String = "",
    val termsAgreed: Boolean = false,
    val privacyAgreed: Boolean = false,
    val documentVersion: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
)