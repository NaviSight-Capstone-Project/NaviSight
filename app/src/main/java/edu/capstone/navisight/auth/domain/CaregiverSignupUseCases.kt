package edu.capstone.navisight.auth.domain

import android.content.Context
import android.net.Uri
import com.google.firebase.Timestamp
import edu.capstone.navisight.auth.data.repository.CaregiverSignupRepository
import edu.capstone.navisight.auth.model.Caregiver
import edu.capstone.navisight.auth.model.OtpResult

class SignupCaregiverUseCase {
    private val repository: CaregiverSignupRepository = CaregiverSignupRepository()

    suspend operator fun invoke(
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
        country: String
    ): Result<Caregiver> {
        return repository.signupCaregiver(
            context, email, password, firstName, lastName, middleName, phoneNumber, address, birthday, sex, imageUri
        )
    }
}

// Parameter-less constructor
class VerifySignupOtpUseCase {
    private val repository: CaregiverSignupRepository = CaregiverSignupRepository()

    suspend operator fun invoke(uid: String, enteredOtp: String): OtpResult.OtpVerificationResult {
        return repository.verifySignupOtp(uid, enteredOtp)
    }
}

// Parameter-less constructor
class ResendSignupOtpUseCase {
    private val repository: CaregiverSignupRepository = CaregiverSignupRepository()

    suspend operator fun invoke(context: Context, uid: String): OtpResult.ResendOtpResult {
        return repository.resendSignupOtp(context, uid)
    }
}

class DeleteUnverifiedUserUseCase {
    private val repository: CaregiverSignupRepository = CaregiverSignupRepository()

    suspend operator fun invoke(uid: String): Boolean {
        return repository.deleteUnverifiedUser(uid)
    }
}