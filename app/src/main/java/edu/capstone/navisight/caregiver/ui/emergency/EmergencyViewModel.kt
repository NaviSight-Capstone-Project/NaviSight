package edu.capstone.navisight.caregiver.ui.emergency

import android.graphics.Color
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EmergencySignal(
    val viuId: String,
    val viuName: String,
    val lastLocation: String
)


class EmergencyViewModel : ViewModel() {
    private val _emergencySignal = MutableStateFlow<EmergencySignal?>(null)
    val emergencySignal = _emergencySignal.asStateFlow()

    fun activateEmergency(signal: EmergencySignal) {
        Log.d("EmergencyDialog", " triggerring emergency view model activatemergency")
        _emergencySignal.value = signal
    }

    fun clearEmergency() {
        _emergencySignal.value = null
    }
}