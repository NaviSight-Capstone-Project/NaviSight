package edu.capstone.navisight.viu.ui.temp_feature
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.content.Context
//import android.content.pm.PackageManager
//import android.location.Location
//import android.location.LocationListener
//import android.location.LocationManager
//import android.os.Bundle
//import android.view.View
//import android.widget.Button
//import android.widget.TextView
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.core.content.ContextCompat
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.lifecycleScope
//import edu.capstone.navisight.R
//import edu.capstone.navisight.viu.ui.SpeechToTextHandler
//import edu.capstone.navisight.viu.utils.TextToSpeechHelper
//
//class VoiceTestFragment : Fragment(R.layout.fragment_voice_test) {
//
//    private lateinit var ttsHelper: TextToSpeechHelper
//    private lateinit var voiceLocationHandler: SpeechToTextHandler
//
//    // GPS Stuff
//    private var locationManager: LocationManager? = null
//
//    // UI Stuff
//    private lateinit var tvLog: TextView
//    private lateinit var btnTest: Button
//
//    // Permission Launcher
//    private val requestPermissionLauncher =
//        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
//            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
//                permissions[Manifest.permission.RECORD_AUDIO] == true
//            ) {
//                log("Permissions granted. Starting location updates.")
//                startLocationUpdates()
//            } else {
//                log("Permissions denied. Feature will not work.")
//            }
//        }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        tvLog = view.findViewById(R.id.tv_debug_log)
//        btnTest = view.findViewById(R.id.btn_test_voice)
//
//        log("Initializing subsystems...")
//
//        ttsHelper = TextToSpeechHelper(requireContext())
//
//        voiceLocationHandler = SpeechToTextHandler(requireContext(), viewLifecycleOwner.lifecycleScope)
//        voiceLocationHandler.initialize()
//
//        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
//
//        btnTest.setOnClickListener {
//            log("Button Pressed. Stopping TTS and Listening...")
//            voiceLocationHandler.startListeningForCommand()
//        }
//
//        checkPermissionsAndStart()
//    }
//
//    private fun checkPermissionsAndStart() {
//        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
//            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
//        ) {
//            log("Requesting permissions...")
//            requestPermissionLauncher.launch(
//                arrayOf(
//                    Manifest.permission.ACCESS_FINE_LOCATION,
//                    Manifest.permission.RECORD_AUDIO
//                )
//            )
//        } else {
//            startLocationUpdates()
//        }
//    }
//
//    @SuppressLint("MissingPermission")
//    private fun startLocationUpdates() {
//        try {
//            log("Requesting GPS updates...")
//
//            locationManager?.requestLocationUpdates(
//                LocationManager.GPS_PROVIDER,
//                5000L,
//                5f,
//                locationListener
//            )
//
//            locationManager?.requestLocationUpdates(
//                LocationManager.NETWORK_PROVIDER,
//                5000L,
//                5f,
//                locationListener
//            )
//
//        } catch (e: Exception) {
//            log("Error starting location: ${e.message}")
//        }
//    }
//
//    private val locationListener = object : LocationListener {
//        override fun onLocationChanged(location: Location) {
//            voiceLocationHandler.currentLat = location.latitude
//            voiceLocationHandler.currentLon = location.longitude
//
//        }
//
//        override fun onProviderEnabled(provider: String) { log("GPS Provider Enabled: $provider") }
//        override fun onProviderDisabled(provider: String) { log("GPS Provider Disabled: $provider") }
//    }
//
//    private fun log(message: String) {
//        val time = android.text.format.DateFormat.format("HH:mm:ss", System.currentTimeMillis())
//        tvLog.append("\n[$time] $message")
//
//        val scroll = tvLog.parent as? android.widget.ScrollView
//        scroll?.post { scroll.fullScroll(View.FOCUS_DOWN) }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        locationManager?.removeUpdates(locationListener)
//        voiceLocationHandler.stopAndCleanup()
//    }
//}