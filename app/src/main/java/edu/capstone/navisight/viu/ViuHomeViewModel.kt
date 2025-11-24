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

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            updateUserLocationUseCase.setOffline()
        }
    }
}