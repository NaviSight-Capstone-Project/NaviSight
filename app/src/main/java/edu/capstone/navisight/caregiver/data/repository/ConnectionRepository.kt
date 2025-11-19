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

    fun isPrimaryCaregiver(caregiverUid: String, viuUid: String): Flow<Boolean> {
        return connectionDataSource.isPrimaryCaregiver(caregiverUid, viuUid)
    }
}