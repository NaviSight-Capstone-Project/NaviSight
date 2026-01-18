package edu.capstone.navisight.auth.ui.signup.caregiver

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
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.Timestamp
import edu.capstone.navisight.R
import edu.capstone.navisight.auth.util.LegalDocuments
import java.text.SimpleDateFormat
import java.util.*

enum class SignupStep { PERSONAL, AVATAR, LEGAL, ACCOUNT }

@Composable
fun CaregiverSignupScreen(
    viewModel: CaregiverSignupViewModel,
    onSelectImageClick: () -> Unit,
    onSignupSuccess: (uid: String) -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(SignupStep.PERSONAL) }

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val offsetX by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1080f, animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse), label = "offsetX")
    val offsetY by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1920f, animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "offsetY")

    val gradientTeal = Brush.radialGradient(listOf(Color(0xFF77F7ED).copy(0.5f), Color(0xFFD9D9D9).copy(0.05f)), center = Offset(offsetX * 0.6f + 100f, offsetY * 0.4f + 50f), radius = 900f)
    val gradientPurple = Brush.radialGradient(listOf(Color(0xFFB446F2).copy(0.5f), Color(0xFFD9D9D9).copy(0.05f)), center = Offset(1080f - offsetX * 0.7f - 150f, 1920f - offsetY * 0.6f - 100f), radius = 1000f)

    LaunchedEffect(uiState.verificationSuccess) { if (uiState.verificationSuccess) onSignupSuccess(uiState.createdUserId ?: "") }
    LaunchedEffect(uiState.errorMessage) { uiState.errorMessage?.let { if (!it.contains("OTP", ignoreCase = true)) { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); viewModel.clearError() } } }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFAFAFA)), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxSize().blur(0.dp).background(gradientTeal).background(gradientPurple))

        IconButton(
            onClick = {
                when (currentStep) {
                    SignupStep.PERSONAL -> onBackClick()
                    SignupStep.AVATAR -> currentStep = SignupStep.PERSONAL
                    SignupStep.LEGAL -> currentStep = SignupStep.AVATAR
                    SignupStep.ACCOUNT -> currentStep = SignupStep.LEGAL
                }
            },
            modifier = Modifier.align(Alignment.TopStart).padding(top = 48.dp, start = 24.dp)
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF4A4A4A)) }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(24.dp)), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painter = painterResource(R.drawable.ic_logo), contentDescription = "Logo", modifier = Modifier.size(160.dp, 80.dp))
            Spacer(Modifier.height(8.dp))
            Text("Companion Signup", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A4A4A))
            Spacer(Modifier.height(16.dp))

            // Use weight to prevent card from being too tall if content is small, but allow it to shrink
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth().animateContentSize()
            ) {
                Box(modifier = Modifier.clip(RoundedCornerShape(24.dp))) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState > initialState) (slideInHorizontally { width -> width } + fadeIn()).togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                            else (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                        },
                        label = "WizardTransition"
                    ) { step ->
                        when (step) {
                            SignupStep.PERSONAL -> StepPersonalDetails(
                                state = formState,
                                onEvent = viewModel::onEvent,
                                onProvinceSelected = { province ->
                                    viewModel.onEvent(SignupEvent.ProvinceChanged(province))
                                },
                                onCitySelected = { city ->
                                    viewModel.onEvent(SignupEvent.CityChanged(city))
                                },
                                onNext = { currentStep = SignupStep.AVATAR }
                            )
                            SignupStep.AVATAR -> StepAvatarSelection(state = formState, onAddPhoto = onSelectImageClick, onNext = { currentStep = SignupStep.LEGAL })
                            SignupStep.LEGAL -> StepLegalAgreements(state = formState, onEvent = viewModel::onEvent, onNext = { currentStep = SignupStep.ACCOUNT })
                            SignupStep.ACCOUNT -> StepAccountCredentials(state = formState, onEvent = viewModel::onEvent, isLoading = uiState.isLoading, onSignup = { viewModel.submitSignup(context) })
                        }
                    }
                    }
            }
        }
    }

    if (uiState.signupSuccess && !uiState.verificationSuccess) {
        OtpVerificationDialog(viewModel, uiState.createdUserId ?: "", { viewModel.cancelSignup(uiState.createdUserId ?: ""); onBackClick() }, gradientTeal, gradientPurple)
    }
}

