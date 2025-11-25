package edu.capstone.navisight

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import edu.capstone.navisight.auth.AuthActivity
import edu.capstone.navisight.auth.data.remote.CloudinaryDataSource
import edu.capstone.navisight.auth.domain.GetUserCollectionUseCase
import edu.capstone.navisight.caregiver.CaregiverHomeFragment
 import edu.capstone.navisight.viu.ViuHomeFragment
import edu.capstone.navisight.common.domain.usecase.GetCurrentUserUidUseCase
import edu.capstone.navisight.guest.GuestFragment
import edu.capstone.navisight.webrtc.repository.MainRepository
import edu.capstone.navisight.webrtc.service.MainService
import edu.capstone.navisight.webrtc.service.MainServiceActions
import edu.capstone.navisight.webrtc.service.MainServiceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var getUserCollectionUseCase: GetUserCollectionUseCase
    private val getCurrentUserUidUseCase = GetCurrentUserUidUseCase()

    private lateinit var mainServiceRepository: MainServiceRepository
    private lateinit var mainRepository: MainRepository

    companion object {
        var firstTimeLaunched: Boolean = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MapLibre.getInstance(this)
        CloudinaryDataSource.init(this)
        FirebaseApp.initializeApp(this)

        mainServiceRepository = MainServiceRepository.getInstance(applicationContext)
        mainRepository = MainRepository.getInstance(applicationContext)

        val permissionHandler = PermissionHandler()
        auth = FirebaseAuth.getInstance()

        permissionHandler.checkAndRequestInitialPermissions()
    }

    fun beginAppFlow() {
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            handleSuccessfulLogin(currentUser.email.toString(), currentUser.uid)

            lifecycleScope.launch {
                try {
                    getUserCollectionUseCase = GetUserCollectionUseCase()
                    val collection = getUserCollectionUseCase(currentUser.uid)

                    when (collection) {
                        "caregivers" -> navigateToHomeFragment(isCaregiver = true)
                        "vius" -> navigateToHomeFragment(isCaregiver = false)
                        else -> navigateToAuth()
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error fetching user collection", e)
                    navigateToAuth()
                }
            }
        } else {
            navigateToGuestMode()
//            navigateToAuth()
        }
    }

    private fun navigateToAuth() {
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToGuestMode() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, GuestFragment())
            .commit()
    }

    private fun navigateToHomeFragment(isCaregiver: Boolean) {
        val fragment = if (isCaregiver) {
            CaregiverHomeFragment()
        } else {
            ViuHomeFragment()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun handleSuccessfulLogin(email: String, uid: String) {
        mainRepository.login(uid) { isDone, reason ->
            if (isDone) {
                Log.d("CallSignal", "User set to ONLINE")
                startWebrtcService(email)
            } else {
                Log.e("CallSignal", "Failed to set user to ONLINE: $reason")
            }
        }

        if (firstTimeLaunched) {
            CoroutineScope(Dispatchers.Main).launch {
                delay(5500)
                firstTimeLaunched = false
            }
        }
    }

    private fun startWebrtcService(currentUser: String) {
        val intent = Intent(this, MainService::class.java).apply {
            action = MainServiceActions.START_SERVICE.name
            putExtra("username", currentUser)
        }
        startForegroundService(intent)
    }

    private inner class PermissionHandler {
        private lateinit var initialPermissionsLauncher: ActivityResultLauncher<Array<String>>
        private lateinit var backgroundLocationPermissionLauncher: ActivityResultLauncher<String>

        private val INITIAL_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        )

        init {
            registerLaunchers()
        }

        private fun registerLaunchers() {
            initialPermissionsLauncher = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val cameraGranted = permissions.getOrDefault(Manifest.permission.CAMERA, false)
                val fineGranted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)
                val coarseGranted = permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)

                if (cameraGranted && (fineGranted || coarseGranted)) {
                    beginAppFlow()
                } else {
                    Toast.makeText(this@MainActivity, "Permissions required to proceed", Toast.LENGTH_SHORT).show()
                }
            }

            backgroundLocationPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (!granted) {
                    Toast.makeText(this@MainActivity, "Please allow background location in settings", Toast.LENGTH_LONG).show()
                }
            }
        }

        fun checkAndRequestInitialPermissions() {
            val permissionsToRequest = INITIAL_PERMISSIONS.filter {
                ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
            }.toTypedArray()

            if (permissionsToRequest.isNotEmpty()) {
                initialPermissionsLauncher.launch(permissionsToRequest)
            } else {
                checkAndRequestBackgroundLocation()
                beginAppFlow()
            }
        }

        private fun checkAndRequestBackgroundLocation() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val granted = ContextCompat.checkSelfPermission(
                    this@MainActivity, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (!granted) {
                    backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }
        }
    }
}