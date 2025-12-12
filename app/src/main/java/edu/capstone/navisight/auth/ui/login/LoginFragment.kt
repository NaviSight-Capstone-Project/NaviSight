package edu.capstone.navisight.auth.ui.login

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import edu.capstone.navisight.auth.AuthActivity
import edu.capstone.navisight.common.Constants.USER_TYPE_CAREGIVER
import edu.capstone.navisight.common.Constants.USER_TYPE_VIU

class LoginFragment : Fragment() {

    private val authActivity: AuthActivity?
        get() = activity as? AuthActivity

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val userCollection by viewModel.userCollection.collectAsState()

                LaunchedEffect(userCollection) {
                    if (userCollection == USER_TYPE_VIU || userCollection == USER_TYPE_CAREGIVER) {
                        // Delegate navigation to the parent Activity
                        authActivity?.onLoginSuccess()
                    }
                }

                LoginScreen(
                    viewModel = viewModel,
                    onSignUp = {
                        authActivity?.navigateToSignUp()
                    }
                )
            }
        }
    }
}