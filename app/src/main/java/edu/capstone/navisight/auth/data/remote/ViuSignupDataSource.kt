package edu.capstone.navisight.auth.data.remote

import android.R
import android.content.ContentValues.TAG
import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import edu.capstone.navisight.auth.model.OtpResult
import edu.capstone.navisight.auth.model.Viu
import edu.capstone.navisight.common.Constants.USER_TYPE_CAREGIVER
import edu.capstone.navisight.common.Constants.USER_TYPE_VIU
import kotlinx.coroutines.tasks.await

class ViuSignupDataSource(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val viusCollection = firestore.collection(USER_TYPE_VIU)
    private val caregiversCollection = firestore.collection(USER_TYPE_CAREGIVER)
    private val relationshipsCollection = firestore.collection("relationships")
    private val otpDataSource: OtpDataSource = OtpDataSource(auth, firestore)

    private suspend fun getCaregiverUidByEmail(email: String): String? {
        return try {
            val query = caregiversCollection.whereEqualTo("email", email).limit(1).get().await()
            if (query.isEmpty) null else query.documents[0].id
        } catch (e: Exception) { null }
    }

    suspend fun signupViu(
        context: Context,
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        middleName: String,
        birthday: String,
        phone: String,
        address: String,
        category: String,
        imageUri: Uri?,
        caregiverEmail: String,
        sex: String,
        country: String,
        province: String,
        city: String,
    ): Result<Pair<Viu, String>> {
        var viuUid: String? = null
        try {
            val caregiverUid = getCaregiverUidByEmail(caregiverEmail)
                ?: return Result.failure(Exception("No Caregiver account found with that email."))

            // Create VIU Auth
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            viuUid = authResult.user?.uid ?: return Result.failure(Exception("VIU user creation failed"))

            // Upload Image
            val imageUrl: String? = if (imageUri != null) {
                try { CloudinaryDataSource.uploadImage(imageUri) } catch (e: Exception) { null }
            } else { null }

            // Create VIU Profile (Unverified)
            val viu = Viu(
                uid = viuUid,
                firstName = firstName, middleName = middleName, lastName = lastName,
                birthday = birthday,
                email = email, phone = phone, address = address, category = category,
                profileImageUrl = imageUrl,
                isEmailVerified = false,
                sex = sex,
                country=country,
                province=province,
                city=city
            )
            viusCollection.document(viuUid).set(viu).await()

            // Request OTP
            val otpResult = otpDataSource.requestOtp(
                context = context,
                uid = caregiverUid, // The OTP belongs to the Caregiver
                emailToSendTo = caregiverEmail,
                type = OtpDataSource.OtpType.VIU_CREATION,
                extraData = mapOf("pendingViuId" to viuUid) // Save VIU ID inside the OTP doc
            )

            if (otpResult == OtpResult.ResendOtpResult.Success) {
                return Result.success(viu to caregiverUid)
            } else {
                deleteUnverifiedUser(viuUid) // Cleanup if email fails
                return Result.failure(Exception("Failed to send verification email."))
            }

        } catch (e: Exception) {
            if (viuUid != null) deleteUnverifiedUser(viuUid)
            return Result.failure(e)
        }
    }

    suspend fun verifySignupOtp(
        caregiverUid: String,
        viuUid: String, // Passed from UI, but should verify it matches stored OTP
        enteredOtp: String
    ): OtpResult.OtpVerificationResult {

        // Verify Code
        val verificationResult = otpDataSource.verifyOtp(
            uid = caregiverUid,
            enteredOtp = enteredOtp,
            type = OtpDataSource.OtpType.VIU_CREATION
        )

        if (verificationResult == OtpResult.OtpVerificationResult.Success) {
            try {
                // Does the stored OTP actually belong to this VIU?
                val storedPendingViuId = otpDataSource.getExtraDataString(
                    caregiverUid,
                    OtpDataSource.OtpType.VIU_CREATION,
                    "pendingViuId"
                )

                if (storedPendingViuId != viuUid) {
                    return OtpResult.OtpVerificationResult.FailureInvalid
                }

                // Link Account
                val relationshipData = hashMapOf(
                    "caregiverUid" to caregiverUid,
                    "viuUid" to viuUid,
                    "primaryCaregiver" to true,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                relationshipsCollection.add(relationshipData).await()
                viusCollection.document(viuUid).update("caregiverId", caregiverUid).await()
                caregiversCollection.document(caregiverUid).update("viuIds", FieldValue.arrayUnion(viuUid)).await()
                viusCollection.document(viuUid).update("isEmailVerified", true).await()

                // Delete the OTP document
                otpDataSource.cleanupOtp(caregiverUid, OtpDataSource.OtpType.VIU_CREATION)

            } catch (e: Exception) {
                return OtpResult.OtpVerificationResult.FailureExpiredOrCooledDown
            }
        }
        return verificationResult
    }

    suspend fun deleteUnverifiedUser(viuUid: String): Boolean {
        return try {
            val user = auth.currentUser
            if (user != null && user.uid == viuUid) {
                viusCollection.document(viuUid).delete().await()
                user.delete().await()
                true
            } else false
        } catch (e: Exception) { false }
    }

    suspend fun resendSignupOtp(context: Context, caregiverUid: String): OtpResult.ResendOtpResult {
        return try {
            val doc = caregiversCollection.document(caregiverUid).get().await()
            val email = doc.getString("email") ?: return OtpResult.ResendOtpResult.FailureGeneric

            // Re-fetch the pendingViuId from the existing OTP doc to keep it consistent
            val pendingViuId = otpDataSource.getExtraDataString(caregiverUid, OtpDataSource.OtpType.VIU_CREATION, "pendingViuId")

            otpDataSource.requestOtp(
                context = context,
                uid = caregiverUid,
                emailToSendTo = email,
                type = OtpDataSource.OtpType.VIU_CREATION,
                extraData = mapOf("pendingViuId" to pendingViuId)
            )
        } catch (e: Exception) { OtpResult.ResendOtpResult.FailureGeneric }
    }
}