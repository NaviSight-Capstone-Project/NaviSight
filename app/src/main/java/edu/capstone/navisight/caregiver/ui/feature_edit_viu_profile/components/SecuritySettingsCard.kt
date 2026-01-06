package edu.capstone.navisight.caregiver.ui.feature_edit_viu_profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import edu.capstone.navisight.caregiver.ui.feature_edit_viu_profile.EditViuProfileViewModel
import edu.capstone.navisight.caregiver.ui.feature_edit_viu_profile.SecurityFlowState

@Composable
fun SecuritySettingsCard(
    viewModel: EditViuProfileViewModel,
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

    // Reset local state when flow resets
    LaunchedEffect(emailFlowState) {
        if (emailFlowState == SecurityFlowState.IDLE) {
            newEmail = ""
            otp = ""
        }
    }

    val timerText = remember(emailResendTimer) {
        if (emailResendTimer > 0) String.format("%d:%02d", emailResendTimer / 60, emailResendTimer % 60)
        else ""
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                "Current: $viuEmail\nAn OTP will be sent to your companion's email for verification.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            // STATE 1: INPUT NEW EMAIL
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
                            securityError?.contains("resend", ignoreCase = true) == true,
                    supportingText = {
                        if (securityError != null && (securityError.contains("email", true) || securityError.contains("resend", true))) {
                            Text(securityError, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                Button(
                    onClick = { viewModel.requestEmailChange(context, newEmail) },
                    enabled = !isLoading && newEmail.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = buttonShape,
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier.background(brush = gradientBrush, shape = buttonShape).fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                        else Text("Send Verification Code", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // STATE 2: ENTER OTP
            if (emailFlowState == SecurityFlowState.PENDING_OTP) {
                OutlinedTextField(
                    value = otp,
                    onValueChange = { if (it.length <= 6) otp = it; viewModel.clearSecurityError() },
                    label = { Text("6-Digit OTP") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    singleLine = true,
                    readOnly = isLoading,
                    isError = securityError != null && !securityError.contains("New OTP sent", ignoreCase = true),
                    supportingText = {
                        if (securityError != null) {
                            Text(
                                securityError,
                                color = if (securityError.contains("New OTP sent", true)) Color(0xFF006400) else MaterialTheme.colorScheme.error
                            )
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
                    ) { Text("Cancel") }

                    TextButton(
                        onClick = { viewModel.resendEmailChangeOtp(context) },
                        enabled = !isLoading && emailResendTimer == 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (emailResendTimer > 0) "Wait ($timerText)" else "Resend")
                    }

                    Button(
                        onClick = { viewModel.verifyEmailChange(otp) },
                        enabled = !isLoading && otp.length == 6,
                        modifier = Modifier.weight(2f)
                    ) {
                        if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                        else Text("Verify")
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // PASSWORD RESET SECTION
            Text("Change VIU Password", style = MaterialTheme.typography.titleSmall)
            Text(
                "A reset link will be sent to the VIU's email: $viuEmail",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Button(
                onClick = { viewModel.sendPasswordReset() },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = buttonShape,
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier.background(brush = gradientBrush, shape = buttonShape).fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading && emailFlowState != SecurityFlowState.IDLE) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("Send Password Reset Email", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}