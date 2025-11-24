package edu.capstone.navisight.caregiver.ui.feature_records

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
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
        if (parentFragment is OnViuClickedListener) {
            navigationListener = parentFragment as OnViuClickedListener
        }
        else if (context is OnViuClickedListener) {
            navigationListener = context
        }
        else {
            throw RuntimeException("$context (or parent fragment) must implement OnViuClickedListener")
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
        composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                RecordsScreen(
                    viewModel = viewModel,
                    onViuClicked = { viuUid ->
                        navigationListener?.onViuClicked(viuUid)
                    },
                    onTransferViuClicked = {
                        val fragment = edu.capstone.navisight.caregiver.ui.feature_scanqr.ScanQrFragment()

                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit()
                    }
                )
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        navigationListener = null
    }
}