package edu.capstone.navisight.auth.data.remote

import android.content.ContentValues.TAG
import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import edu.capstone.navisight.auth.model.Caregiver
import edu.capstone.navisight.auth.model.OtpResult


class CaregiverSignupDataSource (
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
){

    private val usersCollection = firestore.collection("caregivers")

    private val otpDataSource: OtpDataSource = OtpDataSource(auth, firestore)

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
        sex: String,
        imageUri: Uri?
    ): Result<Caregiver> {
        var uid: String? = null // To hold UID for cleanup on failure
        try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            uid = authResult.user?.uid ?: return Result.failure(Exception("User creation failed"))

            val imageUrl: String? = if (imageUri != null) {
                try {
                    CloudinaryDataSource.uploadImage(imageUri)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null // Fail gracefully, user can upload later
                }
            } else {
                null
            }

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
                profileImageUrl = imageUrl
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

            if (otpResult == OtpResult.ResendOtpResult.Success) {
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

    suspend fun verifySignupOtp(uid: String, enteredOtp: String): OtpResult.OtpVerificationResult {
        val verificationResult = otpDataSource.verifyOtp(
            uid = uid,
            enteredOtp = enteredOtp,
            type = OtpDataSource.OtpType.SIGNUP_VERIFICATION
        )

        if (verificationResult == OtpResult.OtpVerificationResult.Success) {
            try {
                // Mark as verified
                usersCollection.document(uid).update("isEmailVerified", true).await()

                // Clean up ALL OTP fields (otp, timestamp, counts, cooldown)
                otpDataSource.cleanupOtpFields(
                    uid = uid,
                    type = OtpDataSource.OtpType.SIGNUP_VERIFICATION
                )
            } catch (e: Exception) {
                e.printStackTrace()
                // If update fails, treat it as a failure
                return OtpResult.OtpVerificationResult.FailureExpiredOrCooledDown
            }
        }

        return verificationResult
    }

    suspend fun resendSignupOtp(context: Context, uid: String): OtpResult.ResendOtpResult {
        return try {
            val doc = usersCollection.document(uid).get().await()
            val email = doc.getString("email")
            if (email == null) {
                Log.e(TAG, "resendSignupOtp: User doc not found or no email for uid $uid")
                return OtpResult.ResendOtpResult.FailureGeneric
            }

            // Just call the OtpDataSource, it handles all logic
            otpDataSource.requestOtp(
                context = context,
                uid = uid,
                emailToSendTo = email,
                type = OtpDataSource.OtpType.SIGNUP_VERIFICATION
            )
        } catch (e: Exception) {
            Log.e(TAG, "resendSignupOtp: Failed with exception", e)
            OtpResult.ResendOtpResult.FailureGeneric
        }
    }

    suspend fun deleteUnverifiedUser(uid: String): Boolean {
        return try {
            val user = auth.currentUser

            // Safety check
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