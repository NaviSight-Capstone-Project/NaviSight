package edu.capstone.navisight.caregiver.ui.feature_scanqr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScanQrScreen(
    viewModel: ScanQrViewModel,
    onNavigateBack: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            successMessage?.let { message ->
                ScanResultDialog(
                    title = "Success",
                    message = message,
                    isError = false,
                    onDismiss = onNavigateBack
                )
            }

            errorMessage?.let { message ->
                ScanResultDialog(
                    title = "Error",
                    message = message,
                    isError = true,
                    onDismiss = { viewModel.resetState() }
                )
            }
        }
    }
}

@Composable
fun ScanResultDialog(
    title: String,
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onDismiss) {
                Text(text = if (isError) "Try Again" else "Done")
            }
        }
    }
}