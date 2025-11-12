package edu.capstone.navisight.auth.ui.signup.caregiver

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverOtpScreen(
    viewModel: CaregiverSignupViewModel,
    uid: String,
    onVerificationSuccess: () -> Unit,
    onCancelSignup: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var otpValue by remember { mutableStateOf("") }

    val gradientStart = Color(0xFF78E4EF)
    val gradientEnd = Color(0xFF6342ED)
    val gradientStartButton = Color(0xFFAA41E5)
    val focusedColor = Color(0xFF6641EC)
    val unfocusedColor = Color.Black

    val customTextFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedTextColor = unfocusedColor,
        unfocusedTextColor = unfocusedColor.copy(alpha = 0.9f),
        focusedIndicatorColor = focusedColor,
        unfocusedIndicatorColor = unfocusedColor.copy(alpha = 0.5f),
        focusedLabelColor = focusedColor,
        unfocusedLabelColor = unfocusedColor.copy(alpha = 0.7f),
        cursorColor = focusedColor,
        errorContainerColor = Color.White,
        errorIndicatorColor = Color.Red,
        errorLabelColor = Color.Red
    )

    LaunchedEffect(uiState.verificationSuccess) {
        if (uiState.verificationSuccess) {
            Toast.makeText(context, uiState.successMessage ?: "Verification Successful!", Toast.LENGTH_LONG).show()
            onVerificationSuccess()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.horizontalGradient(listOf(gradientStart, gradientEnd)))
    ) {
        // Back Button
        IconButton(onClick = onCancelSignup, modifier = Modifier.padding(top = 9.dp, start = 8.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        // White Card
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .align(Alignment.Center)
                .background(Color.White, RoundedCornerShape(24.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Email Verification",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = focusedColor
            )
            Spacer(Modifier.height(16.dp))

            Text(
                "A 6-digit verification code has been sent to your email.",
                textAlign = TextAlign.Center,
                color = unfocusedColor.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = otpValue,
                onValueChange = {
                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                        otpValue = it
                    }
                },
                label = { Text("Enter 6-Digit OTP") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = customTextFieldColors,
                singleLine = true
            )
            Spacer(Modifier.height(24.dp))

            // Gradient Verify Button
            Button(
                onClick = {
                    if (otpValue.length == 6) {
                        viewModel.verifyOtp(uid, otpValue)
                    }
                },
                enabled = !uiState.isLoading && otpValue.length == 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(gradientStartButton, gradientEnd)),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Verify", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            val timer = uiState.resendTimer
            val isTimerActive = timer > 0

            TextButton(
                onClick = { viewModel.resendOtp(context, uid) },
                enabled = !isTimerActive && !uiState.isLoading
            ) {
                Text(
                    text = if (isTimerActive) "Resend in $timer sec" else "Didn't receive code? Resend",
                    color = if (isTimerActive) Color.Gray else focusedColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}