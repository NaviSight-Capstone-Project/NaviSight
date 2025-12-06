package edu.capstone.navisight.viu.ui.emergency

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import edu.capstone.navisight.viu.data.remote.ViuDataSource
import edu.capstone.navisight.R
import edu.capstone.navisight.viu.model.Caregiver
import edu.capstone.navisight.common.webrtc.service.MainServiceRepository
import edu.capstone.navisight.common.webrtc.service.MainService
import edu.capstone.navisight.common.webrtc.utils.convertToHumanTime
import kotlinx.coroutines.*
import androidx.compose.ui.res.colorResource

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
    var emergencyDuration by remember { mutableStateOf("00:00:00") }
    val emergencyOrange = colorResource(id = R.color.emergency_orange)
    val focusRequester = remember { FocusRequester() }

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
        focusRequester.requestFocus()
        for (i in 0..360000) {
            delay(1000)
            emergencyDuration = i.convertToHumanTime()
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.statusBars)
            .focusRequester(focusRequester)
            .focusable()
//            .onKeyEvent{ event ->
//                if (event.key == Key.VolumeUp || event.key == Key.VolumeDown) {
//                    when (event.key) {
//                        println("Both ")
//                    }
//                    return@onKeyEvent true
//                }
//                // Return false to allow propagation
//                false
//            }
    ) {
        // Top bar with stopwatch
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
                text = emergencyDuration,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Titles
            Text(
                text = "Help Requested",
                color = Color.Red,
                fontSize = 50.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Emergency Mode is active.",
                color = Color.Black,
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )
//            Spacer(
//                modifier = Modifier
//                    .size(64.dp) // Match the original icon size
//                    .padding(bottom = 64.dp)
//                // TODO: Replace this Spacer with AndroidView(factory = { ... })
//                // Remote video view (full screen)
//                //        AndroidView(
//                //            factory = { context ->
//                //                SurfaceViewRenderer(context).apply {
//                //                    MainService.remoteSurfaceView = this
//                //                    serviceRepository.setupViews(isVideoCall, isCaller, target)
//                //                    setBackgroundColor(android.graphics.Color.WHITE)
//                //                }
//                //            },
//                //            modifier = Modifier.fillMaxSize()
//                //        )
//            )

            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "Warning",
                tint = emergencyOrange,
                modifier = Modifier.size(250.dp).padding(bottom = 64.dp, top=64.dp)
            )


            // Descriptions
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "You have activated emergency mode." +
                            "To disable, please hold both volume up and down buttons for " +
                            "5 seconds.",
                    color = Color.Red,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Text(
                    text = "Otherwise, please wait for assistance as NaviSight will try to call your caregiver.",
                    color = Color.Black,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "This message will repeat.",
                    color = Color.Red,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
            }
        }


        // Bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Color.White)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text="EMERGENCY MODE ACTIVATED. APP IS LOCKED.", color=Color.Red)
        }
    }
}
