package edu.capstone.navisight

/*

MainActivity.kt

Main flow of NaviSight.
Please do not delete necessary comments or TODOs unless finished or unneeded.

 */

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import edu.capstone.navisight.auth.AuthActivity
import edu.capstone.navisight.auth.data.remote.CloudinaryDataSource
import edu.capstone.navisight.auth.domain.GetUserCollectionUseCase
import edu.capstone.navisight.caregiver.CaregiverHomeFragment
import edu.capstone.navisight.caregiver.service.ViuMonitorService
import edu.capstone.navisight.common.Constants.SHARED_PREFERENCES_NAME
import edu.capstone.navisight.common.Constants.USER_TYPE_KEY
import edu.capstone.navisight.common.TextToSpeechHelper
import edu.capstone.navisight.disclaimer.DisclaimerFragment
import edu.capstone.navisight.viu.ViuHomeFragment
import edu.capstone.navisight.guest.GuestFragment
import edu.capstone.navisight.common.webrtc.repository.MainRepository
import edu.capstone.navisight.common.webrtc.service.MainService
import edu.capstone.navisight.common.webrtc.service.MainServiceActions
import edu.capstone.navisight.common.webrtc.service.MainServiceRepository
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre


private const val IS_DISCLAIMER_AGREED_SP_NAME = "IsDisclaimerAgreed"

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var getUserCollectionUseCase: GetUserCollectionUseCase
    private lateinit var mainServiceRepository: MainServiceRepository
    private lateinit var mainRepository: MainRepository
    private lateinit var currentUser : FirebaseUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Init. TTS for faster processing times and prevent lazy loading
        TextToSpeechHelper.initialize(applicationContext)

        setContentView(R.layout.activity_main)

        MapLibre.getInstance(this)
        CloudinaryDataSource.init(this)
        FirebaseApp.initializeApp(this)

        mainServiceRepository = MainServiceRepository.getInstance(applicationContext)
        mainRepository = MainRepository.getInstance(applicationContext)
        auth = FirebaseAuth.getInstance()

        // Setup all permissions using this helper, begin if set properly
        val permissionHandler = PermissionsHelper(this)
        permissionHandler.checkAndRequestInitialPermissions()

        // Start
        beginAppFlow()
    }

    fun startMonitoringService() {
        val serviceIntent = Intent(this, ViuMonitorService::class.java)

        // Android O (API 26) and higher require startForegroundService()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun saveUserType(userType: String) {
        val sharedPref = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(USER_TYPE_KEY, userType)
            apply()
        }
    }

    fun startAppWithRegisteredUser(){
        currentUser = auth.currentUser!!
        mainRepository.setEmail(currentUser.email.toString())

        // Fetch user collection (userType) synchronously (in a coroutine)
        lifecycleScope.launch {
            try {
                getUserCollectionUseCase = GetUserCollectionUseCase()
                val collection = getUserCollectionUseCase(currentUser.uid)

                // Pass the determined user type to the MainRepository/FirebaseClient
                if (collection == "caregivers" || collection == "vius") {
                    mainRepository.setUserType(collection)
                    startWebrtcService(currentUser.email.toString()) // Start service AFTER userType is set
                    handleSuccessfulLogin(currentUser.email.toString(), currentUser.uid)
                    saveUserType(collection)
                }

                // Navigate based on the type
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
        startMonitoringService()
    }

    fun beginAppFlow() {
        if (isDisclaimerAgreed()){
            if (auth.currentUser != null) {
                startAppWithRegisteredUser()
            } else navigateToGuestMode()
        } else navigateToDisclaimer()
    }

    private fun isDisclaimerAgreed() : Boolean {
        val prefs = getSharedPreferences("NaviData", Context.MODE_PRIVATE)
        return prefs.getBoolean(IS_DISCLAIMER_AGREED_SP_NAME, false)
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

    private fun navigateToDisclaimer() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, DisclaimerFragment())
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
                startWebrtcService(email) // Init. WebRTC handler.
            } else {
                Log.e("CallSignal", "Failed to set user to ONLINE: $reason")
            }
        }
    }

    private fun startWebrtcService(currentUser: String) {
        val intent = Intent(this, MainService::class.java).apply {
            action = MainServiceActions.START_SERVICE.name
            putExtra("email", currentUser)
        }
        startForegroundService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            if (MainService.listener == this) {
                MainService.listener = null
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error clearing listener", e)
        }

        if (::mainRepository.isInitialized) {
            try {
                mainRepository.setOffline()
            } catch (e: Exception) {
                Log.d("MainActivity", "Skipping setOffline (User likely already logged out)")
            }
        }

        // Clear TTS.
        TextToSpeechHelper.shutdown()
    }
}