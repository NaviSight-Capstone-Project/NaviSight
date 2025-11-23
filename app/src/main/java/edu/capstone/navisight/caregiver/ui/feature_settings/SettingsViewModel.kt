package edu.capstone.navisight.caregiver.ui.feature_settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.capstone.navisight.caregiver.domain.CaregiverProfileUseCase
import edu.capstone.navisight.caregiver.model.Caregiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val caregiverProfileUseCase: CaregiverProfileUseCase = CaregiverProfileUseCase()
) : ViewModel() {

    private val _profile = MutableStateFlow<Caregiver?>(null)
    val profile: StateFlow<Caregiver?> = _profile

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _success = MutableStateFlow<Boolean>(false)
    val success: StateFlow<Boolean> = _success

    fun loadProfile(uid: String) {
        viewModelScope.launch {
            try {
                _profile.value = caregiverProfileUseCase.getProfile(uid)
            } catch (e: Exception) {
                _error.value = "Failed to load profile"
            }
        }
    }

}