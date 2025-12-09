package edu.capstone.navisight.viu.domain.locationUseCase

// Updated name to UpdateUserRealTimeDataUseCase (9/12/2025)

import edu.capstone.navisight.viu.data.repository.RealtimeRepository
import edu.capstone.navisight.viu.model.ViuLocation

class UpdateUserRealTimeDataUseCase(
    private val repository: RealtimeRepository = RealtimeRepository()
) {
    suspend fun startPresence() {
        repository.setupPresence()
    }

    suspend fun setOffline() {
        repository.setUserOffline()
    }

    suspend fun setUserEmergencyActivated() {
        repository.setUserEmergencyActivated()
    }

    suspend fun removeUserEmergencyActivated() {
        repository.removeUserEmergencyActivated()
    }

    suspend fun setUserLowBatteryDetected() {
        repository.setUserLowBatteryDetected()
    }

    suspend fun removeUserLowBatteryDetected() {
        repository.removeUserLowBatteryDetected()
    }

    suspend fun getUserLowBatteryDetected() : Boolean? {
        return repository.getUserLowBatteryDetected()
    }

    suspend fun getUserEmergencyActivated() : Boolean? {
        return repository.getUserEmergencyActivated()
    }

    suspend operator fun invoke(lat: Double, lon: Double) {
        val location = ViuLocation(lat, lon)
        repository.updateUserLocation(location)
    }




}