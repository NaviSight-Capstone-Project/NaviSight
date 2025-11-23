package edu.capstone.navisight.caregiver.model

import com.google.firebase.Timestamp

data class SecondaryPairingRequest(
    var id: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val requesterUid: String = "",
    val status: String = "pending",
    val viuName: String = "",
    val viuUid: String = "",
    val requesterName: String = ""
)
