package edu.capstone.navisight.viu.ui.emergency

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.media.projection.MediaProjectionManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import coil.compose.rememberAsyncImagePainter
import edu.capstone.navisight.viu.data.remote.ViuDataSource
import edu.capstone.navisight.viu.model.Caregiver
import edu.capstone.navisight.common.webrtc.service.MainServiceRepository
import edu.capstone.navisight.common.webrtc.service.MainService
import edu.capstone.navisight.common.webrtc.utils.convertToHumanTime
import edu.capstone.navisight.common.webrtc.vendor.RTCAudioManager
import edu.capstone.navisight.R
import kotlinx.coroutines.*
import org.webrtc.SurfaceViewRenderer

@Composable
fun EmergencyScreen(
    target: String,
    isVideoCall: Boolean,
    isCaller: Boolean,
    onEndCall: () -> Unit,
    serviceRepository: MainServiceRepository,
    viuDataSource: ViuDataSource
) {
    val context = LocalContext.current

    var callTime by remember { mutableStateOf("00:00") }
    var isMicrophoneMuted by remember { mutableStateOf(false) }
    var isCameraMuted by remember { mutableStateOf(false) }
    var isSpeakerMode by remember { mutableStateOf(true) }
    var isScreenCasting by remember { mutableStateOf(false) }

    // Retrieve caregiver record.
    var caregiverRecord by remember { mutableStateOf<Caregiver?>(null) }
    LaunchedEffect(target) {
        launch {
            try {
                // Call the suspend function
                caregiverRecord = viuDataSource.getRegisteredCaregiver()
            } catch (e: Exception) {
                Log.e("CallScreen", "Failed to fetch caregiver profile: ${e.message}")
            }
        }
    }

    // Timer coroutine
    LaunchedEffect(Unit) {
        for (i in 0..3600) {
            delay(1000)
            callTime = i.convertToHumanTime()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Remote video view (full screen)
        AndroidView(
            factory = { context ->
                SurfaceViewRenderer(context).apply {
                    MainService.remoteSurfaceView = this
                    serviceRepository.setupViews(isVideoCall, isCaller, target)
                    setBackgroundColor(android.graphics.Color.WHITE)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top bar with timer + title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color.White)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = callTime,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Text(
                text = "In call with Caregiver ${caregiverRecord?.firstName}",
                color = Color.Red,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        // Bottom control panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Color.White)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("EMERGENCY MODE ACTIVATED. APP IS LOCKED.")
        }
    }
}
