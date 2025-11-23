package edu.capstone.navisight.caregiver.ui.feature_scanqr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.capstone.navisight.caregiver.domain.connectionUseCase.GetQrCodeUseCase
import edu.capstone.navisight.caregiver.domain.connectionUseCase.SecondaryConnectionUseCase
import edu.capstone.navisight.caregiver.model.RequestStatus
import edu.capstone.navisight.common.domain.usecase.GetCurrentUserUidUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScanQrViewModel(
    private val getQrCodeUseCase: GetQrCodeUseCase = GetQrCodeUseCase(),
    private val secondaryConnectionUseCase: SecondaryConnectionUseCase = SecondaryConnectionUseCase(),
    private val getCurrentUserUidUseCase: GetCurrentUserUidUseCase = GetCurrentUserUidUseCase()
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage = _successMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private var isProcessing = false

    fun onQrCodeScanned(scannedContent: String) {
        if (isProcessing) return
        isProcessing = true
        _isLoading.value = true

        _errorMessage.value = null
        _successMessage.value = null

        val cleanQrUid = extractQrUid(scannedContent)

        viewModelScope.launch {
            try {
                val currentUserUid = getCurrentUserUidUseCase()
                if (currentUserUid == null) {
                    _errorMessage.value = "User not logged in."
                    stopLoading()
                    return@launch
                }

                val qrData = getQrCodeUseCase(cleanQrUid)

                if (qrData != null) {
                    val result = secondaryConnectionUseCase.sendRequest(
                        requesterUid = currentUserUid,
                        viuUid = qrData.viuUid,
                        viuName = qrData.name
                    )

                    when (result) {
                        is RequestStatus.Success -> {
                            _successMessage.value = "Pairing request sent successfully!"
                        }
                        is RequestStatus.AlreadySent -> {
                            _errorMessage.value = "You have already sent a request to ${qrData.name}. Please wait for approval."
                        }
                        is RequestStatus.Error -> {
                            _errorMessage.value = "Failed: ${result.message}"
                        }
                    }

                } else {
                    _errorMessage.value = "Invalid QR Code: Device not found."
                }
            } catch (e: Exception) {
                _errorMessage.value = "An error occurred: ${e.message}"
            } finally {
                stopLoading()
            }
        }
    }

    private fun stopLoading() {
        _isLoading.value = false
    }

    private fun extractQrUid(raw: String): String {
        return try {
            if (raw.contains("qruid:")) {
                val parts = raw.split("|")
                val idPart = parts.find { it.startsWith("qruid:") }
                idPart?.removePrefix("qruid:") ?: raw
            } else {
                raw
            }
        } catch (e: Exception) {
            raw
        }
    }

    fun resetState() {
        isProcessing = false
        _isLoading.value = false
        _successMessage.value = null
        _errorMessage.value = null
    }
}