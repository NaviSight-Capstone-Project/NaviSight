package edu.capstone.navisight.caregiver.ui.feature_editViuProfile

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import edu.capstone.navisight.caregiver.ui.feature_travel_log.TravelLogScreen
import edu.capstone.navisight.caregiver.ui.feature_travel_log.TravelLogViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun isAgeValid(selectedMillis: Long?): Boolean {
    if (selectedMillis == null) return false
    val selectedDate = Date(selectedMillis)
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
fun ViuProfileScreen(
    viewModel: ViuProfileViewModel,
    travelLogViewModel: TravelLogViewModel, // <--- ADDED THIS
    onNavigateBack: () -> Unit,
    onLaunchImagePicker: () -> Unit
) {
    val viu by viewModel.viu.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val context = LocalContext.current
    val emailFlowState by viewModel.emailFlowState.collectAsState()
    val securityError by viewModel.securityError.collectAsState()
    val passwordResetSuccess by viewModel.passwordResetSuccess.collectAsState()
    val emailChangeSuccess by viewModel.emailChangeSuccess.collectAsState()
    val isUploadingImage by viewModel.isUploadingImage.collectAsState()
    val saveFlowState by viewModel.saveFlowState.collectAsState()
    val saveError by viewModel.saveError.collectAsState()
    val deleteFlowState by viewModel.deleteFlowState.collectAsState()
    val deleteError by viewModel.deleteError.collectAsState()
    val saveResendTimer by viewModel.saveResendTimer.collectAsState()
    val emailResendTimer by viewModel.emailResendTimer.collectAsState()

    var firstName by remember { mutableStateOf("") }
    var middleName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var birthday by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }
    var isSexDropdownExpanded by remember { mutableStateOf(false) }
    val sexOptions = listOf("Male", "Female", "Prefer not to say")
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedBirthdayMillis by remember { mutableStateOf<Long?>(null) }
    val dateFormatter = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()) }
    var isAgeValid by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val gradientBrush = Brush.horizontalGradient(colors = listOf(Color(0xFFB644F1), Color(0xFF6041EC)))
    val canEdit by viewModel.canEdit.collectAsState()

    // --- TAB STATE ---
    var selectedTabIndex by remember { mutableStateOf(0) }

    val hasChanges = viu?.let {
        firstName != it.firstName ||
                middleName != it.middleName ||
                lastName != it.lastName ||
                birthday != (it.birthday ?: "") ||
                sex != (it.sex ?: "") ||
                phone != it.phone ||
                address != (it.address ?: "") ||
                status != (it.category ?: "")
    } ?: false

    LaunchedEffect(viu) {
        viu?.let {
            firstName = it.firstName
            middleName = it.middleName
            lastName = it.lastName
            phone = it.phone
            address = it.address ?: ""
            status = it.category ?: ""
            birthday = it.birthday ?: ""
            sex = it.sex ?: ""
            if (it.birthday != null && it.birthday.isNotEmpty()) {
                try {
                    val date = dateFormatter.parse(it.birthday)
                    selectedBirthdayMillis = date?.time
                    isAgeValid = isAgeValid(selectedBirthdayMillis)
                } catch (e: Exception) {
                    selectedBirthdayMillis = null
                    isAgeValid = false
                }
            } else {
                isAgeValid = true
            }
        }
    }
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            Toast.makeText(context, "Profile Saved!", Toast.LENGTH_SHORT).show()
            viewModel.onSaveSuccessShown()
        }
    }
    LaunchedEffect(passwordResetSuccess) {
        if (passwordResetSuccess) {
            Toast.makeText(context, "Password reset email sent!", Toast.LENGTH_LONG).show()
            viewModel.onPasswordResetSuccessShown()
        }
    }
    LaunchedEffect(emailChangeSuccess) {
        if (emailChangeSuccess) {
            Toast.makeText(context, "VIU contact email updated!", Toast.LENGTH_LONG).show()
            viewModel.onEmailChangeSuccessShown()
        }
    }
    LaunchedEffect(error) { error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() } }
    LaunchedEffect(securityError) {
        securityError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearSecurityError()
        }
    }
    LaunchedEffect(saveError) {
        saveError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearSaveError()
        }
    }
    LaunchedEffect(deleteError) {
        deleteError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearDeleteError()
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedBirthdayMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        selectedBirthdayMillis = datePickerState.selectedDateMillis
                        selectedBirthdayMillis?.let { millis ->
                            birthday = dateFormatter.format(Date(millis))
                            isAgeValid = isAgeValid(millis)
                            viewModel.clearSaveError()
                        }
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete VIU") },
            text = { Text("Are you sure you want to delete this VIU?") },
            confirmButton = {
                Button(
                    onClick = { showDeleteDialog = false; viewModel.startDeleteFlow() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    // --- FLOW DIALOGS ---
    if (saveFlowState == SaveFlowState.PENDING_PASSWORD) {
        PasswordEntryDialog(
            title = "Enter Your Password",
            description = "Please enter your caregiver password to confirm changes.",
            isLoading = (saveFlowState == SaveFlowState.SAVING),
            onDismiss = { viewModel.resetSaveFlow() },
            onConfirm = { password -> viewModel.reauthenticateAndSendOtp(password, context) }
        )
    }

    if (saveFlowState == SaveFlowState.PENDING_OTP) {
        OtpEntryDialog(
            isLoading = (saveFlowState == SaveFlowState.SAVING),
            error = saveError,
            resendCooldownSeconds = saveResendTimer,
            onDismiss = { viewModel.resetSaveFlow() },
            onConfirm = { otp -> viewModel.verifyOtpAndSave(otp) },
            onClearError = { viewModel.clearSaveError() },
            onResend = { viewModel.resendOtpForSave(context) }
        )
    }

    if (deleteFlowState == DeleteFlowState.PENDING_PASSWORD) {
        PasswordEntryDialog(
            title = "Confirm Deletion",
            description = "Enter your password to permanently delete this VIU.",
            isLoading = (deleteFlowState == DeleteFlowState.DELETING),
            onDismiss = { viewModel.resetDeleteFlow() },
            onConfirm = { password ->
                viewModel.reauthenticateAndDelete(password) {
                    Toast.makeText(context, "VIU Deleted", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                }
            }
        )
    }

    if (saveFlowState == SaveFlowState.SAVING) {
        Dialog(onDismissRequest = { }) { CircularProgressIndicator() }
    }

    val focusedColor = Color(0xFF6641EC)
    val unfocusedColor = Color.Black
    val sectionHeaderColor = Color(0xFF8E41E8)
    val fieldLabelColor = Color(0xFF8E41E8)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit VIU Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White, titleContentColor = Color.Black, navigationIconContentColor = Color.Black)
            )
        },
        containerColor = Color(0xFFF0F0F0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading && viu == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                viu != null -> {
                    // --- CHANGED STRUCTURE: Root Column is NOT scrollable ---
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. HEADER (Fixed)
                        ProfileHeader(
                            viuData = viu!!,
                            isUploading = isUploadingImage,
                            onImageClick = {
                                if (canEdit) onLaunchImagePicker()
                                else Toast.makeText(context, "View Only Mode", Toast.LENGTH_SHORT).show()
                            }
                        )

                        if (!canEdit) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFFF3E0))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "View Only Mode (Secondary Caregiver)",
                                    color = Color(0xFFE65100),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        // 2. TABS (Fixed)
                        TabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = Color.White
                        ) {
                            Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text("Edit Profile") })
                            Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("Travel Log") })
                        }

                        // 3. CONTENT (Fills remaining space)
                        when (selectedTabIndex) {
                            0 -> {
                                // === TAB 0: EDIT FORM (Scrolls internally) ===
                                Column(
                                    modifier = Modifier
                                        .weight(1f) // Takes remaining space
                                        .verticalScroll(rememberScrollState()) // Scrolling happens here
                                        .background(Color.White)
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text("Profile Details", style = MaterialTheme.typography.titleLarge, color = sectionHeaderColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally))

                                    // ... FIELDS ...
                                    Text("Name", color = fieldLabelColor, modifier = Modifier.padding(bottom = 4.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = firstName, onValueChange = { firstName = it; viewModel.clearSaveError() },
                                            label = { Text("First Name", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                            readOnly = !canEdit, modifier = Modifier.weight(1f), singleLine = true, colors = customTextFieldColors, shape = RoundedCornerShape(12.dp),
                                            isError = saveError?.contains("First Name", ignoreCase = true) == true
                                        )
                                        OutlinedTextField(
                                            value = middleName, onValueChange = { middleName = it },
                                            label = { Text("Middle", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                            readOnly = !canEdit, modifier = Modifier.weight(1f), singleLine = true, colors = customTextFieldColors, shape = RoundedCornerShape(12.dp)
                                        )
                                        OutlinedTextField(
                                            value = lastName, onValueChange = { lastName = it; viewModel.clearSaveError() },
                                            label = { Text("Last Name", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                            readOnly = !canEdit, modifier = Modifier.weight(1f), singleLine = true, colors = customTextFieldColors, shape = RoundedCornerShape(12.dp),
                                            isError = saveError?.contains("Last Name", ignoreCase = true) == true
                                        )
                                    }

                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Column(Modifier.weight(1f)) {
                                            Text("Birthday", color = fieldLabelColor, modifier = Modifier.padding(bottom = 4.dp))
                                            OutlinedTextField(
                                                value = birthday, onValueChange = { },
                                                label = { Text("Select Date", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                                readOnly = true, enabled = canEdit,
                                                isError = (!isAgeValid && birthday.isNotEmpty()) || saveError?.contains("Birthday", ignoreCase = true) == true,
                                                colors = customTextFieldColors, shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                                                trailingIcon = { Icon(Icons.Default.DateRange, "Select Date", tint = unfocusedColor.copy(alpha = 0.7f), modifier = Modifier.clickable { showDatePicker = true }) }
                                            )
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text("Sex", color = fieldLabelColor, modifier = Modifier.padding(bottom = 4.dp))
                                            ExposedDropdownMenuBox(
                                                expanded = isSexDropdownExpanded && canEdit,
                                                onExpandedChange = { if (canEdit) isSexDropdownExpanded = !isSexDropdownExpanded },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                OutlinedTextField(
                                                    value = sex, onValueChange = {}, readOnly = true, enabled = canEdit,
                                                    isError = saveError?.contains("Sex", ignoreCase = true) == true,
                                                    colors = customTextFieldColors, shape = RoundedCornerShape(12.dp),
                                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSexDropdownExpanded) },
                                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                                )
                                                ExposedDropdownMenu(
                                                    expanded = isSexDropdownExpanded,
                                                    onDismissRequest = { isSexDropdownExpanded = false }
                                                ) {
                                                    sexOptions.forEach { option ->
                                                        DropdownMenuItem(
                                                            text = { Text(option) },
                                                            onClick = { sex = option; isSexDropdownExpanded = false; viewModel.clearSaveError() }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Text("Contact Info", color = fieldLabelColor, modifier = Modifier.padding(bottom = 4.dp))
                                    OutlinedTextField(
                                        value = phone,
                                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 11) { phone = it; viewModel.clearSaveError() } },
                                        label = { Text("Phone Number", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        readOnly = !canEdit, modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                                        singleLine = true, colors = customTextFieldColors, shape = RoundedCornerShape(12.dp),
                                        isError = saveError?.contains("Phone", ignoreCase = true) == true
                                    )

                                    Text("Address", color = fieldLabelColor, modifier = Modifier.padding(bottom = 4.dp))
                                    OutlinedTextField(
                                        value = address, onValueChange = { address = it },
                                        label = { Text("Address", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        readOnly = !canEdit, modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        singleLine = true, colors = customTextFieldColors, shape = RoundedCornerShape(12.dp)
                                    )

                                    Text("Status", color = fieldLabelColor, modifier = Modifier.padding(bottom = 4.dp))
                                    OutlinedTextField(
                                        value = status, onValueChange = { status = it },
                                        readOnly = !canEdit,
                                        label = { Text("Status (e.g., Partially Blind)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        singleLine = true, colors = customTextFieldColors, shape = RoundedCornerShape(12.dp)
                                    )

                                    if (canEdit) {
                                        Button(
                                            onClick = { viewModel.startSaveFlow(firstName, middleName, lastName, birthday, sex, phone, address, status) },
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(50.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
                                            shape = RoundedCornerShape(12.dp),
                                            enabled = hasChanges && !isLoading && saveFlowState == SaveFlowState.IDLE && deleteFlowState == DeleteFlowState.IDLE,
                                            contentPadding = PaddingValues()
                                        ) {
                                            Box(
                                                modifier = Modifier.background(brush = gradientBrush, shape = RoundedCornerShape(12.dp)).fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("SAVE CHANGES", color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    if (canEdit) {
                                        SecuritySettingsCard(
                                            viewModel = viewModel,
                                            viuEmail = viu!!.email,
                                            securityError = securityError,
                                            emailResendTimer = emailResendTimer
                                        )
                                    }

                                    if (canEdit) {
                                        Button(
                                            onClick = { showDeleteDialog = true },
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp).height(50.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                            shape = RoundedCornerShape(12.dp),
                                            enabled = !isLoading && saveFlowState == SaveFlowState.IDLE && deleteFlowState == DeleteFlowState.IDLE && emailFlowState == SecurityFlowState.IDLE && !isUploadingImage
                                        ) {
                                            Text("DELETE VIU", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Space for bottom nav
                                    Spacer(modifier = Modifier.height(80.dp))
                                }
                            }
                            1 -> {
                                // === TAB 1: TRAVEL LOG (Embeds its own list) ===
                                // We wrap it in a Box that takes remaining space
                                Box(modifier = Modifier.fillMaxSize()) {
                                    TravelLogScreen(
                                        viuUid = viu!!.uid,
                                        viuName = "${viu!!.firstName} ${viu!!.lastName}",
                                        viewModel = travelLogViewModel
                                    )
                                }
                            }
                        }
                    }
                }
                error != null && viu == null -> {
                    Text(text = "Failed to load profile", modifier = Modifier.align(Alignment.Center), color = Color.Red)
                }
            }
        }
    }
}