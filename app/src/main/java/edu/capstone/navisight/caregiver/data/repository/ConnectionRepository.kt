package edu.capstone.navisight.caregiver.data.repository

import edu.capstone.navisight.caregiver.data.remote.ConnectionDataSource
import edu.capstone.navisight.caregiver.model.Viu
import kotlinx.coroutines.flow.Flow

class ConnectionRepository(
    private val connectionDataSource: ConnectionDataSource = ConnectionDataSource()
) {

    fun getAllPairedVius(caregiverUid: String): Flow<List<Viu>> {
        return connectionDataSource.getAllPairedVius(caregiverUid)
    }
}