package edu.capstone.navisight.caregiver.ui.feature_editViuProfile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.PaddingValues

@Composable
internal fun SecuritySettingsCard(
    viewModel: ViuProfileViewModel,
    viuEmail: String,
    securityError: String?,
    emailResendTimer: Int
) {
    val context = LocalContext.current
    val emailFlowState by viewModel.emailFlowState.collectAsState()
    val isLoading = emailFlowState == SecurityFlowState.LOADING

    var newEmail by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }

    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(Color(0xFFB644F1), Color(0xFF6041EC))
    )
    val buttonShape = RoundedCornerShape(12.dp)

    LaunchedEffect(emailFlowState) {
        if (emailFlowState == SecurityFlowState.IDLE) {
            newEmail = ""
            otp = ""
        }
    }

    val timerText = remember(emailResendTimer) {
        if (emailResendTimer > 0) {
            val minutes = emailResendTimer / 60
            val seconds = emailResendTimer % 60
            String.format("%d:%02d", minutes, seconds)
        } else {
            ""
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Security Settings", style = MaterialTheme.typography.titleMedium)

            Text("Change VIU Contact Email", style = MaterialTheme.typography.titleSmall)
            Text(
                "Current: $viuEmail\nAn OTP will be sent to *your* (the caregiver's) email for verification.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            if (emailFlowState == SecurityFlowState.IDLE) {
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it; viewModel.clearSecurityError() },
                    label = { Text("New Contact Email") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                    singleLine = true,
                    readOnly = isLoading,
                    isError = securityError?.contains("email", ignoreCase = true) == true ||
                            securityError?.contains("resend", ignoreCase = true) == true, // --- ADDED ---
                    supportingText = {
                        // UPDATED
                        if (securityError != null &&
                            (securityError.contains("email", true) ||
                                    securityError.contains("resend", true) ||
                                    securityError.contains("wait", true))
                        ) {
                            Text(securityError!!, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                Button(
                    onClick = { viewModel.requestEmailChange(context, newEmail) },
                    enabled = !isLoading && newEmail.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    shape = buttonShape,
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                brush = gradientBrush,
                                shape = buttonShape
                            )
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) CircularProgressIndicator(
                            Modifier.size(24.dp),
                            color = Color.White // Make spinner white
                        )
                        else Text(
                            "Send Verification Code",
                            fontWeight = FontWeight.Bold,
                            color = Color.White // Make text white
                        )
                    }
                }
            }

            if (emailFlowState == SecurityFlowState.PENDING_OTP) {
                OutlinedTextField(
                    value = otp,
                    onValueChange = { if (it.length <= 6) otp = it; viewModel.clearSecurityError() },
                    label = { Text("6-Digit OTP") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    singleLine = true,
                    readOnly = isLoading,
                    isError = securityError != null &&
                            !securityError.contains("New OTP sent", ignoreCase = true), // --- UPDATED ---
                    supportingText = {
                        if (securityError != null) {
                            // UPDATED
                            val color = if (securityError.contains("New OTP sent", ignoreCase = true))
                                Color(0xFF006400) // Dark Green
                            else
                                MaterialTheme.colorScheme.error

                            Text(securityError!!, color = color)
                        }
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { viewModel.cancelEmailChangeFlow() },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    // Resend Button
                    TextButton(
                        onClick = { viewModel.resendEmailChangeOtp(context) },
                        enabled = !isLoading && emailResendTimer == 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        // UPDATED
                        when {
                            // 5-minute lockout is active
                            emailResendTimer > 60 -> Text("Limit ($timerText)")
                            // 1-minute resend wait is active
                            emailResendTimer > 0 -> Text("Resend ($timerText)")
                            // Ready to resend
                            else -> Text("Resend")
                        }
                    }
                    Button(
                        onClick = { viewModel.verifyEmailChange(otp) },
                        enabled = !isLoading && otp.length == 6,
                        modifier = Modifier.weight(2f) // Make verify button bigger
                    ) {
                        if (isLoading) CircularProgressIndicator(Modifier.size(24.dp))
                        else Text("Verify & Change") // Shortened text
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Change VIU Password", style = MaterialTheme.typography.titleSmall)
            Text(
                "A reset link will be sent to the VIU's email: $viuEmail",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Button(
                onClick = { viewModel.sendPasswordReset() },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                shape = buttonShape,
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = gradientBrush,
                            shape = buttonShape
                        )
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading && emailFlowState != SecurityFlowState.IDLE) {
                        CircularProgressIndicator(
                            Modifier.size(24.dp),
                            color = Color.White // Make spinner white
                        )
                    }
                    else Text(
                        "Send Password Reset Email",
                        fontWeight = FontWeight.Bold // Added for consistency
                    )
                }
            }
        }
    }
}