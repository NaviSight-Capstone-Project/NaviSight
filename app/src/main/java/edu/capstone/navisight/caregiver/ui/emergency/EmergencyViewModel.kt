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
    val lastLocation: String,
    val timestamp: Long
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

    fun handleEmergencyResponse(response: String, signal: EmergencySignal) {
        _emergencySignal.value = null
        Log.d("EmergencyViewModel", "User responded: '$response' for VIU ${signal.viuId} (${signal.viuName}).")

        // TODO: MOVE THIS THING TO  A REPOSITORY JESUS CHRIST
        when (response) {
            "Yes, I understand!" -> {
                // Placeholder for simple backend acknowledgement
                Log.i("EmergencyVM", "ALERT ACKNOWLEDGED. VIU: ${signal.viuName}. TODO: Implement backend call here.")
            }
            "Call me immediately" -> {
                // Placeholder for complex action
                Log.w("EmergencyVM", "CALL RESPONSE: Call requested for ${signal.viuName}. TODO: Launch call intent from Fragment.")
            }
            // Add other response strings here...
            else -> {
                Log.w("EmergencyVM", "Response '$response' not handled. Clearing alert.")
            }
        }
    }
}