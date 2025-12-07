package edu.capstone.navisight.caregiver.ui.feature_notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import edu.capstone.navisight.R
import edu.capstone.navisight.caregiver.model.Viu

class NotificationFragment : Fragment() {

    private val viewModel: NotificationViewModel by viewModels()

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
