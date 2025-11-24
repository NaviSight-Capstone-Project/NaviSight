package edu.capstone.navisight.caregiver.ui.feature_settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.firebase.auth.FirebaseAuth
import edu.capstone.navisight.R
import edu.capstone.navisight.auth.AuthActivity
import edu.capstone.navisight.caregiver.ui.feature_editProfile.AccountInfoFragment
import edu.capstone.navisight.common.domain.usecase.GetCurrentUserUidUseCase

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
                currentUid?.let { uid ->
                    SettingsScreen(
                        viewModel = viewModel,
                        uid = uid,
                        onLogout = {
                            FirebaseAuth.getInstance().signOut()

                            val intent = Intent(requireContext(), AuthActivity::class.java).apply {
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