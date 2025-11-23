package edu.capstone.navisight.caregiver.ui.feature_records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.capstone.navisight.caregiver.model.Viu
import edu.capstone.navisight.caregiver.domain.connectionUseCase.GetAllPairedViusUseCase
import edu.capstone.navisight.common.domain.usecase.GetCurrentUserUidUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortOrder {
    ASCENDING, DESCENDING
}

class RecordsViewModel(
    private val getAllPairedViusUseCase: GetAllPairedViusUseCase = GetAllPairedViusUseCase(),
    private val getCurrentUserUidUseCase: GetCurrentUserUidUseCase = GetCurrentUserUidUseCase()
) : ViewModel() {

    private val _allVius = MutableStateFlow<List<Viu>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()


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

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        observeVius()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleSortOrder() {
        _sortOrder.value = if (_sortOrder.value == SortOrder.ASCENDING) {
            SortOrder.DESCENDING
        } else {
            SortOrder.ASCENDING
        }
    }

    private fun observeVius() {
        viewModelScope.launch {
            _isLoading.value = true
            val currentUid = getCurrentUserUidUseCase()

            if (currentUid != null) {
                try {
                    getAllPairedViusUseCase(caregiverUid = currentUid).collectLatest { viuList ->
                        _allVius.value = viuList
                        _isLoading.value = false
                    }
                } catch (e: Exception) {
                    _error.value = e.message ?: "Failed to load records"
                    _isLoading.value = false
                }
            } else {
                _error.value = "User is not logged in"
                _isLoading.value = false
            }
        }
    }
}