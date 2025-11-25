package edu.capstone.navisight.viu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.capstone.navisight.viu.domain.locationUseCase.UpdateUserLocationUseCase
import kotlinx.coroutines.launch

class ViuHomeViewModel : ViewModel() {

    private val updateUserLocationUseCase = UpdateUserLocationUseCase()

    init {
        viewModelScope.launch {
            updateUserLocationUseCase.startPresence()
        }
    }

    fun updateLocation(lat: Double, lon: Double) {
        viewModelScope.launch {
            updateUserLocationUseCase(lat, lon)
        }
    }

    // New function called by Fragment when GPS is disabled
    fun setOffline() {
        viewModelScope.launch {
            updateUserLocationUseCase.setOffline()
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Ensure we mark offline when the ViewModel is destroyed
        setOffline()
    }
}