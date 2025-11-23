package edu.capstone.navisight.viu.data.repository

import edu.capstone.navisight.viu.data.model.UserLocation
import edu.capstone.navisight.viu.data.remote.LocationRemoteDataSource

class LocationRepositoryImpl(
    private val remote: LocationRemoteDataSource = LocationRemoteDataSource()
) {
    suspend fun updateUserLocation(location: UserLocation) {
        remote.updateUserLocation(location)
    }
}
