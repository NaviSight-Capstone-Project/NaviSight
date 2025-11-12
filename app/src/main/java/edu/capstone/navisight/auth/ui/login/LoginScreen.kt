package edu.capstone.navisight.auth.ui.login

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import edu.capstone.navisight.R

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var emailFocused by remember { mutableStateOf(false) }
    var passwordFocused by remember { mutableStateOf(false) }
    val anyFieldFocused = emailFocused || passwordFocused

    val errorState by viewModel.error.collectAsState()
    val captchaState by viewModel.captchaState.collectAsState()

    val showCaptcha by viewModel.showCaptchaDialog.collectAsState()
    var showCaptchaDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showCaptcha) {
        showCaptchaDialog = showCaptcha
    }

    LaunchedEffect(captchaState) {
        if (captchaState.solved && showCaptchaDialog) {
            viewModel.dismissCaptchaDialog()
            viewModel.login(email, password)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bg_animation")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1080f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "offsetX"
    )
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1920f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "offsetY"
    )

    val scaleFactor by animateFloatAsState(
        targetValue = if (anyFieldFocused) 1.05f else 1f, label = "scale"
    )
    val blurRadius by animateDpAsState(
        targetValue = if (anyFieldFocused) 40.dp else 0.dp, label = "blur"
    )

    val screenWidth = 1080f
    val screenHeight = 1920f

    val gradientTeal = Brush.radialGradient(
        colors = listOf(Color(0xFF77F7ED).copy(alpha = 0.5f), Color(0xFFD9D9D9).copy(alpha = 0.05f)),
        center = Offset(offsetX * 0.6f + 100f, offsetY * 0.4f + 50f),
        radius = 900f
    )

    val gradientPurple = Brush.radialGradient(
        colors = listOf(Color(0xFFB446F2).copy(alpha = 0.5f), Color(0xFFD9D9D9).copy(alpha = 0.05f)),
        center = Offset(screenWidth - offsetX * 0.7f - 150f, screenHeight - offsetY * 0.6f - 100f),
        radius = 1000f
    )

    val loginGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFB644F1), Color(0xFF6041EC))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color.White, Color(0xFFF8F8F8)))),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scaleFactor)
                .blur(blurRadius)
                .background(gradientTeal)
                .background(gradientPurple)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(width = 280.dp, height = 128.dp)
                    .padding(bottom = 48.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("Email", color = Color.Gray) },
                leadingIcon = {
                    Icon(painter = painterResource(id = R.drawable.ic_email), contentDescription = null)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { emailFocused = it.isFocused }
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(
                        width = 1.5.dp,
                        color = if (emailFocused) Color(0xFF6041EC) else Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(16.dp)
                    )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("Password", color = Color.Gray) },
                leadingIcon = {
                    Icon(painter = painterResource(id = R.drawable.ic_pass), contentDescription = null)
                },
                trailingIcon = {
                    val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(icon, contentDescription = "Toggle password visibility")
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (captchaState.solved) {
                        viewModel.login(email, password)
                    } else {
                        showCaptchaDialog = true
                    }
                }),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { passwordFocused = it.isFocused }
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(
                        width = 1.5.dp,
                        color = if (passwordFocused) Color(0xFF6041EC) else Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(16.dp)
                    )
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Login Button
            Button(
                onClick = {
                    viewModel.login(email, password)
                },
                contentPadding = PaddingValues(),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .shadow(10.dp, RoundedCornerShape(50))
            ) {
                Box(
                    modifier = Modifier
                        .background(loginGradient, RoundedCornerShape(50))
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Login",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            errorState?.let { msg ->
                if (msg.isNotEmpty()) {
                    Text(
                        text = msg,
                        color = Color.Red,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            Text(
                text = "Forgot Password?",
                color = Color(0xFF4A4A4A),
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .clickable { }
            )

            Row(
                modifier = Modifier.padding(top = 60.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "New User? ",
                    color = Color(0xFF4A4A4A),
                    fontSize = 14.sp
                )
                Text(
                    text = "Sign Here",
                    color = Color(0xFF6041EC),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onSignUp() }
                )
            }
        }

        // CAPTCHA Dialog
        if (showCaptchaDialog) {
            Dialog(onDismissRequest = {
                viewModel.dismissCaptchaDialog()
            }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CaptchaBox(
                            state = captchaState,
                            onRefresh = { viewModel.generateNewCaptcha() },
                            onSubmit = { input ->
                                viewModel.submitCaptcha(input)
                            },
                            onPlayAudio = {}
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showCaptchaDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
