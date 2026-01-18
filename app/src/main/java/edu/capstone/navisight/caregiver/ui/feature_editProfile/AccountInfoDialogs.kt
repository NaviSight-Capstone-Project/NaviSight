package edu.capstone.navisight.caregiver.ui.feature_editProfile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

@Composable
internal fun SuccessDialog(
    message: String,
    onDismiss: () -> Unit
) {
    // Automatically dismiss after 2 seconds
    LaunchedEffect(Unit) {
        delay(2000)
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Success Icon Circle
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(color = Color(0xFF4CAF50), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Success!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
internal fun ChangeEmailDialog(onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var newEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Email", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter your new email and current password.", style = MaterialTheme.typography.bodySmall)

                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text("New Email") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Current Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newEmail, password) },
                enabled = newEmail.isNotBlank() && password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A5AE0)), // Brand Purple
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
internal fun ChangePasswordDialog(onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Change Password", fontWeight = FontWeight.Bold)
                Text(
                    text = "Set a new secure password for your account. You will need your old password to proceed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(currentPassword, newPassword) },
                enabled = currentPassword.isNotBlank() && newPassword.length >= 6,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A5AE0)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
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

                TextButton(
                    onClick = onResend,
                    enabled = canResend
                ) {
                    Text(
                        when {
                            // Cooldown is active
                            cooldownSeconds > 0 -> "Limit reached, try again later"
                            // Resend limit is hit (and no cooldown)
                            isResendLimitReached -> "Resend limit reached"
                            // 60s wait timer is active
                            resendWaitSeconds > 0 -> "Resend OTP in (${resendWaitSeconds} s)"
                            // Ready to resend
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
    errorMessage: String? = null, // Ensure this is being passed from the screen
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }

    // Check for hard blocks (Lockout or Firebase device block)
    val isHardBlocked = errorMessage?.contains("Locked", ignoreCase = true) == true ||
            errorMessage?.contains("activity", ignoreCase = true) == true
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Verify Identity") },
        text = {
            Column {
                Text("Please enter your current password to save these changes.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val image = if (passwordVisible)
                            Icons.Filled.Visibility
                        else Icons.Filled.VisibilityOff

                        val description = if (passwordVisible) "Hide password" else "Show password"

                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = description)
                        }
                    },
                    singleLine = true,
                    enabled = !isSaving && !isHardBlocked
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password) },
                // Disable if saving, password empty, or device/account is blocked
                enabled = !isSaving && password.isNotBlank() && !isHardBlocked
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
fun DeleteAccountConfirmDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    isDeleting: Boolean,
    errorMessage: String?
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = {
            Text(
                "Delete Account",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This action is permanent and cannot be undone. All your data and pairings will be wiped.")

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Enter Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(errorMessage, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    enabled = !isDeleting
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                enabled = password.isNotBlank() && !isDeleting
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Delete Forever")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting
            ) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}