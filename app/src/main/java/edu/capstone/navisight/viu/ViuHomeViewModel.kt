package edu.capstone.navisight.viu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.capstone.navisight.viu.domain.locationUseCase.MonitorGeofenceUseCase
import edu.capstone.navisight.viu.domain.locationUseCase.UpdateUserLocationUseCase
import kotlinx.coroutines.launch

class ViuHomeViewModel : ViewModel() {

    private val updateUserLocationUseCase = UpdateUserLocationUseCase()
    private val monitorGeofenceUseCase = MonitorGeofenceUseCase()

    init {
        viewModelScope.launch {
            updateUserLocationUseCase.startPresence()
            monitorGeofenceUseCase.startMonitoring()
        }
    }

    fun updateLocation(lat: Double, lon: Double) {
        viewModelScope.launch {
            updateUserLocationUseCase(lat, lon)

            monitorGeofenceUseCase(lat, lon)
        }
    }

    fun setOffline() {
        viewModelScope.launch {
            updateUserLocationUseCase.setOffline()
        }
    }

    override fun onCleared() {
        super.onCleared()
        setOffline()
    }
}