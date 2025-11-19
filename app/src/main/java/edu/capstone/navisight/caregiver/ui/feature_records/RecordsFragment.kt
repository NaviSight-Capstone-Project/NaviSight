package edu.capstone.navisight.caregiver.ui.feature_records

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import edu.capstone.navisight.R

class RecordsFragment : Fragment() {

    private val viewModel: RecordsViewModel by viewModels()

    private var navigationListener: OnViuClickedListener? = null

    interface OnViuClickedListener {
        fun onViuClicked(viuUid: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnViuClickedListener) {
            navigationListener = context
        } else {
            throw RuntimeException("$context must implement OnViuClickedListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_records, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val composeView = view.findViewById<ComposeView>(R.id.composeRecords)
        composeView.setContent {
            RecordsScreen(
                viewModel = viewModel,
                onViuClicked = { viuUid ->
                    navigationListener?.onViuClicked(viuUid)
                }
            )
        }


        val overlayView = view.findViewById<ComposeView>(R.id.records_compose_overlay)
        overlayView.setContent {
            RecordsOverlayButtons(viewModel)
        }
    }
    override fun onDetach() {
        super.onDetach()
        navigationListener = null
    }
}
