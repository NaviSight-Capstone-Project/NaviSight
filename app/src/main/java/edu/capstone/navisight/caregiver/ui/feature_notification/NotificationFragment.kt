package edu.capstone.navisight.caregiver.ui.feature_notification

import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import edu.capstone.navisight.R
import edu.capstone.navisight.caregiver.domain.connectionUseCase.SecondaryConnectionUseCase
import edu.capstone.navisight.caregiver.domain.connectionUseCase.TransferPrimaryUseCase
import edu.capstone.navisight.caregiver.domain.notificationUseCase.DismissActivityUseCase
import edu.capstone.navisight.caregiver.domain.notificationUseCase.GetActivityFeedUseCase
import edu.capstone.navisight.common.domain.usecase.GetCurrentUserUidUseCase

class NotificationFragment : Fragment() {

    private val factory by lazy { NotificationViewModelFactory(requireActivity().application) }
    private val viewModel: NotificationViewModel by activityViewModels { factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_notification, container, false)

        val composeView = root.findViewById<ComposeView>(R.id.notificationComposeView)
        composeView.setContent {
            NotificationScreen(viewModel = viewModel)
        }
        return root
    }
}


class NotificationViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
            return NotificationViewModel(
                secondaryConnectionUseCase = SecondaryConnectionUseCase(),
                transferPrimaryUseCase = TransferPrimaryUseCase(),
                getCurrentUidUseCase = GetCurrentUserUidUseCase(),
                getActivityFeedUseCase = GetActivityFeedUseCase(),
                dismissActivityUseCase = DismissActivityUseCase(),
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}