package edu.capstone.navisight.viu.ui.emergency

import android.view.KeyEvent
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.colorResource
import edu.capstone.navisight.viu.data.remote.ViuDataSource
import edu.capstone.navisight.R
import edu.capstone.navisight.viu.model.Caregiver
import edu.capstone.navisight.common.webrtc.service.MainServiceRepository
import edu.capstone.navisight.common.webrtc.utils.convertToHumanTime
import kotlinx.coroutines.*
import android.os.Handler
import android.os.Looper
import android.util.Log

private const val SEQUENCE_TIMEOUT_DELAY = 800L
private const val REQUIRED_SEQUENCE_STEPS = 5

@Composable
fun EmergencyScreen(
    target: String,
    isVideoCall: Boolean,
    isCaller: Boolean,
    onEndEmergencyMode: () -> Unit,
    serviceRepository: MainServiceRepository,
    viuDataSource: ViuDataSource
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope() // Still needed for other coroutines (like the fetch and timer)
    var emergencyDuration by remember { mutableStateOf("00:00:00") }
    val emergencyOrange = colorResource(id = R.color.emergency_orange)
    val focusRequester = remember { FocusRequester() }

    // Retrieve caregiver record.
    var caregiverRecord by remember { mutableStateOf<Caregiver?>(null) }

    // Init. key sequence vars.
    var keySequenceStep by remember { mutableStateOf(0) }
    val handler = remember { Handler(Looper.getMainLooper()) }
    val keySequenceResetRunnable = rememberUpdatedState {
        Log.d("Sequence", "Timeout reached, resetting sequence.")
        keySequenceStep = 0 // Reset sequence on timeout
    }

    // DisposableEffect for cleanup
    DisposableEffect(Unit) {
        onDispose {
            handler.removeCallbacks(keySequenceResetRunnable.value)
        }
    }

    // Logic for sequence processing and reset
    val processKeySequence: (Int) -> Unit = { keyCode ->
        handler.removeCallbacks(keySequenceResetRunnable.value)
        val nextStep = keySequenceStep + 1
        var shouldAdvance = false

        when (nextStep) {
            // Require Volume UP (V-UP x2, then V-UP x1 to finish)
            1, 2, 5 -> {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    shouldAdvance = true
                }
            }
            // Require Volume DOWN (V-DOWN x2)
            3, 4 -> {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    shouldAdvance = true
                }
            }
            else -> {}
        }

        if (shouldAdvance) {
            keySequenceStep = nextStep

            if (keySequenceStep == REQUIRED_SEQUENCE_STEPS) {
                keySequenceStep = 0 // Reset
                onEndEmergencyMode() // Stop.

            } else {
                // Not finished, but advanced. Start/restart the timeout timer using Handler
                handler.postDelayed(keySequenceResetRunnable.value, SEQUENCE_TIMEOUT_DELAY)
            }
        } else {
            // Wrong key pressed. Reset sequence immediately.
            keySequenceStep = 0
        }
    }

    // LaunchedEffect to fetch caregiver data
    LaunchedEffect(target) {
        launch {
            try {
                // Call the suspend function
                caregiverRecord = viuDataSource.getRegisteredCaregiver()
            } catch (e: Exception) {
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
            .onKeyEvent { event ->
                val keyCode = event.nativeKeyEvent.keyCode
                val isVolumeKey = keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN

                if (isVolumeKey) {
                    // Only process the key press on ACTION_DOWN for the sequence
                    if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                        processKeySequence(keyCode)
                    }
                    // Consume the key event (both UP and DOWN) so the OS doesn't adjust volume
                    return@onKeyEvent true
                }
                false // Do not consume other key events
            }

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
                    text = emergencyModeDescription,
                    color = Color.Red,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Text(
                    text = emergencyModeDescription2,
                    color = Color.Black,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
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