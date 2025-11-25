package edu.capstone.navisight.caregiver.ui.feature_viu_profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import edu.capstone.navisight.caregiver.ui.feature_edit_viu_profile.EditViuProfileScreen
import edu.capstone.navisight.caregiver.ui.feature_edit_viu_profile.EditViuProfileViewModel
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
    // Data needed for the header comes from EditViewModel
    val viu by editViewModel.viu.collectAsState()
    val isUploadingImage by editViewModel.isUploadingImage.collectAsState()
    val canEdit by editViewModel.canEdit.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val context = LocalContext.current

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
                // 1. Header (Shared)
                ProfileHeader(
                    viuData = viu!!,
                    isUploading = isUploadingImage,
                    onImageClick = {
                        if (canEdit) onLaunchImagePicker()
                        else Toast.makeText(context, "View Only Mode", Toast.LENGTH_SHORT).show()
                    }
                )

                if (!canEdit) {
                    Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF3E0)).padding(8.dp), contentAlignment = Alignment.Center) {
                        Text("View Only Mode (Secondary Caregiver)", color = Color(0xFFE65100), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                }

                // 2. Tabs
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.White
                ) {
                    Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text("Edit Profile") })
                    Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("Travel Log") })
                }

                // 3. Tab Content
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
}