@Composable
private fun OtpVerificationDialog(viewModel: CaregiverSignupViewModel, uid: String, onCancel: () -> Unit, gradientTeal: Brush, gradientPurple: Brush) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var otpValue by remember { mutableStateOf("") }
    val focusedColor = Color(0xFF6641EC)

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
            Card(modifier = Modifier.fillMaxWidth(0.85f).wrapContentHeight(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Box {
                    Box(modifier = Modifier.matchParentSize().alpha(0.1f).background(gradientTeal).background(gradientPurple))
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Verify Email", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = focusedColor)
                        Spacer(Modifier.height(8.dp))
                        Text("Enter the 6-digit code sent to your email.", textAlign = TextAlign.Center, color = Color(0xFF4A4A4A), fontSize = 14.sp)
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

                        if (uiState.errorMessage != null && uiState.errorMessage!!.contains("OTP", ignoreCase = true)) {
                            Text(text = uiState.errorMessage!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }

                        Spacer(Modifier.height(24.dp))
                        GradientButton(text = "Verify Code", onClick = { viewModel.verifyOtp(uid, otpValue) }, isLoading = uiState.isLoading, enabled = otpValue.length == 6)
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = onCancel) { Text("Cancel", color = Color.Gray) }
                            TextButton(onClick = { viewModel.resendOtp(context, uid) }) { Text("Resend Code", color = focusedColor) }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepPersonalDetails(
    state: SignupFormState,
    onProvinceSelected: (String) -> Unit,
    onCitySelected: (String) -> Unit,
    onEvent: (SignupEvent) -> Unit,
    onNext: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var isSexExpanded by remember { mutableStateOf(false) }
    var showCountryInfo by remember { mutableStateOf(false) }

    // Country logic: Hardcoded to Philippines
    LaunchedEffect(Unit) {
        if (state.country != "Philippines") {
            onEvent(SignupEvent.CountryChanged("Philippines"))
        }
    }

    val sexOptions = listOf("Male", "Female", "Rather not say")
    var age by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.birthday) {
        state.birthday?.let {
            val dob = Calendar.getInstance().apply { time = it.toDate() }
            val today = Calendar.getInstance()
            var currentAge = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                currentAge--
            }
            age = currentAge
        }
    }

    val isFirstNameError = state.firstName.isEmpty()
    val isLastNameError = state.lastName.isEmpty()
    val isPhoneError = state.phone.isEmpty() || !state.phone.startsWith("09") || state.phone.length != 11
    val isAddressError = state.addressDetails.isEmpty()
    val isSexError = state.sex.isEmpty()
    val isBirthdayError = state.birthday == null
    val isAgeError = age !in 18..60 && state.birthday != null

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { Button(onClick = { showDatePicker = false; datePickerState.selectedDateMillis?.let { onEvent(SignupEvent.BirthdayChanged(Timestamp(Date(it)))) } }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    // Reduced Padding to 16.dp to decrease height
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Step 1: Personal Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6641EC))
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.firstName,
            onValueChange = { if (it.all { c -> c.isLetter() || c.isWhitespace() }) onEvent(SignupEvent.FirstNameChanged(it)) },
            label = { Text("First Name *") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = isFirstNameError && state.firstName.isNotEmpty(),
            singleLine = true
        )
        // Removed Required Text
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = state.middleName,
            onValueChange = { if (it.all { c -> c.isLetter() || c.isWhitespace() }) onEvent(SignupEvent.MiddleNameChanged(it)) },
            label = { Text("Middle Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = state.lastName,
            onValueChange = { if (it.all { c -> c.isLetter() || c.isWhitespace() }) onEvent(SignupEvent.LastNameChanged(it)) },
            label = { Text("Last Name *") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = isLastNameError && state.lastName.isNotEmpty(),
            singleLine = true
        )
        // Removed Required Text
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = state.phone,
            onValueChange = { if (it.length <= 11 && it.all { c -> c.isDigit() }) onEvent(SignupEvent.PhoneChanged(it)) },
            label = { Text("Phone (09XXXXXXXXX) *") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = isPhoneError && state.phone.isNotEmpty(),
            singleLine = true
        )
        // Kept Specific Error
        if (isPhoneError && state.phone.isNotEmpty()) Text("Must start with 09 and contain 11 digits", color = Color.Red, fontSize = 10.sp)

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min), // Force children to match heights
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Birthday Box
            Box(Modifier.weight(1f).fillMaxHeight()) {
                Column { // Wrap in Column so error text doesn't deform the Row
                    OutlinedTextField(
                        value = if (state.birthday != null) SimpleDateFormat(
                            "MMM dd, yyyy",
                            Locale.getDefault()
                        ).format(state.birthday.toDate()) else "",
                        onValueChange = {}, readOnly = true,
                        label = { Text("Birthday *") },
                        modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        isError = isAgeError,
                        trailingIcon = {
                            Icon(
                                Icons.Default.DateRange,
                                null,
                                Modifier.clickable { showDatePicker = true })
                        }
                    )
                    if (isAgeError) {
                        Text(
                            "Must be 18-60 years old",
                            color = Color.Red,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            // Sex Box
            Box(Modifier.weight(1f).fillMaxHeight()) {
                ExposedDropdownMenuBox(
                    expanded = isSexExpanded,
                    onExpandedChange = { isSexExpanded = !isSexExpanded }
                ) {
                    OutlinedTextField(
                        value = state.sex,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sex *") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(), // Add fillMaxWidth here
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(isSexExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = isSexExpanded,
                        onDismissRequest = { isSexExpanded = false }) {
                        sexOptions.forEach { op ->
                            DropdownMenuItem(
                                text = { Text(op) },
                                onClick = {
                                    onEvent(SignupEvent.SexChanged(op)); isSexExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // Province Dropdown
        LocationDropdown(
            label = "Province *",
            options = state.availableProvinces,
            selectedOption = state.province,
            onOptionSelected = onProvinceSelected
        )

        Spacer(Modifier.height(8.dp))

        // City Dropdown
        LocationDropdown(
            label = if (state.province.isEmpty()) "Please pick a province *" else "City/Municipality *",
            options = state.availableCities,
            selectedOption = state.city,
            onOptionSelected = onCitySelected
        )

        // Home Address field
        OutlinedTextField(
            value = state.addressDetails,
            onValueChange = { onEvent(SignupEvent.AddressChanged(it)) },
            label = { Text("Home Address (Lot./House #/Brgy.) *") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = isAddressError && state.addressDetails.isNotEmpty(),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        // Country Dropdown. Set "Philippines" to fixed. For now.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = "Philippines",
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Country") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = Color.Gray,
                    disabledBorderColor = Color.LightGray,
                    disabledLabelColor = Color.Gray,
                    disabledTrailingIconColor = Color(0xFF6641EC)
                ),
                trailingIcon = {
                    IconButton(onClick = { showCountryInfo = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Country Information"
                        )
                    }
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        val isValid = !isFirstNameError && !isLastNameError && !isPhoneError &&
                !isAddressError && !isSexError && !isBirthdayError && !isAgeError

        GradientButton(text = "Next", onClick = onNext, enabled = isValid)

        if (showCountryInfo) {
            AlertDialog(
                onDismissRequest = { showCountryInfo = false },
                confirmButton = {
                    TextButton(onClick = { showCountryInfo = false }) { Text("Got it") }
                },
                title = { Text("Country Selection") },
                text = {
                    Text("Currently, our services are limited to the Philippines to manage the scope of this capstone project effectively.")
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = Color.White
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }

    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            shape = RoundedCornerShape(12.dp),
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(12.dp)
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption) },
                    onClick = {
                        onOptionSelected(selectionOption)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun StepAvatarSelection(state: SignupFormState, onAddPhoto: () -> Unit, onNext: () -> Unit) {
    val focusedColor = Color(0xFF6641EC)

    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Step 2: Profile Picture", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = focusedColor, modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(32.dp))

        Box(
            modifier = Modifier.size(160.dp).clip(CircleShape).background(Color(0xFFF0F0F0)).border(2.dp, focusedColor, CircleShape).clickable { onAddPhoto() },
            contentAlignment = Alignment.Center
        ) {
            if (state.profileImageUri != null) {
                Image(painter = rememberAsyncImagePainter(state.profileImageUri), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddAPhoto, null, tint = focusedColor, modifier = Modifier.size(40.dp))
                    Text("Add Photo", color = focusedColor, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(48.dp))
        GradientButton(text = if (state.profileImageUri == null) "Skip" else "Next", onClick = onNext)
    }
}

@Composable
fun StepLegalAgreements(state: SignupFormState, onEvent: (SignupEvent) -> Unit, onNext: () -> Unit) {
    val focusedColor = Color(0xFF6641EC)

    var selectedTab by remember { mutableIntStateOf(0) }

    Column(Modifier.padding(24.dp)) {
        Text("Step 3: Legal Agreements", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = focusedColor)
        Text("Please read and accept both policies.", fontSize = 12.sp, color = Color.Gray)

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE0E0E0))) {
            TabButton(
                text = "Terms",
                isSelected = selectedTab == 0,
                isCompleted = state.termsAccepted,
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                text = "Privacy",
                isSelected = selectedTab == 1,
                isCompleted = state.privacyAccepted,
                onClick = { selectedTab = 1 },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
        ) {
            if (selectedTab == 0) {
                ScrollableAgreementViewer(
                    title = "Terms & Conditions",
                    content = LegalDocuments.TERMS_AND_CONDITIONS,
                    isAccepted = state.termsAccepted,
                    onAcceptChange = { onEvent(SignupEvent.TermsChanged(it)) }
                )
            } else {
                ScrollableAgreementViewer(
                    title = "Privacy Policy",
                    content = LegalDocuments.PRIVACY_POLICY,
                    isAccepted = state.privacyAccepted,
                    onAcceptChange = { onEvent(SignupEvent.PrivacyChanged(it)) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        GradientButton(
            text = "Agree & Continue",
            onClick = onNext,
            enabled = state.termsAccepted && state.privacyAccepted
        )
    }
}

@Composable
private fun TabButton(text: String, isSelected: Boolean, isCompleted: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val activeColor = Color(0xFF6641EC)
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(if (isSelected) Color.White else Color.Transparent)
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text,
                color = if (isSelected) activeColor else Color.Gray,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )
            if (isCompleted) {
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun ScrollableAgreementViewer(
    title: String,
    content: String,
    isAccepted: Boolean,
    onAcceptChange: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()
    val focusedColor = Color(0xFF6641EC)

    val isAtBottom by remember {
        derivedStateOf {
            val isScrollable = scrollState.maxValue > 0
            !isScrollable || (scrollState.value >= (scrollState.maxValue - 50))
        }
    }

    var hasRead by remember { mutableStateOf(isAccepted) }
    LaunchedEffect(isAtBottom) {
        if (isAtBottom) hasRead = true
    }

    Column(Modifier.fillMaxSize()) {
        Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))

        HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)

        Box(Modifier.weight(1f)) {
            Text(
                text = content,
                fontSize = 13.sp,
                color = Color.DarkGray,
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = !isAtBottom,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFFF8F9FA))))
                )
            }
        }

        HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = hasRead) { onAcceptChange(!isAccepted) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isAccepted,
                onCheckedChange = null,
                enabled = hasRead,
                colors = CheckboxDefaults.colors(checkedColor = focusedColor, uncheckedColor = Color.Gray)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = if (hasRead) "I have read and agree" else "Scroll to the bottom to agree",
                    fontSize = 14.sp,
                    fontWeight = if (hasRead) FontWeight.Medium else FontWeight.Normal,
                    color = if (hasRead) Color.Black else Color.Gray
                )
            }
            if (!hasRead) {
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.LightGray, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun StepAccountCredentials(state: SignupFormState, onEvent: (SignupEvent) -> Unit, isLoading: Boolean, onSignup: () -> Unit) {
    var passVis by remember { mutableStateOf(false) }
    var rePassVis by remember { mutableStateOf(false) }
    val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(state.email).matches()

    // Password Validation: 8+ chars, 1 Uppercase, 1 Lowercase, 1 Special Char
    val hasUpperCase = state.password.any { it.isUpperCase() }
    val hasLowerCase = state.password.any { it.isLowerCase() }
    val hasSpecialChar = state.password.any { !it.isLetterOrDigit() }
    val hasMinLength = state.password.length >= 8

    val isPassValid = hasMinLength && hasUpperCase && hasLowerCase && hasSpecialChar
    val isRePassValid = state.password == state.rePassword && state.password.isNotEmpty()

    Column(Modifier.padding(24.dp)) {
        Text("Step 4: Account Info", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6641EC))
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(state.email, { onEvent(SignupEvent.EmailChanged(it)) }, label = { Text("Email *") }, isError = !isEmailValid && state.email.isNotEmpty(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true)
        if (state.email.isEmpty()) Text("Required", color = Color.Red, fontSize = 10.sp)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(state.password, { onEvent(SignupEvent.PasswordChanged(it)) }, label = { Text("Password *") }, isError = !isPassValid && state.password.isNotEmpty(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), visualTransformation = if (passVis) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton({ passVis = !passVis }) { Icon(if (passVis) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null) } }, singleLine = true)

        if (!isPassValid && state.password.isNotEmpty()) {
            Column {
                if (!hasMinLength) Text("• Min 8 characters", color = Color.Red, fontSize = 10.sp)
                if (!hasUpperCase) Text("• At least 1 uppercase letter", color = Color.Red, fontSize = 10.sp)
                if (!hasLowerCase) Text("• At least 1 lowercase letter", color = Color.Red, fontSize = 10.sp)
                if (!hasSpecialChar) Text("• At least 1 special character", color = Color.Red, fontSize = 10.sp)
            }
        } else if (state.password.isEmpty()) {
            Text("Required", color = Color.Red, fontSize = 10.sp)
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(state.rePassword, { onEvent(SignupEvent.RePasswordChanged(it)) }, label = { Text("Confirm Password *") }, isError = !isRePassValid && state.rePassword.isNotEmpty(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), visualTransformation = if (rePassVis) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton({ rePassVis = !rePassVis }) { Icon(if (rePassVis) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null) } }, singleLine = true)
        if (!isRePassValid && state.rePassword.isNotEmpty()) Text("Passwords do not match", color = Color.Red, fontSize = 10.sp)
        else if (state.rePassword.isEmpty()) Text("Required", color = Color.Red, fontSize = 10.sp)

        Spacer(Modifier.height(24.dp))
        GradientButton(text = "Create Account", onClick = onSignup, isLoading = isLoading, enabled = isEmailValid && isPassValid && isRePassValid)
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