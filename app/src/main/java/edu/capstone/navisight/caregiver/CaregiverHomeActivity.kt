package edu.capstone.navisight.caregiver

import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import edu.capstone.navisight.R
import edu.capstone.navisight.auth.ui.login.LoginActivity
import edu.capstone.navisight.caregiver.data.remote.ViuDataSource
import edu.capstone.navisight.caregiver.ui.call.CaregiverCallActivity
import edu.capstone.navisight.caregiver.ui.feature_editProfile.AccountInfoFragment
import edu.capstone.navisight.caregiver.ui.feature_editViuProfile.ViuProfileFragment
import edu.capstone.navisight.caregiver.ui.feature_map.MapFragment
import edu.capstone.navisight.caregiver.ui.feature_notification.NotificationFragment
import edu.capstone.navisight.caregiver.ui.feature_records.RecordsFragment
import edu.capstone.navisight.caregiver.ui.feature_settings.SettingsFragment
import edu.capstone.navisight.caregiver.ui.feature_stream.StreamFragment
import edu.capstone.navisight.caregiver.ui.navigation.BottomNavItem
import edu.capstone.navisight.caregiver.ui.navigation.BottomNavigationBar
import edu.capstone.navisight.webrtc.model.DataModel
import edu.capstone.navisight.webrtc.model.DataModelType
import edu.capstone.navisight.webrtc.repository.MainRepository
import edu.capstone.navisight.webrtc.service.MainService
import edu.capstone.navisight.webrtc.utils.getCameraAndMicPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CaregiverHomeActivity : AppCompatActivity(), RecordsFragment.OnViuClickedListener,
    StreamFragment.StreamFragmentListener, MainService.Listener {

    private val viewModel: CaregiverHomeViewModel by viewModels()

    private val mapFragment = MapFragment()
    private val recordsFragment = RecordsFragment()
    private val streamFragment = StreamFragment()
    private val notificationsFragment = NotificationFragment()
    private val settingsFragment = SettingsFragment()

    // WebRTC and calls.
    private val activityJob = SupervisorJob()
    private val activityScope = CoroutineScope(Dispatchers.Main + activityJob)
    private val viuDataSource = ViuDataSource()
    private lateinit var mainRepository : MainRepository
    private var incomingMediaPlayer: MediaPlayer? = null
    companion object {
        var firstTimeLaunched: Boolean = true
        var pendingDeniedCallMessage: String? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_caregiver_home)

        // WebRTC.
        mainRepository = MainRepository.getInstance(applicationContext)
        mainRepository.setMainActivity(this) // TODO: Optimize code, otherwise, leave it for Notification Helper :3
        MainService.listener = this

        setupBottomNavigation()
        observeSession()
        observeNavigation()
    }

    private fun setupBottomNavigation() {
        val bottomNavView = findViewById<ComposeView>(R.id.bottom_nav_compose_view)
        bottomNavView.setContent {
            val currentIndex by viewModel.currentScreenIndex.collectAsState()

            BottomNavigationBar(
                currentIndex = currentIndex,
                onItemSelected = { index ->
                    viewModel.onScreenSelected(index)
                }
            )
        }
    }

    private fun observeSession() {
        lifecycleScope.launch {
            viewModel.isSessionValid.collect { isValid ->
                if (!isValid) {
                    startActivity(Intent(this@CaregiverHomeActivity, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }

    private fun observeNavigation() {
        lifecycleScope.launch {
            viewModel.currentScreenIndex.collect { index ->

                when (index) {
                    0 -> showFragment(mapFragment, "TAG_MAP")
                    1 -> showFragment(recordsFragment, "TAG_RECORDS")
                    2 -> showFragment(streamFragment, "TAG_STREAM")
                    3 -> showFragment(notificationsFragment, "TAG_NOTIFICATIONS")
                    4 -> showFragment(settingsFragment, "TAG_SETTINGS")
                }
            }
        }
    }


    private fun showFragment(fragmentInstance: Fragment, tag: String) {
        val transaction = supportFragmentManager.beginTransaction()

        supportFragmentManager.fragments.forEach {
            transaction.hide(it)
        }

        val existingFragment = supportFragmentManager.findFragmentByTag(tag)

        if (existingFragment != null) {
            transaction.show(existingFragment)
        } else {
            transaction.add(R.id.fragment_container, fragmentInstance, tag)
        }

        transaction.commit()
    }


    private fun handleNavigation(index: Int) {
        val fragment: Fragment = when (index) {
            BottomNavItem.Track.index -> MapFragment()
            BottomNavItem.Records.index -> RecordsFragment()
            BottomNavItem.Stream.index -> StreamFragment()
            BottomNavItem.Notification.index -> NotificationFragment()
            BottomNavItem.Settings.index -> SettingsFragment()
            else -> MapFragment()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onViuClicked(viuUid: String) {
        // This is the navigation logic
        val viuProfileFragment = ViuProfileFragment()

        // Pass the viuUid to the new fragment
        viuProfileFragment.arguments = bundleOf("viuUid" to viuUid)

        // Perform the fragment transaction
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, viuProfileFragment) // Use the same container ID
            .addToBackStack(null)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        Log.d("deniedcall", "this is working")
        pendingDeniedCallMessage?.let { message ->
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            pendingDeniedCallMessage = null
        }
    }


    override fun onVideoCallClickedFromFragment(uid: String) {
        Log.d("CallSignal", "Received Video Call request for: $uid")

        // Request camera + mic permissions
        getCameraAndMicPermission {
            mainRepository.sendConnectionRequest(uid, true) { success ->
                if (success) {
                    Log.d("CallSignal", "Video call request successfully sent.")
                    val intent = Intent(this, CaregiverCallActivity::class.java).apply {
                        putExtra("target", uid)
                        putExtra("isVideoCall", true)
                        putExtra("isCaller", true)
                    }
                    startActivity(intent)
                } else {
                    Log.e("CallSignal", "Failed to send video call request.")
                }
            }
        }
    }

    override fun onAudioCallClickedFromFragment(uid: String) {
        Log.d("CallSignal", "Received Audio Call request for: $uid")

        getCameraAndMicPermission {
            mainRepository.sendConnectionRequest(uid, false) { success ->
                if (success) {
                    Log.d("CallSignal", "Audio call request successfully sent.")
                    val intent = Intent(this, CaregiverCallActivity::class.java).apply {
                        putExtra("target", uid)
                        putExtra("isVideoCall", false)
                        putExtra("isCaller", true)
                    }
                    startActivity(intent)
                } else {
                    Log.e("CallSignal", "Failed to send audio call request.")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityJob.cancel()
        if (MainService.listener == this) {
            MainService.listener = null
        }
        releaseMediaPlayer()
        mainRepository.setOffline() // For some reason, HomeActivity is more superior than MainActivity.
    }

    override fun onCallAborted () {
        runOnUiThread {
            val callRequestDialog = findViewById<View>(R.id.incomingCallLayout)
            if (callRequestDialog.isVisible) {
                callRequestDialog.isVisible = false
                Toast.makeText(
                    this,
                    "VIU aborted their call.",
                    Toast.LENGTH_LONG
                ).show()
                releaseMediaPlayer()
                Log.d("calldeniedmissed", "Incoming call aborted, UI dismissed and ringing stopped.")
            } else {
                Log.d("calldeniedmissed", "Call aborted, but no UI was active.")
            }
        }
    }

    override fun onCallMissed(senderId: String) {
        Log.d("missedcall", "entering on call missed")
        runOnUiThread {
            Log.d("missedcall", "running on ui thread")
            val callRequestDialog = findViewById<View>(R.id.incomingCallLayout)
            if (callRequestDialog.isVisible) {
                callRequestDialog.isVisible = false
                Toast.makeText(
                    applicationContext,
                    "You've missed your VIU's call!",
                    Toast.LENGTH_LONG
                ).show()
                releaseMediaPlayer()
                Log.d("callmissed", "Incoming call missed, dialog dismissed and ringing stopped.")
            } else {
                Log.d("callmissed", "Call missed, but no dialog was active.")
            }
        }
    }

    override fun onCallReceived(model: DataModel) {
        val callRequestDialog = findViewById<View>(R.id.incomingCallLayout)
        val incomingCallTitleTv = findViewById<TextView>(R.id.incomingCallTitleTv)
        val acceptButton = findViewById<AppCompatButton>(R.id.acceptButton)
        val declineButton = findViewById<AppCompatButton>(R.id.declineButton)

        // Begin ringtone
        // Stop and release any existing player for safety
        incomingMediaPlayer?.stop()
        incomingMediaPlayer?.release()
        incomingMediaPlayer = null

        // set MediaPlayer for continuous looping ringtone, retrieve also default rt
        Log.d("ring", "retrieving default ringtone...")
        val notificationUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val audioAttributes = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE) // Best fit for ringtone
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        incomingMediaPlayer = MediaPlayer.create(applicationContext, notificationUri)?.apply {
            setAudioAttributes(audioAttributes) // Set the attributes
            isLooping = true
            start()
        }

        if (callRequestDialog == null || incomingCallTitleTv == null) {
            Log.e("CallSignal", "UI components not found.")
            return
        }

        val isVideoCall = model.type == DataModelType.StartVideoCall
        val isVideoCallText = if (isVideoCall) "video" else "audio"

        // Use a local variable for safety
        val senderUid = model.sender

        // Set initial text with UID immediately (in case network fetch is slow or fails)
        val initialText = "${senderUid ?: "Unknown"} wants to $isVideoCallText call you"
        incomingCallTitleTv.text = initialText
        callRequestDialog.isVisible = true
        callRequestDialog.bringToFront()


        // Explicit check for null and blank before calling getProfile
        if (senderUid != null && senderUid.isNotBlank()) {
            Log.d("CallSignal", "Sender UID is valid. Fetching profile...")

            // Launch coroutine on the background thread (Dispatchers.IO)
            activityScope.launch(Dispatchers.IO) {
                try {
                    // The input is now guaranteed to be a non-nullable String
                    val viu = viuDataSource.getViuDetails(senderUid).first()

                    // Switch back to Main thread to update the UI
                    launch(Dispatchers.Main) {
                        val viuName = viu?.firstName ?: "VIU ($senderUid)"

                        incomingCallTitleTv.text = "$viuName wants to $isVideoCallText call you"
                        Log.d("CallSignal", "Notification text updated to: $viuName")
                    }
                } catch (e: Exception) {
                    Log.e("CallSignal", "Failed to fetch VIU profile for $senderUid: ${e.message}")
                }
            }
        } else {
            // If the UID is bad, log the failure but keep the initial notification visible
            Log.e("CallSignal", "model.sender is null or blank. Cannot fetch profile. Displaying UID as is.")
        }

        // Set click listeners
        acceptButton.setOnClickListener {
            getCameraAndMicPermission {
                releaseMediaPlayer() // Stop ringtone upon user action
                callRequestDialog.isVisible = false
                startActivity(Intent(this, CaregiverCallActivity::class.java).apply {
                    // model.sender is used here for the call intent regardless of profile fetch success
                    putExtra("target", model.sender)
                    putExtra("isVideoCall", isVideoCall)
                    putExtra("isCaller", false)
                })
            }
        }
        declineButton.setOnClickListener {

            releaseMediaPlayer() // Stop ringtone upon user action
            callRequestDialog.isVisible = false
            Log.d("CallSignal", "Call declined by user.")
            MainService.getMainRepository()?.sendDeniedCall()
        }
    }

    private fun releaseMediaPlayer() {
        Log.d("ring", "releasing media player")

        incomingMediaPlayer?.stop()
        incomingMediaPlayer?.release()
        incomingMediaPlayer = null
    }


}