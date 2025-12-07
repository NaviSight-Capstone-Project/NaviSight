package edu.capstone.navisight.viu.ui.ocr

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale
import java.util.concurrent.Executors
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3Api


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentReaderScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current

    var hasPermission by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(true) } // Scanning vs Result Mode
    var isAutoMode by remember { mutableStateOf(true) } // Auto vs Manual Toggle
    var extractedText by remember { mutableStateOf("") }
    var isCapturing by remember { mutableStateOf(false) } // Prevents double triggers

    // --- Tools Initialization ---
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val stabilityManager = remember { TextStabilityManager() }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }

    // TTS Setup
    var tts: TextToSpeech? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.speak("Document Reader. Auto mode active. Point at text.", TextToSpeech.QUEUE_FLUSH, null, "Intro")
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
            toneGenerator.release()
            cameraExecutor.shutdown()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            hasPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            if (!isScanning) {
                TopAppBar(
                    title = { Text("Result") },
                    navigationIcon = {
                        IconButton(onClick = {
                            isScanning = true
                            extractedText = ""
                            stabilityManager.reset()
                        }) {
                            Icon(Icons.Default.ArrowBack, "Scan Again")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isScanning && hasPermission) {
                CameraContent(
                    isAutoMode = isAutoMode,
                    cameraExecutor = cameraExecutor,
                    stabilityManager = stabilityManager,
                    onTextFound = { text ->
                        if (!isCapturing) {
                            isCapturing = true
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)

                            extractedText = text
                            isScanning = false
                            isCapturing = false

                            // Speak result automatically
                            tts?.speak(text.ifBlank { "No text found" }, TextToSpeech.QUEUE_FLUSH, null, "Result")
                        }
                    },
                    onToggleMode = {
                        isAutoMode = !isAutoMode
                        tts?.stop()
                        tts?.speak(if (isAutoMode) "Auto Mode" else "Manual Mode", TextToSpeech.QUEUE_FLUSH, null, "ModeSwitch")
                    },
                    onNavigateBack = onNavigateBack
                )
            } else if (!isScanning) {

                // TTS State Control
                var isPlaying by remember { mutableStateOf(true) }

                LaunchedEffect(isPlaying) {
                    if (isPlaying) {
                        tts?.speak(extractedText, TextToSpeech.QUEUE_FLUSH, null, "Content")
                    } else {
                        tts?.stop()
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Scanned Text") },
                            navigationIcon = {
                                IconButton(
                                    onClick = {
                                        tts?.stop()
                                        onNavigateBack()
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.Close, "Exit Reader")
                                }
                            }
                        )
                    },
                    bottomBar = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { isPlaying = !isPlaying },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isPlaying) "Pause" else "Read",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Button(
                                onClick = {
                                    tts?.stop()
                                    isScanning = true
                                    extractedText = ""
                                    stabilityManager.reset()
                                    // Announce the state change
                                    tts?.speak("Ready to scan.", TextToSpeech.QUEUE_FLUSH, null, "Reset")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Default.CameraAlt, null)
                                Spacer(Modifier.width(8.dp))
                                Text("New Scan", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                ) { resultPadding ->
                    // The Text Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(resultPadding)
                            .padding(horizontal = 24.dp) // Generous side margins
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(Modifier.height(16.dp))

                        // Tip for user
                        Text(
                            text = "Detected Content:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(8.dp))

                        SelectionContainer {
                            Text(
                                text = extractedText.ifBlank { "No text detected." },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.5,
                                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * 1.1
                                ),
                                modifier = Modifier.padding(bottom = 24.dp)
                            )
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Camera permission is required to scan.")
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraContent(
    isAutoMode: Boolean,
    cameraExecutor: java.util.concurrent.ExecutorService,
    stabilityManager: TextStabilityManager,
    onTextFound: (String) -> Unit,
    onToggleMode: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (isAutoMode) {
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                                recognizer.process(image)
                                    .addOnSuccessListener { visionText ->
                                        // Find biggest block of text to track stability
                                        val mainBlock = visionText.textBlocks.maxByOrNull { it.boundingBox?.width() ?: 0 }
                                        val status = stabilityManager.checkStability(mainBlock?.boundingBox)

                                        if (status == TextStabilityManager.StabilityStatus.STEADY_AND_READY) {
                                            // STABLE! Trigger the high-res capture
                                            takeHighResPicture(imageCapture, context, onTextFound)
                                            stabilityManager.reset()
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
                            }
                        } else {
                            imageProxy.close() // Close immediately if not in auto mode to save battery
                        }
                    }

                    //  Image Capture
                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        Log.e("Camera", "Binding failed", e)
                    }

                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // OVERLAYS

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            // Auto/Manual Toggle
            Button(
                onClick = onToggleMode,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if(isAutoMode) MaterialTheme.colorScheme.primary else Color.Gray
                )
            ) {
                Text(if (isAutoMode) "AUTO" else "MANUAL")
            }
        }

        // Tap Anywhere Overlay (For Manual Capture)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp)
                .clickable {
                    // Manual Trigger
                    takeHighResPicture(imageCapture, context, onTextFound)
                }
        ) {
            if (!isAutoMode) {
                Text(
                    text = "Tap screen to scan",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(16.dp)
                )
            } else {
                Text(
                    text = "Hold Steady...",
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 50.dp)
                )
            }
        }
    }
}

/**
 * Helper function to take a high-quality photo and run OCR on it.
 * This is used by both the Auto-Trigger and the Manual Tap.
 */

@OptIn(ExperimentalGetImage::class)
private fun takeHighResPicture(
    imageCapture: ImageCapture?,
    context: Context,
    onResult: (String) -> Unit
) {
    if (imageCapture == null) return

    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                    recognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            onResult(visionText.text)
                            imageProxy.close()
                        }
                        .addOnFailureListener {
                            onResult("Failed to read text.")
                            imageProxy.close()
                        }
                } else {
                    imageProxy.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                onResult("Camera Capture Failed")
            }
        }
    )
}