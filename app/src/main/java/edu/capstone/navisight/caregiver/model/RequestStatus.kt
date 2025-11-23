package edu.capstone.navisight.caregiver.model

sealed class RequestStatus {
    object Success : RequestStatus()
    object AlreadySent : RequestStatus()
    data class Error(val message: String) : RequestStatus()
}