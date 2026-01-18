package edu.capstone.navisight.auth.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.Timestamp
import edu.capstone.navisight.auth.data.remote.CaregiverSignupDataSource
import edu.capstone.navisight.auth.model.Caregiver
import edu.capstone.navisight.auth.model.OtpResult

class CaregiverSignupRepository {

    private val remoteDataSource: CaregiverSignupDataSource = CaregiverSignupDataSource()

    suspend fun signupCaregiver(
        context: Context,
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        middleName: String,
        phoneNumber: String,
        address: String,
        birthday: Timestamp,
        sex: String,
        imageUri: Uri?,
        country: String,
        province: String,
        city: String
    ): Result<Caregiver> {
        return remoteDataSource.signupCaregiver(
            context, email, password, firstName, lastName, middleName, phoneNumber, address,
            birthday, sex, imageUri, country, province, city
        )
    }

    suspend fun verifySignupOtp(uid: String, enteredOtp: String): OtpResult.OtpVerificationResult {
        return remoteDataSource.verifySignupOtp(uid, enteredOtp)
    }

    suspend fun resendSignupOtp(context: Context, uid: String): OtpResult.ResendOtpResult {
        return remoteDataSource.resendSignupOtp(context, uid)
    }

    suspend fun deleteUnverifiedUser(uid: String): Boolean {
        return remoteDataSource.deleteUnverifiedUser(uid)
    }
}