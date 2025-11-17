package edu.capstone.navisight.auth.data.remote

import android.content.Context
import android.util.Log
import edu.capstone.navisight.auth.model.OtpResult.OtpVerificationResult
import edu.capstone.navisight.auth.model.OtpResult.PasswordChangeRequestResult
import edu.capstone.navisight.auth.model.OtpResult.ResendOtpResult
import edu.capstone.navisight.utils.EmailSender
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp // Keep for reading timestamps
import kotlinx.coroutines.tasks.await
import java.util.Date

class OtpDataSource(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        // 5-minute (300,000 ms) lockout for spamming
        const val OTP_COOLDOWN_DURATION_MS = 5 * 60 * 1000L

        // 5-minute (300,000 ms) expiration for a single code
        const val OTP_EXPIRATION_MS = 5 * 60 * 1000L

        // 1-minute (60,000 ms) wait between resend clicks
        const val OTP_RESEND_WAIT_MS = 60 * 1000L

        // Max 3 wrong attempts for a single OTP code
        const val MAX_OTP_ATTEMPTS = 3

        // Max 3 total sends (1 initial + 2 resends)
        const val MAX_TOTAL_SENDS = 3

        private const val TAG = "OtpDataSource_DEBUG"
    }

    private val usersCollection = firestore.collection("caregivers")
    private val viusCollection = firestore.collection("vius")


    enum class OtpType(
        val collection: String,
        val otpField: String,
        val timestampField: String,
        val attemptCountField: String,
        val resendCountField: String,
        val cooldownField: String
    ) {

        VIU_CREATION(
            collection = "caregivers", // Stores OTP on the Caregiver's doc
            otpField = "otp",
            timestampField = "otpTimestamp",
            attemptCountField = "otpAttemptCount",
            resendCountField = "otpResendCount",
            cooldownField = "otpCooldownUntil"
        ),

        SIGNUP_VERIFICATION(
            collection = "caregivers",
            otpField = "otp", // Use existing field
            timestampField = "otpTimestamp", // Use existing field
            attemptCountField = "otpAttemptCount",
            resendCountField = "otpResendCount",
            cooldownField = "otpCooldownUntil"
        ),
        EMAIL_CHANGE(
            collection = "caregivers",
            otpField = "otp",
            timestampField = "otpTimestamp",
            attemptCountField = "otpAttemptCount",
            resendCountField = "otpResendCount",
            cooldownField = "otpCooldownUntil"
        ),
        PASSWORD_CHANGE(
            collection = "caregivers",
            otpField = "passwordChangeOtp",
            timestampField = "passwordChangeOtpTimestamp",
            attemptCountField = "passwordChangeOtpAttemptCount",
            resendCountField = "passwordResendCount",
            cooldownField = "passwordCooldownUntil"
        ),

        VIU_SIGNUP_VERIFICATION(
            collection = "vius",
            otpField = "otp",
            timestampField = "otpTimestamp",
            attemptCountField = "otpAttemptCount",
            resendCountField = "otpResendCount",
            cooldownField = "otpCooldownUntil"
        ),

        VIU_EMAIL_CHANGE(
            collection = "vius",
            otpField = "otp",
            timestampField = "otpTimestamp",
            attemptCountField = "otpAttemptCount",
            resendCountField = "otpResendCount",
            cooldownField = "otpCooldownUntil"
        ),
        VIU_PROFILE_UPDATE(
            collection = "caregivers",
            otpField = "verificationOtp",
            timestampField = "verificationOtpTimestamp",
            attemptCountField = "verificationOtpAttemptCount",
            resendCountField = "verificationOtpResendCount",
            cooldownField = "verificationOtpCooldownUntil"
        )
    }

    /**
     * Gets the correct DocumentReference based on the OtpType.
     */
    private fun getDocRef(uid: String, type: OtpType): DocumentReference {
        return when (type.collection) {
            "vius" -> viusCollection.document(uid)
            "caregivers" -> usersCollection.document(uid)
            else -> throw IllegalArgumentException("Invalid collection type")
        }
    }

    /**
     * Public function to check if a cooldown is active.
     */
    suspend fun isCooldownActive(uid: String, type: OtpType): Boolean {
        return try {
            val doc = getDocRef(uid, type).get().await() // Use helper
            val currentTime = System.currentTimeMillis()
            val cooldownUntilTime = doc.getTimestamp(type.cooldownField)?.toDate()?.time ?: 0L

            if (currentTime < cooldownUntilTime) {
                Log.w(TAG, "[${type.name}] Cooldown is ACTIVE until ${Date(cooldownUntilTime)}")
                true // Cooldown is active
            } else {
                Log.d(TAG, "[${type.name}] Cooldown is INACTIVE.")
                false // Cooldown is not active
            }
        } catch (e: Exception) {
            Log.e(TAG, "[${type.name}] Error checking cooldown", e)
            false // Fail open
        }
    }

    /**
     * Requests an OTP. Handles resend logic and sets cooldown on final failed attempt.
     */
    suspend fun requestOtp(
        context: Context,
        uid: String, // UID of the document where OTP will be stored
        emailToSendTo: String, // Email to send to
        type: OtpType,
        extraData: Map<String, Any?> = emptyMap()
    ): ResendOtpResult {
        return try {
            val docRef = getDocRef(uid, type) // Use helper
            val doc = docRef.get().await()
            val currentTime = System.currentTimeMillis()
            Log.d(TAG, "[${type.name}] Requesting OTP. Current Time: ${Date(currentTime)}")

            val cooldownUntil = doc.getTimestamp(type.cooldownField)?.toDate()?.time ?: 0L
            var resendCount = doc.getLong(type.resendCountField)?.toInt() ?: 0
            val lastOtpTime = doc.getLong(type.timestampField) ?: 0L

            // Check for 5-minute (300s) SPAM COOLDOWN first
            // This is a "hard" lockout from previous spam (either resends or attempts)
            if (currentTime < cooldownUntil) {
                Log.w(TAG, "[${type.name}] Resend failed: On 5-min cooldown until ${Date(cooldownUntil)}")
                return ResendOtpResult.FailureCooldown
            }

            // If the last OTP was > 5 min ago, it's an expired session.
            // Reset the count so the user can start fresh.
            if (resendCount > 0 && (currentTime - lastOtpTime > OTP_EXPIRATION_MS)) {
                Log.d(TAG, "[${type.name}] Previous OTP session expired (> 5 min old). Resetting resend count to 0.")
                resendCount = 0
            }
            // Note: If cooldown expired AND resendCount was >= 3, this also resets it.
            else if (cooldownUntil > 0 && currentTime >= cooldownUntil && resendCount >= MAX_TOTAL_SENDS) {
                Log.d(TAG, "[${type.name}] Previous 5-min cooldown expired. Resetting resend count to 0.")
                resendCount = 0
            }

            // If count is at or over the limit, this is the 4th+ attempt.
            // Start the 5-minute cooldown and fail.
            if (resendCount >= MAX_TOTAL_SENDS) {
                Log.w(TAG, "[${type.name}] Resend failed: Max sends (${MAX_TOTAL_SENDS}) reached. Starting 5-min cooldown.")
                val cooldownTime = Date(currentTime + OTP_COOLDOWN_DURATION_MS)
                docRef.update(type.cooldownField, cooldownTime).await()
                return ResendOtpResult.FailureCooldown
            }

            // This is for the 2nd and 3rd sends.
            val newResendCount = resendCount + 1
            if (resendCount > 0 && currentTime - lastOtpTime < OTP_RESEND_WAIT_MS) {
                Log.w(TAG, "[${type.name}] Resend (Send #${newResendCount}) failed: Must wait 1 minute.")
                return ResendOtpResult.FailureGeneric
            }

            val otp = generateOtp()
            val otpData = mutableMapOf<String, Any?>()
            otpData.putAll(extraData)
            otpData[type.otpField] = otp
            otpData[type.timestampField] = currentTime
            otpData[type.attemptCountField] = 0 // Reset attempt count
            otpData[type.resendCountField] = newResendCount // Increment resend count

            // Explicitly delete any cooldown, as we are starting a new valid session
            otpData[type.cooldownField] = FieldValue.delete()
            Log.d(TAG, "[${type.name}] Sending OTP (Send #${newResendCount}).")

            docRef.set(otpData, SetOptions.merge()).await()

            val subject = when (type) {
                OtpType.EMAIL_CHANGE -> "NaviSight Email Verification"
                OtpType.PASSWORD_CHANGE -> "NaviSight Password Change Verification"
                OtpType.VIU_EMAIL_CHANGE -> "NaviSight Security Verification"
                OtpType.VIU_PROFILE_UPDATE -> "NaviSight Security Verification"
                OtpType.SIGNUP_VERIFICATION -> "NaviSight Signup Verification"
                OtpType.VIU_CREATION -> "NaviSight New VIU Link Request"
                OtpType.VIU_SIGNUP_VERIFICATION -> "NaviSight Signup Verification"
            }
            val body = when (type) {
                OtpType.EMAIL_CHANGE -> "Your verification code is: $otp\n\nIt expires in 5 minutes."
                OtpType.PASSWORD_CHANGE -> "Your verification code to change your password is: $otp\n\nIt expires in 5 minutes."
                OtpType.VIU_EMAIL_CHANGE -> "You requested to change a VIU's contact email. Your verification code is: $otp\n\nIt expires in 5 minutes."
                OtpType.VIU_PROFILE_UPDATE -> "Your verification code to update VIU details is: $otp\n\nIt expires in 5 minutes."
                OtpType.SIGNUP_VERIFICATION -> "Your verification code is: $otp\n\nIt expires in 5 minutes."
                OtpType.VIU_CREATION -> "A new Visually Impaired User is trying to link to your account. Your verification code is: $otp\n\nIt expires in 5 minutes."
                OtpType.VIU_SIGNUP_VERIFICATION -> "Your verification code is: $otp\n\nIt expires in 5 minutes."
            }

            EmailSender.sendVerificationEmail(
                context = context,
                to = emailToSendTo,
                subject = subject,
                body = body
            )
            ResendOtpResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "[${type.name}] OTP Request failed with exception", e)
            ResendOtpResult.FailureGeneric
        }
    }

    /**
     * Verifies OTP, sets cooldown on 3rd try.
     */
    suspend fun verifyOtp(
        uid: String, // UID of the document where OTP is stored
        enteredOtp: String,
        type: OtpType
    ): OtpVerificationResult {
        return try {
            val docRef = getDocRef(uid, type) // Use helper
            val doc = docRef.get().await()
            val currentTime = System.currentTimeMillis()

            val storedOtp = doc.getString(type.otpField)
            val timestamp = doc.getLong(type.timestampField) ?: 0L
            val attemptCount = doc.getLong(type.attemptCountField)?.toInt() ?: 0
            val cooldownUntil = doc.getTimestamp(type.cooldownField)?.toDate()?.time ?: 0L

            if (currentTime < cooldownUntil) {
                return OtpVerificationResult.FailureExpiredOrCooledDown
            }

            if (storedOtp == null) {
                return OtpVerificationResult.FailureExpiredOrCooledDown
            }

            if (currentTime - timestamp > OTP_EXPIRATION_MS) {
                cancelOtpProcess(uid, type) // Expire it
                return OtpVerificationResult.FailureExpiredOrCooledDown
            }

            if (attemptCount >= MAX_OTP_ATTEMPTS) {
                // This case should be rare, as 3rd attempt sets cooldown
                return OtpVerificationResult.FailureMaxAttempts
            }


            if (enteredOtp == storedOtp) {
                // SUCCESS
                OtpVerificationResult.Success
            } else {
                // FAILURE
                val newAttemptCount = attemptCount + 1
                if (newAttemptCount >= MAX_OTP_ATTEMPTS) {
                    // This was the 3rd FAILED attempt. Start 5-min cooldown.
                    val failureUpdateMap = mutableMapOf<String, Any?>()
                    val cooldownTime = Date(currentTime + OTP_COOLDOWN_DURATION_MS)
                    failureUpdateMap[type.cooldownField] = cooldownTime
                    failureUpdateMap[type.attemptCountField] = newAttemptCount
                    docRef.update(failureUpdateMap).await()
                    OtpVerificationResult.FailureMaxAttempts
                } else {
                    // This was attempt 1 or 2. Just increment count.
                    docRef.update(type.attemptCountField, newAttemptCount).await()
                    OtpVerificationResult.FailureInvalid
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[${type.name}] OTP Verification failed with exception", e)
            OtpVerificationResult.FailureExpiredOrCooledDown
        }
    }

    /**
     * Immediately expires an active OTP when the user cancels.
     * This does NOT reset the resend count or any active 5-minute cooldown.
     * This is correct, as it prevents "canceling" to escape a lockout.
     */
    suspend fun cancelOtpProcess(uid: String, type: OtpType) {
        try {
            val docRef = getDocRef(uid, type) // Use helper
            Log.d(TAG, "[${type.name}] User cancelled. Deleting active OTP.")

            val fieldsToCancel = mapOf(
                type.otpField to FieldValue.delete(),
                type.timestampField to FieldValue.delete(),
                type.attemptCountField to FieldValue.delete()
            )

            docRef.update(fieldsToCancel).await()
        } catch (e: Exception) {
            Log.e(TAG, "[${type.name}] Failed to cancel OTP fields", e)
        }
    }

    /**
     * Cleans up ALL OTP fields after a FULLY SUCCESSFUL operation.
     * This is correct, as it resets everything for the next time.
     */
    suspend fun cleanupOtpFields(uid: String, type: OtpType, extraFieldsToDelete: Map<String, FieldValue> = emptyMap()) {
        try {
            val docRef = getDocRef(uid, type) // Use helper
            Log.d(TAG, "[${type.name}] Cleaning up ALL OTP fields after success.")

            val cleanupMap = mutableMapOf<String, Any>(
                type.otpField to FieldValue.delete(),
                type.timestampField to FieldValue.delete(),
                type.attemptCountField to FieldValue.delete(),
                type.resendCountField to FieldValue.delete(), // <-- Resets resend count
                type.cooldownField to FieldValue.delete()
            )
            cleanupMap.putAll(extraFieldsToDelete)
            docRef.update(cleanupMap).await()
        } catch (e: Exception) {
            Log.e(TAG, "[${type.name}] Failed to cleanup OTP fields", e)
        }
    }

    private fun generateOtp(): String {
        return (100000..999999).random().toString()
    }
}