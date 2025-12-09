package edu.capstone.navisight.viu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.capstone.navisight.viu.domain.locationUseCase.MonitorGeofenceUseCase
import edu.capstone.navisight.viu.domain.locationUseCase.UpdateUserRealTimeDataUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ViuHomeViewModel : ViewModel() {

    private val updateUserRealTimeDataUseCase = UpdateUserRealTimeDataUseCase()
    private val monitorGeofenceUseCase = MonitorGeofenceUseCase()

    companion object {
        private var INSTANCE: ViuHomeViewModel? = null
        fun getInstance(): ViuHomeViewModel? = INSTANCE // Getter for the instance
    }

    init {
        INSTANCE = this
        viewModelScope.launch {
            updateUserRealTimeDataUseCase.startPresence()
            monitorGeofenceUseCase.startMonitoring()
        }
    }

    fun updateLocation(lat: Double, lon: Double) {
        viewModelScope.launch {
            updateUserRealTimeDataUseCase(lat, lon)

            monitorGeofenceUseCase(lat, lon)
        }
    }

    fun setOffline() {
        viewModelScope.launch {
            updateUserRealTimeDataUseCase.setOffline()
        }
    }

    fun setUserEmergencyActivated() {
        viewModelScope.launch {
            updateUserRealTimeDataUseCase.setUserEmergencyActivated()
        }
    }

    fun removeUserEmergencyActivated() {
        println("workinngggggggg")
        viewModelScope.launch {
            updateUserRealTimeDataUseCase.removeUserEmergencyActivated()
        }
    }

    fun setUserLowBatteryDetected() {
        viewModelScope.launch {
            updateUserRealTimeDataUseCase.setUserLowBatteryDetected()
        }
    }

    fun removeUserLowBatteryDetected() {
        viewModelScope.launch {
            updateUserRealTimeDataUseCase.removeUserLowBatteryDetected()
        }
    }

    // TODO
    suspend fun getUserEmergencyActivated(): Boolean? {
        return updateUserRealTimeDataUseCase.getUserEmergencyActivated()
    }
    suspend fun getUserLowBatteryDetected(): Boolean? {
        return updateUserRealTimeDataUseCase.getUserLowBatteryDetected()
    }

    override fun onCleared() {
        super.onCleared()
        setOffline()
        INSTANCE = null
    }
}