package edu.capstone.navisight.auth.data.remote

import android.content.ContentValues.TAG
import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import edu.capstone.navisight.auth.model.OtpResult
import edu.capstone.navisight.auth.model.Viu
import kotlinx.coroutines.tasks.await

class ViuSignupDataSource(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val viusCollection = firestore.collection("vius")
    private val caregiversCollection = firestore.collection("caregivers")
    private val relationshipsCollection = firestore.collection("relationships")
    private val otpDataSource: OtpDataSource = OtpDataSource(auth, firestore)

    private suspend fun getCaregiverUidByEmail(email: String): String? {
        return try {
            val query = caregiversCollection.whereEqualTo("email", email).limit(1).get().await()
            if (query.isEmpty) null else query.documents[0].id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun signupViu(
        context: Context,
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        middleName: String,
        phone: String,
        address: String,
        category: String,
        imageUri: Uri?,
        caregiverEmail: String
    ): Result<Pair<Viu, String>> {
        var viuUid: String? = null
        try {
            val caregiverUid = getCaregiverUidByEmail(caregiverEmail)
                ?: return Result.failure(Exception("No Caregiver account found with that email."))

            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            viuUid = authResult.user?.uid ?: return Result.failure(Exception("VIU user creation failed"))

            val imageUrl: String? = if (imageUri != null) {
                try { CloudinaryDataSource.uploadImage(imageUri) } catch (e: Exception) { null }
            } else { null }

            val viu = Viu(
                uid = viuUid,
                firstName = firstName, middleName = middleName, lastName = lastName,
                email = email, phone = phone, address = address, category = category,
                profileImageUrl = imageUrl
            )
            viusCollection.document(viuUid).set(viu).await()

            val otpResult = otpDataSource.requestOtp(
                context = context,
                uid = caregiverUid,
                emailToSendTo = caregiverEmail,
                type = OtpDataSource.OtpType.VIU_CREATION,
                extraData = mapOf(
                    "isEmailVerified" to false,
                    "pendingViuId" to viuUid
                )
            )

            if (otpResult == OtpResult.ResendOtpResult.Success) {
                return Result.success(viu to caregiverUid)
            } else {
                Log.e(TAG, "signupViu: OTP send failed, rolling back VIU user.")
                deleteUnverifiedUser(viuUid)
                return Result.failure(Exception("Failed to send verification email to Caregiver."))
            }

        } catch (e: Exception) {
            e.printStackTrace()
            if (viuUid != null) {
                Log.e(TAG, "signupViu: Signup failed, rolling back user $viuUid")
                deleteUnverifiedUser(viuUid)
            }
            return Result.failure(e)
        }
    }


    suspend fun verifySignupOtp(
        caregiverUid: String,
        viuUid: String,
        enteredOtp: String
    ): OtpResult.OtpVerificationResult {

        val verificationResult = otpDataSource.verifyOtp(
            uid = caregiverUid,
            enteredOtp = enteredOtp,
            type = OtpDataSource.OtpType.VIU_CREATION
        )

        if (verificationResult == OtpResult.OtpVerificationResult.Success) {
            try {

                // Create the relationship data map matching your screenshot
                val relationshipData = hashMapOf(
                    "caregiverUid" to caregiverUid,
                    "viuUid" to viuUid,
                    "primaryCaregiver" to true, // Automatically set as primary
                    "createdAt" to FieldValue.serverTimestamp()
                )

                // Create the document in the relationships collection
                relationshipsCollection.add(relationshipData).await()

                viusCollection.document(viuUid).update("caregiverId", caregiverUid).await()
                caregiversCollection.document(caregiverUid).update("viuIds", FieldValue.arrayUnion(viuUid)).await()

                viusCollection.document(viuUid).update("isEmailVerified", true).await()

                otpDataSource.cleanupOtpFields(
                    uid = caregiverUid,
                    type = OtpDataSource.OtpType.VIU_CREATION,
                    extraFieldsToDelete = mapOf("pendingViuId" to FieldValue.delete())
                )
            } catch (e: Exception) {
                e.printStackTrace()
                // Consider if you want to fail the verification if the DB write fails,
                // or just log it. Currently, it returns failure.
                return OtpResult.OtpVerificationResult.FailureExpiredOrCooledDown
            }
        }
        return verificationResult
    }

    suspend fun resendSignupOtp(context: Context, caregiverUid: String): OtpResult.ResendOtpResult {
        return try {
            val doc = caregiversCollection.document(caregiverUid).get().await()
            val email = doc.getString("email")
            if (email == null) {
                Log.e(TAG, "resendSignupOtp: Caregiver doc not found")
                return OtpResult.ResendOtpResult.FailureGeneric
            }

            otpDataSource.requestOtp(
                context = context,
                uid = caregiverUid,
                emailToSendTo = email,
                type = OtpDataSource.OtpType.VIU_CREATION
            )
        } catch (e: Exception) {
            Log.e(TAG, "resendSignupOtp: Failed with exception", e)
            OtpResult.ResendOtpResult.FailureGeneric
        }
    }

    suspend fun deleteUnverifiedUser(viuUid: String): Boolean {
        return try {
            val user = auth.currentUser
            if (user != null && user.uid == viuUid) {
                viusCollection.document(viuUid).delete().await()
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