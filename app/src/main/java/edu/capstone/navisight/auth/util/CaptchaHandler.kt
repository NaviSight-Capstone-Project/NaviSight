package edu.capstone.navisight.auth.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val MAX_CAPTCHA_ATTEMPTS = 5
private const val MAX_CAPTCHA_REFRESHES = 5
private const val CAPTCHA_LOCKOUT_DURATION_MS = 5 * 60 * 1000L // 5 minutes

//Represents the state of the custom CAPTCHA.
data class CaptchaState(
    val text: String = "",
    val solved: Boolean = false,
    val attemptsFailed: Int = 0,
    val refreshesUsed: Int = 0,
    val lockoutEndTime: Long = 0L,
    val error: String? = null
)


//Manages the state and logic for the custom CAPTCHA.
class CaptchaHandler {

    private val _captchaState = MutableStateFlow(CaptchaState())
    val captchaState: StateFlow<CaptchaState> = _captchaState.asStateFlow()

    init {
        generateNewCaptcha()
    }

    //Checks if the CAPTCHA is currently in a solved state.
    fun isSolved(): Boolean = _captchaState.value.solved

    /**
     * Generates a new CAPTCHA challenge.
     */
    fun generateNewCaptcha() {
        _captchaState.update { currentState ->
            if (System.currentTimeMillis() < currentState.lockoutEndTime) {
                return@update currentState.copy(error = "Try again in a few minutes.")
            }
            if (currentState.refreshesUsed >= MAX_CAPTCHA_REFRESHES) {
                return@update currentState.copy(error = "Max refreshes reached.")
            }
            currentState.copy(
                text = createRandomString(6),
                solved = false,
                attemptsFailed = 0,
                refreshesUsed = currentState.refreshesUsed + 1,
                error = null
            )
        }
    }

    //Submits a user's attempt to solve the CAPTCHA.
    fun submitCaptcha(userInput: String) {
        _captchaState.update { currentState ->
            if (System.currentTimeMillis() < currentState.lockoutEndTime) {
                return@update currentState.copy(error = "Too many attempts. Try again later.")
            }
            if (userInput.equals(currentState.text, ignoreCase = true)) {
                return@update currentState.copy(
                    solved = true,
                    error = null,
                    attemptsFailed = 0
                )
            }

            val newAttempts = currentState.attemptsFailed + 1
            if (newAttempts >= MAX_CAPTCHA_ATTEMPTS) {
                return@update currentState.copy(
                    solved = false,
                    error = "Too many failed attempts. Locked out for 5 minutes.",
                    attemptsFailed = newAttempts,
                    lockoutEndTime = System.currentTimeMillis() + CAPTCHA_LOCKOUT_DURATION_MS
                )
            } else {
                return@update currentState.copy(
                    solved = false,
                    error = "Incorrect. Try again. (${MAX_CAPTCHA_ATTEMPTS - newAttempts} attempts left)",
                    attemptsFailed = newAttempts
                )
            }
        }
    }

    //Resets the CAPTCHA state, typically after a failed login.
    fun resetCaptcha(keepRefreshCount: Boolean = false) {
        _captchaState.update { currentState ->
            val refreshes = if (keepRefreshCount) currentState.refreshesUsed else 0
            if (refreshes >= MAX_CAPTCHA_REFRESHES) {
                currentState.copy(
                    text = "---",
                    solved = false,
                    attemptsFailed = 0,
                    error = "Max refreshes reached. Please restart the app."
                )
            } else {
                currentState.copy(
                    text = createRandomString(6),
                    solved = false,
                    attemptsFailed = 0,
                    refreshesUsed = refreshes + 1,
                    error = "Please re-verify.",
                    lockoutEndTime = 0L
                )
            }
        }
    }

    //Creates a random alphanumeric string.
    private fun createRandomString(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
}