package edu.capstone.navisight.webrtc

import android.util.Log
import edu.capstone.navisight.webrtc.model.FirebaseFieldNames
import edu.capstone.navisight.webrtc.utils.EventListener
import edu.capstone.navisight.webrtc.utils.UserStatus
import edu.capstone.navisight.webrtc.model.DataModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import kotlinx.coroutines.*

/**
 * Non-DI Singleton version of FirebaseClient.
 * No Hilt or @Inject — fully self-contained.
 * Pain in the bum. Fraeron was here.
 */

class FirebaseClient private constructor(
    private val dbRef: DatabaseReference,
    private val gson: Gson
) {
    // Define a Coroutine Scope for the Singleton lifecycle
    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var currentUID: String? = null
    private fun setUID(uid: String) {
        this.currentUID = uid
    }

    // Tracking listeners for removal, mapping Reference -> Listener
    private val statusListeners = mutableMapOf<DatabaseReference, ValueEventListener>()

    fun listenForTargetNodeChanges(userId: String, onNodeChange: (DataSnapshot) -> Unit): () -> Unit {
        // Monitor the entire user node reference
        val userNodeRef = dbRef.child(userId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Pass the entire snapshot to the repository for parsing and checking
                onNodeChange(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseClient", "Target node listener cancelled for $userId.", error.toException())
                // In case of error, we can send an empty snapshot or handle it gracefully
            }
        }

        userNodeRef.addValueEventListener(listener)
        statusListeners[userNodeRef] = listener

        // Return a lambda function that can be called to remove this specific listener
        return {
            userNodeRef.removeEventListener(listener)
            statusListeners.remove(userNodeRef)
            Log.d("FirebaseClient", "Target node listener removed for $userId.")
        }
    }

    // Formerly login.
    fun checkRTDB(uid: String, done: (Boolean, String?) -> Unit) {
        Log.d("AuthenticationCheck", "Passing through checkRTDB with UID $uid...")
        dbRef.addListenerForSingleValueEvent(object : EventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.hasChild(uid)) {
                    dbRef.child(uid).child(FirebaseFieldNames.STATUS)
                        .setValue(UserStatus.ONLINE)
                        .addOnCompleteListener {
                            setUID(uid)
                            // Handle Firebase RTDB not updating properly
                            // if last property is the same in case of abort.
                            clearLatestEvent()
                            done(true, null)
                        }.addOnFailureListener {
                            done(false, "${it.message}")
                        }
                }
            }
        })
    }

    fun observeLatestEvents(listener: Listener) {
        try {
            dbRef.child(currentUID!!).child(FirebaseFieldNames.LATEST_EVENT)
                .addValueEventListener(object : EventListener() {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        super.onDataChange(snapshot)
                        Log.d("observeLatestEvents", "Triggered.")

                        val event = try {
                            Log.d("observeLatestEvents", "Trying")

                            gson.fromJson(snapshot.value.toString(), DataModel::class.java)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.d("observeLatestEvents", "LATEST EVENT IS NULL")

                            null
                        }
                        event?.let {
                            Log.d("observeLatestEvents", "$event")
                            listener.onLatestEventReceived(it)
                        }
                    }
                })
        } catch (e: Exception) {
            Log.d("observeLatestEvents", "FAILED ON OBSERVING LATEST EVENTS ${e.stackTraceToString()}")

            e.printStackTrace()
        }
    }

    fun sendMessageToOtherClient(message: DataModel, success: (Boolean) -> Unit) {
        val convertedMessage = gson.toJson(message.copy(sender = currentUID))
        dbRef.child(message.target).child(FirebaseFieldNames.LATEST_EVENT).setValue(convertedMessage)
            .addOnCompleteListener { success(true) }
            .addOnFailureListener { success(false) }
        Log.d("CallSignal", "Triggered send connection request")

    }

    fun changeMyStatus(status: UserStatus) {
        dbRef.child(currentUID!!).child(FirebaseFieldNames.STATUS).setValue(status.name)
    }

    fun clearLatestEvent() {
        dbRef.child(currentUID!!).child(FirebaseFieldNames.LATEST_EVENT).setValue(null)
    }

    fun logOff(function: () -> Unit) {
        dbRef.child(currentUID!!).child(FirebaseFieldNames.STATUS).setValue(UserStatus.OFFLINE)
            .addOnCompleteListener { function() }
    }

    interface Listener {
        fun onLatestEventReceived(event: DataModel)
    }

    companion object {
        // Lazy-initialized singleton instance
        @Volatile
        private var instance: FirebaseClient? = null

        fun getInstance(): FirebaseClient {
            return instance ?: synchronized(this) {
                instance ?: FirebaseClient(
                    FirebaseDatabase.getInstance().getReference("webrtc_signal"), // Firebase root ref
                    Gson() // single Gson instance
                ).also { instance = it }
            }
        }
    }
}
