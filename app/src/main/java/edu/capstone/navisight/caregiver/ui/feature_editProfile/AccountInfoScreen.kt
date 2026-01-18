package edu.capstone.navisight.caregiver.ui.feature_editProfile

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.capstone.navisight.caregiver.model.Caregiver
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private fun isAgeValid(timestamp: Timestamp?): Boolean {
    if (timestamp == null) return false
    val selectedDate = timestamp.toDate()
    val today = Calendar.getInstance()
    val birthDate = Calendar.getInstance()
    birthDate.time = selectedDate
    var age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)
    if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) {
        age--
    }
    return age in 18..60
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountInfoScreen(
    profile: Caregiver?,
    selectedImageUri: Uri?,
    onPickImage: () -> Unit,
    onCheckLockout: () -> Unit,
    onSave: (String, String, String, String, Timestamp?, String, String) -> Unit,
    onRequestPasswordChange: (String, String) -> Unit,
    onChangeEmail: (String, String) -> Unit,
    onVerifyEmailOtp: (String) -> Unit,
    onVerifyPasswordOtp: (String) -> Unit,
    onResendPasswordOtp: () -> Unit,
    onCancelEmailChange: () -> Unit,
    onDeleteAccount: (String) -> Unit,
    onCancelPasswordChange: () -> Unit,
    isSaving: Boolean,
    reauthError : String?,
    uiMessage: String?,
    onMessageShown: () -> Unit,
    onBackClick: () -> Unit = {}
) {
    var firstName by remember(profile?.firstName) { mutableStateOf(profile?.firstName ?: "") }
    var middleName by remember(profile?.middleName) { mutableStateOf(profile?.middleName ?: "") }
    var lastName by remember(profile?.lastName) { mutableStateOf(profile?.lastName ?: "") }
    var phone by remember(profile?.phoneNumber) { mutableStateOf(profile?.phoneNumber ?: "") }
    var address by remember(profile?.address) { mutableStateOf(profile?.address ?: "") }

    var birthdayText by remember(profile?.birthday) { mutableStateOf("") }
    var selectedBirthdayTimestamp by remember(profile?.birthday) { mutableStateOf<Timestamp?>(null) }
    val dateFormatter = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var firstNameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var birthdayError by remember { mutableStateOf<String?>(null) }

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var showEmailOtpDialog by remember { mutableStateOf(false) }
    var showReauthDialog by remember { mutableStateOf(false) }
    var showPasswordOtpDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // --- SUCCESS POPUP STATE ---
    var showSuccessPopup by remember { mutableStateOf(false) }
    var successPopupMessage by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showCountryInfo by remember { mutableStateOf(false) }

    LaunchedEffect(showReauthDialog) {
        if (showReauthDialog) {
            onCheckLockout()
        }
    }

    // --- DIRTY CHECK ---
    val hasChanges = remember(
        firstName, middleName, lastName, phone, selectedBirthdayTimestamp, address, profile
    ) {
        profile?.let {
            firstName != (it.firstName ?: "") ||
                    middleName != (it.middleName ?: "") ||
                    lastName != (it.lastName ?: "") ||
                    phone != (it.phoneNumber ?: "") ||
                    selectedBirthdayTimestamp != it.birthday ||
                    address != (it.address ?: "")
        } ?: false
    }

    // Check for input validation
    val isFormValid = remember(firstName, lastName, phone, selectedBirthdayTimestamp, address) {
        firstName.isNotBlank() &&
                lastName.isNotBlank() &&
                phone.isNotBlank() &&
                selectedBirthdayTimestamp != null &&
                address.isNotBlank()
    }

    // Colors
    val focusedColor = Color(0xFF6641EC)
    val unfocusedColor = Color.Black
    val sectionHeaderColor = Color(0xFF8E41E8)
    val fieldLabelColor = Color(0xFF8E41E8)
    val gradientBrush = Brush.horizontalGradient(colors = listOf(Color(0xFFAA41E5), Color(0xFF6342ED)))
    val disabledBrush = Brush.horizontalGradient(colors = listOf(Color.Gray, Color.LightGray))

    // OTP State Variables
    var emailOtpTriesLeft by remember { mutableStateOf(3) }
    var emailResendCount by remember { mutableStateOf(0) }
    var emailResendWaitSeconds by remember { mutableStateOf(0) }
    var isEmailResendWaiting by remember { mutableStateOf(false) }
    var emailCooldownSeconds by remember { mutableStateOf(0) }
    var emailBackendError by remember { mutableStateOf<String?>(null) }
    var pendingNewEmail by remember { mutableStateOf("") }
    var pendingPassword by remember { mutableStateOf("") }

    var passwordOtpTriesLeft by remember { mutableStateOf(3) }
    var passwordResendCount by remember { mutableStateOf(0) }
    var passwordResendWaitSeconds by remember { mutableStateOf(0) }
    var isPasswordResendWaiting by remember { mutableStateOf(false) }
    var passwordCooldownSeconds by remember { mutableStateOf(0) }
    var passwordBackendError by remember { mutableStateOf<String?>(null) }


    // Timers
    LaunchedEffect(isEmailResendWaiting) {
        if (isEmailResendWaiting) {
            emailResendWaitSeconds = 60
            while (emailResendWaitSeconds > 0) { delay(1000L); emailResendWaitSeconds-- }
            isEmailResendWaiting = false
        }
    }
    LaunchedEffect(emailCooldownSeconds) {
        if (emailCooldownSeconds > 0) {
            while (emailCooldownSeconds > 0) { delay(1000L); emailCooldownSeconds-- }
        }
    }
    LaunchedEffect(isPasswordResendWaiting) {
        if (isPasswordResendWaiting) {
            passwordResendWaitSeconds = 60
            while (passwordResendWaitSeconds > 0) { delay(1000L); passwordResendWaitSeconds-- }
            isPasswordResendWaiting = false
        }
    }
    LaunchedEffect(passwordCooldownSeconds) {
        if (passwordCooldownSeconds > 0) {
            while (passwordCooldownSeconds > 0) { delay(1000L); passwordCooldownSeconds-- }
        }
    }

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

    LaunchedEffect(profile) {
        if (profile != null) {
            firstName = profile.firstName ?: ""
            middleName = profile.middleName ?: ""
            lastName = profile.lastName ?: ""
            phone = profile.phoneNumber ?: ""
            address = profile.address ?: ""
            selectedBirthdayTimestamp = profile.birthday
            birthdayText = profile.birthday?.toDate()?.let { dateFormatter.format(it) } ?: ""
            birthdayError = if (birthdayText.isNotBlank() && !isAgeValid(selectedBirthdayTimestamp)) "Must be 18-60 years old" else null
        }
    }

    // UNIFIED DIALOG HANDLING LOGIC
    LaunchedEffect(uiMessage) {
        uiMessage?.let { message ->
            // Only show snackbar if it is NOT one of the success messages we handle with a popup
            val isSuccessMessage = message.contains("Profile updated successfully", ignoreCase = true) ||
                    message.contains("Email successfully verified", ignoreCase = true) ||
                    message.contains("Password updated successfully", ignoreCase = true) ||
                    message.contains("Photo updated successfully", ignoreCase = true)

            if (!isSuccessMessage) {
                snackbarHostState.showSnackbar(message)
            }

            // Profile Save Success: Close Reauth Dialog & Show Success
            if (message.contains("Profile updated successfully", ignoreCase = true)) {
                showReauthDialog = false
                successPopupMessage = "Profile updated successfully!"
                showSuccessPopup = true
            }

            // Photo Update Success: Show Success (No dialog to close)
            if (message.contains("Photo updated successfully", ignoreCase = true)) {
                successPopupMessage = "Photo updated successfully!"
                showSuccessPopup = true
            }

            // Email Change Success: Close OTP Dialog & Show Success
            if (message.contains("Email successfully verified", ignoreCase = true)) {
                showEmailOtpDialog = false
                emailCooldownSeconds = 0
                emailResendCount = 0
                successPopupMessage = "Email updated successfully!"
                showSuccessPopup = true
            }

            // Password Change Success: Close OTP Dialog & Show Success
            if (message.contains("Password updated successfully", ignoreCase = true)) {
                showPasswordOtpDialog = false
                passwordCooldownSeconds = 0
                passwordResendCount = 0
                successPopupMessage = "Password updated successfully!"
                showSuccessPopup = true
            }

            // Password Change Request Success: Close Input Dialog, Open OTP Dialog
            if (message.contains("Password OTP Sent", ignoreCase = true)) {
                showPasswordDialog = false
                showPasswordOtpDialog = true
                passwordOtpTriesLeft = 3
                isPasswordResendWaiting = true
                passwordCooldownSeconds = 0
                passwordBackendError = null
                passwordResendCount = 1
            }

            // Email Change Request Success: Open OTP Dialog
            if (message.contains("OTP has been sent to", ignoreCase = true)) {
                showEmailOtpDialog = true
                emailOtpTriesLeft = 3
                isEmailResendWaiting = true
                emailCooldownSeconds = 0
                emailBackendError = null
                emailResendCount++
                if (emailResendCount >= 3) emailCooldownSeconds = 300
            }

            // Error/Flow Handling
            if (message.contains("Please wait 5 minutes to request a new OTP", ignoreCase = true)) {
                showEmailDialog = false
                showEmailOtpDialog = false
                emailCooldownSeconds = 300
                emailResendCount = 3
            }
            if (message.contains("Password OTP Resent", ignoreCase = true)) {
                showPasswordOtpDialog = true
                passwordOtpTriesLeft = 3
                isPasswordResendWaiting = true
                passwordBackendError = null
                passwordResendCount++
                if (passwordResendCount >= 3) passwordCooldownSeconds = 300
            }
            if (message.contains("Please wait 5 minutes to resend password OTP", ignoreCase = true)) {
                showPasswordOtpDialog = false
                passwordCooldownSeconds = 300
                passwordResendCount = 3
            }
            if (message.contains("Password change is locked", ignoreCase = true) ||
                message.contains("Too many attempts. Password change locked", ignoreCase = true)) {
                showPasswordDialog = false
                passwordCooldownSeconds = 300
            }
            if (message.contains("Invalid OTP", ignoreCase = true)) {
                if (showEmailOtpDialog) {
                    emailOtpTriesLeft--
                    emailBackendError = "Wrong OTP. Please try again."
                }
                if (showPasswordOtpDialog) {
                    passwordOtpTriesLeft--
                    passwordBackendError = "Wrong OTP. Please try again."
                }
            }
            if (message.contains("Too many attempts. OTP is now invalid", ignoreCase = true)) {
                if (showEmailOtpDialog) {
                    showEmailOtpDialog = false
                    emailCooldownSeconds = 300
                }
                if (showPasswordOtpDialog) {
                    showPasswordOtpDialog = false
                    passwordCooldownSeconds = 300
                }
            }
            onMessageShown()
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedBirthdayTimestamp?.toDate()?.time)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        datePickerState.selectedDateMillis?.let { millis ->
                            val newDate = Date(millis)
                            selectedBirthdayTimestamp = Timestamp(newDate)
                            birthdayText = dateFormatter.format(newDate)
                            birthdayError = if (isAgeValid(selectedBirthdayTimestamp)) null else "Must be 18-60 years old"
                        }
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        topBar = {
            Box(modifier = Modifier.fillMaxWidth().background(gradientBrush)) {
                TopAppBar(
                    title = { Text("Edit Your Companion Profile", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF0F0F0)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CaregiverProfileHeader(
                    caregiverData = profile,
                    selectedImageUri = selectedImageUri,
                    isUploading = isSaving,
                    onImageClick = onPickImage
                )

                Spacer(Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Personal Information", style = MaterialTheme.typography.titleLarge, color = sectionHeaderColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally))

                    Text("Name", color = fieldLabelColor)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                        // First Name
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = {
                                if (it.all { ch -> ch.isLetter() || ch.isWhitespace() }) firstName =
                                    it
                            },
                            label = { Text("First Name") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            isError = firstName.isBlank(),
                            supportingText = if (firstName.isBlank()) {
                                { Text("Required", color = MaterialTheme.colorScheme.error) }
                            } else null,
                            colors = customTextFieldColors,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // MIDDLE NAME
                        OutlinedTextField(
                            value = middleName,
                            onValueChange = {
                                if (it.all { ch -> ch.isLetter() || ch.isWhitespace() }) middleName =
                                    it
                            },
                            label = { Text("Middle") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = customTextFieldColors,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // LAST NAME
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = {
                                if (it.all { ch -> ch.isLetter() || ch.isWhitespace() }) lastName =
                                    it
                            },
                            label = { Text("Last Name") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            isError = lastName.isBlank(),
                            supportingText = if (lastName.isBlank()) {
                                { Text("Required", color = MaterialTheme.colorScheme.error) }
                            } else null,
                            colors = customTextFieldColors,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1.3f)) {
                            Text(
                                text = "Birthday",
                                color = fieldLabelColor,
                                modifier = Modifier.padding(bottom = 4.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedTextField(
                                value = birthdayText,
                                onValueChange = {},
                                label = { Text("Select Date") },
                                readOnly = true,
                                isError = birthdayError != null,
                                colors = customTextFieldColors,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDatePicker = true },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Select Date",
                                        tint = unfocusedColor.copy(alpha = 0.7f),
                                        modifier = Modifier.clickable { showDatePicker = true }
                                    )
                                },
                                supportingText = if (birthdayError != null) {
                                    { Text(birthdayError!!, color = MaterialTheme.colorScheme.error) }
                                } else null
                            )
                        }

                        // PHONE NUMBER
                        Column(modifier = Modifier.weight(0.7f)) {
                            Text(
                                text = "Phone Number",
                                color = fieldLabelColor,
                                modifier = Modifier.padding(bottom = 4.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { input ->
                                    // Restrict to 11 digits only for PH standards
                                    if (input.all { it.isDigit() } && input.length <= 11) {
                                        phone = input
                                    }
                                },
                                label = { Text("09XXXXXXXXX") },
                                isError = phoneError != null,
                                colors = customTextFieldColors,
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                supportingText = if (phoneError != null) {
                                    { Text(phoneError!!, color = MaterialTheme.colorScheme.error) }
                                } else null
                            )
                        }
                    }


                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Home Address") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = customTextFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        isError = address.isBlank(),
                        supportingText = if (address.isBlank()) {
                            { Text("This field cannot be empty", color = MaterialTheme.colorScheme.error) }
                        } else {
                            null
                        }
                    )


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

                    Text("Authentication & Contact", color = fieldLabelColor)
                    OutlinedTextField(value = profile?.email ?: "", onValueChange = {}, label = { Text("Email (Uneditable)") }, readOnly = true, modifier = Modifier.fillMaxWidth(), colors = customTextFieldColors, shape = RoundedCornerShape(12.dp))

                    Button(
                        onClick = { showReauthDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        // Disable if no changes OR if the form is invalid
                        enabled = hasChanges && isFormValid && !isSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    // Use the gradient only if enabled, otherwise Solid Grey
                                    brush = if (hasChanges && isFormValid) gradientBrush else SolidColor(
                                        Color.LightGray
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "SAVE CHANGES",
                                color = if (hasChanges && isFormValid) Color.White else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(
                            color = Color(0xFF4A3BA0),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp) // Inner padding
                ) {
                    Text(
                        text = "Security & Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White, // Title is now white
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // EMAIL CHANGE
                        Button(
                            onClick = { showEmailDialog = true },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.15f), // This is called "Glassmorphism". Okay...
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Update Email",
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // PASSWORD CHANGE
                        Button(
                            onClick = { showPasswordDialog = true },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.15f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Change Password",
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                // --- NEW DANGER ZONE (Delete Account Separate Square) ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(16.dp)) // Light Red Background
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Danger Zone",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }

                    Text(
                        "Once you delete your account, there is no going back. Please be certain.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Button(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Delete Account", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    // Dialogs

    // Success Popup
    if (showSuccessPopup) {
        SuccessDialog(
            message = successPopupMessage,
            onDismiss = { showSuccessPopup = false }
        )
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(
            onConfirm = { current, new ->
                passwordResendCount = 0
                onRequestPasswordChange(current, new)
            },
            onDismiss = { showPasswordDialog = false }
        )
    }

    if (showEmailDialog) {
        ChangeEmailDialog(
            onConfirm = { newEmail, password ->
                if (emailCooldownSeconds > 0) {
                    scope.launch { snackbarHostState.showSnackbar("Please wait ${emailCooldownSeconds}s to try again.") }
                } else {
                    pendingNewEmail = newEmail
                    pendingPassword = password
                    emailResendCount = 0
                    onChangeEmail(newEmail, password)
                    showEmailDialog = false
                }
            },
            onDismiss = { showEmailDialog = false }
        )
    }

    if (showEmailOtpDialog) {
        OtpVerificationDialog(
            title = "Verify New Email",
            triesLeft = emailOtpTriesLeft,
            backendError = emailBackendError,
            resendWaitSeconds = emailResendWaitSeconds,
            cooldownSeconds = emailCooldownSeconds,
            isResendLimitReached = emailResendCount >= 3,
            onConfirm = { otp -> emailBackendError = null; onVerifyEmailOtp(otp) },
            onDismiss = { showEmailOtpDialog = false; emailBackendError = null; onCancelEmailChange() },
            onResend = { emailBackendError = null; onChangeEmail(pendingNewEmail, pendingPassword); showEmailOtpDialog = false },
            onClearError = { emailBackendError = null }
        )
    }

    if (showPasswordOtpDialog) {
        OtpVerificationDialog(
            title = "Verify Password Change",
            triesLeft = passwordOtpTriesLeft,
            backendError = passwordBackendError,
            resendWaitSeconds = passwordResendWaitSeconds,
            cooldownSeconds = passwordCooldownSeconds,
            isResendLimitReached = passwordResendCount >= 3,
            onConfirm = { otp -> passwordBackendError = null; onVerifyPasswordOtp(otp) },
            onDismiss = { showPasswordOtpDialog = false; passwordBackendError = null; onCancelPasswordChange() },
            onResend = { passwordBackendError = null; onResendPasswordOtp(); showPasswordOtpDialog = false },
            onClearError = { passwordBackendError = null }
        )
    }

    if (showReauthDialog) {
        ReauthenticationDialog(
            isSaving = isSaving,
            errorMessage = reauthError,
            onConfirm = { password ->
                // Your existing save logic
                onSave(firstName, middleName, lastName, phone, selectedBirthdayTimestamp, address, password)
            },
            onDismiss = {
                showReauthDialog = false
            }
        )
    }
    if (showDeleteDialog) {
        DeleteAccountConfirmDialog(
            onConfirm = { password ->
                onDeleteAccount(password)
            },
            onDismiss = { showDeleteDialog = false },
            isDeleting = isSaving,
            errorMessage = reauthError
        )
    }
}