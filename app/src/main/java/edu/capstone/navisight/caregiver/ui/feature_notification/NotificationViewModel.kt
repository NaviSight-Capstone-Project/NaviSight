package edu.capstone.navisight.caregiver.ui.feature_notification

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.capstone.navisight.caregiver.domain.connectionUseCase.SecondaryConnectionUseCase
import edu.capstone.navisight.caregiver.model.RequestStatus
import edu.capstone.navisight.caregiver.model.SecondaryPairingRequest
import edu.capstone.navisight.common.domain.usecase.GetCurrentUserUidUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val secondaryConnectionUseCase: SecondaryConnectionUseCase = SecondaryConnectionUseCase(),
    private val getCurrentUidUseCase: GetCurrentUserUidUseCase = GetCurrentUserUidUseCase()
) : ViewModel() {

    private val _pendingRequests = MutableStateFlow<List<SecondaryPairingRequest>>(emptyList())
    val pendingRequests = _pendingRequests.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun loadPendingRequests() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val caregiverUid = getCurrentUidUseCase()
            if (caregiverUid == null) {
                _errorMessage.value = "User not logged in."
                _isLoading.value = false
                return@launch
            }


            try {
                secondaryConnectionUseCase.getPendingRequests(caregiverUid).collect { requests ->
                    _pendingRequests.value = requests
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load pending requests: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun approveRequest(request: SecondaryPairingRequest) {
        viewModelScope.launch {
            _isLoading.value = true

            val caregiverUid = getCurrentUidUseCase()
            if (caregiverUid == null) {
                _errorMessage.value = "User not logged in."
                _isLoading.value = false
                return@launch
            }

            when (val result = secondaryConnectionUseCase.approveRequest(request)) {
                is RequestStatus.Success -> {
                    _message.value = "Request approved successfully."
                }
                is RequestStatus.Error -> {
                    _errorMessage.value = "Failed to approve: ${result.message}"
                }
                else -> {}
            }

            _isLoading.value = false
        }
    }

    fun denyRequest(requestId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            when (val result = secondaryConnectionUseCase.denyRequest(requestId)) {
                is RequestStatus.Success -> {
                    _message.value = "Request denied."
                }
                is RequestStatus.Error -> {
                    _errorMessage.value = "Failed to deny: ${result.message}"
                }
                else -> {}
            }

            _isLoading.value = false
        }
    }

    fun clearMessages() {
        _message.value = null
        _errorMessage.value = null
    }
}
