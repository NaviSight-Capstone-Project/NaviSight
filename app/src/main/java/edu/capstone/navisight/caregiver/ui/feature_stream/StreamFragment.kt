package edu.capstone.navisight.caregiver.ui.feature_stream

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import kotlin.getValue

class StreamFragment : Fragment() {
    // Define the interface for the callback
    interface StreamFragmentListener {
        fun onVideoCallClickedFromFragment(uid: String)
        fun onAudioCallClickedFromFragment(uid: String)
    }

    private var listener: StreamFragmentListener? = null
    private val viewModel: StreamViewModel by viewModels()

    // Attach the listener when the fragment is attached to the Activity
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is StreamFragmentListener) {
            listener = context
        } else {
            // Throw an exception if the hosting activity doesn't implement the listener
            throw RuntimeException("$context must implement StreamFragmentListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                StreamScreen(
                        viewModel = viewModel,
                        onVideoCall = { uid -> listener?.onVideoCallClickedFromFragment(uid) },
                        onAudioCall = { uid -> listener?.onAudioCallClickedFromFragment(uid) }
                    )
                }
            }
        }

    // Clean up the listener when the fragment is detached
    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}


