package edu.capstone.navisight.auth.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthException
import edu.capstone.navisight.auth.domain.ForgotPasswordUseCase
import edu.capstone.navisight.auth.domain.LoginUseCase
import edu.capstone.navisight.auth.model.LoginResult
import edu.capstone.navisight.auth.util.CaptchaHandler
import edu.capstone.navisight.auth.util.CaptchaState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val LOGIN_LOCKOUT_DURATION_MS = 5 * 60 * 1000L // 5 minutes

class LoginViewModel(
    private val loginUseCase: LoginUseCase = LoginUseCase(),
    private val forgotPasswordUseCase: ForgotPasswordUseCase = ForgotPasswordUseCase()
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _userCollection = MutableStateFlow<String?>(null)
    val userCollection: StateFlow<String?> = _userCollection

    private val _showCaptchaDialog = MutableStateFlow(false)
    val showCaptchaDialog: StateFlow<Boolean> = _showCaptchaDialog

    private val captchaHandler = CaptchaHandler()
    val captchaState: StateFlow<CaptchaState> = captchaHandler.captchaState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var loginFailedAttempts = 0
    private var loginLockoutEndTime: Long = 0


    fun login(email: String, password: String) {
        viewModelScope.launch {
            _error.value = null

            if (!checkLoginRequirements(email, password)) {
                return@launch
            }
            _isLoading.value = true
            try {
                checkNetwork(email, password)
            } finally {
                _isLoading.value = false
            }
        }
    }


    private suspend fun checkNetwork(email: String, password: String) {
        val genericErrorMessage = "Invalid email or password."

        try {
            when (val result = loginUseCase(email, password)) {
                is LoginResult.Success -> {
                    _userCollection.value = result.collection
                    loginFailedAttempts = 0
                    captchaHandler.resetCaptcha()
                }

                is LoginResult.Error -> {
                    if (result.message.contains("No internet connection")) {
                        _error.value = result.message
                    } else {
                        handleFailedLoginAttempt(genericErrorMessage)
                    }
                }

                is LoginResult.InvalidCredentials,
                is LoginResult.UserNotFoundInCollection -> {
                    handleFailedLoginAttempt(genericErrorMessage)
                }
            }
        } catch (e: Exception) {
            handleFailedLoginAttempt(genericErrorMessage)
        }
    }


    private fun checkLoginRequirements(email: String, password: String): Boolean {
        val validationError = validateInput(email, password)
        if (validationError != null) {
            _error.value = validationError
            return false
        }
        val captchaIsRequired = loginFailedAttempts >= 1
        val captchaIsSolved = captchaState.value.solved
        if (captchaIsRequired && !captchaIsSolved) {
            _error.value = "Please solve the CAPTCHA to continue."
            _showCaptchaDialog.value = true
            return false
        }

        return true 
    }


    fun submitCaptcha(input: String) {
        captchaHandler.submitCaptcha(input)
    }

    fun dismissCaptchaDialog() {
        _showCaptchaDialog.value = false
    }

    fun generateNewCaptcha() {
        captchaHandler.generateNewCaptcha()
    }

    fun resetCaptcha() {
        captchaHandler.resetCaptcha()
    }

    private fun handleFailedLoginAttempt(errorMessage: String) {
        loginFailedAttempts++
        if (loginFailedAttempts >= 5) {
            loginLockoutEndTime = System.currentTimeMillis() + LOGIN_LOCKOUT_DURATION_MS
            _error.value = "Too many failed attempts. Try again in 5 minutes."
        } else {
            _error.value = errorMessage
        }
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
    fun resetPassword(email: String) {
        viewModelScope.launch {
            _error.value = null
            _isLoading.value = true

            // Call Domain Layer
            val result = forgotPasswordUseCase(email)

            _isLoading.value = false

            result.fold(
                onSuccess = {
                    _error.value = "Reset link sent! Check your email."
                },
                onFailure = { exception ->
                    // Handle specific Firebase errors for better UX
                    if (exception is FirebaseAuthException) {
                        when (exception.errorCode) {
                            "ERROR_USER_NOT_FOUND" -> _error.value = "User not found."
                            "ERROR_INVALID_EMAIL" -> _error.value = "Invalid email format."
                            else -> _error.value = exception.message
                        }
                    } else {
                        _error.value = exception.message ?: "Error sending email"
                    }
                }
            )
        }
    }
    fun setError(message: String) {
        _error.value = message
    }
}