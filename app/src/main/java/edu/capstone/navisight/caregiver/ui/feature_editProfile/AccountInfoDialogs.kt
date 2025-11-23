package edu.capstone.navisight.caregiver.ui.feature_editProfile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
internal fun ChangeEmailDialog(onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var newEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Email") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = {
                        newEmail = it
                        error = if (!android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches())
                            "Invalid email format"
                        else null
                    },
                    label = { Text("New Email") },
                    isError = error != null,
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Current Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    singleLine = true
                )

                if (error != null)
                    Text(error!!, color = Color.Red, fontSize = MaterialTheme.typography.labelSmall.fontSize)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (newEmail.isNotBlank() && password.isNotBlank() && error == null)
                    onConfirm(newEmail, password)
            }) { Text("Update") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun SectionTitle(text: String) {
    Text(text, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
}

@Composable
internal fun ValidatedField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            isError = error != null,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(12.dp)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color(0xFF6A5AE0)
            )
        )
        if (error != null)
            Text(
                error,
                color = Color.Red,
                fontSize = MaterialTheme.typography.labelSmall.fontSize
            )
    }
}

@Composable
internal fun ChangePasswordDialog(onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password") },
        text = {
            Column {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    isError = error != null,
                    singleLine = true
                )
                if (error != null) Text(error!!, color = Color.Red, fontSize = MaterialTheme.typography.labelSmall.fontSize)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (newPassword.length < 8 || !newPassword.any { it.isDigit() }) {
                    error = "Min 8 chars, at least 1 number"
                } else {
                    error = null
                    onConfirm(currentPassword, newPassword)
                }
            }) { Text("Update") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
@Composable
internal fun OtpVerificationDialog(
    title: String,
    triesLeft: Int,
    backendError: String?, // For "Invalid OTP"
    resendWaitSeconds: Int, // The 60s timer
    cooldownSeconds: Int, // The 5-min timer
    isResendLimitReached: Boolean, // true if resendCount >= 3
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    onResend: () -> Unit,
    onClearError: () -> Unit
) {
    var otp by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) } // For 6-digit check

    val displayError = backendError ?: localError

    // Button is enabled ONLY if 60s wait is 0 AND 5-min cooldown is 0 AND limit is not reached
    val canResend = resendWaitSeconds == 0 && cooldownSeconds == 0 && !isResendLimitReached

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Enter the 6-digit OTP sent to your email address.")

                OutlinedTextField(
                    value = otp,
                    onValueChange = {
                        if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                            otp = it
                            localError = null // Clear local error
                            onClearError() // Clear backend error
                        }
                        localError = if (it.length < 6 && it.isNotEmpty()) "OTP must be 6 digits" else null
                    },
                    label = { Text("OTP Code") },
                    isError = displayError != null,
                    singleLine = true,
                    supportingText = {
                        if (displayError != null) {
                            Text(displayError, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                Text(
                    "Tries left: $triesLeft",
                    color = if (triesLeft == 1) Color.Red else Color.Gray,
                    fontSize = MaterialTheme.typography.labelSmall.fontSize
                )

                // --- NEW RESEND TEXT LOGIC ---
                TextButton(
                    onClick = onResend,
                    enabled = canResend
                ) {
                    Text(
                        when {
                            // Priority 1: Cooldown is active
                            cooldownSeconds > 0 -> "Limit reached, try again later" // <-- CHANGED
                            // Priority 2: Resend limit is hit (and no cooldown)
                            isResendLimitReached -> "Resend limit reached"
                            // Priority 3: 60s wait timer is active
                            resendWaitSeconds > 0 -> "Resend OTP in (${resendWaitSeconds} s)"
                            // Default: Ready to resend
                            else -> "Resend OTP"
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (otp.length == 6) {
                    onConfirm(otp)
                    otp = "" // Clear field after confirm
                } else {
                    localError = "OTP must be 6 digits" // Set local error
                }
            }) { Text("Verify") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun ReauthenticationDialog(
    isSaving: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Verify Identity") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Please enter your current password to save changes.")
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Current Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    singleLine = true,
                    enabled = !isSaving
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (password.isNotBlank()) {
                        onConfirm(password)
                    }
                },
                enabled = !isSaving
            ) {
                Text(if (isSaving) "Verifying..." else "Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )
}