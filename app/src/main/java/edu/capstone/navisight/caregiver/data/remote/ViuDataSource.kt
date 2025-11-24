package edu.capstone.navisight.caregiver.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import edu.capstone.navisight.auth.data.remote.CloudinaryDataSource
import edu.capstone.navisight.auth.data.remote.OtpDataSource
import edu.capstone.navisight.auth.model.OtpResult.OtpVerificationResult
import edu.capstone.navisight.auth.model.OtpResult.ResendOtpResult
import edu.capstone.navisight.caregiver.model.Viu
import edu.capstone.navisight.caregiver.model.ViuLocation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await

class ViuDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val rtdb: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance() // Initialized
) {

    private val TAG = "ViuDataSource"
    private val otpDataSource = OtpDataSource() // Initialized

    companion object {
        private const val VIUS_COLLECTION = "vius"
        private const val VIU_LOCATION_REF = "viu_location"
        private const val VIU_LOCATION_FIELD = "location"
    }

    private val viusCollection = firestore.collection("vius")
    private val relationshipsCollection = firestore.collection("relationships")

    fun getViuDetails(viuUid: String): Flow<Viu?> = callbackFlow {
        val viuRef = viusCollection.document(viuUid)
        val listener = viuRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                close(error)
                return@addSnapshotListener
            }

            if (snapshot == null || !snapshot.exists()) {
                trySend(null)
                return@addSnapshotListener
            }
            // Ensure we handle potential nulls safely
            val viu = snapshot.toObject(Viu::class.java)?.copy(uid = snapshot.id)
            trySend(viu)
        }
        awaitClose { listener.remove() }
    }


    fun getViuByUid(uid: String): Flow<Viu?> {
        val staticDataFlow = getViuStaticDataFirestore(uid)
        val locationDataFlow = getViuLocationRtdb(uid)

        return staticDataFlow.combine(locationDataFlow) { viu, location ->
            val updatedViu = viu?.copy(location = location)
            updatedViu
        }
    }

    private fun getViuStaticDataFirestore(uid: String): Flow<Viu?> = callbackFlow {
        val firestoreRef = firestore.collection(VIUS_COLLECTION).document(uid)

        val firestoreListener = firestoreRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                trySend(null)
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot.toObject(Viu::class.java))
        }

        awaitClose { firestoreListener.remove() }
    }

    private fun getViuLocationRtdb(uid: String): Flow<ViuLocation?> = callbackFlow {
        val rtdbRef = rtdb.getReference(VIU_LOCATION_REF).child(uid).child(VIU_LOCATION_FIELD)
        val rtdbListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val location = snapshot.getValue(ViuLocation::class.java)
                trySend(location)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
                close(error.toException())
            }
        }
        rtdbRef.addValueEventListener(rtdbListener)

        awaitClose {
            rtdbRef.removeEventListener(rtdbListener)
        }
    }

    suspend fun updateViuDetails(viu: Viu): Result<Unit> {
        return try {
            viusCollection.document(viu.uid)
                .set(viu, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteViuAndRelationship(viuUid: String): Result<Unit> {
        val caregiverUid = auth.currentUser?.uid
            ?: return Result.failure(Exception("Caregiver not signed in"))

        return try {
            val relationshipQuery = relationshipsCollection
                .whereEqualTo("caregiverUid", caregiverUid)
                .whereEqualTo("viuUid", viuUid)
                .get()
                .await()

            firestore.runBatch { batch ->
                relationshipQuery.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                val viuDocRef = viusCollection.document(viuUid)
                batch.delete(viuDocRef)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendViuPasswordReset(viuEmail: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(viuEmail).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestViuEmailChange(
        context: Context,
        viuUid: String,
        newViuEmail: String
    ): Result<ResendOtpResult> {
        return try {
            val caregiverEmail = auth.currentUser?.email
                ?: return Result.failure(Exception("Caregiver not signed in"))

            if (otpDataSource.isCooldownActive(viuUid, OtpDataSource.OtpType.VIU_EMAIL_CHANGE)) {
                return Result.success(ResendOtpResult.FailureCooldown)
            }

            val otpResult = otpDataSource.requestOtp(
                context = context,
                uid = viuUid,
                emailToSendTo = caregiverEmail,
                type = OtpDataSource.OtpType.VIU_EMAIL_CHANGE,
                extraData = mapOf("pendingEmail" to newViuEmail)
            )
            Result.success(otpResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelViuEmailChange(viuUid: String) {
        otpDataSource.cancelOtpProcess(viuUid, OtpDataSource.OtpType.VIU_EMAIL_CHANGE)
    }

    suspend fun verifyViuEmailChange(viuUid: String, enteredOtp: String): Result<OtpVerificationResult> {
        return try {
            val verificationResult = otpDataSource.verifyOtp(
                uid = viuUid,
                enteredOtp = enteredOtp,
                type = OtpDataSource.OtpType.VIU_EMAIL_CHANGE
            )

            if (verificationResult == OtpVerificationResult.Success) {
                val doc = viusCollection.document(viuUid).get().await()
                val newEmail = doc.getString("pendingEmail")
                    ?: return Result.success(OtpVerificationResult.FailureExpiredOrCooledDown)

                viusCollection.document(viuUid).update("email", newEmail).await()

                otpDataSource.cleanupOtpFields(
                    uid = viuUid,
                    type = OtpDataSource.OtpType.VIU_EMAIL_CHANGE,
                    extraFieldsToDelete = mapOf("pendingEmail" to FieldValue.delete())
                )
            }
            Result.success(verificationResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reauthenticateCaregiver(password: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Caregiver not logged in"))
            val email = user.email ?: return Result.failure(Exception("Caregiver email not found"))
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Invalid password"))
        }
    }

    suspend fun sendVerificationOtpToCaregiver(context: Context): Result<ResendOtpResult> {
        return try {
            val caregiver = auth.currentUser ?: return Result.failure(Exception("Caregiver not logged in"))
            val caregiverEmail = caregiver.email ?: return Result.failure(Exception("Caregiver email not found"))
            val caregiverUid = caregiver.uid

            if (otpDataSource.isCooldownActive(caregiverUid, OtpDataSource.OtpType.VIU_PROFILE_UPDATE)) {
                return Result.success(ResendOtpResult.FailureCooldown)
            }

            val otpResult = otpDataSource.requestOtp(
                context = context,
                uid = caregiverUid,
                emailToSendTo = caregiverEmail,
                type = OtpDataSource.OtpType.VIU_PROFILE_UPDATE
            )
            Result.success(otpResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelViuProfileUpdate() {
        val caregiverUid = auth.currentUser?.uid ?: return
        otpDataSource.cancelOtpProcess(caregiverUid, OtpDataSource.OtpType.VIU_PROFILE_UPDATE)
    }

    suspend fun verifyCaregiverOtp(enteredOtp: String): Result<OtpVerificationResult> {
        return try {
            val caregiverUid = auth.currentUser?.uid ?: return Result.failure(Exception("Caregiver not logged in"))

            val verificationResult = otpDataSource.verifyOtp(
                uid = caregiverUid,
                enteredOtp = enteredOtp,
                type = OtpDataSource.OtpType.VIU_PROFILE_UPDATE
            )

            if (verificationResult == OtpVerificationResult.Success) {
                otpDataSource.cleanupOtpFields(
                    uid = caregiverUid,
                    type = OtpDataSource.OtpType.VIU_PROFILE_UPDATE
                )
            }
            Result.success(verificationResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadViuProfileImage(viuUid: String, imageUri: Uri): Result<Unit> {
        return try {
            val imageUrl = CloudinaryDataSource.uploadImage(imageUri)
                ?: return Result.failure(Exception("Image upload failed"))

            viusCollection.document(viuUid)
                .update("profileImageUrl", imageUrl)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun checkIfPrimaryCaregiver(viuUid: String): Result<Boolean> {
        val caregiverUid = auth.currentUser?.uid
            ?: return Result.failure(Exception("Caregiver not signed in"))

        return try {
            // Query the relationship between THIS caregiver and THIS VIU
            val querySnapshot = relationshipsCollection
                .whereEqualTo("caregiverUid", caregiverUid)
                .whereEqualTo("viuUid", viuUid)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                // No relationship found at all
                return Result.success(false)
            }

            val document = querySnapshot.documents.first()

            // Get the raw value of the "primaryCaregiver" field
            val primaryField = document.get("primaryCaregiver")

            // Determine if primary based on field type
            val isPrimary = when (primaryField) {
                is String -> primaryField == caregiverUid // If it's a UID string, does it match me?
                is Boolean -> primaryField
                else -> false // Default to false if field is missing or unknown type
            }

            Result.success(isPrimary)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking primary status", e)
            Result.failure(e)
        }
    }
}