package edu.capstone.navisight.caregiver.ui.feature_edit_viu_profile.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun PasswordEntryDialog(
    title: String,
    description: String,
    errorMessage: String? = null, // Add this parameter
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    // Check if the user is actually locked out based on the message content
    val isLockedOut = errorMessage?.contains("Locked", ignoreCase = true) == true

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
                    readOnly = isLoading,
                    isError = !errorMessage.isNullOrBlank()
                )
                // Display brute-force warning/error
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password) },
                enabled = !isLoading && password.isNotBlank() && !isLockedOut
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
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