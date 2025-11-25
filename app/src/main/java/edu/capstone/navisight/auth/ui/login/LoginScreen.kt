package edu.capstone.navisight.auth.ui.login

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import edu.capstone.navisight.R
import edu.capstone.navisight.auth.util.VoiceHandler // Import the new Handler

enum class InputStage { EMAIL, PASSWORD, DONE }

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onSignUp: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // INIT VOICE HANDLER
    // We use remember to keep the instance alive, and DisposableEffect to clean it up
    val voiceHandler = remember { VoiceHandler(context) }

    DisposableEffect(Unit) {
        onDispose { voiceHandler.shutdown() }
    }

    // Observe listening state for UI animations
    val isListening by voiceHandler.isListening.collectAsState()

    // DATA STATE
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Determine Input Stage
    val currentInputStage = remember(email, password) {
        if (email.isEmpty()) InputStage.EMAIL
        else if (password.isEmpty()) InputStage.PASSWORD
        else InputStage.DONE
    }
    val activeInputStage by rememberUpdatedState(currentInputStage)

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) voiceHandler.speak("Permission granted. Hold the bottom button.")
        else voiceHandler.speak("Microphone permission is needed.")
    }

    // VOICE ACTION FUNCTIONS
    fun startVoiceInput() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        // Decide logic based on stage
        val inputType = if (activeInputStage == InputStage.PASSWORD) "password" else "email"

        voiceHandler.startListening(
            inputType = inputType,
            onResult = { result ->
                if (inputType == "email") {
                    email = result
                    voiceHandler.speak("Email set. Hold again for password.")
                } else {
                    password = result
                    voiceHandler.speak("Password entered. Press Login.")
                }
            },
            onError = { errorMsg ->
                voiceHandler.speak(errorMsg)
            }
        )
    }

    // Auto-Speak on Entry
    LaunchedEffect(Unit) {
        // Small delay to ensure TTS engine is bound
        kotlinx.coroutines.delay(500)
        voiceHandler.speak("Welcome to Navi Sight. Hold the bottom button to speak your email.")
    }

    // UI SETUP (Animations & Layout)
    var emailFocused by remember { mutableStateOf(false) }
    var passwordFocused by remember { mutableStateOf(false) }
    val anyFieldFocused = emailFocused || passwordFocused

    val errorState by viewModel.error.collectAsState()
    val captchaState by viewModel.captchaState.collectAsState()
    val showCaptcha by viewModel.showCaptchaDialog.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showCaptchaDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showCaptcha) { showCaptchaDialog = showCaptcha }
    LaunchedEffect(captchaState) {
        if (captchaState.solved && showCaptchaDialog) {
            viewModel.dismissCaptchaDialog()
            viewModel.login(email, password)
        }
    }

    // Background Animation
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1080f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse), label = "offsetX"
    )
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1920f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "offsetY"
    )
    val scaleFactor by animateFloatAsState(if (anyFieldFocused) 1.05f else 1f, label = "scale")
    val blurRadius by animateDpAsState(if (anyFieldFocused) 40.dp else 0.dp, label = "blur")
    val micScale by animateFloatAsState(if (isListening) 1.2f else 1f, label = "micScale")

    val gradientTeal = Brush.radialGradient(listOf(Color(0xFF77F7ED).copy(0.5f), Color(0xFFD9D9D9).copy(0.05f)), center = Offset(offsetX * 0.6f + 100f, offsetY * 0.4f + 50f), radius = 900f)
    val gradientPurple = Brush.radialGradient(listOf(Color(0xFFB446F2).copy(0.5f), Color(0xFFD9D9D9).copy(0.05f)), center = Offset(1080f - offsetX * 0.7f - 150f, 1920f - offsetY * 0.6f - 100f), radius = 1000f)
    val loginGradient = Brush.horizontalGradient(listOf(Color(0xFFB644F1), Color(0xFF6041EC)))

    Box(
        modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color.White, Color(0xFFF8F8F8)))),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.fillMaxSize().scale(scaleFactor).blur(blurRadius).background(gradientTeal).background(gradientPurple))

        // TOP ACTION BUTTONS
        // Reset Button (Top Left)
        FloatingActionButton(
            onClick = {
                email = ""
                password = ""
                voiceHandler.speak("Fields cleared.")
            },
            containerColor = Color(0xFFE0E0E0), contentColor = Color(0xFF4A4A4A),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.align(Alignment.TopStart).padding(top = 48.dp, start = 24.dp).size(56.dp).shadow(8.dp, RoundedCornerShape(16.dp))
        ) { Icon(Icons.Filled.Refresh, "Reset", Modifier.size(28.dp)) }

        // Replay Button (Top Right)
        FloatingActionButton(
            onClick = { voiceHandler.speak("Please enter your email and password.") },
            containerColor = Color(0xFF6041EC), contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 24.dp).size(56.dp).shadow(8.dp, RoundedCornerShape(16.dp))
        ) { Icon(Icons.Filled.VolumeUp, "Replay", Modifier.size(28.dp)) }

        // Main Column
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(painterResource(R.drawable.ic_logo), "Logo", Modifier.size(280.dp, 128.dp).padding(bottom = 48.dp))
            Spacer(Modifier.height(48.dp))

            // Email
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                placeholder = { Text("Email", color = Color.Gray) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_email), null) },
                singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth().onFocusChanged { emailFocused = it.isFocused }
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(1.5.dp, if (emailFocused || activeInputStage == InputStage.EMAIL) Color(0xFF6041EC) else Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
            )
            Spacer(Modifier.height(20.dp))

            // Password
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                placeholder = { Text("Password", color = Color.Gray) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_pass), null) },
                trailingIcon = {
                    IconButton({ passwordVisible = !passwordVisible }) {
                        Icon(if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, "Toggle")
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (captchaState.solved) viewModel.login(email, password) else showCaptchaDialog = true }),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth().onFocusChanged { passwordFocused = it.isFocused }
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(1.5.dp, if (passwordFocused || activeInputStage == InputStage.PASSWORD) Color(0xFF6041EC) else Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
            )
            Spacer(Modifier.height(28.dp))

            Button(
                onClick = { viewModel.login(email, password) },
                contentPadding = PaddingValues(), shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(50.dp).shadow(10.dp, RoundedCornerShape(50))
            ) {
                Box(Modifier.background(loginGradient, RoundedCornerShape(50)).fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Login", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            errorState?.let { msg ->
                if (msg.isNotEmpty()) {
                    LaunchedEffect(msg) { voiceHandler.speak(msg) }
                    Text(msg, color = Color.Red, fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp))
                }
            }

            Text(
                "Forgot Password?",
                color = Color(0xFF4A4A4A),
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .clickable {
                        if(email.isNotBlank()) {
                            voiceHandler.speak("Sending reset email.")
                            viewModel.resetPassword(email)
                        } else {
                            val msg = "Please enter your email first."
                            voiceHandler.speak("Please enter your email first.")
                            viewModel.setError(msg)
                        }
                    }
            )

            Row(Modifier.padding(top = 60.dp), horizontalArrangement = Arrangement.Center) {
                Text("New User? ", color = Color(0xFF4A4A4A), fontSize = 14.sp)
                Text("Sign Here", color = Color(0xFF6041EC), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onSignUp() })
            }
        }

        // BOTTOM MIC BUTTON
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = when(activeInputStage) {
                        InputStage.EMAIL -> "Hold for Email"
                        InputStage.PASSWORD -> "Hold for Password"
                        InputStage.DONE -> "Ready"
                    },
                    color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp)
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(72.dp).scale(micScale).shadow(8.dp, CircleShape)
                        .background(if (isListening) Color.Red else Color(0xFF6041EC), CircleShape)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    startVoiceInput()
                                    tryAwaitRelease()
                                    voiceHandler.stopListening()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            )
                        }
                ) {
                    Icon(Icons.Filled.Mic, "Speak", tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
        }

        // CAPTCHA
        if (showCaptchaDialog) {
            Dialog(onDismissRequest = { viewModel.dismissCaptchaDialog() }) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        LaunchedEffect(Unit) { voiceHandler.speak("Verification required.") }
                        CaptchaBox(captchaState, { viewModel.generateNewCaptcha() }, { viewModel.submitCaptcha(it) }, {})
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { showCaptchaDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("Cancel", color = Color.White) }
                    }
                }
            }
        }
        if (isLoading) LoadingScreen()
    }
}