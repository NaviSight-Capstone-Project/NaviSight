package edu.capstone.navisight.caregiver.ui.call

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.media.projection.MediaProjectionManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import edu.capstone.navisight.caregiver.data.remote.ViuDataSource
import edu.capstone.navisight.caregiver.model.Viu
import edu.capstone.navisight.common.webrtc.service.MainService
import edu.capstone.navisight.common.webrtc.service.MainServiceRepository
import edu.capstone.navisight.common.webrtc.utils.convertToHumanTime
import edu.capstone.navisight.R
import edu.capstone.navisight.common.webrtc.vendor.RTCAudioManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.webrtc.SurfaceViewRenderer

@Composable
fun CallScreen(
    target: String,
    isVideoCall: Boolean,
    isCaller: Boolean,
    onEndCall: () -> Unit,
    serviceRepository: MainServiceRepository,
    onRequestScreenCapture: () -> Unit,
    viuRemoteDataSource: ViuDataSource,
    isConnected: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var callTime by remember { mutableStateOf("00:00:00") }
    var isMicrophoneMuted by remember { mutableStateOf(false) }
    var isCameraMuted by remember { mutableStateOf(false) }
    var isSpeakerMode by remember { mutableStateOf(true) }
    var isScreenCasting by remember { mutableStateOf(false) }

    // Retrieve caregiver record.
    var viuRecord by remember { mutableStateOf<Viu?>(null) }
    val imageUrl = viuRecord?.profileImageUrl.takeUnless { it.isNullOrEmpty() }

    LaunchedEffect(target) {
        if (target != null) {
            launch {
                try {
                    // Call the suspend function
                    viuRecord = viuRemoteDataSource.getViuDetails(target).first()
                } catch (e: Exception) {
                    Log.e("CallScreen", "Failed to fetch caregiver profile: ${e.message}")
                }
            }
        }
    }

    // Timer coroutine
    LaunchedEffect(isConnected) {
        if (isConnected) {
            for (i in 0..360000) {
                delay(1000)
                callTime = i.convertToHumanTime()
            }
        }
    }

    LaunchedEffect(Unit) {
        // Calling setupViews here for better control
        serviceRepository.setupViews(isVideoCall, isCaller, target)
    }

    // Launcher for screen capture
    val requestScreenCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            MainService.screenPermissionIntent = intent
            isScreenCasting = true
            serviceRepository.toggleScreenShare(true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        AndroidView(
            factory = { context ->
                SurfaceViewRenderer(context).apply {
                    MainService.remoteSurfaceView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!isScreenCasting && isVideoCall) {
            AndroidView(
                factory = { context ->
                    SurfaceViewRenderer(context).apply {
                        MainService.localSurfaceView = this
                    }
                },
                modifier = Modifier
                    .size(width = 125.dp, height = 250.dp)
                    .align(Alignment.TopEnd)
                    .padding(end = 10.dp, top = 40.dp)
            )
        }

        if (!isConnected) {
            // Calling
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xC0000000))
                    .padding(bottom=if (isVideoCall) 50.dp else 300.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Calling VIU",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    AsyncImage(
                        model = imageUrl ?: R.drawable.default_profile,
                        contentDescription = "VIU Profile Image",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    viuRecord?.let { record ->
                        Text(
                            text = "${record.firstName} ${record.lastName}",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else if (!isVideoCall){
            // Audio call omsim.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xC0000000))
                    .padding(bottom=300.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Audio Call",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    AsyncImage(
                        model = imageUrl ?: R.drawable.default_profile,
                        contentDescription = "VIU Profile Image",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = "VIU",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${viuRecord?.firstName ?: ""} ${viuRecord?.lastName ?: ""}",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = callTime,
                        color = Color.White,
                        style = TextStyle(
                            fontSize = 24.sp,
                            color = Color.White,
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                }
            }
        } else {
            // Top bar with timer + title, assumes video calling
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(Color(0xAA000000))
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = callTime,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
                Text(
                    text = "In call with ${viuRecord?.firstName ?: "your VIU"}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }



        // Bottom control panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isVideoCall) 100.dp else 400.dp)
                .background(if (isVideoCall) (Color(0xAA000000)) else Color.Transparent)
                .padding(bottom = 32.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Toggle Mic
            IconButton(onClick = {
                if (!isMicrophoneMuted) {
                    serviceRepository.toggleAudio(true)
                } else {
                    serviceRepository.toggleAudio(false)
                }
                isMicrophoneMuted = !isMicrophoneMuted
            }) {
                Icon(
                    painter = rememberAsyncImagePainter(
                        if (isMicrophoneMuted) R.drawable.ic_mic_on else R.drawable.ic_mic_off
                    ),
                    contentDescription = "Mic Toggle",
                    modifier=if(!isVideoCall) Modifier.size(32.dp) else Modifier,
                    tint = Color.White
                )
            }

            // Toggle Camera
            if (isVideoCall) {
                IconButton(onClick = {
                    if (!isCameraMuted) {
                        serviceRepository.toggleVideo(true)
                    } else {
                        serviceRepository.toggleVideo(false)
                    }
                    isCameraMuted = !isCameraMuted
                }) {
                    Icon(
                        painter = rememberAsyncImagePainter(
                            if (isCameraMuted) R.drawable.ic_camera_on else R.drawable.ic_camera_off
                        ),
                        contentDescription = "Camera Toggle",
                        tint = Color.White
                    )
                }
            }

            // Switch Camera
            if (isVideoCall) {
                IconButton(onClick = {
                    serviceRepository.switchCamera()
                }) {
                    Icon(
                        painter = rememberAsyncImagePainter(R.drawable.ic_switch_camera),
                        contentDescription = "Switch Camera",
                        tint = Color.White
                    )
                }
            }

            // Toggle Audio Device
            IconButton(onClick = {
                if (isSpeakerMode) {
                    serviceRepository.toggleAudioDevice(RTCAudioManager.AudioDevice.EARPIECE.name)
                } else {
                    serviceRepository.toggleAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE.name)
                }
                isSpeakerMode = !isSpeakerMode
            }) {
                Icon(
                    painter = rememberAsyncImagePainter(
                        if (isSpeakerMode) R.drawable.ic_speaker else R.drawable.ic_ear
                    ),
                    modifier=if(!isVideoCall) Modifier.size(32.dp) else Modifier,
                    contentDescription = "Audio Device",
                    tint = Color.White
                )
            }

            // Screen Share
            if (isVideoCall) {
                IconButton(onClick = {
                    if (!isScreenCasting) {
                        AlertDialog.Builder(context)
                            .setTitle("Screen Casting")
                            .setMessage("Start casting your screen?")
                            .setPositiveButton("Yes") { dialog, _ ->
                                val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                                        as MediaProjectionManager
                                val intent = mgr.createScreenCaptureIntent()
                                requestScreenCaptureLauncher.launch(intent)
                                dialog.dismiss()
                            }
                            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                            .show()
                    } else {
                        isScreenCasting = false
                        serviceRepository.toggleScreenShare(false)
                    }
                }) {
                    Icon(
                        painter = rememberAsyncImagePainter(
                            if (isScreenCasting) R.drawable.ic_stop_screen_share
                            else R.drawable.ic_screen_share
                        ),
                        contentDescription = "Screen Share",
                        tint = Color.White
                    )
                }
            }
            // End call
            IconButton(
                onClick = {
                    serviceRepository.sendEndOrAbortCall()
                    onEndCall()
                }
            ) {
                Icon(
                    painter = rememberAsyncImagePainter(R.drawable.ic_end_call),
                    contentDescription = "End Call",
                    modifier=if(!isVideoCall) Modifier.size(32.dp) else Modifier,
                    tint = Color.Red
                )
            }
        }
    }
}
