package edu.capstone.navisight.caregiver.ui.feature_editViuProfile

import android.content.Context
import android.net.Uri
import android.util.Patterns
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.capstone.navisight.auth.model.OtpResult
import edu.capstone.navisight.caregiver.model.Viu
import edu.capstone.navisight.caregiver.domain.viuUseCase.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class SecurityFlowState { IDLE, PENDING_OTP, LOADING }

enum class SaveFlowState {
    IDLE,
    PENDING_PASSWORD,
    PENDING_OTP,
    SAVING
}

enum class DeleteFlowState { IDLE, PENDING_PASSWORD, DELETING }


class ViuProfileViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val viuUid: String = checkNotNull(savedStateHandle["viuUid"])

    private val checkEditPermissionUseCase: CheckEditPermissionUseCase = CheckEditPermissionUseCase()

    private val dateFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    private val getViuDetailsUseCase: GetViuDetailsUseCase = GetViuDetailsUseCase()
    private val deleteViuUseCase: DeleteViuUseCase = DeleteViuUseCase()
    private val updateViuUseCase: UpdateViuUseCase = UpdateViuUseCase()

    private val sendViuPasswordResetUseCase: SendViuPasswordResetUseCase = SendViuPasswordResetUseCase()
    private val requestViuEmailChangeUseCase: RequestViuEmailChangeUseCase = RequestViuEmailChangeUseCase()
    private val verifyViuEmailChangeUseCase: VerifyViuEmailChangeUseCase = VerifyViuEmailChangeUseCase()

    private val cancelViuEmailChangeUseCase: CancelViuEmailChangeUseCase = CancelViuEmailChangeUseCase()

    private val reauthenticateCaregiverUseCase: ReauthenticateCaregiverUseCase = ReauthenticateCaregiverUseCase()
    private val sendVerificationOtpToCaregiverUseCase: SendVerificationOtpToCaregiverUseCase = SendVerificationOtpToCaregiverUseCase()
    private val verifyCaregiverOtpUseCase: VerifyCaregiverOtpUseCase = VerifyCaregiverOtpUseCase()

    private val cancelViuProfileUpdateUseCase: CancelViuProfileUpdateUseCase = CancelViuProfileUpdateUseCase()
    private val uploadViuProfileImageUseCase: UploadViuProfileImageUseCase = UploadViuProfileImageUseCase()

    private val _viu = MutableStateFlow<Viu?>(null)
    val viu: StateFlow<Viu?> = _viu.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private val _emailFlowState = MutableStateFlow(SecurityFlowState.IDLE)
    val emailFlowState: StateFlow<SecurityFlowState> = _emailFlowState.asStateFlow()

    private val _securityError = MutableStateFlow<String?>(null)
    val securityError: StateFlow<String?> = _securityError.asStateFlow()

    private val _passwordResetSuccess = MutableStateFlow(false)
    val passwordResetSuccess: StateFlow<Boolean> = _passwordResetSuccess.asStateFlow()

    private val _emailChangeSuccess = MutableStateFlow(false)
    val emailChangeSuccess: StateFlow<Boolean> = _emailChangeSuccess.asStateFlow()

    private val _saveFlowState = MutableStateFlow(SaveFlowState.IDLE)
    val saveFlowState: StateFlow<SaveFlowState> = _saveFlowState.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    private val _deleteFlowState = MutableStateFlow(DeleteFlowState.IDLE)
    val deleteFlowState: StateFlow<DeleteFlowState> = _deleteFlowState.asStateFlow()

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()

    private var pendingViuUpdate: Viu? = null
    private var pendingNewEmail: String? = null

    private var saveResendTimerJob: Job? = null
    private val _saveResendTimer = MutableStateFlow(0)
    val saveResendTimer: StateFlow<Int> = _saveResendTimer.asStateFlow()

    private var emailResendTimerJob: Job? = null
    private val _emailResendTimer = MutableStateFlow(0)
    private val _canEdit = MutableStateFlow(false)
    val canEdit: StateFlow<Boolean> = _canEdit.asStateFlow()
    val emailResendTimer: StateFlow<Int> = _emailResendTimer.asStateFlow()

    init {
        loadViuDetails()
        checkPermissions()
    }

    private fun startSaveResendTimer() {
        saveResendTimerJob?.cancel()
        saveResendTimerJob = viewModelScope.launch {
            val durationSeconds = 60 // 1 minute
            _saveResendTimer.value = durationSeconds
            (durationSeconds - 1 downTo 0).asFlow()
                .onEach { delay(1000) }
                .collect { secondsRemaining ->
                    _saveResendTimer.value = secondsRemaining
                }
        }
    }

    private fun stopSaveResendTimer() {
        saveResendTimerJob?.cancel()
        _saveResendTimer.value = 0
    }

    private fun startEmailResendTimer() {
        emailResendTimerJob?.cancel()
        emailResendTimerJob = viewModelScope.launch {
            val durationSeconds = 60 // 1 minute
            _emailResendTimer.value = durationSeconds
            (durationSeconds - 1 downTo 0).asFlow()
                .onEach { delay(1000) }
                .collect { secondsRemaining ->
                    _emailResendTimer.value = secondsRemaining
                }
        }
    }

    private fun stopEmailResendTimer() {
        emailResendTimerJob?.cancel()
        _emailResendTimer.value = 0
    }

    private fun loadViuDetails() {
        viewModelScope.launch {
            _isLoading.value = true
            getViuDetailsUseCase(viuUid).collect { viuData ->
                _viu.value = viuData
                _isLoading.value = false
            }
        }
    }

    fun uploadProfileImage(imageUri: Uri) {
        if (!_canEdit.value) {
            _error.value = "Only the Primary Caregiver can change the photo."
            return
        }
        viewModelScope.launch {
            _isUploadingImage.value = true
            _error.value = null

            val result = uploadViuProfileImageUseCase(viuUid, imageUri)

            result.fold(
                onSuccess = {
                    _isUploadingImage.value = false
                },
                onFailure = {
                    _isUploadingImage.value = false
                    _error.value = it.message ?: "Failed to upload image"
                }
            )
        }
    }

    private fun validateName(name: String, fieldName: String): String? {
        if (name.isBlank()) return "$fieldName cannot be empty."
        if (name.any { it.isDigit() }) return "$fieldName cannot contain numbers."
        return null
    }

    private fun validatePhone(phone: String): String? {
        if (phone.isBlank()) return "Phone number cannot be empty."
        if (!phone.isDigitsOnly()) return "Phone number must contain only digits."
        if (phone.length !in 10..13) return "Phone number must be 10-13 digits."
        return null
    }

    private fun validateEmail(email: String): String? {
        if (email.isBlank()) return "Email cannot be empty."
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "Invalid email format."
        return null
    }

    private fun validateBirthday(birthday: String): String? {
        if (birthday.isBlank()) return "Birthday cannot be empty."
        try {
            val birthDate = dateFormatter.parse(birthday) ?: return "Invalid birthday format."
            val today = Calendar.getInstance()
            val birthCal = Calendar.getInstance()
            birthCal.time = birthDate

            var age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            if (age !in 18..60) return "Age must be between 18 and 60."
        } catch (e: Exception) {
            return "Invalid birthday format."
        }
        return null
    }

    private fun validateSex(sex: String): String? {
        if (sex.isBlank()) return "Sex cannot be empty."
        return null
    }
    private fun checkPermissions() {
        viewModelScope.launch {
            val result = checkEditPermissionUseCase(viuUid)
            result.fold(
                onSuccess = { isPrimary ->
                    _canEdit.value = isPrimary
                },
                onFailure = {
                    _canEdit.value = false // Default to no edit if error
                }
            )
        }
    }

    fun startSaveFlow(
        firstName: String,
        middleName: String,
        lastName: String,
        birthday: String,
        sex: String,
        phone: String,
        address: String,
        status: String
    ) {
        if (!_canEdit.value) {
            _saveError.value = "Only the Primary Caregiver can edit this profile."
            return
        }
        _saveError.value = null // Clear previous errors

        // Perform Validations
        val firstNameError = validateName(firstName, "First Name")
        if (firstNameError != null) { _saveError.value = firstNameError; return }

        val lastNameError = validateName(lastName, "Last Name")
        if (lastNameError != null) { _saveError.value = lastNameError; return }

        val birthdayError = validateBirthday(birthday)
        if (birthdayError != null) { _saveError.value = birthdayError; return }

        val sexError = validateSex(sex)
        if (sexError != null) { _saveError.value = sexError; return }

        val phoneError = validatePhone(phone)
        if (phoneError != null) { _saveError.value = phoneError; return }

        val currentViu = _viu.value ?: return

        val hasChanges = firstName != currentViu.firstName ||
                middleName != currentViu.middleName ||
                lastName != currentViu.lastName ||
                birthday != (currentViu.birthday ?: "") ||
                sex != (currentViu.sex ?: "") ||
                phone != currentViu.phone ||
                address != (currentViu.address ?: "") ||
                status != (currentViu.status ?: "")

        if (!hasChanges) {
            _saveError.value = "No changes to save"
            return
        }

        pendingViuUpdate = currentViu.copy(
            firstName = firstName.trim(),
            middleName = middleName.trim(),
            lastName = lastName.trim(),
            birthday = birthday.trim(),
            sex = sex.trim(),
            phone = phone.trim(),
            address = address.trim().takeIf { it.isNotEmpty() },
            status = status.trim().takeIf { it.isNotEmpty() }
        )

        _saveFlowState.value = SaveFlowState.PENDING_PASSWORD
    }

    fun reauthenticateAndSendOtp(password: String, context: Context) {
        viewModelScope.launch {
            _saveFlowState.value = SaveFlowState.SAVING
            _saveError.value = null

            val reauthResult = reauthenticateCaregiverUseCase(password)
            reauthResult.fold(
                onSuccess = {
                    val otpResult = sendVerificationOtpToCaregiverUseCase(context)
                    otpResult.fold(
                        onSuccess = { resendResult ->
                            when (resendResult) {
                                OtpResult.ResendOtpResult.Success -> {
                                    _saveFlowState.value = SaveFlowState.PENDING_OTP
                                    startSaveResendTimer()
                                }
                                OtpResult.ResendOtpResult.FailureCooldown -> {
                                    _saveFlowState.value = SaveFlowState.PENDING_PASSWORD
                                    _saveError.value = "Max resends reached. Please wait 5 minutes."
                                }
                                OtpResult.ResendOtpResult.FailureGeneric -> {
                                    _saveFlowState.value = SaveFlowState.PENDING_PASSWORD
                                    _saveError.value = "Failed to send OTP. Please wait 1 minute."
                                }
                                OtpResult.ResendOtpResult.FailureEmailAlreadyInUse -> {
                                    _saveFlowState.value = SaveFlowState.PENDING_PASSWORD
                                    _saveError.value = "An unexpected error occurred."
                                }
                            }
                        },
                        onFailure = {
                            _saveFlowState.value = SaveFlowState.PENDING_PASSWORD
                            _saveError.value = it.message ?: "Failed to send OTP"
                        }
                    )
                },
                onFailure = {
                    _saveFlowState.value = SaveFlowState.PENDING_PASSWORD
                    _saveError.value = it.message ?: "Invalid password"
                }
            )
        }
    }

    fun resendOtpForSave(context: Context) {
        viewModelScope.launch {
            _saveFlowState.value = SaveFlowState.SAVING
            _saveError.value = null

            val otpResult = sendVerificationOtpToCaregiverUseCase(context)
            otpResult.fold(
                onSuccess = { resendResult ->
                    _saveFlowState.value = SaveFlowState.PENDING_OTP
                    when (resendResult) {
                        OtpResult.ResendOtpResult.Success -> {
                            _saveError.value = "New OTP sent"
                            startSaveResendTimer()
                        }
                        OtpResult.ResendOtpResult.FailureCooldown -> {
                            _saveError.value = "Max resends reached. Please wait 5 minutes."
                        }
                        OtpResult.ResendOtpResult.FailureGeneric -> {
                            _saveError.value = "Please wait 1 minute before resending."
                        }
                        OtpResult.ResendOtpResult.FailureEmailAlreadyInUse -> {
                            _saveError.value = "An unexpected error occurred."
                        }
                    }
                },
                onFailure = {
                    _saveFlowState.value = SaveFlowState.PENDING_OTP
                    _saveError.value = it.message ?: "Failed to resend OTP"
                }
            )
        }
    }

    fun verifyOtpAndSave(otp: String) {
        val viuToSave = pendingViuUpdate ?: return

        viewModelScope.launch {
            _saveFlowState.value = SaveFlowState.SAVING
            _saveError.value = null

            val verifyResult = verifyCaregiverOtpUseCase(otp)
            verifyResult.fold(
                onSuccess = { verificationResult ->
                    when (verificationResult) {
                        OtpResult.OtpVerificationResult.Success -> {
                            // OTP is good, proceed to save
                            val updateResult = updateViuUseCase(viuToSave)
                            updateResult.fold(
                                onSuccess = {
                                    _saveFlowState.value = SaveFlowState.IDLE
                                    _saveSuccess.value = true
                                    pendingViuUpdate = null
                                    stopSaveResendTimer()
                                },
                                onFailure = {
                                    _saveFlowState.value = SaveFlowState.IDLE
                                    _saveError.value = it.message ?: "Failed to save profile"
                                }
                            )
                        }
                        OtpResult.OtpVerificationResult.FailureInvalid -> {
                            _saveFlowState.value = SaveFlowState.PENDING_OTP
                            _saveError.value = "Invalid OTP. Please try again."
                        }
                        OtpResult.OtpVerificationResult.FailureMaxAttempts -> {
                            _saveFlowState.value = SaveFlowState.IDLE
                            _saveError.value = "Max attempts reached. Please wait 5 minutes."
                            stopSaveResendTimer()
                        }
                        OtpResult.OtpVerificationResult.FailureExpiredOrCooledDown -> {
                            _saveFlowState.value = SaveFlowState.IDLE
                            _saveError.value = "OTP expired or on cooldown. Please try again."
                            stopSaveResendTimer()
                        }
                    }
                },
                onFailure = {
                    _saveFlowState.value = SaveFlowState.PENDING_OTP
                    _saveError.value = it.message ?: "Verification failed"
                }
            )
        }
    }

    fun resetSaveFlow() {
        _saveFlowState.value = SaveFlowState.IDLE
        _saveError.value = null
        pendingViuUpdate = null
        stopSaveResendTimer()
        viewModelScope.launch {
            cancelViuProfileUpdateUseCase()
        }
    }

    fun clearSaveError() {
        _saveError.value = null
    }

    fun startDeleteFlow() {
        // SECURITY CHECK
        if (!_canEdit.value) {
            _deleteError.value = "Only the Primary Caregiver can delete this profile."
            return
        }
        _deleteFlowState.value = DeleteFlowState.PENDING_PASSWORD
        _deleteError.value = null
    }

    fun reauthenticateAndDelete(password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _deleteFlowState.value = DeleteFlowState.DELETING
            _deleteError.value = null

            val reauthResult = reauthenticateCaregiverUseCase(password)
            reauthResult.fold(
                onSuccess = {
                    val deleteResult = deleteViuUseCase(viuUid)
                    deleteResult.fold(
                        onSuccess = {
                            _deleteFlowState.value = DeleteFlowState.IDLE
                            onSuccess()
                        },
                        onFailure = {
                            _deleteFlowState.value = DeleteFlowState.IDLE
                            _deleteError.value = it.message ?: "Failed to delete profile"
                        }
                    )
                },
                onFailure = {
                    _deleteFlowState.value = DeleteFlowState.PENDING_PASSWORD
                    _deleteError.value = it.message ?: "Invalid password"
                }
            )
        }
    }

    fun resetDeleteFlow() {
        if (!_canEdit.value) {
            _deleteError.value = "Only the Primary Caregiver can delete this profile."
            return
        }
        _deleteFlowState.value = DeleteFlowState.IDLE
        _deleteError.value = null
    }

    fun clearDeleteError() {
        _deleteError.value = null
    }

    fun onSaveSuccessShown() { _saveSuccess.value = false }
    fun onPasswordResetSuccessShown() { _passwordResetSuccess.value = false }
    fun onEmailChangeSuccessShown() { _emailChangeSuccess.value = false }
    fun clearSecurityError() { _securityError.value = null }

    fun sendPasswordReset() {
        val viuEmail = _viu.value?.email ?: return
        viewModelScope.launch {
            _emailFlowState.value = SecurityFlowState.LOADING
            _securityError.value = null
            _passwordResetSuccess.value = false
            val result = sendViuPasswordResetUseCase(viuEmail)
            result.fold(onSuccess = { _emailFlowState.value = SecurityFlowState.IDLE; _passwordResetSuccess.value = true },
                onFailure = { _emailFlowState.value = SecurityFlowState.IDLE; _securityError.value = it.message ?: "Failed to send reset email" }
            )
        }
    }

    fun requestEmailChange(context: Context, newEmail: String) {
        if (!_canEdit.value) {
            _securityError.value = "Only the Primary Caregiver can change security settings."
            return
        }
        _securityError.value = null

        val emailError = validateEmail(newEmail)
        if (emailError != null) { _securityError.value = emailError; return }

        pendingNewEmail = newEmail

        viewModelScope.launch {
            _emailFlowState.value = SecurityFlowState.LOADING

            val result = requestViuEmailChangeUseCase(context, viuUid, newEmail)
            result.fold(
                onSuccess = { resendResult ->
                    when(resendResult) {
                        OtpResult.ResendOtpResult.Success -> {
                            _emailFlowState.value = SecurityFlowState.PENDING_OTP
                            startEmailResendTimer()
                        }
                        OtpResult.ResendOtpResult.FailureCooldown -> {
                            _emailFlowState.value = SecurityFlowState.IDLE
                            _securityError.value = "Max resends reached. Please wait 5 minutes."
                        }
                        OtpResult.ResendOtpResult.FailureGeneric -> {
                            _emailFlowState.value = SecurityFlowState.IDLE
                            _securityError.value = "Failed to send OTP. Please wait 1 minute."
                        }
                        OtpResult.ResendOtpResult.FailureEmailAlreadyInUse -> {
                            _emailFlowState.value = SecurityFlowState.IDLE
                            _securityError.value = "This email is already in use by another account."
                        }
                    }
                },
                onFailure = {
                    _emailFlowState.value = SecurityFlowState.IDLE
                    _securityError.value = it.message ?: "Failed to send OTP"
                }
            )
        }
    }

    fun resendEmailChangeOtp(context: Context) {
        val email = pendingNewEmail
        if (email == null) {
            _securityError.value = "Error: Email not found. Please cancel and try again."
            return
        }
        viewModelScope.launch {
            _emailFlowState.value = SecurityFlowState.LOADING
            _securityError.value = null

            val result = requestViuEmailChangeUseCase(context, viuUid, email)
            result.fold(
                onSuccess = { resendResult ->
                    _emailFlowState.value = SecurityFlowState.PENDING_OTP
                    when (resendResult) {
                        OtpResult.ResendOtpResult.Success -> {
                            _securityError.value = "New OTP sent"
                            startEmailResendTimer()
                        }
                        OtpResult.ResendOtpResult.FailureCooldown -> {
                            _securityError.value = "Max resends reached. Please wait 5 minutes."
                        }
                        OtpResult.ResendOtpResult.FailureGeneric -> {
                            _securityError.value = "Please wait 1 minute before resending."
                        }
                        OtpResult.ResendOtpResult.FailureEmailAlreadyInUse -> {
                            _securityError.value = "This email is already in use by another account."
                        }
                    }
                },
                onFailure = {
                    _emailFlowState.value = SecurityFlowState.PENDING_OTP
                    _securityError.value = it.message ?: "Failed to resend OTP"
                }
            )
        }
    }

    fun cancelEmailChangeFlow() {
        _emailFlowState.value = SecurityFlowState.IDLE
        _securityError.value = null
        pendingNewEmail = null
        stopEmailResendTimer()
        viewModelScope.launch {
            cancelViuEmailChangeUseCase(viuUid)
        }
    }

    fun verifyEmailChange(otp: String) {
        if (otp.length != 6) { _securityError.value = "OTP must be 6 digits"; return }
        viewModelScope.launch {
            _emailFlowState.value = SecurityFlowState.LOADING
            _securityError.value = null
            _emailChangeSuccess.value = false

            val result = verifyViuEmailChangeUseCase(viuUid, otp)
            result.fold(
                onSuccess = { verificationResult ->
                    when (verificationResult) {
                        OtpResult.OtpVerificationResult.Success -> {
                            _emailFlowState.value = SecurityFlowState.IDLE
                            _emailChangeSuccess.value = true
                            stopEmailResendTimer()
                        }
                        OtpResult.OtpVerificationResult.FailureInvalid -> {
                            _emailFlowState.value = SecurityFlowState.PENDING_OTP
                            _securityError.value = "Invalid OTP. Please try again."
                        }
                        OtpResult.OtpVerificationResult.FailureMaxAttempts -> {
                            _emailFlowState.value = SecurityFlowState.IDLE
                            _securityError.value = "Max attempts reached. Please wait 5 minutes."
                            stopEmailResendTimer()
                        }
                        OtpResult.OtpVerificationResult.FailureExpiredOrCooledDown -> {
                            _emailFlowState.value = SecurityFlowState.IDLE
                            _securityError.value = "OTP expired or on cooldown. Please try again."
                            stopEmailResendTimer()
                        }
                    }
                },
                onFailure = {
                    _emailFlowState.value = SecurityFlowState.PENDING_OTP
                    _securityError.value = it.message ?: "Verification failed"
                }
            )
        }
    }
}