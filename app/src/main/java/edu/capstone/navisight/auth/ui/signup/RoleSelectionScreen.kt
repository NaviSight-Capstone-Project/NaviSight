package edu.capstone.navisight.auth.ui.signup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RoleSelectionScreen(
    onCaregiverClicked: () -> Unit,
    onViuClicked: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val gradientStart = Color(0xFF78E4EF)
    val gradientEnd = Color(0xFF6342ED)
    val gradientStartButton = Color(0xFFAA41E5)
    val focusedColor = Color(0xFF6641EC)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.horizontalGradient(listOf(gradientStart, gradientEnd)))
    ) {
        // Back Button
        IconButton(onClick = onBackToLogin, modifier = Modifier.padding(top = 9.dp, start = 8.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Login",
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Join NaviSight",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = focusedColor
            )
            Spacer(Modifier.height(16.dp))

            Text(
                "To get started, please tell us who you are.",
                textAlign = TextAlign.Center,
                color = Color.Black.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(32.dp))

            // Caregiver Button
            Button(
                onClick = onCaregiverClicked,
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
                    Text(
                        "I'm a Caregiver",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // VIU Button
            OutlinedButton(
                onClick = onViuClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = focusedColor
                ),
                border = BorderStroke(2.dp, focusedColor)
            ) {
                Text(
                    "I'm a Visually Impaired User",
                    color = focusedColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}