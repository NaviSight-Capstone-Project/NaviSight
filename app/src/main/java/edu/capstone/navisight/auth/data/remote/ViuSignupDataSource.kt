package edu.capstone.navisight.auth.data.remote

import android.content.ContentValues.TAG
import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.Timestamp
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
    private val otpDataSource: OtpDataSource = OtpDataSource(auth, firestore)

    // 1. Find Caregiver by Email (Helper)
    private suspend fun getCaregiverUidByEmail(email: String): String? {
        return try {
            val query = caregiversCollection.whereEqualTo("email", email).limit(1).get().await()
            if (query.isEmpty) null else query.documents[0].id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 2. SignupViu (Modified)
    suspend fun signupViu(
        context: Context,
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        middleName: String,
        phone: String,
        address: String,
        status: String,
        imageUri: Uri?,
        caregiverEmail: String // <-- New param
    ): Result<Pair<Viu, String>> { // <-- New return type
        var viuUid: String? = null
        try {
            // Step 1: Check if Caregiver exists
            val caregiverUid = getCaregiverUidByEmail(caregiverEmail)
                ?: return Result.failure(Exception("No Caregiver account found with that email."))

            // Step 2: Create the VIU Auth user
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            viuUid = authResult.user?.uid ?: return Result.failure(Exception("VIU user creation failed"))

            // Step 3: Upload Image
            val imageUrl: String? = if (imageUri != null) {
                try { CloudinaryDataSource.uploadImage(imageUri) } catch (e: Exception) { null }
            } else { null }

            // Step 4: Create VIU Firestore Doc
            val viu = Viu(
                uid = viuUid,
                firstName = firstName, middleName = middleName, lastName = lastName,
                email = email, phone = phone, address = address, status = status,
                profileImageUrl = imageUrl
                // Note: caregiverId is NOT set yet
            )
            viusCollection.document(viuUid).set(viu).await()

            // Step 5: Send OTP to Caregiver
            val otpResult = otpDataSource.requestOtp(
                context = context,
                uid = caregiverUid, // <-- Send to Caregiver's doc
                emailToSendTo = caregiverEmail, // <-- Send to Caregiver's email
                type = OtpDataSource.OtpType.VIU_CREATION,
                extraData = mapOf(
                    "isEmailVerified" to false, // This field is for the OTP process itself
                    "pendingViuId" to viuUid  // <-- We store which VIU is pending
                )
            )

            if (otpResult == OtpResult.ResendOtpResult.Success) {
                // Return both the new Viu and the Caregiver's UID
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

    // 3. VerifySignupOtp (Modified)
    suspend fun verifySignupOtp(
        caregiverUid: String, // <-- New param
        viuUid: String,       // <-- New param
        enteredOtp: String
    ): OtpResult.OtpVerificationResult {

        val verificationResult = otpDataSource.verifyOtp(
            uid = caregiverUid, // <-- Verify on Caregiver's doc
            enteredOtp = enteredOtp,
            type = OtpDataSource.OtpType.VIU_CREATION
        )

        if (verificationResult == OtpResult.OtpVerificationResult.Success) {
            try {
                // Step 1: Link accounts
                viusCollection.document(viuUid).update("caregiverId", caregiverUid).await()
                caregiversCollection.document(caregiverUid).update("viuIds", FieldValue.arrayUnion(viuUid)).await()

                // Step 2: Mark VIU as verified
                viusCollection.document(viuUid).update("isEmailVerified", true).await()

                // Step 3: Clean up OTP fields from Caregiver's doc
                otpDataSource.cleanupOtpFields(
                    uid = caregiverUid,
                    type = OtpDataSource.OtpType.VIU_CREATION,
                    extraFieldsToDelete = mapOf("pendingViuId" to FieldValue.delete()) // <-- Clean up pending field
                )
            } catch (e: Exception) {
                e.printStackTrace()
                return OtpResult.OtpVerificationResult.FailureExpiredOrCooledDown
            }
        }
        return verificationResult
    }

    // 4. ResendSignupOtp (Modified)
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

    // 5. DeleteUnverifiedUser (Modified for VIU)
    suspend fun deleteUnverifiedUser(viuUid: String): Boolean {
        return try {
            val user = auth.currentUser
            // We check against viuUid because the VIU is the one logged in
            if (user != null && user.uid == viuUid) {
                // Note: The OTP data is on the Caregiver doc, but we can't find it
                // without the email. We'll just delete the VIU.
                // The OTP on the Caregiver doc will just expire.
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