package edu.capstone.navisight.caregiver.ui.feature_scanqr

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import edu.capstone.navisight.R
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanQrFragment : Fragment() {

    private lateinit var viewModel: ScanQrViewModel
    private lateinit var cameraExecutor: ExecutorService
    private var previewView: PreviewView? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[ScanQrViewModel::class.java]
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scan_qr, container, false)

        previewView = view.findViewById(R.id.viewFinder)
        val composeView = view.findViewById<ComposeView>(R.id.composeView)

        composeView.setContent {
            ScanQrScreen(
                viewModel = viewModel,
                onNavigateBack = { parentFragmentManager.popBackStack() }
            )
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        requireActivity().findViewById<View>(R.id.bottom_nav_compose_view)?.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().findViewById<View>(R.id.bottom_nav_compose_view)?.visibility = View.VISIBLE
        previewView = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            if (!isAdded || previewView == null) return@addListener

            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView?.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, QrCodeAnalyzer { qrCode ->
                        viewModel.onQrCodeScanned(qrCode)
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e("ScanQrFragment", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private class QrCodeAnalyzer(private val onQrCodeDetected: (String) -> Unit) : ImageAnalysis.Analyzer {
        private val scanner = BarcodeScanning.getClient()

        @androidx.annotation.OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            barcode.rawValue?.let { value -> onQrCodeDetected(value) }
                        }
                    }
                    .addOnCompleteListener { imageProxy.close() }
            } else {
                imageProxy.close()
            }
        }
    }
}