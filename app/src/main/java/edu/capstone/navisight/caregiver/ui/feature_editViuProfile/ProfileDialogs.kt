package edu.capstone.navisight.caregiver.ui.feature_editViuProfile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
internal fun PasswordEntryDialog(
    title: String,
    description: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(title) },
        text = {
            Column {
                Text(description)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.padding(top = 16.dp),
                    readOnly = isLoading
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password) },
                enabled = !isLoading && password.isNotBlank()
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(24.dp))
                else Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}

@Composable
internal fun OtpEntryDialog(
    isLoading: Boolean,
    error: String?,
    resendCooldownSeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onClearError: () -> Unit,
    onResend: (() -> Unit)? = null // Optional resend function
) {
    var otp by remember { mutableStateOf("") }

    // Show a specific color for "New OTP sent"
    val isError = error != null && !error.contains("New OTP sent", ignoreCase = true)
    val isSuccessMessage = error != null && error.contains("New OTP sent", ignoreCase = true)

    val timerText = remember(resendCooldownSeconds) {
        if (resendCooldownSeconds > 0) {
            val minutes = resendCooldownSeconds / 60
            val seconds = resendCooldownSeconds % 60
            String.format("%d:%02d", minutes, seconds)
        } else {
            ""
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Enter Verification Code") },
        text = {
            Column {
                Text("A 6-digit OTP was sent to your email. Please enter it below.")
                OutlinedTextField(
                    value = otp,
                    onValueChange = {
                        if (it.length <= 6) otp = it
                        if (error != null) onClearError() // Clears error on new input
                    },
                    label = { Text("6-Digit OTP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.padding(top = 16.dp),
                    readOnly = isLoading,
                    // --- UPDATED ---
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text(error!!, color = MaterialTheme.colorScheme.error)
                        } else if (isSuccessMessage) {
                            Text(error!!, color = Color(0xFF006400)) // Dark Green
                        }
                    }
                )

                if (onResend != null) {
                    TextButton(
                        onClick = onResend,
                        enabled = !isLoading && resendCooldownSeconds == 0,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        when {
                            // 5-minute lockout is active
                            resendCooldownSeconds > 60 -> Text("Limit reached ($timerText)")
                            // 1-minute resend wait is active
                            resendCooldownSeconds > 0 -> Text("Resend in $timerText")
                            // Ready to resend
                            else -> Text("Resend OTP")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(otp) },
                enabled = !isLoading && otp.length == 6
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(24.dp))
                else Text("Verify & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}