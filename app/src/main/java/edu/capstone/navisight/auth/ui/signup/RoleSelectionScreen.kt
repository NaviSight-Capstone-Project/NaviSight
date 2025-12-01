package edu.capstone.navisight.auth.ui.signup

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.capstone.navisight.R

@Composable
fun RoleSelectionScreen(
    onCaregiverClicked: () -> Unit,
    onViuClicked: () -> Unit,
    onBackToLogin: () -> Unit
) {
    // Background Animation
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1080f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse),
        label = "offsetX"
    )
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1920f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse),
        label = "offsetY"
    )

    // Gradients
    val gradientTeal = Brush.radialGradient(
        listOf(Color(0xFF77F7ED).copy(0.5f), Color(0xFFD9D9D9).copy(0.05f)),
        center = Offset(offsetX * 0.6f + 100f, offsetY * 0.4f + 50f),
        radius = 900f
    )
    val gradientPurple = Brush.radialGradient(
        listOf(Color(0xFFB446F2).copy(0.5f), Color(0xFFD9D9D9).copy(0.05f)),
        center = Offset(1080f - offsetX * 0.7f - 150f, 1920f - offsetY * 0.6f - 100f),
        radius = 1000f
    )
    val textColor = Color(0xFF4A4A4A)
    val focusedColor = Color(0xFF6041EC)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color.White, Color(0xFFF8F8F8)))),
        contentAlignment = Alignment.Center
    ) {
        // Animated Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientTeal)
                .background(gradientPurple)
        )

        // Main Layout Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.weight(0.5f))

            // Logo
            Image(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = "Logo",
                modifier = Modifier.size(200.dp, 100.dp)
            )

            // Gap between Logo and Text
            Spacer(Modifier.height(64.dp))

            // Updated Text
            Text(
                text = "Choose your account type",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Gap between Text and Buttons
            Spacer(Modifier.height(32.dp))

            // Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                RoleSelectionCard(
                    text = "Caregiver",
                    imageRes = R.drawable.avatar_caregiver,
                    textColor = textColor,
                    onClick = onCaregiverClicked,
                    modifier = Modifier.weight(1f)
                )

                RoleSelectionCard(
                    text = "Visually Impaired",
                    imageRes = R.drawable.avatar_viu,
                    textColor = textColor,
                    onClick = onViuClicked,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.weight(1f))

            // Back Button
            TextButton(
                onClick = onBackToLogin,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Back to Login",
                    color = focusedColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// Square Button Component
@Composable
private fun RoleSelectionCard(
    text: String,
    imageRes: Int,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = text,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}