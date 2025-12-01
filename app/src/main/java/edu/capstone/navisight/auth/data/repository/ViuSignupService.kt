package edu.capstone.navisight.auth.data.repository

import android.content.Context
import android.net.Uri
import edu.capstone.navisight.auth.data.remote.ViuSignupDataSource
import edu.capstone.navisight.auth.model.OtpResult
import edu.capstone.navisight.auth.model.Viu

class ViuSignupRepository {

    private val remoteDataSource: ViuSignupDataSource = ViuSignupDataSource()

    suspend fun signupViu(
        context: Context,
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        middleName: String,
        phone: String,
        address: String,
        category: String,
        imageUri: Uri?,
        caregiverEmail: String,
        sex: String
    ): Result<Pair<Viu, String>> {
        return remoteDataSource.signupViu(
            context, email, password, firstName, lastName, middleName,
            phone, address, category, imageUri, caregiverEmail, sex
        )
    }

    suspend fun verifySignupOtp(
        caregiverUid: String,
        viuUid: String,
        enteredOtp: String
    ): OtpResult.OtpVerificationResult {
        return remoteDataSource.verifySignupOtp(caregiverUid, viuUid, enteredOtp)
    }

    suspend fun resendSignupOtp(context: Context, caregiverUid: String): OtpResult.ResendOtpResult {
        return remoteDataSource.resendSignupOtp(context, caregiverUid)
    }

    suspend fun deleteUnverifiedUser(viuUid: String): Boolean {
        return remoteDataSource.deleteUnverifiedUser(viuUid)
    }
}