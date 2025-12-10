package edu.capstone.navisight.caregiver.ui.emergency

import android.graphics.Color
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EmergencySignal(
    val viuId: String,
    val viuName: String,
    val lastLocation: String,
    val timestamp: Long
)


class EmergencyViewModel : ViewModel() {
    private val _emergencySignal = MutableStateFlow<EmergencySignal?>(null)
    val emergencySignal = _emergencySignal.asStateFlow()
    private val _callRequest = MutableSharedFlow<Pair<String, Boolean>>()
    val callRequest: SharedFlow<Pair<String, Boolean>> = _callRequest.asSharedFlow()

    fun activateEmergency(signal: EmergencySignal) {
        Log.d("EmergencyDialog", " triggerring emergency view model activatemergency")
        _emergencySignal.value = signal
    }

    fun clearEmergency() {
        _emergencySignal.value = null
    }

    fun turnOffEmergencyOnViuSide(uid: String) {
        ViuEmergencyDataSource.removeUserEmergencyActivated(uid)
        clearEmergency()
    }

    fun handleEmergencyResponse(response: String, signal: EmergencySignal) {
        _emergencySignal.value = null
        Log.d("EmergencyViewModel", "User responded: '$response' for VIU ${signal.viuId} (${signal.viuName}).")

        when (response) {
            EMERGENCY_OPTION_TURN_OFF_VCV -> {
                turnOffEmergencyOnViuSide(signal.viuId)
                viewModelScope.launch {
                    _callRequest.emit(Pair(signal.viuId, true)) // Video call
                }
            }
            EMERGENCY_OPTION_TURN_OFF_ACV -> {
                turnOffEmergencyOnViuSide(signal.viuId)
                viewModelScope.launch {
                    _callRequest.emit(Pair(signal.viuId, false)) // Audio call
                }
            }
            EMERGENCY_OPTION_TURN_OFF -> {
                turnOffEmergencyOnViuSide(signal.viuId)
            }
            EMERGENCY_OPTION_DISMISS -> {
                Log.w("EmergencyVM", "Response '$response' not handled. Clearing alert.")
                clearEmergency()
            }
        }
    }
}