package edu.capstone.navisight.caregiver.ui.feature_viu_profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import edu.capstone.navisight.caregiver.model.TransferPrimaryRequest
import edu.capstone.navisight.caregiver.ui.feature_edit_viu_profile.EditViuProfileScreen
import edu.capstone.navisight.caregiver.ui.feature_edit_viu_profile.EditViuProfileViewModel
import edu.capstone.navisight.caregiver.ui.feature_edit_viu_profile.TransferFlowState
import edu.capstone.navisight.caregiver.ui.feature_edit_viu_profile.UnpairFlowState
import edu.capstone.navisight.caregiver.ui.feature_travel_log.TravelLogScreen
import edu.capstone.navisight.caregiver.ui.feature_travel_log.TravelLogViewModel
import edu.capstone.navisight.caregiver.ui.feature_viu_profile.components.ProfileHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViuProfileContainer(
    editViewModel: EditViuProfileViewModel,
    travelLogViewModel: TravelLogViewModel,
    onNavigateBack: () -> Unit,
    onLaunchImagePicker: () -> Unit
) {
    val viu by editViewModel.viu.collectAsState()
    val isUploadingImage by editViewModel.isUploadingImage.collectAsState()
    val canEdit by editViewModel.canEdit.collectAsState()

    val transferState by editViewModel.transferFlowState.collectAsState()
    val transferCandidates by editViewModel.transferCandidates.collectAsState()
    val transferError by editViewModel.transferError.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val context = LocalContext.current

    // Listen for Transfer Success
    val saveSuccess by editViewModel.saveSuccess.collectAsState()
    LaunchedEffect(saveSuccess) {
        if(saveSuccess) {
            Toast.makeText(context, "Transfer Request Sent!", Toast.LENGTH_LONG).show()
            editViewModel.onSaveSuccessShown()
        }
    }

    val unpairState by editViewModel.unpairFlowState.collectAsState()
    val unpairError by editViewModel.unpairError.collectAsState()
    val unpairSuccess by editViewModel.unpairSuccess.collectAsState()

    // Handle Unpair Success (Navigate back effectively removing the profile from view)
    LaunchedEffect(unpairSuccess) {
        if (unpairSuccess) {
            Toast.makeText(context, "Unpaired successfully.", Toast.LENGTH_SHORT).show()
            onNavigateBack() // Go back to list
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Viu Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF0F0F0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (viu != null) {
                ProfileHeader(
                    viuData = viu!!,
                    isUploading = isUploadingImage,
                    onImageClick = {
                        if (canEdit) onLaunchImagePicker()
                        else Toast.makeText(context, "View Only Mode", Toast.LENGTH_SHORT).show()
                    },
                    showMenu = true,
                    isPrimaryCaregiver = canEdit, // Pass true if primary, false if secondary
                    onTransferRightsClick = { editViewModel.startTransferFlow() },
                    onUnpairClick = { editViewModel.startUnpairFlow() }
                )

                if (!canEdit) {
                    Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF3E0)).padding(8.dp), contentAlignment = Alignment.Center) {
                        Text("View Only Mode (Secondary Caregiver)", color = Color(0xFFE65100), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                }

                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.White
                ) {
                    Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text("Edit Profile") })
                    Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("Travel Log") })
                }

                Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                    when (selectedTabIndex) {
                        0 -> EditViuProfileScreen(
                            viewModel = editViewModel,
                            onDeleteSuccess = onNavigateBack
                        )
                        1 -> TravelLogScreen(
                            viuUid = viu!!.uid,
                            viuName = "${viu!!.firstName} ${viu!!.lastName}",
                            viewModel = travelLogViewModel
                        )
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (transferState == TransferFlowState.SELECTING_CANDIDATE) {
        Dialog(onDismissRequest = { editViewModel.cancelTransferFlow() }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f) // Make it 95% of screen width
                    .heightIn(min = 300.dp, max = 600.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Transfer Primary Rights",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6041EC)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select a secondary caregiver to transfer rights to. You will lose edit access once accepted.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (transferCandidates.isEmpty()) {
                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text("No other caregivers found.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(transferCandidates) { candidate ->
                                CandidateItem(candidate) {
                                    editViewModel.onCandidateSelected(candidate)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = { editViewModel.cancelTransferFlow() },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel", color = Color.Red)
                    }
                }
            }
        }
    }

    if (transferState == TransferFlowState.CONFIRMING_PASSWORD || transferState == TransferFlowState.SENDING) {
        PasswordConfirmationDialog(
            isLoading = (transferState == TransferFlowState.SENDING),
            error = transferError,
            onConfirm = { password -> editViewModel.confirmTransferPassword(password) },
            onCancel = { editViewModel.cancelTransferFlow() }
        )
    }
    if (unpairState == UnpairFlowState.CONFIRMING_PASSWORD || unpairState == UnpairFlowState.UNPAIRING) {
        PasswordConfirmationDialog(
            isLoading = (unpairState == UnpairFlowState.UNPAIRING),
            error = unpairError,
            onConfirm = { password -> editViewModel.confirmUnpairPassword(password) },
            onCancel = { editViewModel.cancelUnpairFlow() }
        )
    }
}

@Composable
fun CandidateItem(candidate: TransferPrimaryRequest, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(candidate.recipientName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(candidate.recipientEmail, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

// Helper Composable for Password Dialog
@Composable
fun PasswordConfirmationDialog(
    isLoading: Boolean,
    error: String?,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!isLoading) onCancel() }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon Header
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8E4FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF6041EC), modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Authenticate", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Please enter your password to confirm this transfer.",
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    enabled = !isLoading,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(image, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6041EC),
                        focusedLabelColor = Color(0xFF6041EC)
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                if (error != null) {
                    Text(
                        text = error,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp).align(Alignment.Start)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = onCancel,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onConfirm(password) },
                        enabled = !isLoading && password.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6041EC)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }
}