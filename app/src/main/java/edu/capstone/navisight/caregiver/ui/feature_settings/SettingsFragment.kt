package edu.capstone.navisight.caregiver.ui.feature_settings

import edu.capstone.navisight.R
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import edu.capstone.navisight.common.domain.usecase.GetCurrentUserUidUseCase
import edu.capstone.navisight.auth.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import edu.capstone.navisight.caregiver.ui.feature_editProfile.AccountInfoFragment

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
            setContent {
                currentUid?.let { uid ->
                    SettingsScreen(
                        viewModel = viewModel,
                        uid = uid,
                        onLogout = {
                            FirebaseAuth.getInstance().signOut()

                            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                        },
                        onEditAccount = {
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, AccountInfoFragment())
                                .addToBackStack(null)
                                .commit()
                        }
                    )
                }
            }
        }
    }
}
