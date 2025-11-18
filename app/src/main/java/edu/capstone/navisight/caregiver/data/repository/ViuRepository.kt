package edu.capstone.navisight.caregiver.data.repository

import edu.capstone.navisight.caregiver.data.remote.ViuDataSource

class ViuRepository (
    private val viuDataSource: ViuDataSource = ViuDataSource()

){
    fun getViuByUid(uid: String) = viuDataSource.getViuByUid(uid)
}

