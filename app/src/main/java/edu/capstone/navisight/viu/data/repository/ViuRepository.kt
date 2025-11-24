package edu.capstone.navisight.viu.data.repository

import edu.capstone.navisight.viu.model.Viu
import edu.capstone.navisight.viu.data.remote.ViuDataSource

class ViuRepository(
    private val remoteDataSource: ViuDataSource = ViuDataSource()
) {
    suspend fun getCurrentViuProfile(): Viu {
        return remoteDataSource.getCurrentViuProfile()
    }
}
