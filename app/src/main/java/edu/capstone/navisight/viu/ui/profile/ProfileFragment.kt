package edu.capstone.navisight.viu.ui.profile

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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import edu.capstone.navisight.R
import edu.capstone.navisight.guest.GuestFragment
import edu.capstone.navisight.viu.utils.TTSHelper
import edu.capstone.navisight.viu.data.remote.ViuDataSource
import edu.capstone.navisight.viu.domain.usecase.GenerateOrFetchQrUseCase
import edu.capstone.navisight.viu.domain.usecase.GetViuProfileUseCase
import edu.capstone.navisight.webrtc.repository.MainRepository
import edu.capstone.navisight.viu.ui.call.CallActivity
import edu.capstone.navisight.webrtc.model.DataModel
import edu.capstone.navisight.webrtc.model.DataModelType
import edu.capstone.navisight.webrtc.service.MainService
import edu.capstone.navisight.webrtc.utils.getCameraAndMicPermission
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(), MainService.Listener {

    private lateinit var mainRepository : MainRepository
    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(ViuDataSource())
    }

    // Init. properties for listening for event call
    private var callRequestDialog: AlertDialog? = null
    private var incomingMediaPlayer: MediaPlayer? = null
    private var isVideoCall: Boolean = true
    private val TAG = "CallEvent"

    // Init. companion object for denied call message
    companion object {
        var firstTimeLaunched: Boolean = true
        var pendingDeniedCallMessage: String? = null
    }


    override fun onResume() {
        super.onResume()
        Log.d("deniedcall", "this is working")
        pendingDeniedCallMessage?.let { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            pendingDeniedCallMessage = null
        }
        // Register for listening to event calls.
        MainService.listener = this
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        val composeView = view.findViewById<ComposeView>(R.id.profile_compose_view)
        mainRepository = MainRepository.getInstance(requireContext())
        viewModel.loadProfile()

        // Handle logout
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.logoutComplete.collectLatest { isComplete ->
                if (isComplete) {
                    parentFragmentManager.commit {
                        replace(R.id.fragment_container, GuestFragment())
                        setReorderingAllowed(true)
                    }
                    viewModel.onLogoutNavigated()
                }
            }
        }

        // Handle audio calls and video calls
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiEvent.collectLatest { event ->
                when (event) {
                    is ProfileUiEvent.StartCall -> {
                        handleStartCall(event.target, event.isVideoCall)
                    }
                }
            }
        }


        composeView.setContent {
            val uiState by viewModel.uiState.collectAsState()
            val targetCaregiverUid by viewModel.caregiverUid.collectAsState()

            ProfileScreen(
                uiState = uiState,
                onLogout = { viewModel.logout() },
                onVideoCall = {
                    // Use the fetched caregiver UID from the ViewModel
                    targetCaregiverUid?.let { uid ->
                        viewModel.videoCall(uid)
                    } ?: run {
                        Toast.makeText(context, "No caregiver linked.", Toast.LENGTH_SHORT).show()
                    }
                },
                onAudioCall = {
                    // Use the fetched caregiver UID from the ViewModel
                    targetCaregiverUid?.let { uid ->
                        viewModel.audioCall(uid)
                    } ?: run {
                        Toast.makeText(context, "No caregiver linked.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // Add delay to make sure everything is initialized first.
        // For Firebase Authorization sync if last event is "denied call"
        if (firstTimeLaunched) {
            viewLifecycleOwner.lifecycleScope.launch {
                delay(5500)
                firstTimeLaunched = false
            }
        }
        return view
    }


    // Listen for WebRTC call events.
    override fun onPause() {
        super.onPause()
        // Unregister to prevent calls from showing up when the fragment is not visible
        if (MainService.listener == this) {
            MainService.listener = null
        }
        // Ensure ringtone is stopped if the user navigates away before answering
        releaseMediaPlayer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Ensure dialog is dismissed if the view is destroyed
        callRequestDialog?.dismiss()
        callRequestDialog = null
    }

    override fun onCallReceived(model: DataModel) {
        showIncomingCallDialog(model, model.type == DataModelType.StartVideoCall)
    }

    override fun onCallAborted() {
        activity?.runOnUiThread {
            // Check if the dialog is currently showing and dismiss it
            if (callRequestDialog?.isShowing == true) {
                callRequestDialog?.dismiss()

                // Show a toast message to the user
                showToastMessageThenTTS("Caregiver aborted their call.")

                // Stop the ringing
                releaseMediaPlayer()
                Log.d(TAG, "Incoming call aborted, dialog dismissed and ringing stopped.")
            } else {
                Log.d(TAG, "Call aborted, but no dialog was active.")
            }
        }
    }

    fun showToastMessageThenTTS(message: String){
        Toast.makeText(
            context,
            message,
            Toast.LENGTH_LONG
        ).show()
        if (context != null) {
            TTSHelper.queueSpeak(context, message)
        }
    }

    override fun onCallMissed(senderId: String) {
        activity?.runOnUiThread {
            // Check if the dialog is currently showing and dismiss it
            if (callRequestDialog?.isShowing == true) {
                callRequestDialog?.dismiss()

                // Show a toast message to the user
                Toast.makeText(
                    context,
                    "You've missed your caregiver's call!",
                    Toast.LENGTH_LONG
                ).show()

                // Stop the ringing
                releaseMediaPlayer()
                Log.d(TAG, "Incoming call missed, dialog dismissed and ringing stopped.")
            } else {
                Log.d(TAG, "Call missed, but no dialog was active.")
            }
        }
    }

    private fun showIncomingCallDialog(model: DataModel, isVideoCall: Boolean) {
        try {
            this.isVideoCall = isVideoCall

            // Non-repeating
            if (callRequestDialog?.isShowing == true) {
                Log.w(TAG, "Call received but dialog is already visible. Ignoring.")
                return
            }

            // Dismiss any existing dialog
            callRequestDialog?.dismiss()

            val inflater = requireActivity().layoutInflater
            val customLayout = inflater.inflate(R.layout.popup_call_request, null)

            val messageTitle = customLayout.findViewById<TextView>(R.id.messageTitle)
            val caregiverTitle = customLayout.findViewById<TextView>(R.id.caregiverTitle)
            val acceptButton = customLayout.findViewById<AppCompatImageButton>(R.id.btnAccept)
            val declineButton = customLayout.findViewById<AppCompatImageButton>(R.id.btnDecline)
            messageTitle.text = if (isVideoCall) "Incoming Video Call" else "Incoming Audio Call"

            caregiverTitle.text = "Your caregiver is calling you!"

            callRequestDialog = AlertDialog.Builder(requireActivity())
                .setView(customLayout)
                .setCancelable(false) // Force user to choose
                .create()

            callRequestDialog?.window?.setBackgroundDrawableResource(R.drawable.bg_popup_rounded)

            // Play the incoming call ringtone
            playRingtone()

            // CLEANUP: Stop and release when the dialog is dismissed for any reason
            callRequestDialog?.setOnDismissListener { // Use safe call
                releaseMediaPlayer()
                callRequestDialog = null // Clear the reference when dismissed
            }

            // Set up click listeners
            acceptButton.setOnClickListener {
                releaseMediaPlayer() // Stop ringtone upon user action

                Log.w(TAG, "Getting reference for CallActivity...")
                val activity = requireActivity() as? AppCompatActivity
                Log.w(TAG, "Reference for CallActivity retrieved.")

                Log.w(TAG, "Requesting permissions for camera and mic...")
                activity?.getCameraAndMicPermission {
                    // This code runs AFTER permissions are granted.
                    Log.w(TAG, "Mic and Camera permissions retrieved. Proceeding to starting activity.")
                    Log.w(TAG, "Mic and Camera permissions retrieved. Releasing camera...")
                        // Launch CallActivity using the launcher instead of startActivity
                    val intent = Intent(requireActivity(), CallActivity::class.java).apply {
                        putExtra("target", model.sender)
                        putExtra("isVideoCall", isVideoCall)
                        putExtra("isCaller", false)
                        putExtra("sender_id", model.sender)
                    }
                    startActivity(intent) // <--- IMPORTANT CHANGE HERE
                    Log.w(TAG, "Call activity started via launcher.")

                } ?: run {
                    // Handle the case where the Activity is not an AppCompatActivity (shouldn't happen)
                    Log.e(TAG, "Failed requesting camera and mic permissions for CallActivity.")
                    Toast.makeText(context, "Cannot request permissions. Activity is missing.", Toast.LENGTH_LONG).show()
                }

                callRequestDialog?.dismiss()
            }

            // Set DECLINE button click listener
            declineButton.setOnClickListener {
                releaseMediaPlayer() // Stop ringtone upon user action
                callRequestDialog?.dismiss()
                Log.d(TAG, "Call declined by user.")
                MainService.getMainRepository()?.sendDeniedCall(model.sender)
            }

            // Show the custom dialog
            callRequestDialog?.show()

        } catch (e: Exception) {
            Log.e("callevent", "Failed to show custom AlertDialog or play ringtone. Error: ${e.message}")
            Toast.makeText(context, "Error showing call prompt.", Toast.LENGTH_LONG).show()
            releaseMediaPlayer()
        }
    }

    // 5. Implement ringtone utility functions
    private fun playRingtone() {
        try {
            // Use the default notification ringtone
            val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            incomingMediaPlayer = MediaPlayer().apply {
                setDataSource(requireContext(), notification)
                setLooping(true)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("ring", "Failed to play ringtone: ${e.message}")
        }
    }

    private fun releaseMediaPlayer() {
        incomingMediaPlayer?.stop()
        incomingMediaPlayer?.release()
        incomingMediaPlayer = null
    }

    private fun handleStartCall(targetUid: String, isVideoCall: Boolean) {
        (requireActivity() as AppCompatActivity).getCameraAndMicPermission {
            mainRepository.sendConnectionRequest(targetUid, isVideoCall) { success ->
                if (success) {
                    Log.d("CallSignal", "Call request successfully sent.")

                    val intent = Intent(requireActivity(), CallActivity::class.java).apply {
                        putExtra("target", targetUid)
                        putExtra("isVideoCall", isVideoCall)
                        putExtra("isCaller", true)
                    }
                    startActivity(intent)
                } else {
                    Log.e("CallSignal", "Failed to send call request.")
                }
            }
        }
    }
}

class ProfileViewModelFactory(
    private val remoteDataSource: ViuDataSource
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(
                remoteDataSource = remoteDataSource,
                getViuProfileUseCase = GetViuProfileUseCase(),
                generateQrUseCase = GenerateOrFetchQrUseCase()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}