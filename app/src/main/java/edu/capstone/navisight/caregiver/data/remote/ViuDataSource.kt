package edu.capstone.navisight.caregiver.data.remote

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import edu.capstone.navisight.caregiver.model.Viu
import edu.capstone.navisight.caregiver.model.ViuLocation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine

class ViuDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val rtdb: FirebaseDatabase
) {

    private val TAG = "FirebaseDataSource"

    companion object {
        private const val VIUS_COLLECTION = "vius"
        private const val VIU_LOCATION_REF = "viu_location"
        private const val VIU_LOCATION_FIELD = "location"
    }


    fun getViuByUid(uid: String): Flow<Viu?> {
        val staticDataFlow = getViuStaticDataFirestore(uid)
        val locationDataFlow = getViuLocationRtdb(uid)

        return staticDataFlow.combine(locationDataFlow) { viu, location ->
            val updatedViu = viu?.copy(location = location)
            Log.d(TAG, "Combine: VIU name: ${updatedViu?.firstName}, Location: ${updatedViu?.location}")
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

        Log.d(TAG, "RTDB: Attaching listener to $uid")

        val rtdbListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val location = snapshot.getValue(ViuLocation::class.java)

                Log.d(TAG, "RTDB: onDataChange received: $location")
                trySend(location)
            }

            override fun onCancelled(error: DatabaseError) {

                Log.e(TAG, "RTDB: onCancelled: ${error.message}")
                trySend(null)
                close(error.toException())
            }
        }
        rtdbRef.addValueEventListener(rtdbListener)

        awaitClose {
            Log.d(TAG, "RTDB: Removing listener from $uid")
            rtdbRef.removeEventListener(rtdbListener)
        }
    }


}