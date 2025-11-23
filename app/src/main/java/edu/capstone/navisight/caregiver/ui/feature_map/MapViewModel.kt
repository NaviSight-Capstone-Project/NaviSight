package edu.capstone.navisight.caregiver.ui.feature_map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.capstone.navisight.caregiver.model.Geofence
import edu.capstone.navisight.caregiver.model.Viu
import edu.capstone.navisight.caregiver.domain.viuUseCase.GetViuByUidUseCase
import edu.capstone.navisight.caregiver.domain.connectionUseCase.GetAllPairedViusUseCase
import edu.capstone.navisight.caregiver.domain.connectionUseCase.IsPrimaryCaregiverUseCase
import edu.capstone.navisight.common.domain.usecase.GetCurrentUserUidUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng

class MapViewModel(

    private val getAllPairedViusUseCase: GetAllPairedViusUseCase = GetAllPairedViusUseCase(),
    private val getViuByUidUseCase: GetViuByUidUseCase = GetViuByUidUseCase(),
    private val getCurrentUserUidUseCase: GetCurrentUserUidUseCase = GetCurrentUserUidUseCase(),
    private val isPrimaryCaregiverUseCase: IsPrimaryCaregiverUseCase = IsPrimaryCaregiverUseCase()
) : ViewModel() {


    private val _vius = MutableStateFlow<List<Viu>>(emptyList())
    val vius: StateFlow<List<Viu>> = _vius

    private val _selectedViu = MutableStateFlow<Viu?>(null)
    val selectedViu: StateFlow<Viu?> = _selectedViu


    private val _isPrimary = MutableStateFlow(false)
    val isPrimary: StateFlow<Boolean> = _isPrimary


    private var viuJob: Job? = null
    private var permissionJob: Job? = null

    private val _longPressedLatLng = MutableStateFlow<LatLng?>(null)
    val longPressedLatLng: StateFlow<LatLng?> = _longPressedLatLng

    private val _selectedGeofence = MutableStateFlow<Geofence?>(null)
    val selectedGeofence: StateFlow<Geofence?> = _selectedGeofence

    init {
        observeViusList()
    }

    private fun observeViusList() {
        viewModelScope.launch {
            val currentUid = getCurrentUserUidUseCase()

            if (currentUid == null) {
                _vius.value = emptyList()
                return@launch
            }

            getAllPairedViusUseCase(currentUid).collect { list ->
                _vius.value = list

                if (_selectedViu.value == null && list.isNotEmpty()) {
                    selectViu(list.first().uid)
                }

                val selectedUid = _selectedViu.value?.uid
                if (selectedUid != null && list.none { it.uid == selectedUid }) {
                    selectViu(null)
                }
            }
        }
    }

    fun selectViu(uid: String?) {
        if (_selectedViu.value?.uid == uid && uid != null) return
        if (uid == null && _selectedViu.value == null) return

        viuJob?.cancel()
        permissionJob?.cancel()

        if (uid == null) {
            _selectedViu.value = null
            _isPrimary.value = false
            return
        }

        val currentCaregiverUid = getCurrentUserUidUseCase()

        if (currentCaregiverUid != null) {
            permissionJob = viewModelScope.launch {
                isPrimaryCaregiverUseCase(currentCaregiverUid, uid).collect { isPrimary ->
                    _isPrimary.value = isPrimary
                }
            }
        }

        viuJob = viewModelScope.launch {
            getViuByUidUseCase(uid).collect { viu ->
                _selectedViu.value = viu
            }
        }
    }

    fun onMapLongPress(latLng: LatLng) {
        if (_isPrimary.value) {
            _longPressedLatLng.value = latLng
        } else {
            _longPressedLatLng.value = null
        }
    }

    fun selectGeofence(geofence: Geofence) {
        _selectedGeofence.value = geofence
    }

    fun dismissGeofenceDetailsDialog() {
        _selectedGeofence.value = null
    }

    fun dismissAddGeofenceDialog() {
        _longPressedLatLng.value = null
    }
}