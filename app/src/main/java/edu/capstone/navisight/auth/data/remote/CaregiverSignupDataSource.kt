package edu.capstone.navisight.auth.data.remote

import android.content.Context
import android.net.Uri
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
        var uid: String? = null
        try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            uid = authResult.user?.uid ?: return Result.failure(Exception("User creation failed"))

            val imageUrl: String? = if (imageUri != null) {
                try { CloudinaryDataSource.uploadImage(imageUri) } catch (e: Exception) { null }
            } else { null }

            val caregiver = Caregiver(
                uid = uid,
                firstName = firstName, middleName = middleName, lastName = lastName,
                email = email, phoneNumber = phoneNumber, address = address,
                birthday = birthday, sex = sex, profileImageUrl = imageUrl,
                isEmailVerified = false
            )

            usersCollection.document(uid).set(caregiver).await()

            // Request OTP
            val otpResult = otpDataSource.requestOtp(
                context = context,
                uid = uid,
                emailToSendTo = email,
                type = OtpDataSource.OtpType.SIGNUP_VERIFICATION
            )

            if (otpResult == OtpResult.ResendOtpResult.Success) {
                return Result.success(caregiver)
            } else {
                deleteUnverifiedUser(uid)
                return Result.failure(Exception("Failed to send verification email."))
            }

        } catch (e: Exception) {
            if (uid != null) deleteUnverifiedUser(uid)
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
                usersCollection.document(uid).update("isEmailVerified", true).await()
                otpDataSource.cleanupOtp(uid, OtpDataSource.OtpType.SIGNUP_VERIFICATION)
            } catch (e: Exception) {
                return OtpResult.OtpVerificationResult.FailureExpiredOrCooledDown
            }
        }
        return verificationResult
    }

    suspend fun deleteUnverifiedUser(uid: String): Boolean {
        return try {
            val user = auth.currentUser
            if (user != null && user.uid == uid) {
                otpDataSource.cleanupOtp(uid, OtpDataSource.OtpType.SIGNUP_VERIFICATION)
                usersCollection.document(uid).delete().await()
                user.delete().await()
                true
            } else false
        } catch (e: Exception) { false }
    }

    suspend fun resendSignupOtp(context: Context, uid: String): OtpResult.ResendOtpResult {
        return try {
            val doc = usersCollection.document(uid).get().await()
            val email = doc.getString("email") ?: return OtpResult.ResendOtpResult.FailureGeneric

            otpDataSource.requestOtp(
                context = context,
                uid = uid,
                emailToSendTo = email,
                type = OtpDataSource.OtpType.SIGNUP_VERIFICATION
            )
        } catch (e: Exception) { OtpResult.ResendOtpResult.FailureGeneric }
    }
}