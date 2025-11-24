package edu.capstone.navisight.auth.domain

import android.content.Context
import android.net.Uri
import edu.capstone.navisight.auth.data.repository.ViuSignupRepository
import edu.capstone.navisight.auth.model.OtpResult
import edu.capstone.navisight.auth.model.Viu

class SignupViuUseCase {
    private val repository: ViuSignupRepository = ViuSignupRepository()

    suspend operator fun invoke(
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
        caregiverEmail: String
    ): Result<Pair<Viu, String>> {
        return repository.signupViu(
            context, email, password, firstName, lastName, middleName,
            phone, address, category, imageUri, caregiverEmail
        )
    }
}

class VerifyViuSignupOtpUseCase {
    private val repository: ViuSignupRepository = ViuSignupRepository()

    suspend operator fun invoke(
        caregiverUid: String,
        viuUid: String,
        enteredOtp: String
    ): OtpResult.OtpVerificationResult {
        return repository.verifySignupOtp(caregiverUid, viuUid, enteredOtp)
    }
}

class ResendViuSignupOtpUseCase {
    private val repository: ViuSignupRepository = ViuSignupRepository()

    suspend operator fun invoke(context: Context, caregiverUid: String): OtpResult.ResendOtpResult {
        return repository.resendSignupOtp(context, caregiverUid)
    }
}

class DeleteUnverifiedViuUserUseCase {
    private val repository: ViuSignupRepository = ViuSignupRepository()

    suspend operator fun invoke(viuUid: String): Boolean {
        return repository.deleteUnverifiedUser(viuUid)
    }
}