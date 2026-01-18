package edu.capstone.navisight.viu

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import edu.capstone.navisight.R
import edu.capstone.navisight.common.initialAppPermissions
import edu.capstone.navisight.viu.ui.LocationEvent
import edu.capstone.navisight.viu.ui.LocationTracker
import edu.capstone.navisight.viu.ui.camera.CameraFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val REQUEST_CHECK_SETTINGS = 1001

class ViuHomeFragment : Fragment() {

    private val viewModel: ViuHomeViewModel by viewModels()
    private lateinit var locationTracker: LocationTracker

    private val gpsSwitchStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (LocationManager.PROVIDERS_CHANGED_ACTION == intent?.action) {
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(1000)
                    if (isAdded && ::locationTracker.isInitialized) {
                        locationTracker.checkSettingsAndStart(requireActivity(), REQUEST_CHECK_SETTINGS)
                    }
                }
            }
        }
    }

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

        locationTracker = LocationTracker(requireContext()) { event ->
            when (event) {
                is LocationEvent.Success -> {
                    viewModel.updateLocation(event.lat, event.lon)
                }
                is LocationEvent.GpsDisabled -> {
                    viewModel.setOffline()
                }
            }
        }

        if (savedInstanceState == null) {
            childFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.viu_child_container, CameraFragment(viewModel))
            }
        }

        checkPermissionsAndStart()
    }

    override fun onResume() {
        super.onResume()

        requireContext().registerReceiver(
            gpsSwitchStateReceiver,
            IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        )

        if (hasInitialPermissions() && ::locationTracker.isInitialized) {
            locationTracker.checkSettingsAndStart(requireActivity(), REQUEST_CHECK_SETTINGS)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            requireContext().unregisterReceiver(gpsSwitchStateReceiver)
        } catch (e: Exception) {
        }
    }

    private fun hasInitialPermissions(): Boolean {
        return initialAppPermissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
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
        if (::locationTracker.isInitialized) {
            locationTracker.stopTracking()
        }
    }
}