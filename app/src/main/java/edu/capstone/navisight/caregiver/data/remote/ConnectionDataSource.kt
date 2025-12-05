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
import edu.capstone.navisight.caregiver.model.TransferPrimaryRequest
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
            val snapshot = firestore.collection("qr_code")
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
            val existingRequest = firestore.collection("secondary_pairing_requests")
                .whereEqualTo("requesterUid", requesterUid)
                .whereEqualTo("viuUid", viuUid)
                .whereEqualTo("status", "pending")
                .get()
                .await()

            if (!existingRequest.isEmpty) {
                Log.d(TAG, "Duplicate request prevented.")
                return RequestStatus.AlreadySent
            }

            val caregiverSnapshot = firestore.collection("caregivers")
                .document(requesterUid)
                .get()
                .await()

            val firstName = caregiverSnapshot.getString("firstName") ?: ""
            val lastName = caregiverSnapshot.getString("lastName") ?: ""
            val requesterName = "$firstName $lastName".trim().ifEmpty { "Caregiver" }

            val request = SecondaryPairingRequest(
                createdAt = Timestamp.now(),
                requesterUid = requesterUid,
                status = "pending",
                viuName = viuName,
                viuUid = viuUid,
                requesterName = requesterName
            )

            firestore.collection("secondary_pairing_requests")
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
            .whereEqualTo("primaryCaregiver", true)
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
                    listener = firestore.collection("secondary_pairing_requests")
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
            val requestRef = firestore.collection("secondary_pairing_requests").document(request.id)

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

            firestore.collection("secondary_pairing_requests")
                .document(requestId)
                .update("status", "denied")
                .await()

            RequestStatus.Success

        } catch (e: Exception) {
            RequestStatus.Error(e.message ?: "Unknown error")
        }
    }
    suspend fun getTransferCandidates(viuUid: String, currentUid: String): List<TransferPrimaryRequest> {
        return try {
            val relSnapshot = firestore.collection("relationships")
                .whereEqualTo("viuUid", viuUid)
                .get()
                .await()

            val otherCaregiverUids = relSnapshot.documents
                .mapNotNull { it.getString("caregiverUid") }
                .filter { it != currentUid }

            if (otherCaregiverUids.isEmpty()) return emptyList()

            val usersSnapshot = firestore.collection("caregivers")
                .whereIn("uid", otherCaregiverUids)
                .get()
                .await()

            usersSnapshot.documents.map { doc ->
                TransferPrimaryRequest(
                    recipientUid = doc.getString("uid") ?: "",
                    recipientName = "${doc.getString("firstName")} ${doc.getString("lastName")}",
                    recipientEmail = doc.getString("email") ?: "",
                    viuUid = viuUid
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching candidates", e)
            emptyList()
        }
    }

    suspend fun sendTransferRequest(request: TransferPrimaryRequest): RequestStatus {
        return try {
            // Check for duplicate pending requests
            val existing = firestore.collection("transfer_primary_requests")
                .whereEqualTo("viuUid", request.viuUid)
                .whereEqualTo("recipientUid", request.recipientUid)
                .whereEqualTo("status", "pending")
                .get().await()

            if (!existing.isEmpty) return RequestStatus.Error("Request already pending")

            firestore.collection("transfer_primary_requests").add(request).await()
            RequestStatus.Success
        } catch (e: Exception) {
            RequestStatus.Error(e.message ?: "Failed to send")
        }
    }

    fun getIncomingTransferRequests(myUid: String): Flow<List<TransferPrimaryRequest>> = callbackFlow {
        val listener = firestore.collection("transfer_primary_requests")
            .whereEqualTo("recipientUid", myUid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snap?.toObjects(TransferPrimaryRequest::class.java) ?: emptyList()
                // Ensure ID is attached
                items.forEachIndexed { i, item -> item.id = snap!!.documents[i].id }
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    suspend fun approveTransferRequest(request: TransferPrimaryRequest): RequestStatus {
        return try {
            val relRef = firestore.collection("relationships")
            val senderSnapshot = relRef
                .whereEqualTo("caregiverUid", request.currentPrimaryCaregiverUid)
                .whereEqualTo("viuUid", request.viuUid)
                .limit(1)
                .get()
                .await()

            // Find the Receiver's Relationship Document
            val receiverSnapshot = relRef
                .whereEqualTo("caregiverUid", request.recipientUid)
                .whereEqualTo("viuUid", request.viuUid)
                .limit(1)
                .get()
                .await()

            // Check if documents exist before starting transaction
            if (senderSnapshot.isEmpty || receiverSnapshot.isEmpty) {
                return RequestStatus.Error("Relationship documents not found")
            }

            // Get the References
            val senderDocRef = senderSnapshot.documents[0].reference
            val receiverDocRef = receiverSnapshot.documents[0].reference
            val requestDocRef = firestore.collection("transfer_primary_requests").document(request.id)

            firestore.runTransaction { transaction ->
                transaction.get(senderDocRef)
                transaction.get(receiverDocRef)
                transaction.update(senderDocRef, "primaryCaregiver", false)
                transaction.update(receiverDocRef, "primaryCaregiver", true)
                transaction.update(requestDocRef, "status", "approved")
            }.await()

            RequestStatus.Success
        } catch (e: Exception) {
            e.printStackTrace()
            RequestStatus.Error(e.message ?: "Transaction failed")
        }
    }

    suspend fun denyTransferRequest(requestId: String): RequestStatus {
        return try {
            firestore.collection("transfer_primary_requests")
                .document(requestId)
                .update("status", "denied")
                .await()
            RequestStatus.Success
        } catch (e: Exception) {
            RequestStatus.Error(e.message ?: "Error denying request")
        }
    }
    suspend fun getCaregiverName(uid: String): String {
        return try {
            val document = firestore.collection("caregivers").document(uid).get().await()
            if (document.exists()) {
                val firstName = document.getString("firstName") ?: ""
                val lastName = document.getString("lastName") ?: ""
                "$firstName $lastName".trim()
            } else {
                "Unknown Caregiver"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching caregiver name", e)
            "Unknown Caregiver"
        }
    }
    suspend fun unpairViu(caregiverUid: String, viuUid: String): RequestStatus {
        return try {
            val snapshot = firestore.collection("relationships")
                .whereEqualTo("caregiverUid", caregiverUid)
                .whereEqualTo("viuUid", viuUid)
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) {
                return RequestStatus.Error("Relationship not found")
            }

            // Delete the document
            snapshot.documents[0].reference.delete().await()
            RequestStatus.Success
        } catch (e: Exception) {
            RequestStatus.Error(e.message ?: "Failed to unpair")
        }
    }
}