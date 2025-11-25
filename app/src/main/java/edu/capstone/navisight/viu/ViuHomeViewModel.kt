package edu.capstone.navisight.viu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.capstone.navisight.viu.data.repository.LocationRepository
import edu.capstone.navisight.viu.domain.locationUseCase.UpdateUserLocationUseCase
//import edu.capstone.navisight.viu.domain.MonitorGeofenceUseCase
import kotlinx.coroutines.launch

class ViuHomeViewModel : ViewModel() {
    private val repository = LocationRepository()
    private val updateUserLocationUseCase = UpdateUserLocationUseCase(repository)
//    private val monitorGeofenceUseCase = MonitorGeofenceUseCase(repository)
    private val currentViuName = "  try"

    init {
        viewModelScope.launch {
            updateUserLocationUseCase.startPresence()
//            monitorGeofenceUseCase.startMonitoring()
        }
    }

    fun updateLocation(lat: Double, lon: Double) {
        viewModelScope.launch {
            updateUserLocationUseCase(lat, lon)
//            monitorGeofenceUseCase(lat, lon, currentViuName)
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