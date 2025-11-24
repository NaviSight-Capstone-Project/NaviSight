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
//import .webrtc.model.FirebaseFieldNames.STATUS  // idunno whats dis raf
//import .webrtc.utils.UserStatus
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
            // --- NEW VALIDATION: Check if email exists in Firestore ---
            val snapshot = usersCollection
                .whereEqualTo("email", newEmail)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                Log.w(TAG, "[EmailChange] Request BLOCKED. Email $newEmail already in use.")
                return ResendOtpResult.FailureEmailAlreadyInUse
            }
            // ----------------------------------------------------------

            if (otpDataSource.isCooldownActive(uid, OtpDataSource.OtpType.EMAIL_CHANGE)) {
                Log.w(TAG, "[EmailChange] Request BLOCKED by active cooldown.")
                return ResendOtpResult.FailureCooldown
            }

            // ... rest of the function (reauth and otp request) ...
            val reauthOk = reauthenticateUser(password)
            if (!reauthOk) {
                return ResendOtpResult.FailureGeneric
            }

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
        otpDataSource.cancelOtpProcess(uid, OtpDataSource.OtpType.EMAIL_CHANGE)
    }

    suspend fun verifyEmailOtp(uid: String, enteredOtp: String): OtpVerificationResult {
        val verificationResult = otpDataSource.verifyOtp(
            uid = uid,
            enteredOtp = enteredOtp,
            type = OtpDataSource.OtpType.EMAIL_CHANGE
        )

        if (verificationResult == OtpVerificationResult.Success) {
            try {
                val doc = usersCollection.document(uid).get().await()
                val newEmail = doc.getString("pendingEmail")
                    ?: return OtpVerificationResult.FailureExpiredOrCooledDown

                auth.currentUser?.updateEmail(newEmail)?.await()

                otpDataSource.cleanupOtpFields(
                    uid = uid,
                    type = OtpDataSource.OtpType.EMAIL_CHANGE,
                    extraFieldsToDelete = mapOf("pendingEmail" to FieldValue.delete())
                )
                usersCollection.document(uid).update("email", newEmail).await()

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

                otpDataSource.cleanupOtpFields(
                    uid = uid,
                    type = OtpDataSource.OtpType.SIGNUP_VERIFICATION
                )
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
            Log.d(TAG, "[PasswordChange] Request: CurrentTime=${Date(currentTime)}")

            if (otpDataSource.isCooldownActive(uid, OtpDataSource.OtpType.PASSWORD_CHANGE)) {
                Log.w(TAG, "[PasswordChange] Request BLOCKED by OTP cooldown.")
                return PasswordChangeRequestResult.FailureCooldown
            }
            val initialPwdCooldownUntil = doc.getTimestamp("passwordAttemptCooldownUntil")?.toDate()?.time ?: 0L
            if (currentTime < initialPwdCooldownUntil) {
                Log.w(TAG, "[PasswordChange] Request BLOCKED by initial attempt cooldown.")
                return PasswordChangeRequestResult.FailureCooldown
            }
            Log.d(TAG, "[PasswordChange] Request: Cooldown checks passed.")

            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            try {
                user.reauthenticate(credential).await()

                Log.d(TAG, "[PasswordChange] Request: Initial re-auth successful.")
                docRef.update("passwordAttemptCount", FieldValue.delete()).await()

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
                    Log.w(TAG, "[PasswordChange] Request: Max initial attempts ($newAttemptCount/5) reached. Setting cooldown.")
                    val cooldownTime = Date(currentTime + OtpDataSource.OTP_COOLDOWN_DURATION_MS)
                    // Set *initial* password attempt cooldown (distinct from OTP cooldown)
                    failureUpdateMap["passwordAttemptCooldownUntil"] = cooldownTime
                    docRef.update(failureUpdateMap).await()
                    PasswordChangeRequestResult.FailureMaxAttempts
                } else {
                    Log.d(TAG, "[PasswordChange] Request: Incrementing initial attempt count to $newAttemptCount.")
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
        otpDataSource.cancelOtpProcess(uid, OtpDataSource.OtpType.PASSWORD_CHANGE)
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
                otpDataSource.cleanupOtpFields(
                    uid = user.uid,
                    type = OtpDataSource.OtpType.PASSWORD_CHANGE
                )
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

            // Add to Firebase Real-Time Database, status defaults to ONLINE.
//            val webRtcRTDB = FirebaseDatabase.getInstance().getReference("webrtc_signal")
//            webRtcRTDB.child(uid).child(email).setValue(email).addOnCompleteListener {
//                webRtcRTDB.child(uid).child(STATUS).setValue(UserStatus.ONLINE).addOnCompleteListener {
//                    Log.d("webrtcrtdb", "caregiver registration complete")
//                }.addOnFailureListener {
//                    Log.e("webrtcrtdb", "caregiver registration failed on status insertion")
//                }
//            }.addOnFailureListener {
//                Log.e("webrtcrtdb", "caregiver registration failed")
//            }

            //idunno whats dis

            if (otpResult == ResendOtpResult.Success) {
                return Result.success(caregiver)
            } else {
                Log.e(TAG, "signupCaregiver: OTP send failed, rolling back user.")
                deleteUnverifiedUser(uid) // Delete auth user + doc
                return Result.failure(Exception("Failed to send verification email. Please try again."))
            }

        } catch (e: Exception) {
            e.printStackTrace()
            if (uid != null) {
                Log.e(TAG, "signupCaregiver: Signup failed with exception, rolling back user $uid")
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
                Log.e(TAG, "resendSignupOtp: User doc not found or no email for uid $uid")
                return ResendOtpResult.FailureGeneric
            }

            otpDataSource.requestOtp(
                context = context,
                uid = uid,
                emailToSendTo = email,
                type = OtpDataSource.OtpType.SIGNUP_VERIFICATION
            )
        } catch (e: Exception) {
            Log.e(TAG, "resendSignupOtp: Failed with exception", e)
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

                otpDataSource.cancelOtpProcess(uid, OtpDataSource.OtpType.SIGNUP_VERIFICATION)
                usersCollection.document(uid).delete().await()

                user.delete().await()

                true
            } else {
                Log.w(TAG, "deleteUnverifiedUser: Mismatch or user null. Cannot delete.")
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

