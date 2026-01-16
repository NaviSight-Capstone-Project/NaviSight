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

    val gradientBrush = Brush.horizontalGradient(colors = listOf(Color(0xFFB644F1), Color(0xFF6041EC)))

    var loadedViuUid by remember { mutableStateOf<String?>(null) }

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
        Text("Profile Details", style = MaterialTheme.typography.titleLarge, color = Color(0xFF8E41E8), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally))

        // Name Fields
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = firstName, onValueChange = { firstName = it; viewModel.clearSaveError() },
                label = { Text("First Name") }, readOnly = !canEdit, modifier = Modifier.weight(1f),
                colors = customTextFieldColors, shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = middleName, onValueChange = { middleName = it },
                label = { Text("Middle") }, readOnly = !canEdit, modifier = Modifier.weight(1f),
                colors = customTextFieldColors, shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = lastName, onValueChange = { lastName = it; viewModel.clearSaveError() },
                label = { Text("Last Name") }, readOnly = !canEdit, modifier = Modifier.weight(1f),
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
        OutlinedTextField(
            value = phone,
            onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 11) { phone = it; viewModel.clearSaveError() } },
            label = { Text("Phone Number (Uneditable)") },
            readOnly = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            colors = customTextFieldColors, shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = address, onValueChange = { address = it },
            label = { Text("Address") }, readOnly = !canEdit, modifier = Modifier.fillMaxWidth(),
            colors = customTextFieldColors, shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenuBox(
            expanded = isStatusDropdownExpanded && canEdit,
            onExpandedChange = { if (canEdit) isStatusDropdownExpanded = !isStatusDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = status, onValueChange = { status = it },
                label = { Text("Status (e.g., Partially Blind)") }, readOnly = !canEdit, modifier = Modifier.fillMaxWidth().menuAnchor(),
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
                onClick = { viewModel.startSaveFlow(firstName, middleName, lastName, birthday, sex, phone, address, status) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(12.dp),
                enabled = hasChanges && !isLoading && saveFlowState == SaveFlowState.IDLE,
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier.background(brush = gradientBrush, shape = RoundedCornerShape(12.dp)).fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SAVE CHANGES", color = Color.White, fontWeight = FontWeight.Bold)
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

        Spacer(modifier = Modifier.height(80.dp))
    }
}