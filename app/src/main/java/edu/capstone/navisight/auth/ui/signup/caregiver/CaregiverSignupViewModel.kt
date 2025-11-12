package edu.capstone.navisight.auth.ui.signup.caregiver

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import edu.capstone.navisight.auth.domain.DeleteUnverifiedUserUseCase
import edu.capstone.navisight.auth.domain.ResendSignupOtpUseCase
import edu.capstone.navisight.auth.domain.SignupCaregiverUseCase
import edu.capstone.navisight.auth.domain.VerifySignupOtpUseCase
import edu.capstone.navisight.auth.model.OtpResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class CaregiverSignupUiState(
    val isLoading: Boolean = false,
    val signupSuccess: Boolean = false,
    val verificationSuccess: Boolean = false,
    val createdUserId: String? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val profileImageUri: Uri? = null,
    val resendTimer: Int = 0
)

class CaregiverSignupViewModel : ViewModel() {

    // Instantiates its own dependencies (from DOMAIN)
    private val signupCaregiverUseCase: SignupCaregiverUseCase = SignupCaregiverUseCase()
    private val verifySignupOtpUseCase: VerifySignupOtpUseCase = VerifySignupOtpUseCase()
    private val resendSignupOtpUseCase: ResendSignupOtpUseCase = ResendSignupOtpUseCase()
    private val deleteUnverifiedUserUseCase: DeleteUnverifiedUserUseCase = DeleteUnverifiedUserUseCase()

    private val _uiState = MutableStateFlow(CaregiverSignupUiState())
    val uiState = _uiState.asStateFlow()

    private var resendTimerJob: Job? = null

    private fun startResendTimer(durationSeconds: Int = 60) {
        resendTimerJob?.cancel()
        resendTimerJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(resendTimer = durationSeconds)
            (durationSeconds - 1 downTo 0).asFlow()
                .onEach { delay(1000) }
                .collect { secondsRemaining ->
                    _uiState.value = _uiState.value.copy(resendTimer = secondsRemaining)
                }
        }
    }

    private fun stopResendTimer() {
        resendTimerJob?.cancel()
        _uiState.value = _uiState.value.copy(resendTimer = 0)
    }

    fun onProfileImageCropped(croppedUri: Uri) {
        _uiState.value = _uiState.value.copy(profileImageUri = croppedUri)
    }

    fun signup(
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
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = signupCaregiverUseCase(
                context = context,
                email = email,
                password = password,
                firstName = firstName,
                lastName = lastName,
                middleName = middleName,
                phoneNumber = phoneNumber,
                address = address,
                birthday = birthday,
                sex = sex,
                imageUri = _uiState.value.profileImageUri
            )

            result.fold(
                onSuccess = { caregiver ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        signupSuccess = true,
                        createdUserId = caregiver.uid
                    )
                    startResendTimer(60)
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Signup failed"
                    )
                }
            )
        }
    }

    fun verifyOtp(uid: String, otp: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = verifySignupOtpUseCase(uid, otp)

            when (result) {
                OtpResult.OtpVerificationResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        verificationSuccess = true,
                        successMessage = "Verification successful! Please log in."
                    )
                    stopResendTimer()
                }
                OtpResult.OtpVerificationResult.FailureInvalid -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Invalid OTP. Please try again."
                    )
                }
                OtpResult.OtpVerificationResult.FailureMaxAttempts -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Max attempts reached. Please wait 5 minutes."
                    )
                    startResendTimer(300)
                }
                OtpResult.OtpVerificationResult.FailureExpiredOrCooledDown -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "OTP expired or on cooldown. Please resend."
                    )
                    stopResendTimer()
                }
            }
        }
    }

    fun resendOtp(context: Context, uid: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = resendSignupOtpUseCase(context, uid)

            when (result) {
                OtpResult.ResendOtpResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "New OTP sent"
                    )
                    startResendTimer(60)
                }
                OtpResult.ResendOtpResult.FailureCooldown -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Limit reached. Please wait 5 minutes."
                    )
                    startResendTimer(300)
                }
                OtpResult.ResendOtpResult.FailureGeneric -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Please wait 1 minute before resending."
                    )
                }
            }
        }
    }

    fun cancelSignup(uid: String) {
        viewModelScope.launch {
            deleteUnverifiedUserUseCase(uid)
            stopResendTimer()
        }
    }

    fun clearError() {
        if (_uiState.value.errorMessage != null) {
            _uiState.value = _uiState.value.copy(errorMessage = null)
        }
    }
}