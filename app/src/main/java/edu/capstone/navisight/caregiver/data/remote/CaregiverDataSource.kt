package edu.capstone.navisight.caregiver.data.remote

import android.content.ContentValues.TAG
import android.content.Context
import android.net.Uri
import android.util.Log
import edu.capstone.navisight.caregiver.model.Caregiver
import edu.capstone.navisight.auth.model.OtpResult.PasswordChangeRequestResult
import edu.capstone.navisight.common.EmailSender
import edu.capstone.navisight.auth.model.OtpResult.OtpVerificationResult
import edu.capstone.navisight.auth.model.OtpResult.ResendOtpResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import edu.capstone.navisight.auth.data.remote.CloudinaryDataSource
import edu.capstone.navisight.auth.data.remote.OtpDataSource
import kotlinx.coroutines.tasks.await
import java.util.Date

class CaregiverDataSource(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val usersCollection = firestore.collection("caregivers")
    private val otpDataSource: OtpDataSource = OtpDataSource(auth, firestore)

    suspend fun getProfile(uid: String): Caregiver? {
        val doc = usersCollection.document(uid).get().await()
        return doc.toObject(Caregiver::class.java)
    }

    suspend fun updateProfile(uid: String, updatedData: Map<String, Any>): Boolean {
        return try {
            usersCollection.document(uid).update(updatedData).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun requestEmailChange(
        context: Context,
        uid: String,
        newEmail: String,
        password: String
    ): ResendOtpResult {
        try {
            // Check for duplicate emails first
            val snapshot = usersCollection
                .whereEqualTo("email", newEmail)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                Log.w(TAG, "[EmailChange] Request BLOCKED. Email $newEmail already in use.")
                return ResendOtpResult.FailureEmailAlreadyInUse
            }

            if (otpDataSource.isCooldownActive(uid, OtpDataSource.OtpType.EMAIL_CHANGE)) {
                Log.w(TAG, "[EmailChange] Request BLOCKED by active cooldown.")
                return ResendOtpResult.FailureCooldown
            }

            val reauthOk = reauthenticateUser(password)
            if (!reauthOk) {
                return ResendOtpResult.FailureGeneric
            }

            // Store pendingEmail in extraData
            return otpDataSource.requestOtp(
                context = context,
                uid = uid,
                emailToSendTo = newEmail,
                type = OtpDataSource.OtpType.EMAIL_CHANGE,
                extraData = mapOf("pendingEmail" to newEmail)
            )
        } catch (e: Exception) {
            Log.e(TAG, "[EmailChange] Request failed with exception", e)
            return ResendOtpResult.FailureGeneric
        }
    }

    suspend fun cancelEmailChange(uid: String) {
        otpDataSource.cleanupOtp(uid, OtpDataSource.OtpType.EMAIL_CHANGE)
    }

    suspend fun verifyEmailOtp(uid: String, enteredOtp: String): OtpVerificationResult {
        val verificationResult = otpDataSource.verifyOtp(
            uid = uid,
            enteredOtp = enteredOtp,
            type = OtpDataSource.OtpType.EMAIL_CHANGE
        )

        if (verificationResult == OtpVerificationResult.Success) {
            try {
                // CHANGED: Get pendingEmail from OTP doc
                val newEmail = otpDataSource.getExtraDataString(uid, OtpDataSource.OtpType.EMAIL_CHANGE, "pendingEmail")
                    ?: return OtpVerificationResult.FailureExpiredOrCooledDown

                auth.currentUser?.updateEmail(newEmail)?.await()

                // Update Firestore
                usersCollection.document(uid).update("email", newEmail).await()

                // CHANGED: Use cleanupOtp
                otpDataSource.cleanupOtp(uid, OtpDataSource.OtpType.EMAIL_CHANGE)

            } catch (e: Exception) {
                e.printStackTrace()
                return OtpVerificationResult.FailureExpiredOrCooledDown
            }
        }
        return verificationResult
    }

    suspend fun verifySignupOtp(uid: String, enteredOtp: String): OtpVerificationResult {
        val verificationResult = otpDataSource.verifyOtp(
            uid = uid,
            enteredOtp = enteredOtp,
            type = OtpDataSource.OtpType.SIGNUP_VERIFICATION
        )

        if (verificationResult == OtpVerificationResult.Success) {
            try {
                usersCollection.document(uid).update("isEmailVerified", true).await()

                // CHANGED: Use cleanupOtp
                otpDataSource.cleanupOtp(uid, OtpDataSource.OtpType.SIGNUP_VERIFICATION)
            } catch (e: Exception) {
                e.printStackTrace()
                return OtpVerificationResult.FailureExpiredOrCooledDown
            }
        }
        return verificationResult
    }

    suspend fun requestPasswordChange(
        context: Context,
        currentPassword: String
    ): PasswordChangeRequestResult {
        return try {
            val user = auth.currentUser ?: return PasswordChangeRequestResult.FailureGeneric
            val email = user.email ?: return PasswordChangeRequestResult.FailureGeneric
            val uid = user.uid
            val docRef = usersCollection.document(uid)
            val doc = docRef.get().await()
            val currentTime = System.currentTimeMillis()

            // 1. Check OTP Cooldown (from stored_otp)
            if (otpDataSource.isCooldownActive(uid, OtpDataSource.OtpType.PASSWORD_CHANGE)) {
                return PasswordChangeRequestResult.FailureCooldown
            }

            // 2. Check Business Logic Cooldown (from User Profile)
            val initialPwdCooldownUntil = doc.getTimestamp("passwordAttemptCooldownUntil")?.toDate()?.time ?: 0L
            if (currentTime < initialPwdCooldownUntil) {
                return PasswordChangeRequestResult.FailureCooldown
            }

            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            try {
                user.reauthenticate(credential).await()

                // Auth Success -> Clear attempt counts on profile
                docRef.update("passwordAttemptCount", FieldValue.delete()).await()

                // Request OTP
                val otpResult = otpDataSource.requestOtp(
                    context = context,
                    uid = uid,
                    emailToSendTo = email,
                    type = OtpDataSource.OtpType.PASSWORD_CHANGE
                )

                when(otpResult) {
                    is ResendOtpResult.Success -> PasswordChangeRequestResult.OtpSent
                    is ResendOtpResult.FailureCooldown -> PasswordChangeRequestResult.FailureCooldown
                    is ResendOtpResult.FailureGeneric -> PasswordChangeRequestResult.FailureGeneric
                    is ResendOtpResult.FailureEmailAlreadyInUse -> PasswordChangeRequestResult.FailureGeneric
                }

            } catch (reauthException: Exception) {
                Log.w(TAG, "[PasswordChange] Request: Initial re-auth failed.")
                val attemptCount = doc.getLong("passwordAttemptCount")?.toInt() ?: 0
                val newAttemptCount = attemptCount + 1
                val failureUpdateMap = mutableMapOf<String, Any?>("passwordAttemptCount" to newAttemptCount)

                if (newAttemptCount >= 5) {
                    val cooldownTime = Date(currentTime + OtpDataSource.OTP_COOLDOWN_DURATION_MS)
                    failureUpdateMap["passwordAttemptCooldownUntil"] = cooldownTime
                    docRef.update(failureUpdateMap).await()
                    PasswordChangeRequestResult.FailureMaxAttempts
                } else {
                    docRef.update(failureUpdateMap as Map<String, Any>).await()
                    PasswordChangeRequestResult.FailureInvalidPassword
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[PasswordChange] Request failed with exception", e)
            PasswordChangeRequestResult.FailureGeneric
        }
    }

    suspend fun cancelPasswordChange(uid: String) {
        // CHANGED: Use cleanupOtp
        otpDataSource.cleanupOtp(uid, OtpDataSource.OtpType.PASSWORD_CHANGE)
    }

    suspend fun resendPasswordChangeOtp(context: Context): ResendOtpResult {
        val user = auth.currentUser ?: return ResendOtpResult.FailureGeneric
        val email = user.email ?: return ResendOtpResult.FailureGeneric
        return otpDataSource.requestOtp(
            context = context,
            uid = user.uid,
            emailToSendTo = email,
            type = OtpDataSource.OtpType.PASSWORD_CHANGE
        )
    }

    suspend fun verifyPasswordChangeOtp(
        enteredOtp: String,
        newPassword: String
    ): OtpVerificationResult {
        val user = auth.currentUser ?: return OtpVerificationResult.FailureExpiredOrCooledDown
        val verificationResult = otpDataSource.verifyOtp(
            uid = user.uid,
            enteredOtp = enteredOtp,
            type = OtpDataSource.OtpType.PASSWORD_CHANGE
        )

        if (verificationResult == OtpVerificationResult.Success) {
            try {
                user.updatePassword(newPassword).await()
                // CHANGED: Use cleanupOtp
                otpDataSource.cleanupOtp(user.uid, OtpDataSource.OtpType.PASSWORD_CHANGE)
            } catch (e: Exception) {
                e.printStackTrace()
                return OtpVerificationResult.FailureExpiredOrCooledDown
            }
        }
        return verificationResult
    }

    suspend fun reauthenticateUser(password: String): Boolean {
        return try {
            val user = auth.currentUser ?: return false
            val email = user.email ?: return false
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
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
        var uid: String? = null
        try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            uid = authResult.user?.uid ?: return Result.failure(Exception("User creation failed"))

            val caregiver = Caregiver(
                uid = uid,
                firstName = firstName,
                middleName = middleName,
                lastName = lastName,
                email = email,
                phoneNumber = phoneNumber,
                address = address,
                birthday = birthday,
                sex = sex,
            )

            firestore.collection("caregivers")
                .document(uid)
                .set(caregiver)
                .await()

            val otpResult = otpDataSource.requestOtp(
                context = context,
                uid = uid,
                emailToSendTo = email,
                type = OtpDataSource.OtpType.SIGNUP_VERIFICATION,
                extraData = mapOf("isEmailVerified" to false)
            )

            if (otpResult == ResendOtpResult.Success) {
                return Result.success(caregiver)
            } else {
                Log.e(TAG, "signupCaregiver: OTP send failed, rolling back user.")
                deleteUnverifiedUser(uid)
                return Result.failure(Exception("Failed to send verification email. Please try again."))
            }

        } catch (e: Exception) {
            e.printStackTrace()
            if (uid != null) {
                deleteUnverifiedUser(uid)
            }
            return Result.failure(e)
        }
    }

    suspend fun resendSignupOtp(context: Context, uid: String): ResendOtpResult {
        return try {
            val doc = usersCollection.document(uid).get().await()
            val email = doc.getString("email")
            if (email == null) {
                return ResendOtpResult.FailureGeneric
            }

            otpDataSource.requestOtp(
                context = context,
                uid = uid,
                emailToSendTo = email,
                type = OtpDataSource.OtpType.SIGNUP_VERIFICATION
            )
        } catch (e: Exception) {
            ResendOtpResult.FailureGeneric
        }
    }

    suspend fun uploadAndSaveProfileImage(uid: String, imageUri: Uri): Boolean {
        return try {
            val imageUrl = CloudinaryDataSource.uploadImage(imageUri)
            if (imageUrl != null) {
                val updated = updateProfile(uid, mapOf("profileImageUrl" to imageUrl))
                updated
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteUnverifiedUser(uid: String): Boolean {
        return try {
            val user = auth.currentUser

            if (user != null && user.uid == uid) {
                // CHANGED: Use cleanupOtp
                otpDataSource.cleanupOtp(uid, OtpDataSource.OtpType.SIGNUP_VERIFICATION)

                usersCollection.document(uid).delete().await()
                user.delete().await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}