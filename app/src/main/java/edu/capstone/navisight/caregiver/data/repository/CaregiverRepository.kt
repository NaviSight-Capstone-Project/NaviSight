package edu.capstone.navisight.caregiver.data.repository

import android.net.Uri
import edu.capstone.navisight.caregiver.model.Caregiver
import edu.capstone.navisight.caregiver.data.remote.CaregiverDataSource
import android.content.Context
import edu.capstone.navisight.auth.model.OtpResult
import com.google.firebase.Timestamp

class CaregiverRepository(
    private val caregiverDataSource: CaregiverDataSource = CaregiverDataSource()
) {

    suspend fun getProfile(uid: String): Caregiver? {
        return caregiverDataSource.getProfile(uid)
    }

    suspend fun updateProfile(uid: String, updatedData: Map<String, Any>): Boolean {
        return caregiverDataSource.updateProfile(uid, updatedData)
    }

    suspend fun signupCaregiver(
        context: Context,
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        middleName: String,
        phoneNumber: String,

        address: String,
        birthday: Timestamp,
        sex: String
    ): Result<Caregiver> {
        return caregiverDataSource.signupCaregiver(
            context,
            email,
            password,
            firstName,
            lastName,
            middleName,
            phoneNumber,
            address,
            birthday,
            sex
        )
    }

    suspend fun verifySignupOtp(uid: String, enteredOtp: String): OtpResult.OtpVerificationResult {
        return caregiverDataSource.verifySignupOtp(uid, enteredOtp)
    }

    suspend fun resendSignupOtp(context: Context, uid: String): OtpResult.ResendOtpResult {
        return caregiverDataSource.resendSignupOtp(context, uid)
    }

    suspend fun uploadAndSaveProfileImage(uid: String, imageUri: Uri): Boolean =
        caregiverDataSource.uploadAndSaveProfileImage(uid, imageUri)

    suspend fun reauthenticateUser(password: String): Boolean {
        return caregiverDataSource.reauthenticateUser(password)
    }

    suspend fun updateEmail(
        context: Context,
        uid: String,
        newEmail: String,
        password: String
    ): OtpResult.ResendOtpResult {
        return caregiverDataSource.requestEmailChange(context, uid, newEmail, password)
    }

    suspend fun verifyEmailOtp(uid: String, enteredOtp: String): OtpResult.OtpVerificationResult {
        return caregiverDataSource.verifyEmailOtp(uid, enteredOtp)
    }

    suspend fun requestPasswordChange(
        context: Context,
        currentPassword: String
    ): OtpResult.PasswordChangeRequestResult {
        return caregiverDataSource.requestPasswordChange(context, currentPassword)
    }

    suspend fun resendPasswordChangeOtp(context: Context): OtpResult.ResendOtpResult {
        return caregiverDataSource.resendPasswordChangeOtp(context)
    }

    suspend fun verifyPasswordChangeOtp(
        enteredOtp: String,
        newPassword: String
    ): OtpResult.OtpVerificationResult {
        return caregiverDataSource.verifyPasswordChangeOtp(enteredOtp, newPassword)
    }

    suspend fun cancelEmailChange(uid: String) {
        caregiverDataSource.cancelEmailChange(uid)
    }

    suspend fun cancelPasswordChange(uid: String) {
        caregiverDataSource.cancelPasswordChange(uid)
    }
    suspend fun deleteUnverifiedUser(uid: String): Boolean {
        return caregiverDataSource.deleteUnverifiedUser(uid)
    }
}