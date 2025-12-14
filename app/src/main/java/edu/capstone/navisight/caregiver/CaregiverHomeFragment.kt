package edu.capstone.navisight.caregiver

import edu.capstone.navisight.caregiver.ui.emergency.GreenResponse
import edu.capstone.navisight.caregiver.ui.emergency.RedAlert
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import edu.capstone.navisight.R
import edu.capstone.navisight.caregiver.data.remote.ViuDataSource
import edu.capstone.navisight.caregiver.service.ACTION_EMERGENCY_ALERT
import edu.capstone.navisight.caregiver.service.EXTRA_LOCATION
import edu.capstone.navisight.caregiver.service.EXTRA_TIMESTAMP
import edu.capstone.navisight.caregiver.service.EXTRA_VIU_ID
import edu.capstone.navisight.caregiver.service.EXTRA_VIU_NAME
import edu.capstone.navisight.caregiver.service.ViuMonitorService
import edu.capstone.navisight.caregiver.ui.call.CaregiverCallActivity
import edu.capstone.navisight.caregiver.ui.emergency.*
import edu.capstone.navisight.caregiver.ui.emergency.EmergencyAlertDialog
import edu.capstone.navisight.caregiver.ui.emergency.EmergencySignal
import edu.capstone.navisight.caregiver.ui.emergency.EmergencyViewModel
import edu.capstone.navisight.caregiver.ui.feature_viu_profile.ViuProfileFragment
import edu.capstone.navisight.caregiver.ui.feature_map.MapFragment
import edu.capstone.navisight.caregiver.ui.feature_notification.NotificationFragment
import edu.capstone.navisight.caregiver.ui.feature_records.RecordsFragment
import edu.capstone.navisight.caregiver.ui.feature_settings.SettingsFragment
import edu.capstone.navisight.caregiver.ui.feature_stream.StreamFragment
import edu.capstone.navisight.caregiver.ui.navigation.BottomNavigationBar
import edu.capstone.navisight.common.VibrationHelper
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
import kotlin.collections.listOf

