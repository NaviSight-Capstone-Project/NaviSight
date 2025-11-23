package edu.capstone.navisight.caregiver.ui.feature_editProfile

import android.net.Uri
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    onSave: (String, String, String, String, Timestamp?, String, String) -> Unit,
    onRequestPasswordChange: (String, String) -> Unit,
    onChangeEmail: (String, String) -> Unit,
    onVerifyEmailOtp: (String) -> Unit,
    onVerifyPasswordOtp: (String) -> Unit,
    onResendPasswordOtp: () -> Unit,
    onCancelEmailChange: () -> Unit,
    onCancelPasswordChange: () -> Unit,
    isSaving: Boolean,
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

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var emailOtpTriesLeft by remember { mutableStateOf(3) }
    var emailResendCount by remember { mutableStateOf(0) }
    var emailResendWaitSeconds by remember { mutableStateOf(0) }
    var isEmailResendWaiting by remember { mutableStateOf(false) }
    var emailCooldownSeconds by remember { mutableStateOf(0) }
    var emailBackendError by remember { mutableStateOf<String?>(null) }

    val focusedColor = Color(0xFF6641EC)
    val unfocusedColor = Color.Black
    val sectionHeaderColor = Color(0xFF8E41E8)
    val fieldLabelColor = Color(0xFF8E41E8)

    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(Color(0xFFAA41E5), Color(0xFF6342ED))
    )

    var pendingNewEmail by remember { mutableStateOf("") }
    var pendingPassword by remember { mutableStateOf("") }

    var passwordOtpTriesLeft by remember { mutableStateOf(3) }
    var passwordResendCount by remember { mutableStateOf(0) }
    var passwordResendWaitSeconds by remember { mutableStateOf(0) }
    var isPasswordResendWaiting by remember { mutableStateOf(false) }
    var passwordCooldownSeconds by remember { mutableStateOf(0) }
    var passwordBackendError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isEmailResendWaiting) {
        if (isEmailResendWaiting) {
            emailResendWaitSeconds = 60
            while (emailResendWaitSeconds > 0) {
                delay(1000L)
                emailResendWaitSeconds--
            }
            isEmailResendWaiting = false
        }
    }

    LaunchedEffect(emailCooldownSeconds) {
        if (emailCooldownSeconds > 0) {
            while (emailCooldownSeconds > 0) {
                delay(1000L)
                emailCooldownSeconds--
            }
        }
    }

    LaunchedEffect(isPasswordResendWaiting) {
        if (isPasswordResendWaiting) {
            passwordResendWaitSeconds = 60
            while (passwordResendWaitSeconds > 0) {
                delay(1000L)
                passwordResendWaitSeconds--
            }
            isPasswordResendWaiting = false
        }
    }

    LaunchedEffect(passwordCooldownSeconds) {
        if (passwordCooldownSeconds > 0) {
            while (passwordCooldownSeconds > 0) {
                delay(1000L)
                passwordCooldownSeconds--
            }
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
            // Re-validate birthday on load
            birthdayError = if (birthdayText.isNotBlank() && !isAgeValid(selectedBirthdayTimestamp)) "Must be 18-60 years old" else null
        }
    }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)

            val cooldownTime = 300

            if (it.contains("OTP has been sent to", ignoreCase = true)) {
                showEmailOtpDialog = true
                emailOtpTriesLeft = 3
                isEmailResendWaiting = true
                emailCooldownSeconds = 0
                emailBackendError = null
                emailResendCount++
                if (emailResendCount >= 3) {
                    emailCooldownSeconds = cooldownTime
                }
            }
            if (it.contains("Email successfully verified", ignoreCase = true)) {
                showEmailOtpDialog = false
                emailCooldownSeconds = 0
                emailResendCount = 0
            }
            if (it.contains("Please wait 5 minutes to request a new OTP", ignoreCase = true)) {
                showEmailDialog = false
                showEmailOtpDialog = false
                emailCooldownSeconds = cooldownTime
                emailResendCount = 3
            }

            if (it.contains("Password OTP Sent", ignoreCase = true)) {
                showPasswordDialog = false
                showPasswordOtpDialog = true
                passwordOtpTriesLeft = 3
                isPasswordResendWaiting = true
                passwordCooldownSeconds = 0
                passwordBackendError = null
                passwordResendCount = 1
            }
            if (it.contains("Password OTP Resent", ignoreCase = true)) {
                showPasswordOtpDialog = true
                passwordOtpTriesLeft = 3
                isPasswordResendWaiting = true
                passwordBackendError = null
                passwordResendCount++
                if (passwordResendCount >= 3) {
                    passwordCooldownSeconds = cooldownTime
                }
            }
            if (it.contains("Please wait 5 minutes to resend password OTP", ignoreCase = true)) {
                showPasswordOtpDialog = false
                passwordCooldownSeconds = cooldownTime
                passwordResendCount = 3
            }
            if (it.contains("Password updated successfully", ignoreCase = true)) {
                showPasswordOtpDialog = false
                passwordCooldownSeconds = 0
                passwordResendCount = 0
            }

            if (it.contains("Password change is locked", ignoreCase = true) ||
                it.contains("Too many attempts. Password change locked", ignoreCase = true)) {
                showPasswordDialog = false
                passwordCooldownSeconds = cooldownTime
            }

            if (it.contains("Invalid OTP. Please try again.", ignoreCase = true)) {
                if (showEmailOtpDialog) {
                    emailOtpTriesLeft--
                    emailBackendError = "Wrong OTP. Please try again."
                }
                if (showPasswordOtpDialog) {
                    passwordOtpTriesLeft--
                    passwordBackendError = "Wrong OTP. Please try again."
                }
            }

            if (it.contains("Too many attempts. OTP is now invalid.", ignoreCase = true)) {
                if (showEmailOtpDialog) {
                    showEmailOtpDialog = false
                    emailCooldownSeconds = cooldownTime
                }
                if (showPasswordOtpDialog) {
                    showPasswordOtpDialog = false
                    passwordCooldownSeconds = cooldownTime
                }
            }
            onMessageShown()
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedBirthdayTimestamp?.toDate()?.time
        )
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
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(gradientBrush)
            ) {
                TopAppBar(
                    title = { Text("Edit Caregiver Profile", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF0F0F0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 100.dp),
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
                    Text(
                        "Personal Information", // Section Header
                        style = MaterialTheme.typography.titleLarge,
                        color = sectionHeaderColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .align(Alignment.CenterHorizontally)
                    )

                    Text(
                        "Name",
                        color = fieldLabelColor,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { if (it.all { ch -> ch.isLetter() || ch.isWhitespace() }) firstName = it },
                            label = { Text("First Name", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = customTextFieldColors,
                            shape = RoundedCornerShape(12.dp),
                        )
                        OutlinedTextField(
                            value = middleName,
                            onValueChange = { if (it.all { ch -> ch.isLetter() || ch.isWhitespace() }) middleName = it },
                            label = { Text("Middle", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = customTextFieldColors,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { if (it.all { ch -> ch.isLetter() || ch.isWhitespace() }) lastName = it },
                            label = { Text("Last Name", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = customTextFieldColors,
                            shape = RoundedCornerShape(12.dp),
                        )
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Birthday",
                                color = fieldLabelColor,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            OutlinedTextField(
                                value = birthdayText,
                                onValueChange = { },
                                label = { Text("Select Date", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                readOnly = true,
                                isError = birthdayError != null,
                                colors = customTextFieldColors,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDatePicker = true },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = "Select Date",
                                        tint = unfocusedColor.copy(alpha = 0.7f),
                                        modifier = Modifier.clickable { showDatePicker = true }
                                    )
                                },
                                supportingText = {
                                    if (birthdayError != null) {
                                        Text(birthdayError!!, color = MaterialTheme.colorScheme.error, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Phone Number",
                                color = fieldLabelColor,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            OutlinedTextField(
                                value = phone,
                                onValueChange = {
                                    if (it.all { c -> c.isDigit() }) phone = it
                                    phoneError = if (phone.isNotBlank() && !phone.matches(Regex("^09\\d{9}$")))
                                        "Must be 11 digits starting with 09"
                                    else
                                        null
                                },
                                label = { Text("09XXXXXXXXX", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                isError = phoneError != null,
                                colors = customTextFieldColors,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                supportingText = {
                                    if (phoneError != null) {
                                        Text(phoneError!!, color = MaterialTheme.colorScheme.error, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            )
                        }
                    }

                    Text(
                        "Address",
                        color = fieldLabelColor,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = customTextFieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text(
                        "Email",
                        color = fieldLabelColor,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = profile?.email ?: "",
                        onValueChange = {},
                        label = { Text("Email (Cannot Change Here)") },
                        readOnly = true,

                        modifier = Modifier.fillMaxWidth(),
                        colors = customTextFieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            phoneError = if (phone.isNotBlank() && !phone.matches(Regex("^09\\d{9}$")))
                                "Must be 11 digits starting with 09"
                            else null

                            birthdayError = if (birthdayText.isNotBlank() && !isAgeValid(selectedBirthdayTimestamp))
                                "Must be 18-60 years old"
                            else if (birthdayText.isBlank())
                                "Birthday is required"
                            else null

                            if (phoneError == null && birthdayError == null) {
                                val hasChanges = (firstName != (profile?.firstName ?: "")) ||
                                        (middleName != (profile?.middleName ?: "")) ||
                                        (lastName != (profile?.lastName ?: "")) ||
                                        (phone != (profile?.phoneNumber ?: "")) ||
                                        (selectedBirthdayTimestamp != profile?.birthday) ||
                                        (address != (profile?.address ?: ""))

                                if (hasChanges) {
                                    showReauthDialog = true
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("No changes to update.")
                                    }
                                }
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    brush = gradientBrush,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (isSaving) "Saving..." else "Save Changes",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                TextButton(onClick = { showPasswordDialog = true }) {
                    Text("Change Password", color = Color.Black)
                }
                TextButton(onClick = { showEmailDialog = true }) {
                    Text("Change Email", color = Color.Black)
                }

                Spacer(Modifier.height(24.dp))

            }
        }
    }


    if (showPasswordDialog) {
        ChangePasswordDialog(
            onConfirm = { current, new ->
                passwordResendCount = 0 // Reset count on new request
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
                    emailResendCount = 0 // Reset count on new request
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
            isResendLimitReached = emailResendCount >= 3, // Check limit (3 total sends)
            onConfirm = { otp ->
                emailBackendError = null
                onVerifyEmailOtp(otp)
            },
            onDismiss = {
                showEmailOtpDialog = false
                emailBackendError = null
                onCancelEmailChange() // Call cancel function
            },
            onResend = {
                emailBackendError = null
                onChangeEmail(pendingNewEmail, pendingPassword) // Call resend
                showEmailOtpDialog = false // Close and wait
            },
            onClearError = {
                emailBackendError = null
            }
        )
    }

    if (showPasswordOtpDialog) {
        OtpVerificationDialog(
            title = "Verify Password Change",
            triesLeft = passwordOtpTriesLeft,
            backendError = passwordBackendError,
            resendWaitSeconds = passwordResendWaitSeconds,
            cooldownSeconds = passwordCooldownSeconds,
            isResendLimitReached = passwordResendCount >= 3, // Check limit (3 total sends)
            onConfirm = { otp ->
                passwordBackendError = null
                onVerifyPasswordOtp(otp)
            },
            onDismiss = {
                showPasswordOtpDialog = false
                passwordBackendError = null
                onCancelPasswordChange() // Call cancel function
            },
            onResend = {
                passwordBackendError = null
                onResendPasswordOtp() // Call resend
                showPasswordOtpDialog = false // Close and wait
            },
            onClearError = {
                passwordBackendError = null
            }
        )
    }

    if (showReauthDialog) {
        ReauthenticationDialog(
            isSaving = isSaving,
            onConfirm = { password ->
                onSave(firstName, middleName, lastName, phone, selectedBirthdayTimestamp, address, password)
            },
            onDismiss = { showReauthDialog = false }
        )
    }
}