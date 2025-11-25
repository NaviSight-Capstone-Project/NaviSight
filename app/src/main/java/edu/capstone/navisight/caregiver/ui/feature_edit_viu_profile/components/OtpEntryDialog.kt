package edu.capstone.navisight.caregiver.ui.feature_edit_viu_profile.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun OtpEntryDialog(
    isLoading: Boolean,
    error: String?,
    resendCooldownSeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onClearError: () -> Unit,
    onResend: (() -> Unit)? = null
) {
    var otp by remember { mutableStateOf("") }

    // Check if error is actually a success message (hacky but functional based on VM logic)
    val isSuccessMessage = error != null && error.contains("New OTP sent", ignoreCase = true)
    val isError = error != null && !isSuccessMessage

    val timerText = remember(resendCooldownSeconds) {
        if (resendCooldownSeconds > 0) {
            val minutes = resendCooldownSeconds / 60
            val seconds = resendCooldownSeconds % 60
            String.format("%d:%02d", minutes, seconds)
        } else { "" }
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
                        if (error != null) onClearError()
                    },
                    label = { Text("6-Digit OTP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.padding(top = 16.dp),
                    readOnly = isLoading,
                    isError = isError,
                    supportingText = {
                        if (error != null) {
                            Text(
                                text = error,
                                color = if (isSuccessMessage) Color(0xFF006400) else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )

                if (onResend != null) {
                    TextButton(
                        onClick = onResend,
                        enabled = !isLoading && resendCooldownSeconds == 0,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            when {
                                resendCooldownSeconds > 60 -> "Limit reached ($timerText)"
                                resendCooldownSeconds > 0 -> "Resend in $timerText"
                                else -> "Resend OTP"
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(otp) },
                enabled = !isLoading && otp.length == 6
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
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