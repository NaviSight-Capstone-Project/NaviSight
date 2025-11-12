package edu.capstone.navisight.auth.ui.login

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.util.Locale

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onNavigateToViuHome: () -> Unit,
    onNavigateToCaregiverHome: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showCaptchaDialog by remember { mutableStateOf(false) }

    val loginState by viewModel.loginState.collectAsState()
    val errorState by viewModel.error.collectAsState()
    val userCollection by viewModel.userCollection.collectAsState()
    val captchaState by viewModel.captchaState.collectAsState()

    val context = LocalContext.current
    var tts: TextToSpeech? by remember { mutableStateOf(null) }

    DisposableEffect(key1 = context) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    LaunchedEffect(loginState, userCollection) {
        if (loginState != null && userCollection != null) {
            when (userCollection) {
                "vius" -> onNavigateToViuHome()
                "caregivers" -> onNavigateToCaregiverHome()
            }
        }
    }

    LaunchedEffect(captchaState.solved) {
        if (captchaState.solved && showCaptchaDialog) {
            showCaptchaDialog = false
            viewModel.login(email, password)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { showCaptchaDialog = true }),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { showCaptchaDialog = true },
            enabled = true,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }

        errorState?.let {
            if (it.isNotBlank()) {
                Text(it, color = Color.Red, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "New User? Sign Up",
            color = Color.Blue,
            modifier = Modifier.clickable { /* navigate to signup */ }
        )
    }

    if (showCaptchaDialog) {
        Dialog(
            onDismissRequest = {
                showCaptchaDialog = false
                viewModel.resetCaptcha()
            }
        ) {
            // This runs once when the dialog opens (because captchaState.text changes)
            // and again every time the user clicks "Refresh".
            LaunchedEffect(key1 = captchaState.text) {
                if (captchaState.text.isNotBlank()) {
                    // Speak the text with spaces between letters
                    val spacedText = captchaState.text.split("").joinToString(" ")
                    tts?.speak(spacedText, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Complete Security Check",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    CaptchaBox(
                        state = captchaState,
                        onRefresh = { viewModel.generateNewCaptcha() },
                        onSubmit = { viewModel.submitCaptcha(it) },
                        onPlayAudio = {
                            // Replay Button
                            val spacedText = captchaState.text.split("").joinToString(" ")
                            tts?.speak(spacedText, TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    )
                }
            }
        }
    }
}