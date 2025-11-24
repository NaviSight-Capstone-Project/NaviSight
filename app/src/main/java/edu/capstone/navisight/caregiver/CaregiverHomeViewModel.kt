package edu.capstone.navisight.caregiver

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CaregiverHomeViewModel : ViewModel() {

    private val _currentScreenIndex = MutableStateFlow(0)
    val currentScreenIndex: StateFlow<Int> = _currentScreenIndex.asStateFlow()

    fun onScreenSelected(index: Int) {
        _currentScreenIndex.value = index
    }
}