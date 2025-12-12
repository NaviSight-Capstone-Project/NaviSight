package edu.capstone.navisight.caregiver.ui.feature_settings

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.firebase.auth.FirebaseAuth
import edu.capstone.navisight.R
import edu.capstone.navisight.auth.AuthActivity
import edu.capstone.navisight.caregiver.ui.feature_editProfile.AccountInfoFragment
import edu.capstone.navisight.common.domain.usecase.GetCurrentUserUidUseCase
import edu.capstone.navisight.common.webrtc.repository.MainRepository

class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModels()
    private val getCurrentUserUidUseCase = GetCurrentUserUidUseCase()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val currentUid = getCurrentUserUidUseCase.invoke()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                var showLogoutDialog by remember { mutableStateOf(false) }
                val context = requireContext()
                var appNotificationEnabled by remember {
                    mutableStateOf(CaregiverSettingsManager.getBoolean(context,
                        CaregiverSettingsManager.KEY_APP_NOTIFICATION))
                }
                var soundAlertEnabled by remember {
                    mutableStateOf(CaregiverSettingsManager.getBoolean(context,
                        CaregiverSettingsManager.KEY_SOUND_ALERT))
                }
                var vibrationEnabled by remember {
                    mutableStateOf(CaregiverSettingsManager.getBoolean(context,
                        CaregiverSettingsManager.KEY_VIBRATION))
                }

                currentUid?.let { uid ->
                    SettingsScreen(
                        viewModel = viewModel,
                        uid = uid,
                        onLogout = {
                            // Show a logout dialog BEFORE logging out
                            showLogoutDialog = true
                        },
                        onEditAccount = {
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, AccountInfoFragment())
                                .addToBackStack(null)
                                .commit()
                        },
                        // --- START Pass Settings State and Handlers ---
                        appNotificationEnabled = appNotificationEnabled,
                        onAppNotificationChange = { newValue: Boolean ->
                            appNotificationEnabled = newValue
                            CaregiverSettingsManager.setBoolean(context,
                                CaregiverSettingsManager.KEY_APP_NOTIFICATION, newValue)
                        },
                        soundAlertEnabled = soundAlertEnabled,
                        onSoundAlertChange = { newValue: Boolean ->
                            soundAlertEnabled = newValue
                            CaregiverSettingsManager.setBoolean(context,
                                CaregiverSettingsManager.KEY_SOUND_ALERT, newValue)
                        },
                        vibrationEnabled = vibrationEnabled,
                        onVibrationChange = { newValue: Boolean ->
                            vibrationEnabled = newValue
                            CaregiverSettingsManager.setBoolean(context,
                                CaregiverSettingsManager.KEY_VIBRATION, newValue)
                        }
                    )
                }

                if (showLogoutDialog) {
                    LogoutDialogScreen (
                        onDismissRequest = {
                            // User tapped outside or pressed back, dismiss the dialog
                            showLogoutDialog = false
                        },
                        onConfirm = {
                            // User clicked "Yes, Logout"
                            showLogoutDialog = false
                            try {
                                val repo = MainRepository.getInstance(requireContext())
                                repo.setOffline()
                            } catch (e: Exception) {
                                Log.e("SettingsFragment", "Error setting offline: ${e.message}")
                            }
                            FirebaseAuth.getInstance().signOut()
                            val intent = Intent(requireContext(), AuthActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                        },
                        onCancel = {
                            // User clicked "No, Stay Logged In" or something something
                            showLogoutDialog = false
                        }
                    )
                }
            }
        }
    }
}