package edu.capstone.navisight.auth.ui.signup.caregiver

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.common.reflect.TypeToken
import com.google.firebase.Timestamp
import com.google.gson.Gson
import edu.capstone.navisight.auth.util.LegalDocuments
import edu.capstone.navisight.auth.domain.DeleteUnverifiedUserUseCase
import edu.capstone.navisight.auth.domain.ResendSignupOtpUseCase
import edu.capstone.navisight.auth.domain.SignupCaregiverUseCase
import edu.capstone.navisight.auth.domain.VerifySignupOtpUseCase
import edu.capstone.navisight.auth.domain.usecase.AcceptLegalDocumentsUseCase
import edu.capstone.navisight.auth.model.OtpResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignupFormState(
    val firstName: String = "",
    val middleName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val addressDetails: String = "",
    val country: String = "",
    val sex: String = "",
    val birthday: Timestamp? = null,
    val email: String = "",
    val password: String = "",
    val rePassword: String = "",
    val profileImageUri: Uri? = null,
    val termsAccepted: Boolean = false,
    val privacyAccepted: Boolean = false,
    val province: String = "",
    val city: String = "",
    val availableProvinces: List<String> = emptyList(),
    val availableCities: List<String> = emptyList()
)




data class CaregiverSignupUiState(
    val isLoading: Boolean = false,
    val signupSuccess: Boolean = false,
    val verificationSuccess: Boolean = false,
    val createdUserId: String? = null,
    val errorMessage: String? = null,
    val resendTimer: Int = 0
)

sealed class SignupEvent {
    data class FirstNameChanged(val value: String) : SignupEvent()
    data class MiddleNameChanged(val value: String) : SignupEvent()
    data class LastNameChanged(val value: String) : SignupEvent()
    data class PhoneChanged(val value: String) : SignupEvent()
    data class AddressChanged(val value: String) : SignupEvent()
    data class CountryChanged(val value: String) : SignupEvent()
    data class SexChanged(val value: String) : SignupEvent()
    data class BirthdayChanged(val value: Timestamp?) : SignupEvent()
    data class EmailChanged(val value: String) : SignupEvent()
    data class PasswordChanged(val value: String) : SignupEvent()
    data class RePasswordChanged(val value: String) : SignupEvent()
    data class ImageSelected(val uri: Uri?) : SignupEvent()
    data class TermsChanged(val value: Boolean) : SignupEvent()
    data class PrivacyChanged(val value: Boolean) : SignupEvent()
    data class ProvinceChanged(val value: String) : SignupEvent()
    data class CityChanged(val value: String) : SignupEvent()
}

private var allLocationData: Map<String, List<String>> = emptyMap()



class CaregiverSignupViewModel : ViewModel() {

    private val _formState = MutableStateFlow(SignupFormState())
    val formState = _formState.asStateFlow()

    private val _uiState = MutableStateFlow(CaregiverSignupUiState())
    val uiState = _uiState.asStateFlow()

    private val signupCaregiverUseCase = SignupCaregiverUseCase()
    private val verifySignupOtpUseCase = VerifySignupOtpUseCase()
    private val resendSignupOtpUseCase = ResendSignupOtpUseCase()
    private val deleteUnverifiedUserUseCase = DeleteUnverifiedUserUseCase()
    private val acceptLegalDocumentsUseCase = AcceptLegalDocumentsUseCase()

