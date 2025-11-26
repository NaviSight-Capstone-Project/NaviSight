package edu.capstone.navisight.auth.ui.signup.viu

import android.net.Uri
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViuSignupScreen(
    viewModel: ViuSignupViewModel,
    onSelectImageClick: () -> Unit,
    onSignupSuccess: (uid: String) -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    //  State for fields (Simpler)
    var firstName by remember { mutableStateOf("") }
    var middleName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rePassword by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var caregiverEmail by remember { mutableStateOf("") }

    var isCategoryExpanded by remember { mutableStateOf(false) }
    val categoryOptions = listOf("Partially Blind", "Totally Blind")

    //  State for validation
    var isEmailValid by remember { mutableStateOf(true) }
    var isPasswordValid by remember { mutableStateOf(true) }
    var isRePasswordValid by remember { mutableStateOf(true) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isRePasswordVisible by remember { mutableStateOf(false) }
    var isPhoneValid by remember { mutableStateOf(true) }
    var isCaregiverEmailValid by remember { mutableStateOf(true) }

    //  Custom Colors
    val gradientStart = Color(0xFF78E4EF)
    val gradientEnd = Color(0xFF6342ED)
    val gradientStartButton = Color(0xFFAA41E5)
    val focusedColor = Color(0xFF6641EC)
    val unfocusedColor = Color.Black
    val sectionHeaderColor = Color(0xFF8E41E8)
    val fieldLabelColor = Color(0xFF8E41E8)

    //  Custom TextField Colors
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

    LaunchedEffect(uiState.signupSuccess) {
        if (uiState.signupSuccess && uiState.createdUserId != null) {
            Toast.makeText(context, "Signup successful! Please check your email for an OTP.", Toast.LENGTH_LONG).show()
            onSignupSuccess(uiState.createdUserId!!)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            if (!it.contains("New OTP sent")) {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top Gradient Background
        val gradientHeight = 280.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(gradientHeight)
                .background(Brush.horizontalGradient(listOf(gradientStart, gradientEnd)))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 9.dp, start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Create VIU Account",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // White Card Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 230.dp)
                .background(
                    Color.White,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 30.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    "Personal Information",
                    style = MaterialTheme.typography.titleLarge,
                    color = sectionHeaderColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .align(Alignment.CenterHorizontally)
                )

                // Name Fields
                Text("Name", color = fieldLabelColor, modifier = Modifier.align(Alignment.Start).padding(bottom = 4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = firstName, onValueChange = { if (it.matches(Regex("^[a-zA-Z]*$"))) firstName = it }, label = { Text("First Name", maxLines = 1, overflow = TextOverflow.Ellipsis) }, colors = customTextFieldColors, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = middleName, onValueChange = { if (it.matches(Regex("^[a-zA-Z]*$"))) middleName = it }, label = { Text("Middle Name", maxLines = 1, overflow = TextOverflow.Ellipsis) }, colors = customTextFieldColors, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = lastName, onValueChange = { if (it.matches(Regex("^[a-zA-Z]*$"))) lastName = it }, label = { Text("Last Name", maxLines = 1, overflow = TextOverflow.Ellipsis) }, colors = customTextFieldColors, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f), singleLine = true)
                }
                Spacer(Modifier.height(16.dp))

                // Contact Info
                Text("Contact Info", color = fieldLabelColor, modifier = Modifier.align(Alignment.Start).padding(bottom = 4.dp))
                OutlinedTextField(value = phone, onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 11) { phone = it; isPhoneValid = it.matches(Regex("^09\\d{9}$")) } }, label = { Text("09XXXXXXXXX", maxLines = 1, overflow = TextOverflow.Ellipsis) }, isError = !isPhoneValid, colors = customTextFieldColors, shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                if (!isPhoneValid && phone.isNotEmpty()) { Text(text = "Must be 11 digits, start with '09'", color = Color.Red, style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(16.dp))

                // Address
                Text("Address", color = fieldLabelColor, modifier = Modifier.align(Alignment.Start).padding(bottom = 4.dp))
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address (City, Country)", maxLines = 1, overflow = TextOverflow.Ellipsis) }, colors = customTextFieldColors, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(16.dp))

                // category
                Text("Category", color = fieldLabelColor, modifier = Modifier.align(Alignment.Start).padding(bottom = 4.dp))
                ExposedDropdownMenuBox(
                    expanded = isCategoryExpanded,
                    onExpandedChange = { isCategoryExpanded = !isCategoryExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {}, // ReadOnly
                        readOnly = true,
                        label = { Text("Select Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryExpanded) },
                        colors = customTextFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = isCategoryExpanded,
                        onDismissRequest = { isCategoryExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        categoryOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    category = option
                                    isCategoryExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))

                // Account Information Header
                Text("Account Information", style = MaterialTheme.typography.titleLarge, color = sectionHeaderColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally))

                // Email
                Text("Email", color = fieldLabelColor, modifier = Modifier.align(Alignment.Start).padding(bottom = 4.dp))
                OutlinedTextField(value = email, onValueChange = { email = it; isEmailValid = Patterns.EMAIL_ADDRESS.matcher(it).matches() }, label = { Text("Email", maxLines = 1, overflow = TextOverflow.Ellipsis) }, isError = !isEmailValid, colors = customTextFieldColors, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                if (!isEmailValid && email.isNotEmpty()) { Text(text = "Please enter a valid email address", color = Color.Red, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp, top = 2.dp)) }
                Spacer(Modifier.height(16.dp))

                Text("Caregiver's Email", color = fieldLabelColor, modifier = Modifier.align(Alignment.Start).padding(bottom = 4.dp))
                OutlinedTextField(
                    value = caregiverEmail,
                    onValueChange = {
                        caregiverEmail = it
                        isCaregiverEmailValid = Patterns.EMAIL_ADDRESS.matcher(it).matches()
                    },
                    label = { Text("Caregiver's Email", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    isError = !isCaregiverEmailValid,
                    colors = customTextFieldColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                if (!isCaregiverEmailValid && caregiverEmail.isNotEmpty()) {
                    Text(text = "Please enter a valid email address", color = Color.Red, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
                }
                Spacer(Modifier.height(16.dp))

                // Password Fields
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Password", color = fieldLabelColor, modifier = Modifier.padding(bottom = 4.dp))
                        OutlinedTextField(value = password, onValueChange = { password = it; isPasswordValid = it.length >= 8; isRePasswordValid = it == rePassword }, label = { Text("Password", maxLines = 1, overflow = TextOverflow.Ellipsis) }, isError = !isPasswordValid, colors = customTextFieldColors, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), trailingIcon = { val image = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff; IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) { Icon(imageVector = image, contentDescription = "Toggle password visibility") } })
                        if (!isPasswordValid && password.isNotEmpty()) { Text(text = "Min 8 characters", color = Color.Red, style = MaterialTheme.typography.bodySmall) }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Re-enter Password", color = fieldLabelColor, modifier = Modifier.padding(bottom = 4.dp))
                        OutlinedTextField(value = rePassword, onValueChange = { rePassword = it; isRePasswordValid = it == password }, label = { Text("Re-enter Password", maxLines = 1, overflow = TextOverflow.Ellipsis) }, isError = !isRePasswordValid, colors = customTextFieldColors, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), visualTransformation = if (isRePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), trailingIcon = { val image = if (isRePasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff; IconButton(onClick = { isRePasswordVisible = !isRePasswordVisible }) { Icon(imageVector = image, contentDescription = "Toggle password visibility") } })
                        if (!isRePasswordValid && rePassword.isNotEmpty()) { Text(text = "Passwords don't match", color = Color.Red, style = MaterialTheme.typography.bodySmall) }
                    }
                }
                Spacer(Modifier.height(32.dp))

                // Gradient Button
                Button(
                    onClick = {
                        isRePasswordValid = password == rePassword
                        //   VALIDATION
                        val allFieldsValid = isEmailValid && isPasswordValid && isRePasswordValid && isPhoneValid &&
                                isCaregiverEmailValid &&
                                firstName.isNotBlank() && lastName.isNotBlank() && email.isNotBlank() &&
                                password.isNotBlank() && rePassword.isNotBlank() && phone.isNotBlank() &&
                                address.isNotBlank() && category.isNotBlank() &&
                                caregiverEmail.isNotBlank()

                        if (allFieldsValid) {
                            //  VIEWMODEL CALL
                            viewModel.signup(
                                context = context, email = email, password = password,
                                firstName = firstName.trim(), middleName = middleName.trim(), lastName = lastName.trim(),
                                phone = phone, address = address.trim(), category = category.trim(),
                                caregiverEmail = caregiverEmail.trim()
                            )
                        } else {
                            Toast.makeText(context, "Please fill all required fields correctly", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(gradientStartButton, gradientEnd)), shape = RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (uiState.isLoading) "Sending OTP..." else "Save Changes", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (uiState.errorMessage != null && !uiState.verificationSuccess) {
                    Text(uiState.errorMessage!!, color = Color.Red)
                }
            }
        }

        // Profile Image (On Top)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 90.dp)
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(4.dp, Color.White, CircleShape)
                .clickable { onSelectImageClick() },
            contentAlignment = Alignment.Center
        ) {
            val profileUri = uiState.profileImageUri
            if (profileUri != null) {
                Image(painter = rememberAsyncImagePainter(profileUri), contentDescription = "Profile Photo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Add photo", tint = Color.LightGray, modifier = Modifier.size(50.dp))
                    Text("Add Photo", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}