package edu.capstone.navisight.common.webrtc

import android.util.Log
import androidx.lifecycle.viewModelScope
import edu.capstone.navisight.common.webrtc.model.FirebaseFieldNames
import edu.capstone.navisight.common.webrtc.utils.EventListener
import edu.capstone.navisight.common.webrtc.utils.UserStatus
import edu.capstone.navisight.common.webrtc.model.DataModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import edu.capstone.navisight.caregiver.data.remote.ViuDataSource
import edu.capstone.navisight.caregiver.model.Viu
import edu.capstone.navisight.common.webrtc.model.FirebaseFieldNames.EMAIL
import edu.capstone.navisight.common.webrtc.model.FirebaseFieldNames.LATEST_EVENT
import edu.capstone.navisight.common.webrtc.model.FirebaseFieldNames.STATUS
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

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
        Log.d("AuthenticationCheck", "UID has been set. Value is: $uid")
        this.currentUID = uid
    }

    // Set this function for Camera's quick menu actions
    fun getUserUID(): String {
        return this.currentUID!!
    }

    private val viuRemoteDataSource = ViuDataSource()

    // For checking if the user is a VIU or Caregiver
    private lateinit var userType : String

    /*
        Set current email as a "catcher" if ever, for whatever reason, the registration failed
        to set up the real-time database right.
     */
    private var currentEmail: String? = null

    fun setEmail(email: String) {
        this.currentEmail = email
    }

    fun setUserType(userType: String) {
        this.userType = userType
        Log.d("UserTypeCheck", "User type has been set to: $userType")
    }

    // Handle relationships
    suspend fun getAssociatedViuUids(caregiverUid: String): List<String> {
        val firestoreDb = FirebaseFirestore.getInstance()
        val vius = mutableListOf<String>()

        try {
            val querySnapshot = firestoreDb.collection("relationships")
                .whereEqualTo("caregiverUid", caregiverUid)
                .get()
                .await() // Requires kotlinx-coroutines-play-services dependency

            for (document in querySnapshot.documents) {
                val viuUid = document.getString("viuUid")
                if (viuUid != null) {
                    vius.add(viuUid)
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseClient", "Error fetching associated VIUs: ${e.message}")
        }
        return vius
    }

    fun observeAssociatedUsersStatus(
        associatedViuUids: List<String>,
        status: (List<Pair<Viu?, String>>) -> Unit
    ) {
        // Only proceed if there are UIDs to observe
        if (associatedViuUids.isEmpty()) {
            status(emptyList())
            return
        }

        dbRef.addValueEventListener(object : EventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                clientScope.launch {
                    val deferredFetches = snapshot.children
                        // Include children whose key is in the associated list
                        .filter { it.key in associatedViuUids }
                        .mapNotNull { snapshotChild ->
                            val uid = snapshotChild.key ?: return@mapNotNull null
                            val userStatus = snapshotChild.child(FirebaseFieldNames.STATUS).value?.toString() ?: "OFFLINE"

                            async {
                                val credentials = retrieveVIUCredentials(uid)
                                Pair(credentials, userStatus)
                            }
                        }

                    val results = deferredFetches.awaitAll()
                        .filter { (viu, _) -> viu != null } // Filter out any VIUs whose credentials failed to load

                    withContext(Dispatchers.Main) {
                        status(results)
                    }
                }
            }
        })
    }


    private suspend fun retrieveVIUCredentials(uid: String): Viu? {
        return viuRemoteDataSource.getViuDetails(uid).first()
    }

    fun getUserType(): String {
        return userType
    }

    // Formerly login.
    fun checkRTDB(uid: String, done: (Boolean, String?) -> Unit) {
        setUID(uid) // DO NOT REMOVE THIS.
        Log.d("AuthenticationCheck", "Passing through checkRTDB with UID $uid...")
        dbRef.addListenerForSingleValueEvent(object : EventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.hasChild(uid)) {
                    dbRef.child(uid).child(STATUS)
                        .setValue(UserStatus.ONLINE)
                        .addOnCompleteListener {
                            /*
                            Handle Firebase RTDB not updating properly
                             */
                            clearLatestEvent()
                            done(true, null)
                        }.addOnFailureListener {
                            done(false, "${it.message}")
                        }
                } else {
                    // Detect  to make a document for RTDB, make one so NullPointerException also
                    // won't hit status checking in MainActivity's OnDestroy.

                    dbRef.child(uid).child(EMAIL).setValue(currentEmail)
                        .addOnCompleteListener {
                            dbRef.child(uid).child(STATUS)
                                .setValue(UserStatus.ONLINE)
                                .addOnCompleteListener {
                                    done(true, null)
                                }.addOnFailureListener {
                                    done(false, it.message)
                                }
                    }.addOnFailureListener {
                        done(false, it.message)
                    }
                }
            }
        })
    }


    fun observeLatestEvents(listener: Listener) {
        try {

            Log.d("observeLatestEvents", "Triggered with currentuid: $currentUID.")
            dbRef.child(currentUID!!).child(LATEST_EVENT)
                .addValueEventListener(object : EventListener() {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        super.onDataChange(snapshot)
                        Log.d("observeLatestEvents", "overriding...")
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
            Log.d(
                "observeLatestEvents",
                "FAILED ON OBSERVING LATEST EVENTS ${e.stackTraceToString()}")
            e.printStackTrace()
        }
    }

    fun sendMessageToOtherClient(message: DataModel, success: (Boolean) -> Unit) {
        val convertedMessage = gson.toJson(message.copy(sender = currentUID))
        dbRef.child(message.target).child(LATEST_EVENT).setValue(convertedMessage)
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
                    GsonSingleton.instance
                ).also { instance = it }
            }
        }
    }
}
