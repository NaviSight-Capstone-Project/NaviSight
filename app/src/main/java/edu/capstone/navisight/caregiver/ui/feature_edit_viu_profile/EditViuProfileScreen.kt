package edu.capstone.navisight.caregiver.ui.feature_edit_viu_profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import edu.capstone.navisight.caregiver.ui.feature_edit_viu_profile.components.*
import java.text.SimpleDateFormat
import java.util.*

private fun ageValid(selectedMillis: Long?): Boolean {
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
fun EditViuProfileScreen(
    viewModel: EditViuProfileViewModel,
    state: SignupFormState,
    onProvinceSelected: (String) -> Unit,
    onCitySelected: (String) -> Unit,
    onDeleteSuccess: () -> Unit
) {
    val viu by viewModel.viu.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val saveFlowState by viewModel.saveFlowState.collectAsState()
    val saveError by viewModel.saveError.collectAsState()
    val deleteFlowState by viewModel.deleteFlowState.collectAsState()
    val deleteError by viewModel.deleteError.collectAsState()
    val saveResendTimer by viewModel.saveResendTimer.collectAsState()
    val securityError by viewModel.securityError.collectAsState()
    val emailResendTimer by viewModel.emailResendTimer.collectAsState()
    val emailFlowState by viewModel.emailFlowState.collectAsState()
    val canEdit by viewModel.canEdit.collectAsState()
    val transferSuccess by viewModel.transferSuccess.collectAsState()
    val passwordResetSuccess by viewModel.passwordResetSuccess.collectAsState()
    val emailChangeSuccess by viewModel.emailChangeSuccess.collectAsState()

    val context = LocalContext.current

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
    var isStatusDropdownExpanded by remember { mutableStateOf(false) }
    val statusOptions = listOf("Partially Blind", "Totally Blind")
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedBirthdayMillis by remember { mutableStateOf<Long?>(null) }
    val dateFormatter = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()) }
    var isAgeValid by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCountryInfo by remember { mutableStateOf(false) }

    val gradientBrush = Brush.horizontalGradient(colors = listOf(Color(0xFFB644F1), Color(0xFF6041EC)))

    var loadedViuUid by remember { mutableStateOf<String?>(null) }

    val isFormValid = remember(firstName, lastName, birthday, sex, phone) {
        viewModel.isValidName(firstName) &&
                viewModel.isValidName(lastName) &&
                viewModel.isValidBirthday(birthday) &&
                sex.isNotBlank() &&
                viewModel.isValidPhone(phone)
    }

    LaunchedEffect(viu) {
        if (viu != null && loadedViuUid != viu?.uid) {
            val it = viu!!
            loadedViuUid = it.uid

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
                    isAgeValid = ageValid(selectedBirthdayMillis)
                } catch (e: Exception) {
                    selectedBirthdayMillis = null
                    isAgeValid = false
                }
            }
        }
    }

    val saveSuccess by viewModel.saveSuccess.collectAsState()
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            Toast.makeText(context, "Profile Saved!", Toast.LENGTH_SHORT).show()
            viewModel.onSaveSuccessShown()
        }
    }
    LaunchedEffect(transferSuccess) {
        if (transferSuccess) {
            Toast.makeText(context, "Transfer request sent!", Toast.LENGTH_SHORT).show()
            viewModel.onTransferSuccessShown()
        }
    }
    LaunchedEffect(passwordResetSuccess) {
        if (passwordResetSuccess) {
            Toast.makeText(context, "Password reset email sent!", Toast.LENGTH_SHORT).show()
            viewModel.onPasswordResetSuccessShown()
        }
    }
    LaunchedEffect(emailChangeSuccess) {
        if (emailChangeSuccess) {
            Toast.makeText(context, "Email address updated successfully!", Toast.LENGTH_SHORT).show()
            viewModel.onEmailChangeSuccessShown()
        }
    }

    // DIALOGS
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedBirthdayMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    selectedBirthdayMillis = datePickerState.selectedDateMillis
                    selectedBirthdayMillis?.let { millis ->
                        birthday = dateFormatter.format(Date(millis))
                        isAgeValid = ageValid(millis)
                        viewModel.clearSaveError()
                    }
                }) { Text("OK") }
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

    if (saveFlowState == SaveFlowState.PENDING_PASSWORD) {
        PasswordEntryDialog(
            title = "Enter Your Password",
            description = "Please enter your companion password to confirm changes.",
            errorMessage = saveError,
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
                    onDeleteSuccess()
                }
            }
        )
    }

    if (saveFlowState == SaveFlowState.SAVING) {
        Dialog(onDismissRequest = { }) { CircularProgressIndicator() }
    }

    // FORM UI
    val customTextFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedIndicatorColor = Color(0xFF6641EC),
        unfocusedIndicatorColor = Color.Black.copy(alpha = 0.5f)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("VIU Profile Details", style = MaterialTheme.typography.titleLarge, color = Color(0xFF8E41E8), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally))

        // Name Fields
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name") },
                readOnly = !canEdit, modifier = Modifier.weight(1f),
                isError = firstName.isBlank(),
                enabled = canEdit,
                supportingText = if (firstName.isBlank()) {
                    { Text("Cannot be empty", color = MaterialTheme.colorScheme.error) }
                } else null,
                colors = customTextFieldColors, shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = middleName, onValueChange = { middleName = it },
                enabled = canEdit,
                label = { Text("Middle") }, readOnly = !canEdit, modifier = Modifier.weight(1f),
                colors = customTextFieldColors, shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                enabled = canEdit,
                label = { Text("Last Name") },
                readOnly = !canEdit, modifier = Modifier.weight(1f),
                isError = lastName.isBlank(),
                supportingText = if (lastName.isBlank()) {
                    { Text("Cannot be empty", color = MaterialTheme.colorScheme.error) }
                } else null,
                colors = customTextFieldColors, shape = RoundedCornerShape(12.dp)
            )
        }

        // Birthday & Sex
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) {
                OutlinedTextField(
                    value = birthday, onValueChange = { },
                    label = { Text("Birthday") }, readOnly = true, enabled = canEdit,
                    isError = !isAgeValid,
                    colors = customTextFieldColors, shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().clickable { if(canEdit) showDatePicker = true },
                    trailingIcon = { Icon(Icons.Default.DateRange, "Select Date", modifier = Modifier.clickable { if(canEdit) showDatePicker = true }) }
                )
            }
            Column(Modifier.weight(1f)) {
                ExposedDropdownMenuBox(
                    expanded = isSexDropdownExpanded && canEdit,
                    onExpandedChange = { if (canEdit) isSexDropdownExpanded = !isSexDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        label = { Text("Sex") },
                        value = sex, onValueChange = {}, readOnly = true, enabled = canEdit,
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

        // Phone & Address
        val isPhoneInvalid = phone.isNotEmpty() && (phone.length != 11 || !phone.startsWith("09"))

        OutlinedTextField(
            value = phone,
            enabled = canEdit,
            onValueChange = { input ->
                if (input.all { it.isDigit() } && input.length <= 11) {
                    phone = input
                    viewModel.clearSaveError()
                }
            },
            label = { Text("Phone Number") },
            isError = isPhoneInvalid,
            supportingText = if (isPhoneInvalid) {
                {
                    Text(
                        text = "Enter a valid 11-digit PH number (e.g., 09123456789)",
                        color = Color.Red
                    )
                }
            } else null,
            readOnly = !canEdit,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            ),
            colors = customTextFieldColors,
            shape = RoundedCornerShape(12.dp)
        )

        // Province Dropdown
        LocationDropdown(
            enabled = canEdit,
            label = "Province *",
            options = state.availableProvinces,
            selectedOption = state.province,
            onOptionSelected = onProvinceSelected
        )

        // City Dropdown
        LocationDropdown(
            enabled = canEdit,
            label = if (state.province.isEmpty()) "Please pick a province *" else "City/Municipality *",
            options = state.availableCities,
            selectedOption = state.city,
            onOptionSelected = onCitySelected
        )

        OutlinedTextField(
            value = address,
            enabled = canEdit,
            onValueChange = { address = it },
            label = { Text("Home Address (Lot./House #/Brgy.)") },
            readOnly = !canEdit,
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

        // Set "Philippines" to fixed. For now.
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

        ExposedDropdownMenuBox(
            expanded = isStatusDropdownExpanded && canEdit,
            onExpandedChange = { if (canEdit) isStatusDropdownExpanded = !isStatusDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = status, onValueChange = { status = it },
                label = { Text("Status (e.g., Partially Blind)") }, enabled=canEdit, readOnly = !canEdit, modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = if (canEdit) {
                    { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStatusDropdownExpanded) }
                } else null
            )
            ExposedDropdownMenu(
                expanded = isStatusDropdownExpanded,
                onDismissRequest = { isStatusDropdownExpanded = false }
            ) {
                statusOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { status = option; isStatusDropdownExpanded = false; viewModel.clearSaveError() }
                    )
                }
            }
        }
        // Save Button
        if (canEdit) {
            val hasChanges = viu?.let {
                firstName != it.firstName || middleName != it.middleName || lastName != it.lastName ||
                        birthday != (it.birthday ?: "") || sex != (it.sex ?: "") || phone != it.phone ||
                        address != (it.address ?: "") || status != (it.category ?: "")
            } ?: false

            Button(
                onClick = {
                    viewModel.startSaveFlow(firstName, middleName, lastName, birthday, sex, phone, address, status)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                // Disable if no changes OR if the form is invalid
                enabled = hasChanges && isFormValid && !isLoading && saveFlowState == SaveFlowState.IDLE,
                contentPadding = PaddingValues(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                )
            ) {
                Box(
                    modifier = Modifier
                        .background(
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
                        color = if (hasChanges && isFormValid) Color.White else Color.Gray, // Darker text when disabled
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Security Card
            SecuritySettingsCard(
                viewModel = viewModel,
                viuEmail = viu!!.email,
                securityError = securityError,
                emailResendTimer = emailResendTimer
            )

            // Delete Button
            Button(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading && saveFlowState == SaveFlowState.IDLE && deleteFlowState == DeleteFlowState.IDLE && emailFlowState == SecurityFlowState.IDLE
            ) {
                Text("DELETE VIU", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDropdown(
    label: String,
    enabled: Boolean,
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
            enabled = enabled,
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
                    enabled = enabled,
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