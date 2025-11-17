package edu.capstone.navisight.caregiver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CaregiverHomeViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Session Management
    private val _isSessionValid = MutableStateFlow(true)
    val isSessionValid: StateFlow<Boolean> = _isSessionValid.asStateFlow()

    //  Navigation State
    private val _currentScreenIndex = MutableStateFlow(0) // Default to index 0 (Track)
    val currentScreenIndex: StateFlow<Int> = _currentScreenIndex.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            if (auth.currentUser == null) {
                _isSessionValid.value = false
            }
        }
    }

    fun logout() {
        auth.signOut()
        _isSessionValid.value = false
    }

    /**
     * Called by the BottomNavigationBar when a new item is selected.
     */
    fun onScreenSelected(index: Int) {
        _currentScreenIndex.value = index
    }
}