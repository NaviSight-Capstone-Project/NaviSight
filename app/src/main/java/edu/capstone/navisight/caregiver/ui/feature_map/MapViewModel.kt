package edu.capstone.navisight.caregiver.ui.feature_map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.capstone.navisight.caregiver.model.Viu
import edu.capstone.navisight.caregiver.domain.viuUseCase.GetViuByUidUseCase
import edu.capstone.navisight.caregiver.domain.connectionUseCase.GetAllPairedViusUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MapViewModel(
    private val getAllPairedViusUseCase: GetAllPairedViusUseCase = GetAllPairedViusUseCase(),
    private val getViuByUidUseCase: GetViuByUidUseCase = GetViuByUidUseCase()
) : ViewModel() {

    private val _vius = MutableStateFlow<List<Viu>>(emptyList())
    val vius: StateFlow<List<Viu>> = _vius

    private val _selectedViu = MutableStateFlow<Viu?>(null)
    val selectedViu: StateFlow<Viu?> = _selectedViu

    init {
        observeVius()
    }

    private fun observeVius() {
        viewModelScope.launch {
            getAllPairedViusUseCase().collect { list ->
                _vius.value = list

                if (_selectedViu.value == null && list.isNotEmpty()) {
                    selectViu(list.first().uid)
                }

                val selectedUid = _selectedViu.value?.uid
                if (selectedUid != null && list.none { it.uid == selectedUid }) {
                    _selectedViu.value = null
                }
            }
        }
    }

    fun selectViu(uid: String) {
        viewModelScope.launch {
            getViuByUidUseCase(uid).collect { viu ->
                _selectedViu.value = viu
            }
        }
    }
}