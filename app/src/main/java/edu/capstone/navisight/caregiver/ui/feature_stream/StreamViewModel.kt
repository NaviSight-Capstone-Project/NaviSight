package edu.capstone.navisight.caregiver.ui.feature_stream

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.capstone.navisight.caregiver.data.remote.ViuDataSource
import edu.capstone.navisight.caregiver.model.Caregiver
import edu.capstone.navisight.caregiver.model.Viu
import edu.capstone.navisight.caregiver.ui.feature_records.SortOrder
import edu.capstone.navisight.common.webrtc.FirebaseClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    private var viuStatusListenerDisposer: (() -> Unit)? = null

    private val viuDataSource = ViuDataSource()

    private val _vius = MutableStateFlow<List<Triple<Viu, String, String>>>(emptyList())
    val vius: StateFlow<List<Triple<Viu, String, String>>> = _vius.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refreshViuStatuses(caregiverUid: String, firebaseClient: FirebaseClient) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // Re-fetch UIDs and re-observe RTDB
                val uids = firebaseClient.getAssociatedViuUids(caregiverUid)
                observeViuStatuses(uids, firebaseClient)
            } finally {
                delay(500) // Small delay for UX
                _isRefreshing.value = false
            }
        }
    }

    fun observeViuStatuses(associatedViuUids: List<String>, firebaseClient: FirebaseClient) {
        viuStatusListenerDisposer?.invoke()

        val disposer = firebaseClient.observeAssociatedUsersStatus(associatedViuUids) { result ->
            // Call the suspend function checkIfPrimaryCaregiver
            viewModelScope.launch {
                val processedUsers = result
                    .filter { it.first != null }
                    .map { (viu, status) ->
                        val isPrimary = viuDataSource.checkIfPrimaryCaregiver(viu!!.uid)
                            .getOrDefault(false)

                        // Triple: Viu, Status, Role
                        Triple(viu!!, status, if (isPrimary) "PRIMARY" else "SECONDARY")
                    }
                _vius.value = processedUsers
            }
        }
        viuStatusListenerDisposer = { disposer }
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
        val filteredList = if (query.isBlank()) list
        else list.filter { it.first.firstName.contains(query, ignoreCase = true) ||
                it.first.lastName.contains(query, ignoreCase = true) }

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
}