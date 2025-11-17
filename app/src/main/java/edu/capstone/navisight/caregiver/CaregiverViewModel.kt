package edu.capstone.navisight.caregiver

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CaregiverHomeViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val authStateListener: FirebaseAuth.AuthStateListener

    private val _isSessionValid = MutableStateFlow(auth.currentUser != null)
    val isSessionValid: StateFlow<Boolean> = _isSessionValid.asStateFlow()

    private val _currentScreenIndex = MutableStateFlow(0)
    val currentScreenIndex: StateFlow<Int> = _currentScreenIndex.asStateFlow()

    init {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _isSessionValid.value = (user != null)
        }
        auth.addAuthStateListener(authStateListener)
    }

    fun logout() {
        auth.signOut()
    }

    fun onScreenSelected(index: Int) {
        _currentScreenIndex.value = index
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
    }
}