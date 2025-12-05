package edu.capstone.navisight.viu

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import edu.capstone.navisight.R
import edu.capstone.navisight.viu.ui.camera.CameraFragment
import edu.capstone.navisight.viu.ui.LocationEvent
import edu.capstone.navisight.viu.ui.LocationTracker
import edu.capstone.navisight.viu.ui.temp_feature.VoiceTestFragment // Added for testing

private const val REQUEST_CHECK_SETTINGS = 1001

class ViuHomeFragment : Fragment() {

    private val viewModel: ViuHomeViewModel by viewModels()
    private lateinit var locationTracker: LocationTracker

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                locationTracker.checkSettingsAndStart(requireActivity(), REQUEST_CHECK_SETTINGS)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_viu_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Listen for Location AND Status events
        locationTracker = LocationTracker(requireContext()) { event ->
            when (event) {
                is LocationEvent.Success -> {
                    viewModel.updateLocation(event.lat, event.lon)
                }
                is LocationEvent.GpsDisabled -> {
                    viewModel.setOffline() // Mark offline if GPS dies
                }
            }
        }

        checkPermissionsAndStart()

        if (savedInstanceState == null) {
            childFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.viu_child_container, CameraFragment())
                //replace(R.id.viu_child_container, VoiceTestFragment()) // Switched for testing
            }
        }
    }

    private fun checkPermissionsAndStart() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationTracker.checkSettingsAndStart(requireActivity(), REQUEST_CHECK_SETTINGS)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Stop tracker to save battery
        if (::locationTracker.isInitialized) {
            locationTracker.stopTracking()
        }
    }
}