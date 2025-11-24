package edu.capstone.navisight.auth.ui.signup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import edu.capstone.navisight.auth.AuthActivity

class RoleSelectionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                RoleSelectionScreen(
                    onCaregiverClicked = {
                        (requireActivity() as AuthActivity).navigateToCaregiverSignup()
                    },
                    onViuClicked = {
                        (requireActivity() as AuthActivity).navigateToViuSignup()
                    },
                    onBackToLogin = {
                        parentFragmentManager.popBackStack()
                    }
                )
            }
        }
    }
}