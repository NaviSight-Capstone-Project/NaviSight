package edu.capstone.navisight.caregiver.data.repository

import edu.capstone.navisight.caregiver.data.remote.ConnectionDataSource

class ConnectionRepository(

    private val connectionDataSource: ConnectionDataSource = ConnectionDataSource()
) {

    fun getAllPairedVius() = connectionDataSource.getAllPairedVius()
}