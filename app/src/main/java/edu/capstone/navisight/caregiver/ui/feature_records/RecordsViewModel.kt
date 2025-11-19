package edu.capstone.navisight.caregiver.ui.feature_records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.capstone.navisight.caregiver.model.Viu
import edu.capstone.navisight.caregiver.domain.connectionUseCase.GetAllPairedViusUseCase
import edu.capstone.navisight.common.domain.usecase.GetCurrentUserUidUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RecordsViewModel(
    private val getAllPairedViusUseCase: GetAllPairedViusUseCase = GetAllPairedViusUseCase(),
    private val getCurrentUserUidUseCase: GetCurrentUserUidUseCase = GetCurrentUserUidUseCase()
) : ViewModel() {

    private val _vius = MutableStateFlow<List<Viu>>(emptyList())
    val vius: StateFlow<List<Viu>> = _vius

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        observeVius()
    }

    private fun observeVius() {
        viewModelScope.launch {
            _isLoading.value = true

            val currentUid = getCurrentUserUidUseCase()

            if (currentUid != null) {
                try {
                    getAllPairedViusUseCase(caregiverUid = currentUid).collectLatest { viuList ->
                        _vius.value = viuList
                        _isLoading.value = false
                    }
                } catch (e: Exception) {
                    _error.value = e.message ?: "Failed to load records"
                    _isLoading.value = false
                }
            } else {
                _error.value = "User is not logged in"
                _isLoading.value = false
            }
        }
    }
}