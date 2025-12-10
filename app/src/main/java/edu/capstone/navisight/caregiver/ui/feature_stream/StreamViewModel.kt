package edu.capstone.navisight.caregiver.ui.feature_stream

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.capstone.navisight.caregiver.model.Caregiver
import edu.capstone.navisight.caregiver.model.Viu
import edu.capstone.navisight.caregiver.ui.feature_records.SortOrder
import edu.capstone.navisight.common.webrtc.FirebaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class StreamViewModel () : ViewModel() {

    private val _profile = MutableStateFlow<Caregiver?>(null)
    val profile: StateFlow<Caregiver?> = _profile

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _success = MutableStateFlow<Boolean>(false)
    val success: StateFlow<Boolean> = _success

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.ASCENDING)

    private val _vius = MutableStateFlow<List<Triple<Viu, String, String>>>(emptyList())
    val vius: StateFlow<List<Triple<Viu, String, String>>> = _vius.asStateFlow()
    private var viuStatusListenerDisposer: (() -> Unit)? = null

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun observeViuStatuses(associatedViuUids: List<String>, firebaseClient: FirebaseClient) {
        viuStatusListenerDisposer?.invoke()
        viuStatusListenerDisposer = null
        val disposer = firebaseClient.observeAssociatedUsersStatus(associatedViuUids) {
            result: List<Pair<Viu?, String>> ->
            val processedUsers = result
                .filter { it.first != null } // Filter out null VIU objects
                .map { (viu, status) ->
                    // Map Pair<Viu?, String> to Triple<Viu, String, String>
                    Triple(viu!!, viu.uid, status)
                }

            _vius.value = processedUsers
        }
        viuStatusListenerDisposer = { disposer }// Store the function to dispose of the listener
    }

    // Clean up when the ViewModel is destroyed
    override fun onCleared() {
        super.onCleared()
        // Stop the real-time listener when the ViewModel is destroyed
        viuStatusListenerDisposer?.invoke()
        viuStatusListenerDisposer = null
    }

    val filteredViuTriples: StateFlow<List<Triple<Viu, String, String>>> = combine(
        _vius, _searchQuery, _sortOrder) { list, query, order ->
        val filteredList = if (query.isBlank()) {
            list
        } else {
            list.filter { (viu, _, _) ->
                // Filter by first name or last name
                (viu.firstName.contains(query, ignoreCase = true)) ||
                        (viu.lastName.contains(query, ignoreCase = true))
            }
        }

        when (order) {
            SortOrder.ASCENDING -> filteredList.sortedBy { it.first.firstName.lowercase() }
            SortOrder.DESCENDING -> filteredList.sortedByDescending { it.first.firstName.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val viuList: StateFlow<List<Viu>> = filteredViuTriples
        .map { triples ->
            // Use the map function on the List itself
            triples.map { it.first }
        }
        // Convert the intermediate Flow into a StateFlow to be collected by the UI
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

//    private fun setDummyViusForTesting() {
//        val dummyVius = listOf(
//            Viu(uid = "1", firstName = "Zack", lastName = "Taylor"),
//            Viu(uid = "2", firstName = "Alpha", lastName = "Dog"),
//            Viu(uid = "3", firstName = "Beta", lastName = "Ray"),
//            Viu(uid = "4", firstName = "Charlie", lastName = "Brown"),
//            Viu(uid = "5", firstName = "Delta", lastName = "Force"),
//            Viu(uid = "6", firstName = "Echo", lastName = "Location"),
//            Viu(uid = "7", firstName = "Foxtrot", lastName = "Lima"),
//            Viu(uid = "8", firstName = "Golf", lastName = "Tango"),
//            Viu(uid = "9", firstName = "Hotel", lastName = "Zulu")
//        )
//
//        // Convert Viu to the Triple<Viu, String, String> format (assuming "Online" status)
//        val dummyTriples = dummyVius.map { viu ->
//            Triple(viu, viu.uid, "Online")
//        }
//
//        // Replace the real data from the observation with dummy data for testing the scroll
//        _vius.value = dummyTriples
//    }
}