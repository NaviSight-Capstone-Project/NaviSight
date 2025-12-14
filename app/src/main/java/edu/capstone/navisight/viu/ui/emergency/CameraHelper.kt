package edu.capstone.navisight.viu.ui.emergency
import android.content.Context
import android.net.Uri
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.coroutines.resume

object CameraHelper {
    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun captureImage(
        androidContext: Context,
        lifecycleOwner: LifecycleOwner,
        lensFacing: Int
    ): Uri? = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->

            lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                try {
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(androidContext)
                    cameraProviderFuture.addListener({
                        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

                        // Rotation logic
                        val rotation = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            androidContext.display?.rotation ?: 0
                        } else {
                            @Suppress("DEPRECATION")
                            (androidContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay?.rotation ?: 0
                        }

                        val imageCapture = ImageCapture.Builder()
                            .setTargetRotation(rotation)
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()

                        cameraProvider.unbindAll()
                        val photoFile = createPhotoFile(androidContext, if (lensFacing == CameraSelector.LENS_FACING_FRONT) "FRONT" else "BACK")
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            imageCapture
                        )

                        imageCapture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(androidContext),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    cameraProvider.unbindAll()
                                    if (continuation.isActive) {
                                        continuation.resume(output.savedUri ?: Uri.fromFile(photoFile))
                                    }
                                }

                                override fun onError(exc: ImageCaptureException) {
                                    cameraProvider.unbindAll()
                                    if (continuation.isActive) {
                                        continuation.resume(null)
                                    }
                                }
                            })

                    }, ContextCompat.getMainExecutor(androidContext))
                } catch (e: Exception) {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
        }
    }

    private fun createPhotoFile(androidContext: Context, cameraType: String): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "VIU_EMERGENCY_${cameraType}_${timeStamp}.jpg"
        val storageDir = File(androidContext.filesDir, "emergency_photos")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        return File(storageDir, fileName)
    }
}