    fun loadLocationData(context: Context) {
        viewModelScope.launch {
            try {
                val jsonString = context.assets.open("philippine_locations.json")
                    .bufferedReader()
                    .use { it.readText() }

                val type = object : TypeToken<Map<String, List<String>>>() {}.type
                allLocationData = Gson().fromJson(jsonString, type)

                _formState.update { it.copy(
                    availableProvinces = allLocationData.keys.sorted()
                )}
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onEvent(event: SignupEvent) {
        when(event) {
            is SignupEvent.FirstNameChanged -> _formState.update { it.copy(firstName = event.value) }
            is SignupEvent.MiddleNameChanged -> _formState.update { it.copy(middleName = event.value) }
            is SignupEvent.LastNameChanged -> _formState.update { it.copy(lastName = event.value) }
            is SignupEvent.PhoneChanged -> _formState.update { it.copy(phone = event.value) }
            is SignupEvent.AddressChanged -> _formState.update { it.copy(addressDetails = event.value) }
            is SignupEvent.CountryChanged -> _formState.update { it.copy(country = event.value) }
            is SignupEvent.SexChanged -> _formState.update { it.copy(sex = event.value) }
            is SignupEvent.BirthdayChanged -> _formState.update { it.copy(birthday = event.value) }
            is SignupEvent.EmailChanged -> _formState.update { it.copy(email = event.value) }
            is SignupEvent.PasswordChanged -> _formState.update { it.copy(password = event.value) }
            is SignupEvent.RePasswordChanged -> _formState.update { it.copy(rePassword = event.value) }
            is SignupEvent.ImageSelected -> _formState.update { it.copy(profileImageUri = event.uri) }
            is SignupEvent.TermsChanged -> _formState.update { it.copy(termsAccepted = event.value) }
            is SignupEvent.PrivacyChanged -> _formState.update { it.copy(privacyAccepted = event.value) }
            is SignupEvent.ProvinceChanged -> {
                val cities = allLocationData[event.value] ?: emptyList()
                _formState.update { it.copy(
                    province = event.value,
                    city = "", // Reset city when province changes
                    availableCities = cities.sorted()
                )}
            }
            is SignupEvent.CityChanged -> {
                _formState.update { it.copy(city = event.value) }
            }
        }
    }

    fun submitSignup(context: Context) {
        val form = _formState.value
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val result = signupCaregiverUseCase(
                    context = context,
                    email = form.email,
                    password = form.password,
                    firstName = form.firstName,
                    lastName = form.lastName,
                    middleName = form.middleName,
                    phoneNumber = form.phone,
                    address = form.addressDetails,
                    country = form.country,
                    birthday = form.birthday!!,
                    sex = form.sex,
                    imageUri = form.profileImageUri
                )

                result.onSuccess { caregiver ->
                    val uid = caregiver.uid

                    val legalResult = acceptLegalDocumentsUseCase(
                        uid = uid,
                        email = form.email,
                        termsAccepted = form.termsAccepted,
                        privacyAccepted = form.privacyAccepted,
                        version = LegalDocuments.TERMS_VERSION
                    )

                    if (legalResult.isSuccess) {
                        _uiState.update { it.copy(isLoading = false, signupSuccess = true, createdUserId = uid) }
                    } else {
                        deleteUnverifiedUserUseCase(uid)
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to save legal consent. Signup cancelled.") }
                    }

                }.onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Signup Failed") }
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun verifyOtp(uid: String, code: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                when (val result = verifySignupOtpUseCase(uid, code)) {
                    is OtpResult.OtpVerificationResult.Success -> {
                        _uiState.update { it.copy(isLoading = false, verificationSuccess = true) }
                    }
                    is OtpResult.OtpVerificationResult.FailureInvalid -> {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Invalid OTP. Please try again.") }
                    }
                    is OtpResult.OtpVerificationResult.FailureMaxAttempts -> {
                        deleteUnverifiedUserUseCase(uid)
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Maximum attempts reached. Signup cancelled.", signupSuccess = false) }
                    }
                    is OtpResult.OtpVerificationResult.FailureExpiredOrCooledDown -> {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Code expired or in cooldown.") }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun resendOtp(context: Context, uid: String) {
        viewModelScope.launch {
            try {
                when (resendSignupOtpUseCase(context, uid)) {
                    is OtpResult.ResendOtpResult.Success -> {
                        Toast.makeText(context, "Verification code resent.", Toast.LENGTH_SHORT).show()
                    }
                    is OtpResult.ResendOtpResult.FailureCooldown -> {
                        Toast.makeText(context, "Please wait before resending.", Toast.LENGTH_SHORT).show()
                    }
                    is OtpResult.ResendOtpResult.FailureEmailAlreadyInUse -> {
                        Toast.makeText(context, "Email is already in use.", Toast.LENGTH_SHORT).show()
                    }
                    is OtpResult.ResendOtpResult.FailureGeneric -> {
                        Toast.makeText(context, "Failed to resend code.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to resend code.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun cancelSignup(uid: String) {
        viewModelScope.launch {
            try {
                deleteUnverifiedUserUseCase(uid)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _uiState.update { CaregiverSignupUiState() }
            _formState.update { SignupFormState() }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}