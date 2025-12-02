package edu.capstone.navisight.caregiver.ui.feature_stream

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.capstone.navisight.caregiver.model.Caregiver
import edu.capstone.navisight.caregiver.model.Viu
import edu.capstone.navisight.caregiver.ui.feature_records.SortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    private val _allVius = MutableStateFlow<List<Viu>>(emptyList())

    private val _sortOrder = MutableStateFlow(SortOrder.ASCENDING)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()


    val vius: StateFlow<List<Viu>> = combine(_allVius, _searchQuery, _sortOrder) { list, query, order ->
        val filteredList = if (query.isBlank()) {
            list
        } else {
            list.filter { viu ->
                (viu.firstName?.contains(query, ignoreCase = true) == true) ||
                        (viu.lastName?.contains(query, ignoreCase = true) == true)
            }
        }
        when (order) {
            SortOrder.ASCENDING -> filteredList.sortedBy { it.firstName?.lowercase() }
            SortOrder.DESCENDING -> filteredList.sortedByDescending { it.firstName?.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}