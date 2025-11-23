package edu.capstone.navisight.viu.domain.usecase

import edu.capstone.navisight.viu.data.model.UserLocation
import edu.capstone.navisight.viu.data.repository.LocationRepositoryImpl


class UpdateUserLocationUseCase(
    private val repository: LocationRepositoryImpl = LocationRepositoryImpl()
) {
    suspend operator fun invoke(lat: Double, lon: Double) {
        val location = UserLocation(lat, lon)
        repository.updateUserLocation(location)
    }
}
