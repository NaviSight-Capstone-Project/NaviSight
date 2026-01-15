package edu.capstone.navisight

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import edu.capstone.navisight.common.initialAppPermissions

class PermissionsHelper (private val mainActivity: AppCompatActivity) {
    private lateinit var initialPermissionsLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var backgroundLocationPermissionLauncher: ActivityResultLauncher<String>
    private var context = mainActivity.applicationContext
    private val tag = "PermissionsHelper"

    init {
        registerLaunchers()
    }

    private fun registerLaunchers() {
        // TODO: Make a fragment for each to make it more intuitive to allow permissions
        initialPermissionsLauncher = mainActivity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions -> }
        backgroundLocationPermissionLauncher = mainActivity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted -> }
    }

    fun checkAndRequestInitialPermissions() {
        val permissionsToRequest = initialAppPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        if (permissionsToRequest.isNotEmpty()) {
            initialPermissionsLauncher.launch(permissionsToRequest)
            checkAndRequestBackgroundLocation()
        }
    }

    private fun checkAndRequestBackgroundLocation() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }
}