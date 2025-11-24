package edu.capstone.navisight

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import edu.capstone.navisight.auth.data.remote.CloudinaryDataSource
import edu.capstone.navisight.auth.domain.GetUserCollectionUseCase
import edu.capstone.navisight.auth.ui.login.LoginActivity
import edu.capstone.navisight.caregiver.CaregiverHomeActivity
import edu.capstone.navisight.common.domain.usecase.GetCurrentUserUidUseCase
import edu.capstone.navisight.guest.GuestFragment
import edu.capstone.navisight.viu.ViuHomeActivity
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

    // WebRTC init.
    private lateinit var mainServiceRepository: MainServiceRepository
    private lateinit var mainRepository : MainRepository
    companion object {
        var firstTimeLaunched : Boolean = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Begin app here.
        // TODO: Make the functions more seamless and their names more accurate
        mainServiceRepository = MainServiceRepository.getInstance(applicationContext)
        mainRepository = MainRepository.getInstance(applicationContext)
        Log.d("CallSignal", "Finished getting instance of main and mainservice repos")

        FirebaseApp.initializeApp(this)
        val permissionHandler = PermissionHandler()
//        val locationTracker = LocationTracker(this)
        auth = FirebaseAuth.getInstance()

        // Activate services if user is logged-in.
        val currentUser = auth.currentUser
        Log.d("AuthenticationCheck", auth.currentUser?.email.toString())
        if (currentUser != null) {
            Log.d("AuthenticationCheck",
                "Current email of user logged in is: ${currentUser.email}")
            Log.d("AuthenticationCheck",
                "Current UID of user logged in is: ${currentUser.uid}")

            handleSuccessfulLogin(currentUser.email.toString(), currentUser.uid)
        } else {
            Log.d("CallSignal", "warning! user is not logged in, call services are off")

        }
        permissionHandler.checkAndRequestInitialPermissions()
    }

    fun navigateToGuestMode() {
        supportFragmentManager.commit {
            add(R.id.fragment_container, GuestFragment())
        }
    }

    private fun navigateTo(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
        finish()
    }

    fun handleSuccessfulLogin(email: String, uid: String) {
        mainRepository.login(uid) { isDone, reason ->
            if (isDone) {
                Log.d("CallSignal", "User just logged in and set to ONLINE")
                startWebrtcService(email)

                // Additionally, call decideAppFlow() to ensure all navigation/permissions are finalized.
                beginAppFlow()
            } else {
                Log.e("CallSignal", "Failed to set user to ONLINE: $reason")
            }
        }
        // Add delay to make sure everything is initialized first.
        // For Firebase Authorization sync if last event is "denied call"
        val mainScope = CoroutineScope(Dispatchers.Main)
        if (firstTimeLaunched) {
            mainScope.launch {
                delay(5500)
                firstTimeLaunched = false
            }
        }
    }

    private fun startWebrtcService(currentUser: String) {
        Log.d("ServiceCheck", "Attempting to start MainService via startForegroundService()")
        val intent = Intent(this, MainService::class.java).apply {
            action = MainServiceActions.START_SERVICE.name
            putExtra("username", currentUser)
        }
        startForegroundService(intent)
    }

    // Set how the app will go depending on user credentials
    fun beginAppFlow() {
        MapLibre.getInstance(this)
        CloudinaryDataSource.init(this)
        auth = FirebaseAuth.getInstance()
        getUserCollectionUseCase = GetUserCollectionUseCase()
        val currentUser = getCurrentUserUidUseCase()
        lifecycleScope.launch {
            if (currentUser == null) {
                navigateToGuestMode()
            } else {
                try {
                    val collection = getUserCollectionUseCase(currentUser)
                    when (collection) {
                        "vius" -> navigateTo(ViuHomeActivity::class.java)
                        "caregivers" -> navigateTo(CaregiverHomeActivity::class.java)
                        else -> {
                            auth.signOut()
                            navigateTo(LoginActivity::class.java)
                        }
                    }
                } catch (e: Exception) { navigateTo(LoginActivity::class.java) }
            }
        }
    }

    // Set all initial permissions here
    private inner class PermissionHandler {
        private lateinit var initialPermissionsLauncher: ActivityResultLauncher<Array<String>>
        private lateinit var backgroundLocationPermissionLauncher: ActivityResultLauncher<String>

        private val INITIAL_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        private val locationTag = "LocationPermission"

        init {
            registerLaunchers()
        }

        private fun registerLaunchers() {
            initialPermissionsLauncher = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->

                val cameraGranted = permissions.getOrDefault(Manifest.permission.CAMERA, false)
                val fineGranted =
                    permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)
                val coarseGranted =
                    permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)

                if (cameraGranted) Log.d(locationTag, "Camera permission granted.")
                else Log.w(locationTag, "Camera permission denied.")

                if (fineGranted || coarseGranted) {
                    Log.d(
                        locationTag,
                        "Foreground location granted. Proceeding to background check."
                    )
                    checkAndRequestBackgroundLocation()
                } else {
                    Log.w(
                        locationTag,
                        "Foreground location denied. Location features will be disabled."
                    )
                }
                if (cameraGranted && (fineGranted || coarseGranted)) beginAppFlow()
            }


            backgroundLocationPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    Log.d(locationTag, "Background location granted.")
                } else {
                    Log.w(locationTag, "Background location denied.")
                }
            }
        }

        fun checkAndRequestInitialPermissions() {
            val permissionsToRequest = INITIAL_PERMISSIONS.filter {
                ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            }.toTypedArray()

            if (permissionsToRequest.isNotEmpty()) {
                Log.d(locationTag, "Requesting: ${permissionsToRequest.contentToString()}")
                initialPermissionsLauncher.launch(permissionsToRequest)
            } else {
                Log.d(locationTag, "All initial permissions already granted.")
                checkAndRequestBackgroundLocation()
                beginAppFlow()
            }
        }

        private fun checkAndRequestBackgroundLocation() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val granted = ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (!granted) {
                    Log.d(locationTag, "Requesting background location permission.")
                    backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    Log.d(locationTag, "Background location already granted.")
                }
            } else {
                Log.d(locationTag, "Pre-Q device — background location is implicit.")
            }
        }
    }
}