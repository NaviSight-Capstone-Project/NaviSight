package edu.capstone.navisight.auth.ui.signup.viu

import android.util.Patterns
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import edu.capstone.navisight.R
import edu.capstone.navisight.auth.util.LegalDocuments
import edu.capstone.navisight.common.Constants
import edu.capstone.navisight.common.ConverterHelpers.convertMillisToDate
import edu.capstone.navisight.common.VibrationHelper
import java.util.*

private enum class ViuSignupStep { PERSONAL, AVATAR, LEGAL, ACCOUNT }

fun isAgeValid(birthMillis: Long): Boolean {
    val birthDate = Calendar.getInstance().apply { timeInMillis = birthMillis }
    val today = Calendar.getInstance()

    var age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)

    if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) {
        age--
    }
    return age in 18..60
}

@Composable
fun ViuSignupScreen(
    viewModel: ViuSignupViewModel,
    onSelectImageClick: () -> Unit,
    onSignupSuccess: (uid: String) -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var currentStep by remember { mutableStateOf(ViuSignupStep.PERSONAL) }

    // Data State
    var firstName by remember { mutableStateOf("") }
    var middleName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthday by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var detailedAddress by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var caregiverEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rePassword by remember { mutableStateOf("") }

    // Legal State
    var termsAccepted by remember { mutableStateOf(false) }
    var privacyAccepted by remember { mutableStateOf(false) }

    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val offsetX by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1080f, animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse), label = "offsetX")
    val offsetY by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1920f, animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "offsetY")
    val gradientTeal = Brush.radialGradient(listOf(Color(0xFF78E4EF).copy(0.5f), Color(0xFFD9D9D9).copy(0.05f)), center = Offset(offsetX * 0.6f + 100f, offsetY * 0.4f + 50f), radius = 900f)
    val gradientPurple = Brush.radialGradient(listOf(Color(0xFF6342ED).copy(0.5f), Color(0xFFD9D9D9).copy(0.05f)), center = Offset(1080f - offsetX * 0.7f - 150f, 1920f - offsetY * 0.6f - 100f), radius = 1000f)

    LaunchedEffect(uiState.signupSuccess) {
        if (uiState.signupSuccess && uiState.createdUserId != null) {
            VibrationHelper.vibratePattern(context, Constants.VIBRATE_SUCCESS)
            onSignupSuccess(uiState.createdUserId!!)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            if (!it.contains("OTP", ignoreCase = true)) {
                VibrationHelper.vibratePattern(context, Constants.VIBRATE_ERROR)
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFAFAFA)), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxSize().blur(0.dp).background(gradientTeal).background(gradientPurple))

        IconButton(
            onClick = {
                VibrationHelper.vibrate(context, Constants.VIBRATE_KEY_PRESS)
                when (currentStep) {
                    ViuSignupStep.PERSONAL -> onBackClick()
                    ViuSignupStep.AVATAR -> currentStep = ViuSignupStep.PERSONAL
                    ViuSignupStep.LEGAL -> currentStep = ViuSignupStep.AVATAR
                    ViuSignupStep.ACCOUNT -> currentStep = ViuSignupStep.LEGAL
                }
            },
            modifier = Modifier.align(Alignment.TopStart).padding(top = 48.dp, start = 24.dp)
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF4A4A4A)) }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painter = painterResource(R.drawable.ic_logo), contentDescription = "Logo", modifier = Modifier.size(160.dp, 80.dp))
            Spacer(Modifier.height(8.dp))
            Text("VIU Signup", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A4A4A))
            Spacer(Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth().animateContentSize()
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) (slideInHorizontally { width -> width } + fadeIn()).togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                        else (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                    },
                    label = "WizardTransition"
                ) { step ->
                    when (step) {
                        ViuSignupStep.PERSONAL -> StepViuPersonal(
                            fName = firstName, mName = middleName, lName = lastName, birthday = birthday, phone = phone, address = detailedAddress, sex = sex, category = category,
                            onUpdate = { f, m, l, b, p, a, s, c -> firstName = f; middleName = m; lastName = l; birthday = b; phone = p; detailedAddress = a; sex = s; category = c },
                            onNext = { currentStep = ViuSignupStep.AVATAR }
                        )
                        ViuSignupStep.AVATAR -> StepViuAvatar(
                            imageUri = uiState.profileImageUri,
                            onAddPhoto = onSelectImageClick,
                            onNext = { currentStep = ViuSignupStep.LEGAL }
                        )
                        ViuSignupStep.LEGAL -> StepViuLegal(
                            termsAccepted = termsAccepted,
                            privacyAccepted = privacyAccepted,
                            onUpdate = { t, p -> termsAccepted = t; privacyAccepted = p },
                            onNext = { currentStep = ViuSignupStep.ACCOUNT }
                        )
                        ViuSignupStep.ACCOUNT -> StepViuAccount(
                            email = email, caregiverEmail = caregiverEmail, pass = password, rePass = rePassword,
                            isLoading = uiState.isLoading,
                            onUpdate = { e, ce, p, rp -> email = e; caregiverEmail = ce; password = p; rePassword = rp },
                            onSignup = {
                                // Combine Address and Country
                                val fullAddress = "$detailedAddress, Philippines"
                                viewModel.signup(
                                    context = context,
                                    email = email,
                                    password = password,
                                    firstName = firstName.trim(),
                                    lastName = lastName.trim(),
                                    middleName = middleName.trim(),
                                    birthday = birthday,
                                    phone = phone,
                                    address = fullAddress,
                                    sex = sex,
                                    category = category.trim(),
                                    caregiverEmail = caregiverEmail.trim(),
                                    termsAccepted = termsAccepted,
                                    privacyAccepted = privacyAccepted
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepViuPersonal(
    fName: String, mName: String, lName: String, birthday: String, phone: String, address: String, sex: String, category: String,
    onUpdate: (String, String, String, String, String, String, String, String) -> Unit,
    onNext: () -> Unit
) {
    var isCategoryExpanded by remember { mutableStateOf(false) }
    val categoryOptions = listOf("Partially Blind", "Totally Blind")

    var isSexExpanded by remember { mutableStateOf(false) }
    val sexOptions = listOf("Male", "Female", "Rather not say")

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var isAgeInvalid by remember { mutableStateOf(false) }

    val isFirstNameError = fName.isEmpty()
    val isLastNameError = lName.isEmpty()
    val isPhoneError = phone.isEmpty() || !phone.startsWith("09") || phone.length != 11
    val isAddressError = address.isEmpty()
    val isCategoryError = category.isEmpty()
    val isSexError = sex.isEmpty()
    val isBirthdayError = birthday.isEmpty() || isAgeInvalid
    val context = LocalContext.current

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    VibrationHelper.vibrate(context, Constants.VIBRATE_KEY_PRESS)
                    datePickerState.selectedDateMillis?.let { millis ->
                        // Validate Age 18-60
                        if (isAgeValid(millis)) {
                            isAgeInvalid = false
                            val formattedDate = convertMillisToDate(millis)
                            onUpdate(fName, mName, lName, formattedDate, phone, address, sex, category)
                        } else {
                            isAgeInvalid = true
                            // Still update the text, but the error flag blocks "Next"
                            val formattedDate = convertMillisToDate(millis)
                            onUpdate(fName, mName, lName, formattedDate, phone, address, sex, category)
                        }
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    VibrationHelper.vibrate(context, Constants.VIBRATE_KEY_PRESS)
                    showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Step 1: Personal Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6641EC))
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = fName,
            onValueChange = { if (it.all { c -> c.isLetter() || c.isWhitespace() }) onUpdate(it, mName, lName, birthday, phone, address, sex, category) },
            label = { Text("First Name *") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = isFirstNameError && fName.isNotEmpty(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = mName,
            onValueChange = { if (it.all { c -> c.isLetter() || c.isWhitespace() }) onUpdate(fName, it, lName, birthday, phone, address, sex, category) },
            label = { Text("Middle Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = lName,
            onValueChange = { if (it.all { c -> c.isLetter() || c.isWhitespace() }) onUpdate(fName, mName, it, birthday, phone, address, sex, category) },
            label = { Text("Last Name *") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = isLastNameError && lName.isNotEmpty(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
            OutlinedTextField(
                value = birthday,
                onValueChange = {},
                readOnly = true,
                enabled = true,
                label = { Text("Birthday (MM/dd/yyyy) *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        VibrationHelper.vibrate(context, Constants.VIBRATE_KEY_PRESS)
                        showDatePicker = true }) {
                        Icon(Icons.Default.DateRange,
                            contentDescription = "Date",
                            tint = if (showDatePicker) Color(0xFF6641EC) else Color.Gray)
                    }
                },
                isError = isBirthdayError && birthday.isNotEmpty(),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = if(isBirthdayError && birthday.isNotEmpty()) Color.Red else Color.Gray,
                    disabledLabelColor = if(isBirthdayError && birthday.isNotEmpty()) Color.Red else Color.Gray,
                    disabledTrailingIconColor = Color.Gray ,
                    unfocusedBorderColor = if (showDatePicker) Color(0xFF6641EC) else MaterialTheme.colorScheme.outline,
                    unfocusedLabelColor = if (showDatePicker) Color(0xFF6641EC) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            // Invisible box to capture click over disabled text field
            Box(Modifier.matchParentSize().clickable { showDatePicker = true })
        }
        if (isAgeInvalid && birthday.isNotEmpty()) {
            Text("Age must be between 18 and 60 years old.", color = Color.Red, fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp))
        }
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { if (it.length <= 11 && it.all { c -> c.isDigit() }) onUpdate(fName, mName, lName, birthday, it, address, sex, category) },
            label = { Text("Phone (09XXXXXXXXX) *") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = isPhoneError && phone.isNotEmpty(),
            singleLine = true
        )
        if (isPhoneError && phone.isNotEmpty()) Text("Must start with 09 and contain 11 digits", color = Color.Red, fontSize = 10.sp)

        Spacer(Modifier.height(8.dp))

        // Sex Dropdown
        ExposedDropdownMenuBox(isSexExpanded, { isSexExpanded = !isSexExpanded }) {
            OutlinedTextField(
                value = sex,
                onValueChange = {},
                readOnly = true,
                label = { Text("Sex *") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(isSexExpanded) },
                isError = isSexError && sex.isNotEmpty()
            )
            ExposedDropdownMenu(isSexExpanded, { isSexExpanded = false }) {
                sexOptions.forEach { op ->
                    DropdownMenuItem(text = { Text(op) }, onClick = {
                        VibrationHelper.vibrate(context, Constants.VIBRATE_KEY_PRESS)
                        onUpdate(fName, mName, lName, birthday, phone, address, op, category); isSexExpanded = false })
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // Fixed Country "Philippines"
        OutlinedTextField(
            value = "Philippines",
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text("Country") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = Color.Black,
                disabledBorderColor = Color.LightGray,
                disabledLabelColor = Color.Gray
            )
        )
        Spacer(Modifier.height(8.dp))

        // Detailed Address
        OutlinedTextField(
            value = address,
            onValueChange = { onUpdate(fName, mName, lName, birthday, phone, it, sex, category) },
            label = { Text("Detailed Address *") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = isAddressError && address.isNotEmpty(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        ExposedDropdownMenuBox(isCategoryExpanded, { isCategoryExpanded = !isCategoryExpanded }) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                label = { Text("Visual Category *") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(isCategoryExpanded) },
                isError = isCategoryError && category.isNotEmpty()
            )
            ExposedDropdownMenu(isCategoryExpanded, { isCategoryExpanded = false }) {
                categoryOptions.forEach { op ->
                    DropdownMenuItem(text = { Text(op) }, onClick = {
                        VibrationHelper.vibrate(context, Constants.VIBRATE_KEY_PRESS)
                        onUpdate(fName, mName, lName, birthday ,phone, address, sex, op); isCategoryExpanded = false })
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        val isValid = !isFirstNameError && !isLastNameError && !isBirthdayError && !isPhoneError && !isAddressError && !isSexError && !isCategoryError

        GradientButton(text = "Next", onClick = {
            VibrationHelper.vibrate(context, Constants.VIBRATE_KEY_PRESS)
            onNext}, enabled = isValid)
    }
}

@Composable
fun StepViuAvatar(imageUri: android.net.Uri?, onAddPhoto: () -> Unit, onNext: () -> Unit) {
    val context = LocalContext.current
    val focusedColor = Color(0xFF6641EC)
    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Step 2: Profile Picture", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = focusedColor, modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(32.dp))
        Box(
            modifier = Modifier.size(160.dp).clip(CircleShape).background(Color(0xFFF0F0F0)).border(2.dp, focusedColor, CircleShape).clickable { onAddPhoto() },
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                Image(painter = rememberAsyncImagePainter(imageUri), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddAPhoto, null, tint = focusedColor, modifier = Modifier.size(40.dp))
                    Text("Add Photo", color = focusedColor, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(48.dp))
        GradientButton(text = if (imageUri == null) "Skip" else "Next", onClick = {

            VibrationHelper.vibrate(context, Constants.VIBRATE_KEY_PRESS)
            onNext})
    }
}

@Composable
fun StepViuLegal(
    termsAccepted: Boolean,
    privacyAccepted: Boolean,
    onUpdate: (Boolean, Boolean) -> Unit,
    onNext: () -> Unit
) {
    val focusedColor = Color(0xFF6641EC)
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Terms, 1 = Privacy
    val context = LocalContext.current

    Column(Modifier.padding(24.dp)) {
        Text("Step 3: Legal Agreements", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = focusedColor)
        Text("Please read and accept both policies.", fontSize = 12.sp, color = Color.Gray)

        Spacer(Modifier.height(16.dp))

        // Tabs
        Row(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE0E0E0))) {
            TabButton(
                text = "Terms",
                isSelected = selectedTab == 0,
                isCompleted = termsAccepted,
                onClick = {
                    VibrationHelper.vibrate(context, Constants.VIBRATE_KEY_PRESS)
                    selectedTab = 0 },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                text = "Privacy",
                isSelected = selectedTab == 1,
                isCompleted = privacyAccepted,
                onClick = {
                    VibrationHelper.vibrate(context, Constants.VIBRATE_KEY_PRESS)
                    selectedTab = 1 },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Content
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
            modifier = Modifier.fillMaxWidth().height(350.dp).border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
        ) {
            if (selectedTab == 0) {
                ScrollableAgreementViewer(
                    title = "Terms & Conditions",
                    content = LegalDocuments.TERMS_AND_CONDITIONS,
                    isAccepted = termsAccepted,
                    onAcceptChange = { onUpdate(it, privacyAccepted) }
                )
            } else {
                ScrollableAgreementViewer(
                    title = "Privacy Policy",
                    content = LegalDocuments.PRIVACY_POLICY,
                    isAccepted = privacyAccepted,
                    onAcceptChange = { onUpdate(termsAccepted, it) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        GradientButton(
            text = "Agree & Continue",
            onClick = {VibrationHelper.vibrate(context, Constants.VIBRATE_KEY_PRESS)
                onNext},
            enabled = termsAccepted && privacyAccepted
        )
    }
}

@Composable
fun StepViuAccount(
    email: String, caregiverEmail: String, pass: String, rePass: String,
    isLoading: Boolean,
    onUpdate: (String, String, String, String) -> Unit,
    onSignup: () -> Unit
) {
    var passVis by remember { mutableStateOf(false) }
    var rePassVis by remember { mutableStateOf(false) }

    val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isCaregiverEmailValid = Patterns.EMAIL_ADDRESS.matcher(caregiverEmail).matches()

    val hasUpperCase = pass.any { it.isUpperCase() }
    val hasLowerCase = pass.any { it.isLowerCase() }
    val hasSpecialChar = pass.any { !it.isLetterOrDigit() }
    val hasMinLength = pass.length >= 8
    val isPassValid = hasMinLength && hasUpperCase && hasLowerCase && hasSpecialChar
    val isRePassValid = pass == rePass && pass.isNotEmpty()
    val context = LocalContext.current

    Column(Modifier.padding(24.dp)) {
        Text("Step 4: Account Info", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6641EC))
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(email, { onUpdate(it, caregiverEmail, pass, rePass) }, label = { Text("Your Email *") }, isError = !isEmailValid && email.isNotEmpty(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(caregiverEmail, { onUpdate(email, it, pass, rePass) }, label = { Text("Caregiver's Email *") }, isError = !isCaregiverEmailValid && caregiverEmail.isNotEmpty(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(pass, { onUpdate(email, caregiverEmail, it, rePass) }, label = { Text("Password *") }, isError = !isPassValid && pass.isNotEmpty(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), visualTransformation = if (passVis) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton({ passVis = !passVis }) { Icon(if (passVis) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null) } }, singleLine = true)

        if (!isPassValid && pass.isNotEmpty()) {
            Column {
                if (!hasMinLength) Text("• Min 8 characters", color = Color.Red, fontSize = 10.sp)
                if (!hasUpperCase) Text("• At least 1 uppercase letter", color = Color.Red, fontSize = 10.sp)
                if (!hasLowerCase) Text("• At least 1 lowercase letter", color = Color.Red, fontSize = 10.sp)
                if (!hasSpecialChar) Text("• At least 1 special character", color = Color.Red, fontSize = 10.sp)
            }
        } else if (pass.isEmpty()) {
            Text("Required", color = Color.Red, fontSize = 10.sp)
        }
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(rePass, { onUpdate(email, caregiverEmail, pass, it) }, label = { Text("Confirm Password *") }, isError = !isRePassValid && rePass.isNotEmpty(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), visualTransformation = if (rePassVis) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton({ rePassVis = !rePassVis }) { Icon(if (rePassVis) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null) } }, singleLine = true)
        if (!isRePassValid && rePass.isNotEmpty()) Text("Passwords do not match", color = Color.Red, fontSize = 10.sp)
        else if (rePass.isEmpty()) Text("Required", color = Color.Red, fontSize = 10.sp)

        Spacer(Modifier.height(24.dp))

        val isValid = isEmailValid && isCaregiverEmailValid && isPassValid && isRePassValid && !isLoading
        GradientButton(text = "Create Account", onClick = {VibrationHelper.vibrate(context, Constants.VIBRATE_KEY_PRESS)
            onSignup}, isLoading = isLoading, enabled = isValid)
    }
}

@Composable
private fun TabButton(text: String, isSelected: Boolean, isCompleted: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val activeColor = Color(0xFF6641EC)
    Box(
        modifier = modifier.fillMaxHeight().background(if (isSelected) Color.White else Color.Transparent).clickable { onClick() }.padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = if (isSelected) activeColor else Color.Gray, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
            if (isCompleted) { Spacer(Modifier.width(4.dp)); Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp)) }
        }
    }
}

@Composable
private fun ScrollableAgreementViewer(title: String, content: String, isAccepted: Boolean, onAcceptChange: (Boolean) -> Unit) {
    val scrollState = rememberScrollState()
    val focusedColor = Color(0xFF6641EC)
    val isAtBottom by remember { derivedStateOf { !scrollState.canScrollForward } }
    var hasRead by remember { mutableStateOf(isAccepted) }
    LaunchedEffect(isAtBottom) { if (isAtBottom) hasRead = true }

    Column(Modifier.fillMaxSize()) {
        Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
        HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
        Box(Modifier.weight(1f)) {
            Text(text = content, fontSize = 13.sp, color = Color.DarkGray, modifier = Modifier.padding(16.dp).verticalScroll(scrollState))
            androidx.compose.animation.AnimatedVisibility(visible = !isAtBottom, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.BottomCenter)) {
                Box(Modifier.fillMaxWidth().height(60.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFFF8F9FA)))))
            }
        }
        HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
        Row(modifier = Modifier.fillMaxWidth().clickable(enabled = hasRead) { onAcceptChange(!isAccepted) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isAccepted, onCheckedChange = null, enabled = hasRead, colors = CheckboxDefaults.colors(checkedColor = focusedColor, uncheckedColor = Color.Gray))
            Spacer(Modifier.width(8.dp))
            Text(text = if (hasRead) "I have read and agree" else "Scroll to the bottom to agree", fontSize = 14.sp, fontWeight = if (hasRead) FontWeight.Medium else FontWeight.Normal, color = if (hasRead) Color.Black else Color.Gray)
            if (!hasRead) { Spacer(Modifier.weight(1f)); Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.LightGray, modifier = Modifier.size(16.dp)) }
        }
    }
}

@Composable
fun ViuOtpScreen(
    viewModel: ViuSignupViewModel,
    uid: String,
    onVerificationSuccess: () -> Unit,
    onCancelSignup: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var otpValue by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val offsetX by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1080f, animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse), label = "offsetX")
    val offsetY by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1920f, animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "offsetY")
    val gradientTeal = Brush.radialGradient(listOf(Color(0xFF78E4EF).copy(0.5f), Color(0xFFD9D9D9).copy(0.05f)), center = Offset(offsetX * 0.6f + 100f, offsetY * 0.4f + 50f), radius = 900f)
    val gradientPurple = Brush.radialGradient(listOf(Color(0xFF6342ED).copy(0.5f), Color(0xFFD9D9D9).copy(0.05f)), center = Offset(1080f - offsetX * 0.7f - 150f, 1920f - offsetY * 0.6f - 100f), radius = 1000f)

    LaunchedEffect(uiState.verificationSuccess) {
        if (uiState.verificationSuccess) {
            Toast.makeText(context, uiState.successMessage ?: "Verification Successful!", Toast.LENGTH_LONG).show()
            onVerificationSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFAFAFA)), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxSize().blur(0.dp).background(gradientTeal).background(gradientPurple))

        Card(modifier = Modifier.fillMaxWidth(0.85f).wrapContentHeight(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Verify Email", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6641EC))
                Spacer(Modifier.height(8.dp))
                Text("Enter the 6-digit code sent to the Caregiver's email.", textAlign = TextAlign.Center, color = Color(0xFF4A4A4A), fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = otpValue,
                    onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) otpValue = it },
                    label = { Text("Enter OTP") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (uiState.errorMessage != null) {
                    Text(text = uiState.errorMessage!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }

                Spacer(Modifier.height(24.dp))

                GradientButton(
                    text = "Verify Code",
                    onClick = {
                        val caregiverUid = uiState.createdCaregiverId
                        VibrationHelper.vibrate(context, Constants.VIBRATE_BUTTON_TAP)
                        if (otpValue.length == 6 && caregiverUid != null) {
                            viewModel.verifyOtp(uid, caregiverUid, otpValue)
                        } else {
                            Toast.makeText(context, "Caregiver ID missing or OTP incomplete", Toast.LENGTH_SHORT).show()
                        }
                    },
                    isLoading = uiState.isLoading,
                    enabled = otpValue.length == 6
                )

                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = {VibrationHelper.vibrate(context, Constants.VIBRATE_BUTTON_TAP)
                            onCancelSignup}) { Text("Cancel", color = Color.Gray) }
                    TextButton(
                        onClick = { VibrationHelper.vibrate(context, Constants.VIBRATE_BUTTON_TAP)
                            uiState.createdCaregiverId?.let { viewModel.resendOtp(context, it) } },
                        enabled = uiState.resendTimer == 0
                    ) {
                        Text(if (uiState.resendTimer > 0) "Wait ${uiState.resendTimer}s" else "Resend Code", color = Color(0xFF6641EC))
                    }
                }
            }
        }
    }
}

@Composable
fun GradientButton(text: String, onClick: () -> Unit, enabled: Boolean = true, isLoading: Boolean = false) {
    val buttonGradient = Brush.horizontalGradient(listOf(Color(0xFFAA41E5), Color(0xFF6342ED)))
    Button(onClick = onClick, enabled = enabled && !isLoading, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)) {
        Box(modifier = Modifier.fillMaxSize().background(if (enabled) buttonGradient else Brush.linearGradient(listOf(Color.Gray, Color.Gray))), contentAlignment = Alignment.Center) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text(text, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}