package edu.capstone.navisight.viu.ui.profile

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import edu.capstone.navisight.viu.data.remote.ViuDataSource
import edu.capstone.navisight.viu.model.QR
import edu.capstone.navisight.viu.model.Viu
import edu.capstone.navisight.viu.domain.usecase.GenerateOrFetchQrUseCase
import edu.capstone.navisight.viu.domain.usecase.GetViuProfileUseCase
import edu.capstone.navisight.common.webrtc.FirebaseClient
import edu.capstone.navisight.common.webrtc.utils.UserStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: Viu? = null,
    val qr: QR? = null,
    val qrBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)


sealed class ProfileUiEvent {
    data class StartCall(val target: String, val isVideoCall: Boolean) : ProfileUiEvent()
    // You could also add a LoggedOut event here if desired
}

class ProfileViewModel(
    private val getViuProfileUseCase: GetViuProfileUseCase = GetViuProfileUseCase(),
    private val generateQrUseCase: GenerateOrFetchQrUseCase = GenerateOrFetchQrUseCase(),
    private val remoteDataSource: ViuDataSource

) : ViewModel() {

    private val firebaseClient = FirebaseClient.getInstance()
    private val _caregiverUid = MutableStateFlow<String?>(null)
    val caregiverUid: StateFlow<String?> = _caregiverUid.asStateFlow()

    init {
        // Start fetching the Caregiver UID as soon as the ViewModel is created
        fetchCaregiverUid()
    }

    private val _uiEvent = Channel<ProfileUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow() // Use this in your Fragment/Activity

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val _logoutComplete = MutableStateFlow(false)
    val logoutComplete: StateFlow<Boolean> = _logoutComplete.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)


                val viuUser = getViuProfileUseCase()


                val (qrModel, qrBitmap) = generateQrUseCase(viuUser.uid, viuUser.firstName)


                _uiState.value = ProfileUiState(
                    user = viuUser,
                    qr = qrModel,
                    qrBitmap = qrBitmap,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = ProfileUiState(
                    error = e.localizedMessage ?: "Failed to load profile",
                    isLoading = false
                )
            }
        }
    }

    fun fetchCaregiverUid() {
        // Must be called inside a coroutine scope (viewModelScope)
        viewModelScope.launch {
            try {
                val caregiver = remoteDataSource.getRegisteredCaregiver()

                // Update the state with the fetched UID
                _caregiverUid.value = caregiver.uid

            } catch (e: Exception) {
                // Handle exceptions (e.g., "VIU has no relationship with any Caregiver")
                Log.e("CaregiverFetch", "Failed to fetch caregiver UID: ${e.message}")
                _caregiverUid.value = null
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            // Logout visually then update all firebase instances:
            //      Firestore DB
            //      Realtime DB
            val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
            firebaseAuth.signOut()
            firebaseClient.changeMyStatus(UserStatus.OFFLINE)

            // Trigger navigation in the Fragment
            _logoutComplete.value = true
        }
    }

    // Update your videoCall/audioCall functions to use the UID directly
    fun videoCall(targetUid: String) {
        viewModelScope.launch {
            _uiEvent.send(ProfileUiEvent.StartCall(target = targetUid, isVideoCall = true))
        }
    }

    fun audioCall(targetUid: String) {
        viewModelScope.launch {
            _uiEvent.send(ProfileUiEvent.StartCall(target = targetUid, isVideoCall = false))
        }
    }

    fun onLogoutNavigated() {
        _logoutComplete.value = false
    }
}
