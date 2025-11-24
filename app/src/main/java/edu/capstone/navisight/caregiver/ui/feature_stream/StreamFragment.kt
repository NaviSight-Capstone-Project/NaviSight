package edu.capstone.navisight.caregiver.ui.feature_stream

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels

class StreamFragment : Fragment() {

    interface StreamFragmentListener {
        fun onVideoCallClickedFromFragment(uid: String)
        fun onAudioCallClickedFromFragment(uid: String)
    }

    private var listener: StreamFragmentListener? = null
    private val viewModel: StreamViewModel by viewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (parentFragment is StreamFragmentListener) {
            listener = parentFragment as StreamFragmentListener
        }
        else if (context is StreamFragmentListener) {
            listener = context
        }
        else {
            throw RuntimeException("$context (or parent fragment) must implement StreamFragmentListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                StreamScreen(
                    viewModel = viewModel,
                    onVideoCall = { uid -> listener?.onVideoCallClickedFromFragment(uid) },
                    onAudioCall = { uid -> listener?.onAudioCallClickedFromFragment(uid) }
                )
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}