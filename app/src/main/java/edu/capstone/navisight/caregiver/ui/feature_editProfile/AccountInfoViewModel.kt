package edu.capstone.navisight.caregiver.ui.feature_editProfile

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.common.reflect.TypeToken
import edu.capstone.navisight.caregiver.model.Caregiver
import edu.capstone.navisight.auth.model.OtpResult
import edu.capstone.navisight.caregiver.domain.CaregiverProfileUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.gson.Gson
import edu.capstone.navisight.common.domain.usecase.GetCurrentUserUidUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


data class SignupFormState(
    val province: String = "",
    val city: String = "",
    val availableProvinces: List<String> = emptyList(),
    val availableCities: List<String> = emptyList()
)

sealed class SignupEvent {
    data class ProvinceChanged(val value: String) : SignupEvent()
    data class CityChanged(val value: String) : SignupEvent()
}

private var allLocationData: Map<String, List<String>> = emptyMap()

class AccountInfoViewModel(
    private val profileUseCase: CaregiverProfileUseCase = CaregiverProfileUseCase(),
    private val getCurrentUserUidUseCase: GetCurrentUserUidUseCase = GetCurrentUserUidUseCase()
) : ViewModel() {

    private val _profile = MutableStateFlow<Caregiver?>(null)
    val profile: StateFlow<Caregiver?> = _profile.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()
    private val _pendingNewPassword = MutableStateFlow<String?>(null)

    private val _reauthError = MutableStateFlow<String?>(null)
    val reauthError: StateFlow<String?> = _reauthError.asStateFlow()

    private val _isLockedOut = MutableStateFlow(false)
    val isLockedOut: StateFlow<Boolean> = _isLockedOut.asStateFlow()

    init {
        checkGlobalLockout()
    }

    private fun checkGlobalLockout() {
        viewModelScope.launch {
            val uid = getCurrentUserUidUseCase() ?: return@launch
            profileUseCase.checkLockout(uid).fold(
                onSuccess = {
                    _isLockedOut.value = false
                },
                onFailure = { exception ->
                    _isLockedOut.value = true
                    val lockoutMsg = exception.message ?: "Account is locked."
                    _reauthError.value = lockoutMsg
                    // _uiEvent.send(lockoutMsg) // Optional notification
                }
            )
        }
    }
    // Add this to handle the shared lockout verification
    fun checkLockoutAndPerform(uid: String, onAvailable: () -> Unit) {
        viewModelScope.launch {
            profileUseCase.checkLockout(uid).fold(
                onSuccess = { onAvailable() },
                onFailure = { _reauthError.value = it.message }
            )
        }
    }
    private val _formState = MutableStateFlow(SignupFormState())
    val formState = _formState.asStateFlow()

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

    // Update the existing reauth check
    fun handleReauthenticationFailure(uid: String) {
        viewModelScope.launch {
            val message = profileUseCase.handleFailedAttempt(uid)
            _reauthError.value = message
        }
    }

    fun clearReauthError() {
        _reauthError.value = null
    }


    fun loadProfile(uid: String) {
        viewModelScope.launch {
            _profile.value = profileUseCase.getProfile(uid)
        }
    }

    fun uploadProfileImageNow(uid: String, uri: Uri) {
        viewModelScope.launch {
            _isSaving.value = true // Show loading spinner
            try {
                val success = profileUseCase.uploadProfileImage(uid, uri)
                if (success) {
                    _uiEvent.send("Photo updated successfully.")
                    loadProfile(uid) // Refresh profile to get new URL
                } else {
                    _uiEvent.send("Photo upload failed.")
                }
            } catch (e: Exception) {
                _uiEvent.send("Error uploading photo: ${e.message}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun saveProfileChanges(
        uid: String,
        firstName: String,
        middleName: String,
        lastName: String,
        phoneNumber: String,
        birthday: Timestamp?,
        address: String,
        password: String,
        province: String,
        city: String
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            _reauthError.value = null

            try {
                // Check lockout state from Firestore before attempting
                profileUseCase.checkLockout(uid).fold(
                    onSuccess = {
                        val reauthOk = profileUseCase.reauthenticateUser(password)
                        if (!reauthOk) {
                            val lockoutMsg = profileUseCase.handleFailedAttempt(uid)
                            _reauthError.value = lockoutMsg
                            _isSaving.value = false
                            return@launch
                        }

                        // Success: Clear the lockout counter
                        profileUseCase.clearLockout(uid)

                        val current = _profile.value
                        val updatedData = mutableMapOf<String, Any?>()
                        if (firstName != (current?.firstName ?: "")) updatedData["firstName"] = firstName
                        if (middleName != (current?.middleName ?: "")) updatedData["middleName"] = middleName
                        if (lastName != (current?.lastName ?: "")) updatedData["lastName"] = lastName
                        if (phoneNumber != (current?.phoneNumber ?: "")) updatedData["phoneNumber"] = phoneNumber
                        if (birthday != current?.birthday) updatedData["birthday"] = birthday
                        if (address != (current?.address ?: "")) updatedData["address"] = address
                        if (province != (current?.province ?: "")) updatedData["province"] = province
                        if (city != (current?.city ?: "")) updatedData["city"] = city

                        val nonNullUpdatedData = updatedData.filterValues { it != null } as Map<String, Any>

                        if (nonNullUpdatedData.isNotEmpty()) {
                            val success = profileUseCase.updateProfile(uid, nonNullUpdatedData)
                            if (success) {
                                _uiEvent.send("Profile updated successfully.")
                                loadProfile(uid)
                            } else {
                                _uiEvent.send("Update failed.")
                            }
                        } else {
                            _uiEvent.send("No changes to update.")
                        }
                    },
                    onFailure = {
                        _reauthError.value = it.message
                    }
                )
            } catch (e: Exception) {
                _uiEvent.send("Error: ${e.message}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun updateEmail(context: Context, uid: String, newEmail: String, password: String) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val result = profileUseCase.updateEmail(context, uid, newEmail, password)
                when (result) {
                    is OtpResult.ResendOtpResult.Success ->
                        _uiEvent.send("An OTP has been sent to $newEmail. Please check your inbox.")

                    is OtpResult.ResendOtpResult.FailureEmailAlreadyInUse -> // <--- HANDLE NEW CASE
                        _uiEvent.send("That email address is already in use by another account.")

                    is OtpResult.ResendOtpResult.FailureCooldown ->
                        _uiEvent.send("Please wait 5 minutes to request a new OTP.")

                    is OtpResult.ResendOtpResult.FailureGeneric ->
                        _uiEvent.send("Failed to send OTP. Please check your password.")
                }
            } catch (e: Exception) {
                _uiEvent.send("Error updating email: ${e.message}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun verifyEmailOtp(uid: String, otp: String) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val result = profileUseCase.verifyEmailOtp(uid, otp)
                when (result) {
                    is OtpResult.OtpVerificationResult.Success -> {
                        loadProfile(uid) // Reload profile to show updated email
                        _uiEvent.send("Email successfully verified and updated.")
                    }
                    is OtpResult.OtpVerificationResult.FailureInvalid -> _uiEvent.send("Invalid OTP. Please try again.")
                    is OtpResult.OtpVerificationResult.FailureMaxAttempts -> _uiEvent.send("Too many attempts. OTP is now invalid.")
                    is OtpResult.OtpVerificationResult.FailureExpiredOrCooledDown -> _uiEvent.send("Invalid or expired OTP.")
                }
            } catch (e: Exception) {
                _uiEvent.send("Error verifying email OTP: ${e.message}")
            } finally {
                _isSaving.value = false
            }
        }
    }


    fun requestPasswordChange(context: Context, uid: String, currentPass: String, newPass: String) {
        viewModelScope.launch {
            _isSaving.value = true
            _pendingNewPassword.value = newPass // Store new pass temporarily
            try {
                val result = profileUseCase.requestPasswordChange(context, currentPass)
                when(result) {
                    is OtpResult.PasswordChangeRequestResult.OtpSent -> _uiEvent.send("Password OTP Sent. Please check your email.")
                    is OtpResult.PasswordChangeRequestResult.FailureInvalidPassword -> _uiEvent.send("Invalid current password. Please try again.")
                    is OtpResult.PasswordChangeRequestResult.FailureMaxAttempts -> _uiEvent.send("Too many attempts. Password change locked for 5 minutes.")
                    is OtpResult.PasswordChangeRequestResult.FailureCooldown -> _uiEvent.send("Password change is locked. Please wait.")
                    is OtpResult.PasswordChangeRequestResult.FailureGeneric -> _uiEvent.send("An error occurred requesting password change.")
                }
            } catch (e: Exception) {
                _uiEvent.send("Error requesting password change: ${e.message}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun resendPasswordChangeOtp(context: Context) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val result = profileUseCase.resendPasswordChangeOtp(context)
                when(result) {
                    is OtpResult.ResendOtpResult.Success -> _uiEvent.send("Password OTP Resent. Please check your email.")
                    is OtpResult.ResendOtpResult.FailureCooldown -> _uiEvent.send("Please wait 5 minutes to resend password OTP.")
                    is OtpResult.ResendOtpResult.FailureGeneric -> _uiEvent.send("Please wait 1 minute to resend password OTP.") // More specific message
                    is OtpResult.ResendOtpResult.FailureEmailAlreadyInUse -> _uiEvent.send("Email is already used. Please try a different email.")
                }
            } catch (e: Exception) {
                _uiEvent.send("Error resending password OTP: ${e.message}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun verifyPasswordChangeOtp(otp: String) {
        viewModelScope.launch {
            val newPassword = _pendingNewPassword.value
            if (newPassword == null) {
                _uiEvent.send("Error: Session expired. Please request password change again.")
                return@launch
            }

            _isSaving.value = true
            try {
                val result = profileUseCase.verifyPasswordChangeOtp(otp, newPassword)
                when (result) {
                    is OtpResult.OtpVerificationResult.Success -> {
                        _uiEvent.send("Password updated successfully.")
                        _pendingNewPassword.value = null // Clear temporary password
                    }
                    is OtpResult.OtpVerificationResult.FailureInvalid -> _uiEvent.send("Invalid OTP. Please try again.")
                    is OtpResult.OtpVerificationResult.FailureMaxAttempts -> _uiEvent.send("Too many attempts. OTP is now invalid.")
                    is OtpResult.OtpVerificationResult.FailureExpiredOrCooledDown -> _uiEvent.send("Invalid or expired OTP.")
                }
            } catch (e: Exception) {
                _uiEvent.send("Error verifying password OTP: ${e.message}")
            } finally {
                _isSaving.value = false
            }
        }
    }
    fun cancelEmailChange(uid: String) {
        viewModelScope.launch {
            profileUseCase.cancelEmailChange(uid)
        }
    }

    fun cancelPasswordChange(uid: String) {
        viewModelScope.launch {
            profileUseCase.cancelPasswordChange(uid)
        }
    }

    fun syncVerifiedEmail(uid: String) {
        viewModelScope.launch {
            try {
                val user = FirebaseAuth.getInstance().currentUser
                user?.reload()?.await()

                if (user != null && user.isEmailVerified) {
                    val verifiedEmail = user.email
                    if (!verifiedEmail.isNullOrBlank()) {
                        val updated = profileUseCase.updateProfile(
                            uid,
                            mapOf(
                                "email" to verifiedEmail,
                                "pendingEmail" to null
                            ) as Map<String, Any>
                        )

                        if (updated) {
                            _uiEvent.send("Your email has been verified and synced.")
                            loadProfile(uid)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("EmailSync", "Failed to sync verified email: ${e.message}")
            }
        }
    }
    fun deleteAccount(uid: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (_isLockedOut.value) {
                _uiEvent.send("Account is locked due to too many failed attempts. Please wait.")
                return@launch
            }

            _isSaving.value = true
            _reauthError.value = null

            // Use the Sequence which now properly calls handleFailedAttempt
            profileUseCase.deleteAccountSequence(uid, password).fold(
                onSuccess = {
                    _isLockedOut.value = false
                    _uiEvent.send("Account deleted successfully.")
                    onSuccess()
                },
                onFailure = { error ->
                    val errorMsg = error.message ?: "Delete failed"
                    _reauthError.value = errorMsg

                    // Update Lockout State based on the message returned by the UseCase
                    if (errorMsg.contains("Locked", ignoreCase = true) ||
                        errorMsg.contains("15 minutes", ignoreCase = true)) {
                        _isLockedOut.value = true
                    }
                }
            )
            _isSaving.value = false
        }
    }
}