class CaregiverHomeFragment : Fragment(),
    RecordsFragment.OnViuClickedListener,
    StreamFragment.StreamFragmentListener,
    MainService.Listener {

    private val viewModel: CaregiverHomeViewModel by viewModels()
    private val emergencyViewModel: EmergencyViewModel by activityViewModels()

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
    private var incomingCallDialog: IncomingCallDialog? = null

    companion object {
        var firstTimeLaunched: Boolean = true
        var pendingDeniedCallMessage: String? = null
    }

    private val emergencyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_EMERGENCY_ALERT) {
                val viuId = intent.getStringExtra(EXTRA_VIU_ID) ?: return
                val viuName = intent.getStringExtra(EXTRA_VIU_NAME) ?: "VIU"
                val location = intent.getStringExtra(EXTRA_LOCATION) ?: "Unknown Location"
                val timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
                val signal = EmergencySignal(viuId, viuName, location, timestamp)
                Log.d("EmergencyReceiver", "Broadcast received: PUSH for new alert from $viuName.")
                emergencyViewModel.activateEmergency(signal)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(ACTION_EMERGENCY_ALERT)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(emergencyReceiver, filter)
        Log.d("EmergencyReceiver", "Receiver registered for: $ACTION_EMERGENCY_ALERT")
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(emergencyReceiver)
        Log.d("EmergencyReceiver", "Receiver unregistered.")
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
        setupEmergencyDialogComposables(view)
        observeEmergencySignalVisibility(view)
        observeEmergencyCallRequests()
        checkExistingEmergencyAlertFromService()
    }

    private fun observeEmergencyCallRequests() {
        viewLifecycleOwner.lifecycleScope.launch {
            emergencyViewModel.callRequest.collect { (viuUid, isVideoCall) ->
                if (isVideoCall) {
                    onEmergencyVideoCallClickedFromFragment(viuUid)
                } else {
                    onEmergencyAudioCallClickedFromFragment(viuUid)
                }
            }
        }
    }

    private fun checkExistingEmergencyAlertFromService() {
        ViuMonitorService.INSTANCE?.getCurrentEmergencySignal()?.let { signal ->
            emergencyViewModel.activateEmergency(signal)
        } ?: run {
            Log.d("EmergencyReceiver", "The quick brown fox jumps over the lazy dawg")
        }
    }

    private fun setupEmergencyDialogComposables(view: View) {
        val emergencyHostView = view.findViewById<ViewGroup>(R.id.emergency_dialog_host)

        val composeOverlayView = ComposeView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            id = View.generateViewId()
        }
        emergencyHostView?.addView(composeOverlayView)

        composeOverlayView.setContent {
            MaterialTheme {
                val signal by emergencyViewModel.emergencySignal.collectAsState()

                signal?.let { emergencySignal ->
                    // TODO: REMOVE ME ONCE YOU ARE NO LONGER TRAUMATIZED
                    Log.d("EmergencyDialog", "Emergency signal received. Rendering dialog for ${emergencySignal.viuName}")
                    val message = "${emergencySignal.viuName} has activated an emergency signal! " +
                            "Last known location: ${emergencySignal.lastLocation}"
                    val responses = remember {
                        listOf(
                            Pair(EMERGENCY_OPTION_TURN_OFF_VCV, GreenResponse),
                            Pair(EMERGENCY_OPTION_TURN_OFF_ACV, GreenResponse),
                            Pair(EMERGENCY_OPTION_TURN_OFF, OrangeResponse),
                            Pair(EMERGENCY_OPTION_DISMISS, RedAlert)
                        )
                    }

                    VibrationHelper.emergencyVibrate(requireContext()) // Vibrate ASAP
                    EmergencyAlertDialog(
                        onDismissRequest = {
                            emergencyViewModel.clearEmergency()
                        },
                        message = message,
                        responses = responses,
                        onResponseSelected = { response ->
                            // This is where the button's functionality is added:
                            emergencyViewModel.handleEmergencyResponse(response, emergencySignal)
                        },
                        timestamp = emergencySignal.timestamp
                    )
                }
            }
        }
    }

    private fun observeEmergencySignalVisibility(view: View) {
        val emergencyHostView = view.findViewById<ViewGroup>(R.id.emergency_dialog_host)

        viewLifecycleOwner.lifecycleScope.launch {
            emergencyViewModel.emergencySignal.collect { signalState ->
                if (signalState != null) {
                    emergencyHostView?.visibility = View.VISIBLE
                } else {
                    emergencyHostView?.visibility = View.GONE
                }
            }
        }
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
        pendingDeniedCallMessage?.let { message ->
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

    fun onEmergencyVideoCallClickedFromFragment(uid: String) {
        Log.d("CallSignal", "Received Video Call request for: $uid")

        (requireActivity() as AppCompatActivity).getCameraAndMicPermission {
            mainRepository.sendEmergencyConnectionRequest(uid, true) { success ->
                if (success) {
                    // No expiration timer, do or no
                    Log.d("CallSignal", "Emergency Video call request successfully sent.")
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

    fun onEmergencyAudioCallClickedFromFragment(uid: String) {
        Log.d("CallSignal", "Received Audio Call request for: $uid")

        (requireActivity() as AppCompatActivity).getCameraAndMicPermission {
            mainRepository.sendEmergencyConnectionRequest(uid, false) { success ->
                if (success) {
                    // No expiration timer, do or no
                    Log.d("CallSignal", "Emergency  call request successfully sent.")
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
            if (incomingCallDialog?.isShowing == true) { // Check if the dialog is showing
                incomingCallDialog?.dismiss() // Dismiss the dialog
                incomingCallDialog = null
                Toast.makeText(requireContext(), "VIU aborted their call.", Toast.LENGTH_LONG).show()
                releaseMediaPlayer()
                Log.d("calldeniedmissed", "Incoming call aborted, UI dismissed.")
            }
        }
    }

    override fun onCallMissed(senderId: String) {
        activity?.runOnUiThread {
            if (incomingCallDialog?.isShowing == true) { // Check if the dialog is showing
                incomingCallDialog?.dismiss() // Dismiss the dialog
                incomingCallDialog = null
                Toast.makeText(requireContext(), "You've missed your VIU's call!", Toast.LENGTH_LONG).show()
                releaseMediaPlayer()
                Log.d("callmissed", "Incoming call missed, dialog dismissed.")
            }
        }
    }

    override fun onCallReceived(model: DataModel) {
        releaseMediaPlayer()
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
        val senderUid = model.sender

        // Define Call Actions
        val onAcceptAction: (DataModel) -> Unit = { callModel ->
            (requireActivity() as AppCompatActivity).getCameraAndMicPermission{
                releaseMediaPlayer()
                service.stopCallTimeoutTimer() // Stop timer on end/miss call timeout
                startActivity(Intent(requireContext(), CaregiverCallActivity::class.java).apply {
                    putExtra("target", callModel.sender)
                    putExtra("isVideoCall", isVideoCall)
                    putExtra("isCaller", false)
                })
            }
        }

        val onDeclineAction: (DataModel) -> Unit = {
            releaseMediaPlayer()
            MainService.getMainRepository()?.sendDeniedCall(model.sender)
            service.stopCallTimeoutTimer()
        }

        // Create and Show Dialog (Using null initially, as before)
        incomingCallDialog = IncomingCallDialog(
            context = requireContext(),
            callModel = model,
            onAccept = onAcceptAction,
            onDecline = onDeclineAction,
            viuName = null // Still start with null
        )
        // Show the dialog FIRST. This triggers the smooth slide-down animation.
        incomingCallDialog?.show()

        // Fetch VIU Name to Update Dialog
        if (!senderUid.isNullOrBlank()) {
            fragmentScope.launch(Dispatchers.IO) {
                try {
                    val viu = viuDataSource.getViuDetails(senderUid).first()
                    launch(Dispatchers.Main) {
                        val viuName = "${viu?.firstName} ${viu?.lastName}"
                        incomingCallDialog?.updateViuName(viuName)
                    }
                } catch (e: Exception) {
                Log.e("CallSignal", "Failed to fetch VIU profile: ${e.message}")
            }
            }
        }
    }

    override fun onEmergencyCallReceived(model: DataModel) {
        // WALA MANGYAYARI DAPAT DITO KASI CAREGIVER ITO >:D
    }

    private fun releaseMediaPlayer() {
        incomingMediaPlayer?.stop()
        incomingMediaPlayer?.release()
        incomingMediaPlayer = null
    }
}