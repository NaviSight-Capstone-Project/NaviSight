package edu.capstone.navisight.caregiver.ui.feature_edit_viu_profile

import android.content.Context
import android.net.Uri
import android.util.Patterns
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.capstone.navisight.auth.model.OtpResult
import edu.capstone.navisight.caregiver.domain.connectionUseCase.TransferPrimaryUseCase
import edu.capstone.navisight.caregiver.domain.connectionUseCase.UnpairViuUseCase
import edu.capstone.navisight.caregiver.model.Viu
import edu.capstone.navisight.caregiver.domain.viuUseCase.*
import edu.capstone.navisight.caregiver.model.RequestStatus
import edu.capstone.navisight.caregiver.model.TransferPrimaryRequest
import edu.capstone.navisight.common.domain.usecase.GetCurrentUserUidUseCase
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

// Enums for UI State management
enum class SecurityFlowState { IDLE, PENDING_OTP, LOADING }

enum class SaveFlowState {
    IDLE,
    PENDING_PASSWORD,
    PENDING_OTP,
    SAVING
}

enum class TransferFlowState {
    IDLE,
    SELECTING_CANDIDATE, // Showing the list
    CONFIRMING_PASSWORD, // Showing the password dialog
    SENDING              // Network call
}
enum class DeleteFlowState { IDLE, PENDING_PASSWORD, DELETING }

enum class UnpairFlowState { IDLE, CONFIRMING_PASSWORD, UNPAIRING }

class EditViuProfileViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Retrieved from Navigation Arguments
    private val viuUid: String = checkNotNull(savedStateHandle["viuUid"])

    // UseCases
    private val checkEditPermissionUseCase = CheckEditPermissionUseCase()
    private val getViuDetailsUseCase = GetViuDetailsUseCase()
    private val deleteViuUseCase = DeleteViuUseCase()
    private val updateViuUseCase = UpdateViuUseCase()
    private val sendViuPasswordResetUseCase = SendViuPasswordResetUseCase()
    private val requestViuEmailChangeUseCase = RequestViuEmailChangeUseCase()
    private val verifyViuEmailChangeUseCase = VerifyViuEmailChangeUseCase()
    private val cancelViuEmailChangeUseCase = CancelViuEmailChangeUseCase()
    private val reauthenticateCaregiverUseCase = ReauthenticateCaregiverUseCase()
    private val sendVerificationOtpToCaregiverUseCase = SendVerificationOtpToCaregiverUseCase()
    private val verifyCaregiverOtpUseCase = VerifyCaregiverOtpUseCase()
    private val cancelViuProfileUpdateUseCase = CancelViuProfileUpdateUseCase()
    private val uploadViuProfileImageUseCase = UploadViuProfileImageUseCase()
    private val transferPrimaryUseCase = TransferPrimaryUseCase()
    private val getCurrentUserUidUseCase = GetCurrentUserUidUseCase()
    private val unpairViuUseCase = UnpairViuUseCase()

    // State Flows
    private val _viu = MutableStateFlow<Viu?>(null)
    val viu: StateFlow<Viu?> = _viu.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private val _transferSuccess = MutableStateFlow(false)
    val transferSuccess: StateFlow<Boolean> = _transferSuccess.asStateFlow()

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

    private val _canEdit = MutableStateFlow(false)
    val canEdit: StateFlow<Boolean> = _canEdit.asStateFlow()

    private val _transferCandidates = MutableStateFlow<List<TransferPrimaryRequest>>(emptyList())
    val transferCandidates: StateFlow<List<TransferPrimaryRequest>> = _transferCandidates.asStateFlow()

    private val _transferFlowState = MutableStateFlow(TransferFlowState.IDLE)
    val transferFlowState: StateFlow<TransferFlowState> = _transferFlowState.asStateFlow()

    private val _transferError = MutableStateFlow<String?>(null)
    val transferError: StateFlow<String?> = _transferError.asStateFlow()

    private val _passwordRetryCount = MutableStateFlow(0)
    val passwordRetryCount: StateFlow<Int> = _passwordRetryCount.asStateFlow()

    private var pendingCandidate: TransferPrimaryRequest? = null
    private val _unpairFlowState = MutableStateFlow(UnpairFlowState.IDLE)
    val unpairFlowState: StateFlow<UnpairFlowState> = _unpairFlowState.asStateFlow()

    private val _unpairError = MutableStateFlow<String?>(null)
    val unpairError: StateFlow<String?> = _unpairError.asStateFlow()

    private val _unpairSuccess = MutableStateFlow(false)
    val unpairSuccess: StateFlow<Boolean> = _unpairSuccess.asStateFlow()

    // Internal State
    private val dateFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    private var pendingViuUpdate: Viu? = null
    private var pendingNewEmail: String? = null

    // Timers
    private var saveResendTimerJob: Job? = null
    private val _saveResendTimer = MutableStateFlow(0)
    val saveResendTimer: StateFlow<Int> = _saveResendTimer.asStateFlow()

    private var emailResendTimerJob: Job? = null
    private val _emailResendTimer = MutableStateFlow(0)
    val emailResendTimer: StateFlow<Int> = _emailResendTimer.asStateFlow()

    init {
        loadViuDetails()
        checkPermissions()
    }

    private fun checkPermissions() {
        viewModelScope.launch {
            val result = checkEditPermissionUseCase(viuUid)
            result.fold(
                onSuccess = { isPrimary -> _canEdit.value = isPrimary },
                onFailure = { _canEdit.value = false }
            )
        }
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

    // Image Upload
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
                onSuccess = { _isUploadingImage.value = false },
                onFailure = {
                    _isUploadingImage.value = false
                    _error.value = it.message ?: "Failed to upload image"
                }
            )
        }
    }

    // Timer Logic
    private fun startSaveResendTimer() {
        saveResendTimerJob?.cancel()
        saveResendTimerJob = viewModelScope.launch {
            val duration = 60
            _saveResendTimer.value = duration
            (duration - 1 downTo 0).asFlow().onEach { delay(1000) }.collect { _saveResendTimer.value = it }
        }
    }

    private fun stopSaveResendTimer() {
        saveResendTimerJob?.cancel()
        _saveResendTimer.value = 0
    }

    private fun startEmailResendTimer() {
        emailResendTimerJob?.cancel()
        emailResendTimerJob = viewModelScope.launch {
            val duration = 60
            _emailResendTimer.value = duration
            (duration - 1 downTo 0).asFlow().onEach { delay(1000) }.collect { _emailResendTimer.value = it }
        }
    }

    private fun stopEmailResendTimer() {
        emailResendTimerJob?.cancel()
        _emailResendTimer.value = 0
    }

    // Validations
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
            if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) age--
            if (age !in 18..60) return "Age must be between 18 and 60."
        } catch (e: Exception) {
            return "Invalid birthday format."
        }
        return null
    }

    // Save Flow (Edit Profile)
    fun startSaveFlow(
        firstName: String, middleName: String, lastName: String,
        birthday: String, sex: String, phone: String, address: String, status: String
    ) {
        if (!_canEdit.value) {
            _saveError.value = "Only the Primary Caregiver can edit this profile."
            return
        }
        _saveError.value = null

        val firstNameError = validateName(firstName, "First Name")
        if (firstNameError != null) { _saveError.value = firstNameError; return }

        val lastNameError = validateName(lastName, "Last Name")
        if (lastNameError != null) { _saveError.value = lastNameError; return }

        val birthdayError = validateBirthday(birthday)
        if (birthdayError != null) { _saveError.value = birthdayError; return }

        if (sex.isBlank()) { _saveError.value = "Sex cannot be empty"; return }

        val phoneError = validatePhone(phone)
        if (phoneError != null) { _saveError.value = phoneError; return }

        val currentViu = _viu.value ?: return
        val hasChanges = firstName != currentViu.firstName || middleName != currentViu.middleName ||
                lastName != currentViu.lastName || birthday != (currentViu.birthday ?: "") ||
                sex != (currentViu.sex ?: "") || phone != currentViu.phone ||
                address != (currentViu.address ?: "") || status != (currentViu.category ?: "")

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
            category = status.trim().takeIf { it.isNotEmpty() }
        )

        _saveFlowState.value = SaveFlowState.PENDING_PASSWORD
    }

    fun reauthenticateAndSendOtp(password: String, context: Context) {
        viewModelScope.launch {
            _saveFlowState.value = SaveFlowState.SAVING
            _saveError.value = null

            reauthenticateCaregiverUseCase(password).fold(
                onSuccess = {
                    sendVerificationOtpToCaregiverUseCase(context).fold(
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
            sendVerificationOtpToCaregiverUseCase(context).fold(
                onSuccess = { resendResult ->
                    _saveFlowState.value = SaveFlowState.PENDING_OTP
                    when (resendResult) {
                        OtpResult.ResendOtpResult.Success -> {
                            _saveError.value = "New OTP sent"
                            startSaveResendTimer()
                        }
                        OtpResult.ResendOtpResult.FailureCooldown -> _saveError.value = "Max resends reached. Please wait 5 minutes."
                        OtpResult.ResendOtpResult.FailureGeneric -> _saveError.value = "Please wait 1 minute before resending."
                        OtpResult.ResendOtpResult.FailureEmailAlreadyInUse -> _saveError.value = "An unexpected error occurred."
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

            verifyCaregiverOtpUseCase(otp).fold(
                onSuccess = { verificationResult ->
                    when (verificationResult) {
                        OtpResult.OtpVerificationResult.Success -> {
                            updateViuUseCase(viuToSave).fold(
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
                        else -> {
                            _saveFlowState.value = SaveFlowState.IDLE
                            _saveError.value = "OTP Expired or limit reached."
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
        viewModelScope.launch { cancelViuProfileUpdateUseCase() }
    }

    // Delete Flow
    fun startDeleteFlow() {
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

            reauthenticateCaregiverUseCase(password).fold(
                onSuccess = {
                    deleteViuUseCase(viuUid).fold(
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
        _deleteFlowState.value = DeleteFlowState.IDLE
        _deleteError.value = null
    }

    // Security / Email Change Flow
    fun sendPasswordReset() {
        val viuEmail = _viu.value?.email ?: return
        viewModelScope.launch {
            _emailFlowState.value = SecurityFlowState.LOADING
            _securityError.value = null
            _passwordResetSuccess.value = false
            sendViuPasswordResetUseCase(viuEmail).fold(
                onSuccess = {
                    _emailFlowState.value = SecurityFlowState.IDLE
                    _passwordResetSuccess.value = true
                },
                onFailure = {
                    _emailFlowState.value = SecurityFlowState.IDLE
                    _securityError.value = it.message ?: "Failed to send reset email"
                }
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
            requestViuEmailChangeUseCase(context, viuUid, newEmail).fold(
                onSuccess = { resendResult ->
                    when (resendResult) {
                        OtpResult.ResendOtpResult.Success -> {
                            _emailFlowState.value = SecurityFlowState.PENDING_OTP
                            startEmailResendTimer()
                        }
                        OtpResult.ResendOtpResult.FailureCooldown -> {
                            _emailFlowState.value = SecurityFlowState.IDLE
                            _securityError.value = "Max resends reached. Wait 5 minutes."
                        }
                        else -> {
                            _emailFlowState.value = SecurityFlowState.IDLE
                            _securityError.value = "Failed to send OTP."
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
        val email = pendingNewEmail ?: run { _securityError.value = "Error: Email missing"; return }
        viewModelScope.launch {
            _emailFlowState.value = SecurityFlowState.LOADING
            requestViuEmailChangeUseCase(context, viuUid, email).fold(
                onSuccess = { resendResult ->
                    _emailFlowState.value = SecurityFlowState.PENDING_OTP
                    when(resendResult) {
                        OtpResult.ResendOtpResult.Success -> {
                            _securityError.value = "New OTP sent"
                            startEmailResendTimer()
                        }
                        else -> _securityError.value = "Wait before resending."
                    }
                },
                onFailure = {
                    _emailFlowState.value = SecurityFlowState.PENDING_OTP
                    _securityError.value = it.message
                }
            )
        }
    }

    fun verifyEmailChange(otp: String) {
        if (otp.length != 6) { _securityError.value = "OTP must be 6 digits"; return }
        viewModelScope.launch {
            _emailFlowState.value = SecurityFlowState.LOADING
            _securityError.value = null
            _emailChangeSuccess.value = false

            verifyViuEmailChangeUseCase(viuUid, otp).fold(
                onSuccess = { result ->
                    if (result == OtpResult.OtpVerificationResult.Success) {
                        _emailFlowState.value = SecurityFlowState.IDLE
                        _emailChangeSuccess.value = true
                        stopEmailResendTimer()
                    } else {
                        _emailFlowState.value = SecurityFlowState.PENDING_OTP
                        _securityError.value = "Invalid OTP or Expired."
                    }
                },
                onFailure = {
                    _emailFlowState.value = SecurityFlowState.PENDING_OTP
                    _securityError.value = it.message ?: "Verification failed"
                }
            )
        }
    }

    fun cancelEmailChangeFlow() {
        _emailFlowState.value = SecurityFlowState.IDLE
        _securityError.value = null
        pendingNewEmail = null
        stopEmailResendTimer()
        viewModelScope.launch { cancelViuEmailChangeUseCase(viuUid) }
    }

    fun startTransferFlow() {
        val currentUid = getCurrentUserUidUseCase() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val candidates = transferPrimaryUseCase.getCandidates(viuUid, currentUid)
            _transferCandidates.value = candidates
            _isLoading.value = false
            _transferFlowState.value = TransferFlowState.SELECTING_CANDIDATE
        }
    }

    // Candidate Selected: Move to Password Confirmation
    fun onCandidateSelected(candidate: TransferPrimaryRequest) {
        pendingCandidate = candidate
        _passwordRetryCount.value = 0
        _transferError.value = null
        _transferFlowState.value = TransferFlowState.CONFIRMING_PASSWORD
    }

    // Confirm Password & Send
    fun confirmTransferPassword(password: String) {
        if (password.isBlank()) {
            _transferError.value = "Password cannot be empty"
            return
        }

        if (_passwordRetryCount.value >= 5) {
            _transferError.value = "Too many attempts. Please try again later."
            return
        }

        viewModelScope.launch {
            _transferFlowState.value = TransferFlowState.SENDING // Show loading in dialog

            reauthenticateCaregiverUseCase(password).fold(
                onSuccess = {
                    // Send Request
                    performTransferRequest()
                },
                onFailure = {
                    // Password Wrong
                    _passwordRetryCount.value += 1
                    val remaining = 5 - _passwordRetryCount.value
                    _transferFlowState.value = TransferFlowState.CONFIRMING_PASSWORD // Go back to input

                    if (remaining <= 0) {
                        _transferError.value = "Too many failed attempts. Action cancelled."
                        cancelTransferFlow()
                    } else {
                        _transferError.value = "Incorrect password. $remaining attempts left."
                    }
                }
            )
        }
    }

    private suspend fun performTransferRequest() {
        val candidate = pendingCandidate ?: return
        val currentUid = getCurrentUserUidUseCase() ?: return
        val currentViu = _viu.value ?: return

        // Fetch name
        val actualName = transferPrimaryUseCase.getCurrentCaregiverName(currentUid)

        val request = candidate.copy(
            createdAt = com.google.firebase.Timestamp.now(),
            currentPrimaryCaregiverUid = currentUid,
            currentPrimaryCaregiverName = actualName,
            viuUid = currentViu.uid,
            viuName = "${currentViu.firstName} ${currentViu.lastName}",
            status = "pending"
        )

        val result = transferPrimaryUseCase.sendRequest(request)

        if (result is RequestStatus.Error) {
            _transferError.value = result.message
            _transferFlowState.value = TransferFlowState.CONFIRMING_PASSWORD // Stay on dialog to show error
        } else {
            _transferSuccess.value = true
            cancelTransferFlow() // Reset states
        }
    }
    fun onTransferSuccessShown() { _transferSuccess.value = false }

    // Cancel / Close Dialogs
    fun cancelTransferFlow() {
        _transferFlowState.value = TransferFlowState.IDLE
        pendingCandidate = null
        _passwordRetryCount.value = 0
        _transferError.value = null
    }

    fun loadTransferCandidates() {
        val currentUid = getCurrentUserUidUseCase() ?: return

        viewModelScope.launch {
            _isLoading.value = true
            // Call UseCase
            val candidates = transferPrimaryUseCase.getCandidates(viuUid, currentUid)
            _transferCandidates.value = candidates
            _isLoading.value = false
        }
    }

    fun sendTransferRequest(candidate: TransferPrimaryRequest) {
        val currentUid = getCurrentUserUidUseCase() ?: return
        val currentViu = _viu.value ?: return

        viewModelScope.launch {
            _isLoading.value = true

            val caregiverName = transferPrimaryUseCase.getCurrentCaregiverName(currentUid)

            val request = candidate.copy(
                createdAt = com.google.firebase.Timestamp.now(),
                currentPrimaryCaregiverUid = currentUid,
                currentPrimaryCaregiverName = caregiverName,
                viuUid = currentViu.uid,
                viuName = "${currentViu.firstName} ${currentViu.lastName}",
                status = "pending"
            )

            val result = transferPrimaryUseCase.sendRequest(request)

            if (result is RequestStatus.Error) {
                _error.value = result.message
            } else {
                _saveSuccess.value = true
            }
            _isLoading.value = false
        }
    }

    fun startUnpairFlow() {
        if (_canEdit.value) { // Safety check: Primary cannot do this
            _unpairError.value = "Primary caregivers cannot unpair. Use Transfer instead."
            return
        }
        _passwordRetryCount.value = 0
        _unpairError.value = null
        _unpairFlowState.value = UnpairFlowState.CONFIRMING_PASSWORD
    }

    fun confirmUnpairPassword(password: String) {
        if (password.isBlank()) {
            _unpairError.value = "Password cannot be empty"
            return
        }
        if (_passwordRetryCount.value >= 5) {
            _unpairError.value = "Too many attempts. Action cancelled."
            return
        }

        viewModelScope.launch {
            _unpairFlowState.value = UnpairFlowState.UNPAIRING // Loading state

            reauthenticateCaregiverUseCase(password).fold(
                onSuccess = {
                    performUnpair()
                },
                onFailure = {
                    _passwordRetryCount.value += 1
                    val remaining = 5 - _passwordRetryCount.value

                    if (remaining <= 0) {
                        _unpairError.value = "Too many failed attempts. Action cancelled."
                        cancelUnpairFlow()
                    } else {
                        _unpairFlowState.value = UnpairFlowState.CONFIRMING_PASSWORD // Go back to dialog
                        _unpairError.value = "Incorrect password. $remaining attempts left."
                    }
                }
            )
        }
    }

    private suspend fun performUnpair() {
        val currentUid = getCurrentUserUidUseCase() ?: return

        val result = unpairViuUseCase.invoke(currentUid, viuUid)

        if (result is RequestStatus.Error) {
            _unpairError.value = result.message
            _unpairFlowState.value = UnpairFlowState.CONFIRMING_PASSWORD
        } else {
            _unpairSuccess.value = true
            _unpairFlowState.value = UnpairFlowState.IDLE
        }
    }

    fun cancelUnpairFlow() {
        _unpairFlowState.value = UnpairFlowState.IDLE
        _unpairError.value = null
        _passwordRetryCount.value = 0
    }

    // Cleanup/Reset helpers
    fun clearSaveError() { _saveError.value = null }
    fun clearDeleteError() { _deleteError.value = null }
    fun clearSecurityError() { _securityError.value = null }
    fun onSaveSuccessShown() { _saveSuccess.value = false }
    fun onPasswordResetSuccessShown() { _passwordResetSuccess.value = false }
    fun onEmailChangeSuccessShown() { _emailChangeSuccess.value = false }
}