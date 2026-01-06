package edu.capstone.navisight.caregiver.ui.feature_notification


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.capstone.navisight.caregiver.data.repository.NotificationRepository
import edu.capstone.navisight.caregiver.domain.connectionUseCase.SecondaryConnectionUseCase
import edu.capstone.navisight.caregiver.domain.connectionUseCase.TransferPrimaryUseCase
import edu.capstone.navisight.caregiver.domain.notificationUseCase.DismissActivityUseCase
import edu.capstone.navisight.caregiver.domain.notificationUseCase.GetActivityFeedUseCase
import edu.capstone.navisight.caregiver.model.AlertNotification
import edu.capstone.navisight.caregiver.model.RequestStatus
import edu.capstone.navisight.caregiver.model.SecondaryPairingRequest
import edu.capstone.navisight.caregiver.model.TransferPrimaryRequest
import edu.capstone.navisight.common.domain.usecase.GetCurrentUserUidUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import edu.capstone.navisight.caregiver.model.GeofenceActivity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class NotificationViewModel(
    private val secondaryConnectionUseCase: SecondaryConnectionUseCase = SecondaryConnectionUseCase(),
    private val transferPrimaryUseCase: TransferPrimaryUseCase = TransferPrimaryUseCase(),
    private val getCurrentUidUseCase: GetCurrentUserUidUseCase = GetCurrentUserUidUseCase(),
    private val getActivityFeedUseCase: GetActivityFeedUseCase = GetActivityFeedUseCase(),
    private val dismissActivityUseCase: DismissActivityUseCase = DismissActivityUseCase(),
    private val notificationRepository: NotificationRepository = NotificationRepository()
) : ViewModel() {

    // Geofence activities
    private val _activities = MutableStateFlow<List<GeofenceActivity>>(emptyList())
    val activities = _activities.asStateFlow()

    // Alerts and general
    val unreadAlerts = notificationRepository.getUnreadAlerts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Requests (pending)
    private val _pendingRequests = MutableStateFlow<List<SecondaryPairingRequest>>(emptyList())
    val pendingRequests = _pendingRequests.asStateFlow()

    // Requests (transfers)
    private val _transferRequests = MutableStateFlow<List<TransferPrimaryRequest>>(emptyList())
    val transferRequests = _transferRequests.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _combinedFeed = MutableStateFlow<List<Any>>(emptyList())
    val combinedFeed = _combinedFeed.asStateFlow()

    init {
        viewModelScope.launch {
            // Collect Geofence activities
            launch {
                getActivityFeedUseCase().collect { feedList ->
                    _activities.value = feedList
                    updateCombinedFeed()
                }
            }
            launch {
                unreadAlerts.collect {
                    updateCombinedFeed()
                }
            }
        }
    }

    private fun updateCombinedFeed() {
        val allItems = (_activities.value + unreadAlerts.value).sortedByDescending { item ->
            when (item) {
                is GeofenceActivity -> item.timestamp?.toDate()?.time ?: 0L
                is AlertNotification -> item.timestamp?.time ?: 0L
                else -> 0L
            }
        }
        _combinedFeed.value = allItems
    }

    fun dismissAlert(alertId: String) {
        viewModelScope.launch {
            notificationRepository.dismissAlert(alertId)
        }
    }

    fun deleteFeedItem(itemId: String, isAlert: Boolean, isRead: Boolean = false) {
        viewModelScope.launch {
            if (isAlert) {
                dismissAlert(itemId)
            } else {
                // If it's a GeofenceActivity, call the use case
                dismissActivityUseCase(itemId)
            }
        }
    }

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
                launch {
                    secondaryConnectionUseCase.getPendingRequests(caregiverUid).collect { requests ->
                        _pendingRequests.value = requests
                    }
                }
                launch {
                    transferPrimaryUseCase.getIncomingRequests(caregiverUid).collect { requests ->
                        _transferRequests.value = requests
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load requests: ${e.message}"
            } finally {
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

    fun approveTransfer(request: TransferPrimaryRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = transferPrimaryUseCase.approveRequest(request)) {
                is RequestStatus.Success -> {
                    _message.value = "You are now the Primary Companion."
                }
                is RequestStatus.Error -> {
                    _errorMessage.value = result.message
                }
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun denyTransfer(requestId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = transferPrimaryUseCase.denyRequest(requestId)) {
                is RequestStatus.Success -> {
                    _message.value = "Transfer request denied."
                }
                is RequestStatus.Error -> {
                    _errorMessage.value = result.message
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