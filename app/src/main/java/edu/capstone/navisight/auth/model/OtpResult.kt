package edu.capstone.navisight.auth.model

class OtpResult {
    sealed class OtpVerificationResult {
        object Success : OtpVerificationResult()
        object FailureInvalid : OtpVerificationResult()
        object FailureMaxAttempts : OtpVerificationResult()
        object FailureExpiredOrCooledDown : OtpVerificationResult()
    }

    /**
     * Represents the result of an OTP resend request.
     */
    sealed class ResendOtpResult {
        object Success : ResendOtpResult()
        object FailureCooldown : ResendOtpResult()
        object FailureEmailAlreadyInUse : ResendOtpResult()
        object FailureGeneric : ResendOtpResult() // For re-auth fail or other errors
    }
    sealed class PasswordChangeRequestResult {
        object OtpSent : PasswordChangeRequestResult()
        object FailureInvalidPassword : PasswordChangeRequestResult()
        object FailureMaxAttempts : PasswordChangeRequestResult()
        object FailureCooldown : PasswordChangeRequestResult()
        object FailureGeneric : PasswordChangeRequestResult()
    }
}