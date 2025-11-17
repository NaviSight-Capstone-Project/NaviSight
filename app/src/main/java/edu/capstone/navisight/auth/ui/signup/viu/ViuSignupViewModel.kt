package edu.capstone.navisight.auth.ui.signup.viu

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import edu.capstone.navisight.auth.domain.DeleteUnverifiedViuUserUseCase
import edu.capstone.navisight.auth.domain.ResendViuSignupOtpUseCase
import edu.capstone.navisight.auth.domain.SignupViuUseCase
import edu.capstone.navisight.auth.domain.VerifyViuSignupOtpUseCase
import edu.capstone.navisight.auth.model.OtpResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


data class ViuSignupUiState(
    val isLoading: Boolean = false,
    val signupSuccess: Boolean = false,
    val verificationSuccess: Boolean = false,
    val createdUserId: String? = null,
    val createdCaregiverId: String? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val profileImageUri: Uri? = null,
    val resendTimer: Int = 0
)

class ViuSignupViewModel : ViewModel() {

    // Instantiates VIU Use Cases
    private val signupViuUseCase: SignupViuUseCase = SignupViuUseCase()
    private val verifyViuSignupOtpUseCase: VerifyViuSignupOtpUseCase = VerifyViuSignupOtpUseCase()
    private val resendViuSignupOtpUseCase: ResendViuSignupOtpUseCase = ResendViuSignupOtpUseCase()
    private val deleteUnverifiedViuUserUseCase: DeleteUnverifiedViuUserUseCase = DeleteUnverifiedViuUserUseCase()

    private val _uiState = MutableStateFlow(ViuSignupUiState())
    val uiState = _uiState.asStateFlow()

    private var resendTimerJob: Job? = null

    //  Timer logic is identical to Caregiver's
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

    //  Simplified Signup function
    fun signup(
        context: Context,
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        middleName: String,
        phone: String,
        address: String,
        status: String,
        caregiverEmail: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = signupViuUseCase(
                context = context,
                email = email,
                password = password,
                firstName = firstName,
                lastName = lastName,
                middleName = middleName,
                phone = phone,
                address = address,
                status = status,
                imageUri = _uiState.value.profileImageUri,
                caregiverEmail = caregiverEmail
            )

            result.fold(
                onSuccess = { (viu, caregiverUid) -> // Destructure the Pair
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        signupSuccess = true,
                        createdUserId = viu.uid,
                        createdCaregiverId = caregiverUid // Store Caregiver UID
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

    fun verifyOtp(viuUid: String, caregiverUid: String, otp: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = verifyViuSignupOtpUseCase(caregiverUid, viuUid, otp)

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

    fun resendOtp(context: Context, caregiverUid: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = resendViuSignupOtpUseCase(context, caregiverUid)

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

    fun cancelSignup(viuUid: String) { // Modified
        viewModelScope.launch {
            deleteUnverifiedViuUserUseCase(viuUid)
            stopResendTimer()
        }
    }

    fun clearError() {
        if (_uiState.value.errorMessage != null) {
            _uiState.value = _uiState.value.copy(errorMessage = null)
        }
    }
}