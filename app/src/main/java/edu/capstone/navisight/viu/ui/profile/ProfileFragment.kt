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
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation.findNavController
import edu.capstone.navisight.R
import edu.capstone.navisight.guest.GuestFragment
import edu.capstone.navisight.viu.data.remote.ViuDataSource
import edu.capstone.navisight.viu.domain.usecase.GenerateOrFetchQrUseCase
import edu.capstone.navisight.viu.domain.usecase.GetViuProfileUseCase
import edu.capstone.navisight.viu.ui.call.ViuCallActivity
import edu.capstone.navisight.common.TTSHelper
import edu.capstone.navisight.common.webrtc.repository.MainRepository
import edu.capstone.navisight.common.webrtc.service.MainService
import edu.capstone.navisight.common.webrtc.model.DataModel
import edu.capstone.navisight.common.webrtc.model.DataModelType
import edu.capstone.navisight.common.webrtc.utils.getCameraAndMicPermission
import edu.capstone.navisight.viu.ui.camera.CameraFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(), MainService.Listener {
    private lateinit var mainRepository : MainRepository
    private lateinit var service: MainService
    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(ViuDataSource())
    }

    // Init. properties for listening for event call
    private var callRequestDialog: AlertDialog? = null
    private var incomingMediaPlayer: MediaPlayer? = null
    private var isVideoCall: Boolean = true
    private val TAG = "CallEvent"

    companion object {
        var firstTimeLaunched: Boolean = true
        var pendingDeniedCallMessage: String? = null
    }

    override fun onResume() {
        super.onResume()
        pendingDeniedCallMessage?.let { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            pendingDeniedCallMessage = null
        }
        MainService.listener = this
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        service = MainService.getInstance()
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        val composeView = view.findViewById<ComposeView>(R.id.profile_compose_view)
        mainRepository = MainRepository.getInstance(requireContext())

        composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val uiState by viewModel.uiState.collectAsState()
                val targetCaregiverUid by viewModel.caregiverUid.collectAsState()

                ProfileScreen(
                    uiState = uiState,
                    onLogout = { viewModel.logout() },
                    onVideoCall = {
                        targetCaregiverUid?.let { uid -> viewModel.videoCall(uid) }
                            ?: Toast.makeText(context, "No caregiver linked.", Toast.LENGTH_SHORT).show()
                    },
                    onAudioCall = {
                        targetCaregiverUid?.let { uid -> viewModel.audioCall(uid) }
                            ?: Toast.makeText(context, "No caregiver linked.", Toast.LENGTH_SHORT).show()
                    },
                    onBackClick = {
                        requireActivity().supportFragmentManager.commit {
                            replace(R.id.fragment_container, CameraFragment()) // Replace with your actual CameraFragment class
                            setReorderingAllowed(true)
                        }
                    }
                )
            }
        }

        viewModel.loadProfile()
        setupObservers()

        if (firstTimeLaunched) {
            viewLifecycleOwner.lifecycleScope.launch {
                delay(5500)
                firstTimeLaunched = false
            }
        }
        return view
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.logoutComplete.collectLatest { isComplete ->
                if (isComplete) {
                    requireActivity().supportFragmentManager.commit {
                        replace(R.id.fragment_container, GuestFragment())
                        setReorderingAllowed(true)
                    }
                    viewModel.onLogoutNavigated()
                }
            }
        }

        // Handle Audio/Video Calls
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiEvent.collectLatest { event ->
                when (event) {
                    is ProfileUiEvent.StartCall -> {
                        handleStartCall(event.target, event.isVideoCall)
                    }
                }
            }
        }
    }


    override fun onPause() {
        super.onPause()
        if (MainService.listener == this) {
            MainService.listener = null
        }
        releaseMediaPlayer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        callRequestDialog?.dismiss()
        callRequestDialog = null
    }

    override fun onCallReceived(model: DataModel) {
        showIncomingCallDialog(model, model.type == DataModelType.StartVideoCall)
    }

    override fun onCallAborted() {
        activity?.runOnUiThread {
            if (callRequestDialog?.isShowing == true) {
                callRequestDialog?.dismiss()
                showToastMessageThenTTS("Caregiver aborted their call.")
                releaseMediaPlayer()
            }
        }
    }

    private fun showToastMessageThenTTS(message: String){
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        context?.let { TTSHelper.queueSpeak(it, message) }
    }

    override fun onCallMissed(senderId: String) {
        activity?.runOnUiThread {
            if (callRequestDialog?.isShowing == true) {
                callRequestDialog?.dismiss()
                Toast.makeText(context, "You've missed your caregiver's call!", Toast.LENGTH_LONG).show()
                releaseMediaPlayer()
            }
        }
    }

    private fun showIncomingCallDialog(model: DataModel, isVideoCall: Boolean) {
        try {
            this.isVideoCall = isVideoCall
            if (callRequestDialog?.isShowing == true) return

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
                .setCancelable(false)
                .create()

            callRequestDialog?.window?.setBackgroundDrawableResource(R.drawable.bg_popup_rounded)
            playRingtone()

            callRequestDialog?.setOnDismissListener {
                releaseMediaPlayer()
                callRequestDialog = null
            }

            acceptButton.setOnClickListener {
                releaseMediaPlayer()

                // Stop timer on end/miss call timeout
                service.stopCallTimeoutTimer()

                val activity = requireActivity() as? AppCompatActivity

                activity?.getCameraAndMicPermission {
                    val intent = Intent(requireActivity(), ViuCallActivity::class.java).apply {
                        putExtra("target", model.sender)
                        putExtra("isVideoCall", isVideoCall)
                        putExtra("isCaller", false)
                        putExtra("sender_id", model.sender)
                    }
                    startActivity(intent)
                } ?: run {
                    Toast.makeText(context, "Cannot request permissions. Activity is missing.", Toast.LENGTH_LONG).show()
                }
                callRequestDialog?.dismiss()
            }

            declineButton.setOnClickListener {
                releaseMediaPlayer()
                callRequestDialog?.dismiss()
                MainService.getMainRepository()?.sendDeniedCall(model.sender)
            }
            callRequestDialog?.show()

        } catch (e: Exception) {
            Log.e("callevent", "Failed to show call dialog: ${e.message}")
            releaseMediaPlayer()
        }
    }

    private fun playRingtone() {
        try {
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
                    service.startCallTimeoutTimer() // Begin expiration
                    val intent = Intent(requireActivity(), ViuCallActivity::class.java).apply {
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