package edu.capstone.navisight.caregiver.domain.viuUseCase

import android.content.Context
import android.net.Uri
import edu.capstone.navisight.auth.model.OtpResult.OtpVerificationResult
import edu.capstone.navisight.auth.model.OtpResult.ResendOtpResult
import edu.capstone.navisight.caregiver.data.repository.ViuRepository
import edu.capstone.navisight.caregiver.model.Viu
import kotlinx.coroutines.flow.Flow

// Single instance for repository
private val repository: ViuRepository = ViuRepository()

class GetViuDetailsUseCase(private val repo: ViuRepository = repository) {
    operator fun invoke(viuUid: String): Flow<Viu?> = repo.getViuDetails(viuUid)
}

class UpdateViuUseCase(private val repo: ViuRepository = repository) {
    suspend operator fun invoke(viu: Viu): Result<Unit> = repo.updateViu(viu)
}

class DeleteViuUseCase(private val repo: ViuRepository = repository) {
    suspend operator fun invoke(viuUid: String): Result<Unit> = repo.deleteViu(viuUid)
}

class SendViuPasswordResetUseCase(private val repo: ViuRepository = repository) {
    suspend operator fun invoke(viuEmail: String): Result<Unit> = repo.sendViuPasswordReset(viuEmail)
}

class RequestViuEmailChangeUseCase(private val repo: ViuRepository = repository) {
    suspend operator fun invoke(context: Context, viuUid: String, newViuEmail: String): Result<ResendOtpResult> = repo.requestViuEmailChange(context, viuUid, newViuEmail)
}

class CancelViuEmailChangeUseCase(private val repo: ViuRepository = repository) {
    suspend operator fun invoke(viuUid: String) = repo.cancelViuEmailChange(viuUid)
}

class VerifyViuEmailChangeUseCase(private val repo: ViuRepository = repository) {
    suspend operator fun invoke(viuUid: String, enteredOtp: String): Result<OtpVerificationResult> = repo.verifyViuEmailChange(viuUid, enteredOtp)
}

class SendVerificationOtpToCaregiverUseCase(private val repo: ViuRepository = repository) {
    suspend operator fun invoke(context: Context): Result<ResendOtpResult> = repo.sendVerificationOtpToCaregiver(context)
}

class CancelViuProfileUpdateUseCase(private val repo: ViuRepository = repository) {
    suspend operator fun invoke() = repo.cancelViuProfileUpdate()
}

class VerifyCaregiverOtpUseCase(private val repo: ViuRepository = repository) {
    suspend operator fun invoke(enteredOtp: String): Result<OtpVerificationResult> = repo.verifyCaregiverOtp(enteredOtp)
}

class ReauthenticateCaregiverUseCase(private val repo: ViuRepository = repository) {
    suspend operator fun invoke(password: String): Result<Unit> = repo.reauthenticateCaregiver(password)
}

class UploadViuProfileImageUseCase(private val repo: ViuRepository = repository) {
    suspend operator fun invoke(viuUid: String, imageUri: Uri): Result<Unit> = repo.uploadViuProfileImage(viuUid, imageUri)
}
class CheckEditPermissionUseCase(private val repo: ViuRepository = repository) {
    suspend operator fun invoke(viuUid: String): Result<Boolean> = repo.checkIfPrimaryCaregiver(viuUid)
}