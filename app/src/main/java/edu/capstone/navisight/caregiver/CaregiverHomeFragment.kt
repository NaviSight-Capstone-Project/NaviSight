package edu.capstone.navisight.caregiver

import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import edu.capstone.navisight.R
import edu.capstone.navisight.caregiver.data.remote.ViuDataSource
import edu.capstone.navisight.caregiver.ui.call.CaregiverCallActivity
import edu.capstone.navisight.caregiver.ui.feature_viu_profile.ViuProfileFragment
import edu.capstone.navisight.caregiver.ui.feature_map.MapFragment
import edu.capstone.navisight.caregiver.ui.feature_notification.NotificationFragment
import edu.capstone.navisight.caregiver.ui.feature_records.RecordsFragment
import edu.capstone.navisight.caregiver.ui.feature_settings.SettingsFragment
import edu.capstone.navisight.caregiver.ui.feature_stream.StreamFragment
import edu.capstone.navisight.caregiver.ui.navigation.BottomNavigationBar
import edu.capstone.navisight.common.webrtc.model.DataModel
import edu.capstone.navisight.common.webrtc.model.DataModelType
import edu.capstone.navisight.common.webrtc.repository.MainRepository
import edu.capstone.navisight.common.webrtc.service.MainService
import edu.capstone.navisight.common.webrtc.utils.getCameraAndMicPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CaregiverHomeFragment : Fragment(),
    RecordsFragment.OnViuClickedListener,
    StreamFragment.StreamFragmentListener,
    MainService.Listener {

    private val viewModel: CaregiverHomeViewModel by viewModels()

    private val mapFragment = MapFragment()
    private val recordsFragment = RecordsFragment()
    private val streamFragment = StreamFragment()
    private val notificationsFragment = NotificationFragment()
    private val settingsFragment = SettingsFragment()
    private lateinit var service: MainService

    private val fragmentJob = SupervisorJob()
    private val fragmentScope = CoroutineScope(Dispatchers.Main + fragmentJob)

    private val viuDataSource = ViuDataSource()
    private lateinit var mainRepository: MainRepository
    private var incomingMediaPlayer: MediaPlayer? = null

    companion object {
        var firstTimeLaunched: Boolean = true
        var pendingDeniedCallMessage: String? = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_caregiver_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainRepository = MainRepository.getInstance(requireContext())
        MainService.listener = this
        service = MainService.getInstance()

        setupBottomNavigation(view)
        observeNavigation()
    }

    private fun setupBottomNavigation(view: View) {
        val bottomNavView = view.findViewById<ComposeView>(R.id.bottom_nav_compose_view)
        bottomNavView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val currentIndex by viewModel.currentScreenIndex.collectAsState()

                BottomNavigationBar(
                    currentIndex = currentIndex,
                    onItemSelected = { index ->
                        viewModel.onScreenSelected(index)
                    }
                )
            }
        }
    }

    private fun observeNavigation() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentScreenIndex.collect { index ->
                when (index) {
                    0 -> showChildFragment(recordsFragment, "TAG_RECORDS")
                    1 -> showChildFragment(streamFragment, "TAG_STREAM")
                    2 -> showChildFragment(mapFragment, "TAG_MAP")
                    3 -> showChildFragment(notificationsFragment, "TAG_NOTIFICATIONS")
                    4 -> showChildFragment(settingsFragment, "TAG_SETTINGS")
                }
            }
        }
    }

    private fun showChildFragment(fragmentInstance: Fragment, tag: String) {
        val transaction = childFragmentManager.beginTransaction()

        childFragmentManager.fragments.forEach {
            transaction.hide(it)
        }

        val existingFragment = childFragmentManager.findFragmentByTag(tag)

        if (existingFragment != null) {
            transaction.show(existingFragment)
        } else {
            transaction.add(R.id.fragment_container, fragmentInstance, tag)
        }

        transaction.commit()
    }

    override fun onViuClicked(viuUid: String) {
        val viuProfileFragment = ViuProfileFragment()
        viuProfileFragment.arguments = bundleOf("viuUid" to viuUid)

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, viuProfileFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        Log.d("deniedcall", "this is working")
        pendingDeniedCallMessage?.let { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            pendingDeniedCallMessage = null
        }
    }

    override fun onVideoCallClickedFromFragment(uid: String) {
        Log.d("CallSignal", "Received Video Call request for: $uid")

        (requireActivity() as AppCompatActivity).getCameraAndMicPermission {
            mainRepository.sendConnectionRequest(uid, true) { success ->
                if (success) {
                    service.startCallTimeoutTimer() // Begin expiration
                    Log.d("CallSignal", "Video call request successfully sent.")
                    val intent = Intent(requireContext(), CaregiverCallActivity::class.java).apply {
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

        (requireActivity() as AppCompatActivity).getCameraAndMicPermission {
            mainRepository.sendConnectionRequest(uid, false) { success ->
                if (success) {
                    service.startCallTimeoutTimer() // Begin expiration
                    Log.d("CallSignal", "Audio call request successfully sent.")
                    val intent = Intent(requireContext(), CaregiverCallActivity::class.java).apply {
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

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentJob.cancel()
        if (MainService.listener == this) {
            MainService.listener = null
        }
        releaseMediaPlayer()
    }

    override fun onCallAborted() {
        activity?.runOnUiThread {
            view?.let { v ->
                val callRequestDialog = v.findViewById<View>(R.id.incomingCallLayout)
                if (callRequestDialog.isVisible) {
                    callRequestDialog.isVisible = false
                    Toast.makeText(requireContext(), "VIU aborted their call.", Toast.LENGTH_LONG).show()
                    releaseMediaPlayer()
                    Log.d("calldeniedmissed", "Incoming call aborted, UI dismissed.")
                }
            }
        }
    }

    override fun onCallMissed(senderId: String) {
        activity?.runOnUiThread {
            view?.let { v ->
                val callRequestDialog = v.findViewById<View>(R.id.incomingCallLayout)
                if (callRequestDialog.isVisible) {
                    callRequestDialog.isVisible = false
                    Toast.makeText(requireContext(), "You've missed your VIU's call!", Toast.LENGTH_LONG).show()
                    releaseMediaPlayer()
                    Log.d("callmissed", "Incoming call missed, dialog dismissed.")
                }
            }
        }
    }

    override fun onCallReceived(model: DataModel) {
        val v = view ?: return

        val callRequestDialog = v.findViewById<View>(R.id.incomingCallLayout)
        val incomingCallTitleTv = v.findViewById<TextView>(R.id.incomingCallTitleTv)
        val acceptButton = v.findViewById<AppCompatButton>(R.id.acceptButton)
        val declineButton = v.findViewById<AppCompatButton>(R.id.declineButton)

        incomingMediaPlayer?.stop()
        incomingMediaPlayer?.release()
        incomingMediaPlayer = null

        val notificationUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val audioAttributes = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        incomingMediaPlayer = MediaPlayer.create(requireContext(), notificationUri)?.apply {
            setAudioAttributes(audioAttributes)
            isLooping = true
            start()
        }

        val isVideoCall = model.type == DataModelType.StartVideoCall
        val isVideoCallText = if (isVideoCall) "video" else "audio"
        val senderUid = model.sender
        val initialText = "${senderUid ?: "Unknown"} wants to $isVideoCallText call you"

        incomingCallTitleTv.text = initialText
        callRequestDialog.isVisible = true
        callRequestDialog.bringToFront()

        if (!senderUid.isNullOrBlank()) {
            fragmentScope.launch(Dispatchers.IO) {
                try {
                    val viu = viuDataSource.getViuDetails(senderUid).first()
                    launch(Dispatchers.Main) {
                        val viuName = viu?.firstName ?: "VIU ($senderUid)"
                        incomingCallTitleTv.text = "$viuName wants to $isVideoCallText call you"
                    }
                } catch (e: Exception) {
                    Log.e("CallSignal", "Failed to fetch VIU profile: ${e.message}")
                }
            }
        }

        acceptButton.setOnClickListener {
            (requireActivity() as AppCompatActivity).getCameraAndMicPermission{
                releaseMediaPlayer()
                service.stopCallTimeoutTimer() // Stop timer on end/miss call timeout
                callRequestDialog.isVisible = false
                startActivity(Intent(requireContext(), CaregiverCallActivity::class.java).apply {
                    putExtra("target", model.sender)
                    putExtra("isVideoCall", isVideoCall)
                    putExtra("isCaller", false)
                })
            }
        }

        declineButton.setOnClickListener {
            releaseMediaPlayer()
            callRequestDialog.isVisible = false
            MainService.getMainRepository()?.sendDeniedCall()
        }
    }

    private fun releaseMediaPlayer() {
        incomingMediaPlayer?.stop()
        incomingMediaPlayer?.release()
        incomingMediaPlayer = null
    }
}