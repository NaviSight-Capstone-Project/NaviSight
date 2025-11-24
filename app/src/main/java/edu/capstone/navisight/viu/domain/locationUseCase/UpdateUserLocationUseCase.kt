package edu.capstone.navisight.viu.domain.locationUseCase

import edu.capstone.navisight.viu.data.repository.LocationRepository
import edu.capstone.navisight.viu.model.ViuLocation

class UpdateUserLocationUseCase(
    private val repository: LocationRepository = LocationRepository()
) {
    suspend fun startPresence() {
        repository.setupPresence()
    }

    suspend fun setOffline() {
        repository.setUserOffline()
    }

    suspend operator fun invoke(lat: Double, lon: Double) {
        val location = ViuLocation(lat, lon)
        repository.updateUserLocation(location)
    }




}