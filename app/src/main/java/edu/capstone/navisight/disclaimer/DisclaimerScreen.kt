package edu.capstone.navisight.disclaimer

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.capstone.navisight.disclaimer.audioVisualizer.AudioWaveVisualizer
import edu.capstone.navisight.R

@Composable
fun DisclaimerScreen(
    volumeLevel: Float,
    onAgree: () -> Unit
) {
    val context = LocalContext.current

    // Checkbox states
    var readDisclaimer by remember { mutableStateOf(false) }
    var agreeTerms by remember { mutableStateOf(false) }

    // Button pulse animation
    val infiniteTransition = rememberInfiniteTransition()
    val buttonScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Background wave visual
        AudioWaveVisualizer(
            volumeLevel = volumeLevel,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x55FFFFFF))
        )

        // Foreground content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (visible) LogoSection()

            if (visible) DisclaimerText()

            if (visible) AgreementCheckboxes(
                readDisclaimer = readDisclaimer,
                onReadDisclaimerChange = { readDisclaimer = it },
                agreeTerms = agreeTerms,
                onAgreeTermsChange = { agreeTerms = it }
            )

            if (visible) ContinueButton(
                readDisclaimer = readDisclaimer,
                agreeTerms = agreeTerms,
                buttonScale = buttonScale,
                onClick = {
                    if (readDisclaimer && agreeTerms) {
                        onAgree()
                    } else {
                        Toast.makeText(
                            context,
                            "Please check both boxes to continue.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }
    }
}

@Composable
fun LogoSection() {
    Image(
        painter = painterResource(id = R.drawable.ic_logo),
        contentDescription = "NaviSight Logo",
        modifier = Modifier
            .size(100.dp)
            .padding(top = 16.dp),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun DisclaimerText() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 16.dp)
    ) {
        Text(
            text = "DISCLAIMER",
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = Color.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = "NaviSight is a supportive tool and not a substitute for complete visual aids. " +
                    "By using this app, you agree not to rely on it solely. " +
                    "The NaviSight team is not liable for any damages or accidents.",
            fontSize = 16.sp,
            color = Color.Black,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun AgreementCheckboxes(
    readDisclaimer: Boolean,
    onReadDisclaimerChange: (Boolean) -> Unit,
    agreeTerms: Boolean,
    onAgreeTermsChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = readDisclaimer,
                onCheckedChange = onReadDisclaimerChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color.Black,
                    uncheckedColor = Color.Black
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "I have read the disclaimer",
                color = Color.Black,
                fontSize = 16.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = agreeTerms,
                onCheckedChange = onAgreeTermsChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color.Black,
                    uncheckedColor = Color.Black
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "I agree to the Terms and Conditions",
                color = Color.Black,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun ContinueButton(
    readDisclaimer: Boolean,
    agreeTerms: Boolean,
    buttonScale: Float,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .scale(buttonScale)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color(0xFF6041EC),
                spotColor = Color(0xFFB644F1)
            )
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFB644F1), Color(0xFF6041EC))
                ),
                shape = RoundedCornerShape(28.dp)
            )
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Continue",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
