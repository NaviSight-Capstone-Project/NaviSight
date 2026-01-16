package edu.capstone.navisight.caregiver.domain

import android.net.Uri
import edu.capstone.navisight.caregiver.model.Caregiver
import edu.capstone.navisight.caregiver.data.repository.CaregiverRepository
import android.content.Context
import edu.capstone.navisight.auth.model.OtpResult

class CaregiverProfileUseCase(
    private val repository: CaregiverRepository = CaregiverRepository()
) {

    suspend fun getProfile(uid: String): Caregiver? {
        return repository.getProfile(uid)
    }

    suspend fun updateProfile(uid: String, updatedData: Map<String, Any>): Boolean {
        return repository.updateProfile(uid, updatedData)
    }

    suspend fun uploadProfileImage(uid: String, imageUri: Uri): Boolean {
        return repository.uploadAndSaveProfileImage(uid, imageUri)
    }

    suspend fun reauthenticateUser(password: String): Boolean {
        return repository.reauthenticateUser(password)
    }

    suspend fun updateEmail(
        context: Context,
        uid: String,
        newEmail: String,
        password: String
    ): OtpResult.ResendOtpResult {
        return repository.updateEmail(context, uid, newEmail, password)
    }

    suspend fun verifyEmailOtp(uid: String, enteredOtp: String): OtpResult.OtpVerificationResult {
        return repository.verifyEmailOtp(uid, enteredOtp)
    }

    // Add these to CaregiverProfileUseCase.kt
    suspend fun checkLockout(uid: String) = repository.checkLockout(uid)
    suspend fun handleFailedAttempt(uid: String) = repository.handleFailedAttempt(uid)
    suspend fun clearLockout(uid: String) = repository.clearLockout(uid)

    suspend fun requestPasswordChange(
        context: Context,
        currentPassword: String
    ): OtpResult.PasswordChangeRequestResult {
        return repository.requestPasswordChange(context, currentPassword)
    }

    suspend fun resendPasswordChangeOtp(context: Context): OtpResult.ResendOtpResult {
        return repository.resendPasswordChangeOtp(context)
    }

    suspend fun verifyPasswordChangeOtp(
        enteredOtp: String,
        newPassword: String
    ): OtpResult.OtpVerificationResult {
        return repository.verifyPasswordChangeOtp(enteredOtp, newPassword)
    }
    suspend fun cancelEmailChange(uid: String) {
        repository.cancelEmailChange(uid)
    }

    suspend fun cancelPasswordChange(uid: String) {
        repository.cancelPasswordChange(uid)
    }
}
