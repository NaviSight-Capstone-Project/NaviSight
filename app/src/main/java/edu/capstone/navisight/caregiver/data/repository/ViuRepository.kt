package edu.capstone.navisight.caregiver.data.repository

import android.content.Context
import android.net.Uri
import edu.capstone.navisight.auth.model.OtpResult
import edu.capstone.navisight.caregiver.data.remote.ViuDataSource
import edu.capstone.navisight.caregiver.model.Viu
import kotlinx.coroutines.flow.Flow
class ViuRepository (
    private val viuDataSource: ViuDataSource = ViuDataSource()

){
    fun getViuByUid(uid: String) = viuDataSource.getViuByUid(uid)

    fun getViuDetails(viuUid: String): Flow<Viu?> {
        return viuDataSource.getViuDetails(viuUid)
    }

    fun getConnectedViuUids(): Flow<List<String>?> {
        return viuDataSource.getConnectedViuUids()
    }

    suspend fun updateViu(viu: Viu): Result<Unit> {
        return viuDataSource.updateViuDetails(viu)
    }

    suspend fun deleteViu(viuUid: String): Result<Unit> {
        return viuDataSource.deleteViuAndRelationship(viuUid)
    }

    suspend fun sendViuPasswordReset(viuEmail: String): Result<Unit> {
        return viuDataSource.sendViuPasswordReset(viuEmail)
    }

    suspend fun requestViuEmailChange(context: Context, viuUid: String, newViuEmail: String): Result<OtpResult.ResendOtpResult> {
        return viuDataSource.requestViuEmailChange(context, viuUid, newViuEmail)
    }

    suspend fun cancelViuEmailChange(viuUid: String) {
        viuDataSource.cancelViuEmailChange(viuUid)
    }

    suspend fun verifyViuEmailChange(viuUid: String, enteredOtp: String): Result<OtpResult.OtpVerificationResult> {
        return viuDataSource.verifyViuEmailChange(viuUid, enteredOtp)
    }

    suspend fun sendVerificationOtpToCaregiver(context: Context): Result<OtpResult.ResendOtpResult> {
        return viuDataSource.sendVerificationOtpToCaregiver(context)
    }

    suspend fun cancelViuProfileUpdate() {
        viuDataSource.cancelViuProfileUpdate()
    }

    suspend fun verifyCaregiverOtp(enteredOtp: String): Result<OtpResult.OtpVerificationResult> {
        return viuDataSource.verifyCaregiverOtp(enteredOtp)
    }

    suspend fun reauthenticateCaregiver(password: String): Result<Unit> {
        return viuDataSource.reauthenticateCaregiver(password)
    }

    suspend fun uploadViuProfileImage(viuUid: String, imageUri: Uri): Result<Unit> {
        return viuDataSource.uploadViuProfileImage(viuUid, imageUri)
    }
    suspend fun checkIfPrimaryCaregiver(viuUid: String): Result<Boolean> {
        return viuDataSource.checkIfPrimaryCaregiver(viuUid)
    }
}

