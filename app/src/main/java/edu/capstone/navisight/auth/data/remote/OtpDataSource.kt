package edu.capstone.navisight.auth.data.remote

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import edu.capstone.navisight.auth.model.OtpResult.OtpVerificationResult
import edu.capstone.navisight.auth.model.OtpResult.ResendOtpResult
import edu.capstone.navisight.common.EmailSender
import kotlinx.coroutines.tasks.await

class OtpDataSource(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val COLLECTION_NAME = "stored_otp"
        private const val TAG = "OtpDataSource"

        // Constants for limits
        const val OTP_EXPIRATION_MS = 5 * 60 * 1000L        // 5 Minutes
        const val OTP_COOLDOWN_DURATION_MS = 5 * 60 * 1000L // 5 Minutes Lockout
        const val OTP_RESEND_WAIT_MS = 60 * 1000L           // 1 Minute between resends
        const val MAX_OTP_ATTEMPTS = 3
        const val MAX_TOTAL_SENDS = 3
    }

    private val otpCollection = firestore.collection(COLLECTION_NAME)

    // Defines the "Instance" of the OTP
    enum class OtpType {
        SIGNUP_VERIFICATION,      // Caregiver Signup
        VIU_CREATION,             // VIU Signup (Sent to Caregiver)
        EMAIL_CHANGE,             // Change Caregiver Email
        PASSWORD_CHANGE,          // Reset Password
        VIU_PROFILE_UPDATE,       // Edit VIU Profile
        TRANSFER_PRIMARY,         // Transfer Caregiver
        VIU_EMAIL_CHANGE          // <--- ADDED THIS (Change VIU Email)
    }

    private fun getOtpDocId(uid: String, type: OtpType): String {
        return "${uid}_${type.name}"
    }

    /**
     * Checks if the user is currently locked out from requesting this specific OTP type.
     */
    suspend fun isCooldownActive(uid: String, type: OtpType): Boolean {
        return try {
            val docId = getOtpDocId(uid, type)
            val doc = otpCollection.document(docId).get().await()
            val cooldownUntil = doc.getLong("cooldownUntil") ?: 0L
            System.currentTimeMillis() < cooldownUntil
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Requests an OTP: Generates it, stores it in `stored_otp`, and sends the email.
     */
    suspend fun requestOtp(
        context: Context,
        uid: String,
        emailToSendTo: String,
        type: OtpType,
        extraData: Map<String, Any?> = emptyMap()
    ): ResendOtpResult {
        return try {
            val docId = getOtpDocId(uid, type)
            val docRef = otpCollection.document(docId)
            val doc = docRef.get().await()

            val currentTime = System.currentTimeMillis()
            val cooldownUntil = doc.getLong("cooldownUntil") ?: 0L
            var resendCount = doc.getLong("resendCount")?.toInt() ?: 0
            val lastOtpTime = doc.getLong("timestamp") ?: 0L

            // 1. Check Hard Lockout
            if (currentTime < cooldownUntil) {
                return ResendOtpResult.FailureCooldown
            }

            // 2. Check Session Expiration (Reset logic if > 5 mins have passed)
            if (resendCount > 0 && (currentTime - lastOtpTime > OTP_EXPIRATION_MS)) {
                resendCount = 0 // Expired, start fresh
            }

            // 3. Check Max Resends (Spam Protection)
            if (resendCount >= MAX_TOTAL_SENDS) {
                // Apply Lockout
                docRef.update("cooldownUntil", currentTime + OTP_COOLDOWN_DURATION_MS).await()
                return ResendOtpResult.FailureCooldown
            }

            // 4. Check 1-minute wait between clicks
            if (resendCount > 0 && currentTime - lastOtpTime < OTP_RESEND_WAIT_MS) {
                return ResendOtpResult.FailureGeneric // "Please wait"
            }

            // 5. Generate and Store
            val otp = (100000..999999).random().toString()
            val newResendCount = resendCount + 1

            // Prepare the data for stored_otp collection
            val otpData = mutableMapOf<String, Any?>(
                "uid" to uid,
                "otp" to otp,
                "type" to type.name,
                "timestamp" to currentTime,
                "expiresAt" to currentTime + OTP_EXPIRATION_MS,
                "attemptCount" to 0,
                "resendCount" to newResendCount,
                "cooldownUntil" to 0 // Clear any previous cooldown
            )
            // Add extra data (like pendingViuId) directly to the OTP document
            otpData.putAll(extraData)

            docRef.set(otpData, SetOptions.merge()).await()

            // 6. Send Email using your EmailSender object
            sendEmailForType(context, emailToSendTo, type, otp)

            ResendOtpResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Request OTP Failed", e)
            ResendOtpResult.FailureGeneric
        }
    }

    suspend fun verifyOtp(
        uid: String,
        enteredOtp: String,
        type: OtpType
    ): OtpVerificationResult {
        return try {
            val docId = getOtpDocId(uid, type)
            val docRef = otpCollection.document(docId)
            val doc = docRef.get().await()

            // Does not exist?
            if (!doc.exists()) return OtpVerificationResult.FailureExpiredOrCooledDown

            val storedOtp = doc.getString("otp")
            val expiresAt = doc.getLong("expiresAt") ?: 0L
            val cooldownUntil = doc.getLong("cooldownUntil") ?: 0L
            val attemptCount = doc.getLong("attemptCount")?.toInt() ?: 0
            val currentTime = System.currentTimeMillis()

            // Validation Checks
            if (currentTime < cooldownUntil) return OtpVerificationResult.FailureExpiredOrCooledDown

            // Check Expiration
            if (currentTime > expiresAt) {
                cleanupOtp(uid, type) // Auto-delete expired doc
                return OtpVerificationResult.FailureExpiredOrCooledDown
            }

            if (attemptCount >= MAX_OTP_ATTEMPTS) return OtpVerificationResult.FailureMaxAttempts

            if (enteredOtp == storedOtp) {
                return OtpVerificationResult.Success
            } else {
                // Wrong OTP Logic
                val newCount = attemptCount + 1
                if (newCount >= MAX_OTP_ATTEMPTS) {
                    // Lockout
                    docRef.update("cooldownUntil", currentTime + OTP_COOLDOWN_DURATION_MS).await()
                    return OtpVerificationResult.FailureMaxAttempts
                } else {
                    docRef.update("attemptCount", newCount).await()
                    return OtpVerificationResult.FailureInvalid
                }
            }

        } catch (e: Exception) {
            OtpVerificationResult.FailureExpiredOrCooledDown
        }
    }

    // Completely removes the document from stored_otp
    suspend fun cleanupOtp(uid: String, type: OtpType) {
        try {
            val docId = getOtpDocId(uid, type)
            otpCollection.document(docId).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup failed", e)
        }
    }

    // Helper to retrieve extra data (like pendingViuId) stored inside the OTP doc
    suspend fun getExtraDataString(uid: String, type: OtpType, fieldKey: String): String? {
        return try {
            val docId = getOtpDocId(uid, type)
            val doc = otpCollection.document(docId).get().await()
            doc.getString(fieldKey)
        } catch (e: Exception) { null }
    }

    private suspend fun sendEmailForType(context: Context, email: String, type: OtpType, otp: String) {
        val (subject, body) = when (type) {
            OtpType.SIGNUP_VERIFICATION -> "NaviSight Caregiver Signup" to "Your verification code is: $otp"
            OtpType.VIU_CREATION -> "NaviSight VIU Link Request" to "A new VIU is requesting to link to your account. Code: $otp"
            OtpType.EMAIL_CHANGE -> "NaviSight Security Alert" to "You requested to change your email. Code: $otp"
            OtpType.PASSWORD_CHANGE -> "NaviSight Password Reset" to "Your password reset code is: $otp"
            OtpType.TRANSFER_PRIMARY -> "NaviSight Transfer Request" to "Code to transfer primary caregiver rights: $otp"
            OtpType.VIU_EMAIL_CHANGE -> "NaviSight VIU Email Change" to "You requested to change the email address for a VIU. Code: $otp"
            else -> "NaviSight Verification" to "Your code is: $otp"
        }
        EmailSender.sendVerificationEmail(context, to = email, subject = subject, body = body)
    }
}