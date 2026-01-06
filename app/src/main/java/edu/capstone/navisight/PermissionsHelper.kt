package edu.capstone.navisight

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class PermissionsHelper (private val mainActivity: AppCompatActivity) {
    private lateinit var initialPermissionsLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var backgroundLocationPermissionLauncher: ActivityResultLauncher<String>
    private var context = mainActivity.applicationContext
    private val tag = "PermissionsHelper"

    private val initialPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.VIBRATE,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.SEND_SMS
    )

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
        val permissionsToRequest = initialPermissions.filter {
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