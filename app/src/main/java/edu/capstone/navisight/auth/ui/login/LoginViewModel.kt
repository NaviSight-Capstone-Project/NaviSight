package edu.capstone.navisight.auth.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.capstone.navisight.auth.model.LoginRequest
import edu.capstone.navisight.auth.domain.LoginUseCase
import edu.capstone.navisight.auth.util.CaptchaHandler
import edu.capstone.navisight.auth.util.CaptchaState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val LOGIN_LOCKOUT_DURATION_MS = 5 * 60 * 1000L // 5 minutes

class LoginViewModel(
    private val loginUseCase: LoginUseCase = LoginUseCase()
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginRequest?>(null)
    val loginState: StateFlow<LoginRequest?> = _loginState

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _userCollection = MutableStateFlow<String?>(null)
    val userCollection: StateFlow<String?> = _userCollection

    private val captchaHandler = CaptchaHandler()
    val captchaState: StateFlow<CaptchaState> = captchaHandler.captchaState

    private var loginFailedAttempts = 0
    private var loginLockoutEndTime: Long = 0

    // The UI will now only call this function AFTER the CAPTCHA is solved.
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
                    // Login Success
                    _loginState.value = user
                    loginFailedAttempts = 0 // Reset failed attempts
                    val collection = loginUseCase.getUserCollection(user.uid)
                    if (collection != null) {
                        _userCollection.value = collection
                    } else {
                        _error.value = "User not found in any collection."
                    }
                } else {
                    // --- Login Failed (Wrong credentials) ---
                    handleFailedLoginAttempt("Invalid email or password.")
                }
            } catch (e: Exception) {
                // --- Login Failed (Exception) ---
                handleFailedLoginAttempt("An error occurred: ${e.message}")
            }
        }
    }

    fun submitCaptcha(input: String) {
        captchaHandler.submitCaptcha(input)
    }

    fun generateNewCaptcha() {
        captchaHandler.generateNewCaptcha()
    }

    // This is for when the user dismisses the dialog.
    fun resetCaptcha() {
        captchaHandler.resetCaptcha()
    }


    // --- Private Helper Functions ---
    private fun handleFailedLoginAttempt(errorMessage: String) {
        loginFailedAttempts++
        if (loginFailedAttempts >= 5) {
            loginLockoutEndTime = System.currentTimeMillis() + LOGIN_LOCKOUT_DURATION_MS
            _error.value = "Too many failed attempts. Try again in 5 minutes."
        } else {
            _error.value = errorMessage
        }
        // Force user to re-solve CAPTCHA
        captchaHandler.resetCaptcha(keepRefreshCount = true)
    }

    private fun isLoginLockedOut(): Boolean {
        return System.currentTimeMillis() < loginLockoutEndTime
    }

    private fun validateInput(email: String, password: String): String? {
        if (isLoginLockedOut()) {
            val remaining = (loginLockoutEndTime - System.currentTimeMillis()) / 1000 / 60 + 1
            return "Too many attempts. Please try again in $remaining minute(s)."
        }
        if (email.isBlank() || password.isBlank()) return "Email and password cannot be empty."
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
        if (!emailRegex.matches(email)) return "Invalid email format."
        if (password.length !in 6..64) return "Password must be 6–64 characters."
        val unsafeChars = listOf(" ", ";", "'", "\"", "--")
        if (unsafeChars.any { email.contains(it) || password.contains(it) }) return "Inputs contain invalid characters."
        return null
    }
}