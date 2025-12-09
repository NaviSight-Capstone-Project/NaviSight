package edu.capstone.navisight.caregiver.data.remote

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import edu.capstone.navisight.caregiver.model.AlertNotification
import edu.capstone.navisight.caregiver.model.GeofenceActivity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await


private const val TAG = "NotificationDataSource"

class NotificationDataSource {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val alertsCollection = db.collection("alerts")

    // Fetches events ONLY for Vius paired with this Caregiver
    fun getRelevantEventsFlow(): Flow<List<GeofenceActivity>> = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        var eventsListener: ListenerRegistration? = null

        val relationshipListener = db.collection("relationships")
            .whereEqualTo("caregiverUid", currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val pairedViuUids = snapshot?.documents?.mapNotNull { it.getString("viuUid") } ?: emptyList()

                eventsListener?.remove()

                if (pairedViuUids.isEmpty()) {
                    trySend(emptyList())
                } else {
                    eventsListener = db.collection("geofence_events")
                        .whereIn("viuUid", pairedViuUids.take(10))
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(50)
                        .addSnapshotListener { eventSnap, eventError ->
                            if (eventError != null) {
                                trySend(emptyList())
                                return@addSnapshotListener
                            }

                            val events = eventSnap?.documents?.mapNotNull { doc ->
                                doc.toObject(GeofenceActivity::class.java)?.copy(id = doc.id)
                            } ?: emptyList()

                            trySend(events)
                        }
                }
            }

        awaitClose {
            relationshipListener.remove()
            eventsListener?.remove()
        }
    }

    // Listen to personal dismissed list
    fun getUserDismissedIdsFlow(): Flow<Set<String>> = callbackFlow {
        val currentUser = auth.currentUser ?: return@callbackFlow

        val listener = db.collection("users").document(currentUser.uid)
            .collection("dismissed_events")
            .addSnapshotListener { snapshot, _ ->
                val ids = snapshot?.documents?.map { it.id }?.toSet() ?: emptySet()
                trySend(ids)
            }
        awaitClose { listener.remove() }
    }

    // Add ID to personal blacklist
    suspend fun dismissEvent(eventId: String) {
        val currentUser = auth.currentUser ?: return
        val data = hashMapOf("timestamp" to com.google.firebase.Timestamp.now())
        try {
            db.collection("users").document(currentUser.uid)
                .collection("dismissed_events")
                .document(eventId)
                .set(data)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    suspend fun saveAlert(alert: AlertNotification): Result<Unit> {
        return try {
            // Firestore documents require non-null IDs. Assuming AlertNotification has a unique ID field.
            val documentId = alert.id
            alertsCollection.document(documentId).set(alert).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationDataSource", "Error saving alert: ${e.message}")
            Result.failure(e)
        }
    }

    fun getAlertsFlow(): Flow<List<AlertNotification>> = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        var alertsListener: ListenerRegistration? = null
        val relationshipListener = db.collection("relationships")
            .whereEqualTo("caregiverUid", currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    Log.e(TAG, "Error fetching relationships: ${error.message}")
                    return@addSnapshotListener
                }

                val pairedViuUids = snapshot?.documents?.mapNotNull { it.getString("viuUid") } ?: emptyList()
                Log.d(TAG, "Fetched paired VIU UIDs: $pairedViuUids") // LOG 1: Check UIDs

                alertsListener?.remove()

                if (pairedViuUids.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val uidsToQuery = pairedViuUids.take(10) // Respect the whereIn limit TODO: FIX LIMIT
                Log.d(TAG, "Querying alerts for UIDs: $uidsToQuery")

                alertsListener = alertsCollection
                    .whereIn("viu.uid", uidsToQuery)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener { alertSnap, _ ->
                        if (alertSnap == null) {
                            Log.e(TAG, "Alerts snapshot is null.")
                            trySend(emptyList())
                            return@addSnapshotListener
                        }

                        Log.d(TAG, "Received ${alertSnap.documents.size} raw alert documents.")

                        val alerts = alertSnap.documents.mapNotNull { doc ->
                            try {
                                val alert = doc.toObject(AlertNotification::class.java)?.copy(id = doc.id)
                                if (alert != null) {
                                    Log.v(TAG, "Successfully mapped alert ID: ${alert.id}")
                                }
                                alert
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to map alert document ID: ${doc.id}", e)
                                null
                            }
                        }
                        Log.d(TAG, "Successfully processed ${alerts.size} final alerts.")
                        trySend(alerts)
                    }
            }

        awaitClose {
            relationshipListener.remove()
            alertsListener?.remove()
        }
    }


    fun getDismissedAlertIdsFlow(): Flow<Set<String>> = callbackFlow {
        val currentUser = auth.currentUser ?: return@callbackFlow

        val listener = db.collection("users").document(currentUser.uid)
            .collection("dismissed_alerts")
            .addSnapshotListener { snapshot, _ ->
                val ids = snapshot?.documents?.map { it.id }?.toSet() ?: emptySet()
                trySend(ids)
            }
        awaitClose { listener.remove() }
    }


    suspend fun dismissAlert(alertId: String) {
        val currentUser = auth.currentUser ?: return
        val data = hashMapOf("timestamp" to Timestamp.now())

        try {
            db.collection("users").document(currentUser.uid)
                .collection("dismissed_alerts")
                .document(alertId)
                .set(data)
                .await()
        } catch (e: Exception) {
            Log.e("NotificationDataSource", "Error dismissing alert: ${e.message}")
        }
    }
}