package edu.capstone.navisight.caregiver.data.remote

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import edu.capstone.navisight.caregiver.model.QRModel
import edu.capstone.navisight.caregiver.model.Viu
import edu.capstone.navisight.caregiver.model.Relationship
import edu.capstone.navisight.caregiver.model.RequestStatus
import edu.capstone.navisight.caregiver.model.SecondaryPairingRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val TAG = "ConnectionDataSource"




class ConnectionDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun getAllPairedVius(caregiverUid: String): Flow<List<Viu>> = callbackFlow {

        var viuListener: ListenerRegistration? = null

        val relationshipListener = firestore.collection("relationships")
            .whereEqualTo("caregiverUid", caregiverUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching relationships", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val viuUids = snapshot?.documents?.mapNotNull { it.getString("viuUid") } ?: emptyList()

                viuListener?.remove()

                if (viuUids.isEmpty()) {
                    trySend(emptyList())
                } else {
                    viuListener = firestore.collection("vius")
                        .whereIn("uid", viuUids)
                        .addSnapshotListener { viuSnap, viuErr ->
                            if (viuErr != null) {
                                Log.e(TAG, "Error fetching VIU profiles", viuErr)
                                trySend(emptyList())
                                return@addSnapshotListener
                            }
                            val viuList = viuSnap?.toObjects(Viu::class.java) ?: emptyList()
                            trySend(viuList)
                        }
                }
            }

        awaitClose {
            relationshipListener.remove()
            viuListener?.remove()
        }
    }

    fun isPrimaryCaregiver(caregiverUid: String, viuUid: String): Flow<Boolean> = callbackFlow {
        val listener = firestore.collection("relationships")
            .whereEqualTo("caregiverUid", caregiverUid)
            .whereEqualTo("viuUid", viuUid)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error checking permission", error)
                    trySend(false)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val relationship = snapshot.documents[0].toObject(Relationship::class.java)
                    trySend(relationship?.primaryCaregiver ?: false)
                } else {

                    trySend(false)
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun getQrByUid(qrUid: String): QRModel? {
        return try {
            val snapshot = firestore.collection("QR_Code")
                .whereEqualTo("qrUid", qrUid)
                .limit(1)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                snapshot.documents.first().toObject(QRModel::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun checkIfRelationshipExists(caregiverUid: String, viuUid: String): Boolean {
        return try {
            val snapshot = firestore.collection("relationships")
                .whereEqualTo("caregiverUid", caregiverUid)
                .whereEqualTo("viuUid", viuUid)
                .limit(1)
                .get()
                .await()

            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    suspend fun sendSecondaryPairingRequest(
        requesterUid: String,
        viuUid: String,
        viuName: String
    ): RequestStatus {
        return try {
            val existingRequest = firestore.collection("secondaryPairingRequests")
                .whereEqualTo("requesterUid", requesterUid)
                .whereEqualTo("viuUid", viuUid)
                .whereEqualTo("status", "pending")
                .get()
                .await()

            if (!existingRequest.isEmpty) {
                Log.d(TAG, "Duplicate request prevented.")
                return RequestStatus.AlreadySent
            }

            val request = SecondaryPairingRequest(
                createdAt = Timestamp.now(),
                requesterUid = requesterUid,
                status = "pending",
                viuName = viuName,
                viuUid = viuUid,
                requesterName = "Caregiver"
            )

            firestore.collection("secondaryPairingRequests")
                .add(request)
                .await()

            RequestStatus.Success

        } catch (e: Exception) {
            e.printStackTrace()
            RequestStatus.Error(e.message ?: "Unknown Error")
        }
    }

    fun getSecondaryPendingRequestsForCaregiver(caregiverUid: String): Flow<List<SecondaryPairingRequest>> = callbackFlow {
        var listener: ListenerRegistration? = null

        val relationshipListener = firestore.collection("relationships")
            .whereEqualTo("caregiverUid", caregiverUid)
            .addSnapshotListener { relSnapshot, relError ->
                if (relError != null) {
                    Log.e("ConnectionDataSource", "Error fetching relationships", relError)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val viuUids = relSnapshot?.documents?.mapNotNull { it.getString("viuUid") } ?: emptyList()
                listener?.remove()

                if (viuUids.isEmpty()) {
                    trySend(emptyList())
                } else {
                    listener = firestore.collection("secondaryPairingRequests")
                        .whereIn("viuUid", viuUids)
                        .whereEqualTo("status", "pending")
                        .addSnapshotListener { reqSnapshot, reqError ->
                            if (reqError != null) {
                                Log.e("ConnectionDataSource", "Error fetching pending requests", reqError)
                                trySend(emptyList())
                                return@addSnapshotListener
                            }

                            val requests = reqSnapshot?.documents?.map { doc ->
                                val request = doc.toObject(SecondaryPairingRequest::class.java)
                                request?.id = doc.id
                                request!!
                            } ?: emptyList()
                            Log.d("ConnectionDataSource", "Fetched ${requests.size} pending requests")
                            trySend(requests)
                        }
                }
            }

        awaitClose {
            relationshipListener.remove()
            listener?.remove()
        }
    }

    suspend fun approveSecondaryRequest(request: SecondaryPairingRequest): RequestStatus {
        return try {
            val requestRef = firestore.collection("secondaryPairingRequests").document(request.id)

            requestRef.update("status", "approved").await()

            val relationship = Relationship(
                createdAt = Timestamp.now(),
                caregiverUid = request.requesterUid,
                viuUid = request.viuUid,
                primaryCaregiver = false
            )

            firestore.collection("relationships").add(relationship).await()

            RequestStatus.Success

        } catch (e: Exception) {
            RequestStatus.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun denySecondaryRequest(requestId: String): RequestStatus {
        return try {

            firestore.collection("secondaryPairingRequests")
                .document(requestId)
                .update("status", "denied")
                .await()

            RequestStatus.Success

        } catch (e: Exception) {
            RequestStatus.Error(e.message ?: "Unknown error")
        }
    }



}