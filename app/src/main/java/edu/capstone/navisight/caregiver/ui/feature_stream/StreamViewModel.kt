package edu.capstone.navisight.caregiver.ui.feature_stream

import androidx.lifecycle.ViewModel
import edu.capstone.navisight.caregiver.model.Caregiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class StreamViewModel () : ViewModel() {

    private val _profile = MutableStateFlow<Caregiver?>(null)
    val profile: StateFlow<Caregiver?> = _profile

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _success = MutableStateFlow<Boolean>(false)
    val success: StateFlow<Boolean> = _success
}