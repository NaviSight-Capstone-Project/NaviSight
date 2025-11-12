package edu.capstone.navisight.auth.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.capstone.navisight.auth.model.LoginRequest
import edu.capstone.navisight.auth.domain.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase = LoginUseCase()
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginRequest?>(null)
    val loginState: StateFlow<LoginRequest?> = _loginState

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _userCollection = MutableStateFlow<String?>(null)
    val userCollection: StateFlow<String?> = _userCollection

    fun login(email: String, password: String) {
        viewModelScope.launch {
            val validationError = validateInput(email, password)
            if (validationError != null) {
                _error.value = validationError
                return@launch
            }

            try {
                val user = loginUseCase(email, password)
                if (user != null) {
                    _loginState.value = user
                    val collection = loginUseCase.getUserCollection(user.uid)
                    if (collection != null) {
                        _userCollection.value = collection
                    } else {
                        _error.value = "User not found in any collection."
                    }
                } else {
                    _error.value = "Invalid email or password."
                }
            } catch (e: Exception) {
                _error.value = "An error occurred: ${e.message}"
            }
        }
    }

    private fun validateInput(email: String, password: String): String? {
        if (email.isBlank() || password.isBlank()) return "Email and password cannot be empty."
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
        if (!emailRegex.matches(email)) return "Invalid email format."
        if (password.length !in 6..64) return "Password must be 6–64 characters."
        val unsafeChars = listOf(" ", ";", "'", "\"", "--")
        if (unsafeChars.any { email.contains(it) || password.contains(it) }) return "Inputs contain invalid characters."
        return null
    }
}

