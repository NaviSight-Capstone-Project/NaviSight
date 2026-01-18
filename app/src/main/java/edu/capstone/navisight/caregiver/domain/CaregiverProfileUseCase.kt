package edu.capstone.navisight.caregiver.domain

import android.net.Uri
import edu.capstone.navisight.caregiver.model.Caregiver
import edu.capstone.navisight.caregiver.data.repository.CaregiverRepository
import android.content.Context
import edu.capstone.navisight.auth.model.OtpResult
import edu.capstone.navisight.caregiver.data.repository.ConnectionRepository

class CaregiverProfileUseCase(
    private val repository: CaregiverRepository = CaregiverRepository(),
    private val connectionRepository: ConnectionRepository = ConnectionRepository()
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

    suspend fun deleteAccountSequence(uid: String, password: String): Result<Unit> {
        return try {
            // If the user is currently locked out, stop here.
            val lockoutCheck = repository.checkLockout(uid)
            if (lockoutCheck.isFailure) {
                return Result.failure(lockoutCheck.exceptionOrNull() ?: Exception("Account is locked."))
            }

            val reauth = repository.reauthenticateUser(password)
            if (!reauth) {
                // If password is wrong, we MUST tell the repository to increment the fail count
                val errorMsg = repository.handleFailedAttempt(uid)
                return Result.failure(Exception(errorMsg))
            }

            // If they got the password right, reset the counter to 0
            repository.clearLockout(uid)

            if (connectionRepository.isPrimaryForAny(uid)) {
                return Result.failure(Exception("Cannot delete account. You are currently a Primary Caregiver. Please transfer primary rights to another caregiver first."))
            }

            val relationshipsRemoved = connectionRepository.removeAllRelationships(uid)
            if (!relationshipsRemoved) {
                // Log warning but proceed
            }

            val deleted = repository.deleteAccount(uid)
            if (deleted) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete account. Please try again."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
