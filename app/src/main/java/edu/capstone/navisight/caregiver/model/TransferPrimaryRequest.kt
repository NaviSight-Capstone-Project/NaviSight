package edu.capstone.navisight.caregiver.model

import com.google.firebase.Timestamp

data class TransferPrimaryRequest(
    var id: String = "", // Document ID
    val createdAt: Timestamp = Timestamp.now(),
    val currentPrimaryCaregiverName: String = "",
    val currentPrimaryCaregiverUid: String = "",
    val recipientEmail: String = "",
    val recipientName: String = "",
    val recipientUid: String = "",
    val status: String = "pending", // "pending", "approved", "denied"
    val viuName: String = "",
    val viuUid: String = ""
)