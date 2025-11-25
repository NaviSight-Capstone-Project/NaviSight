package edu.capstone.navisight.caregiver.ui.feature_travel_log

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import edu.capstone.navisight.R

class TravelLogFragment : Fragment() {

    // Initialize the ViewModel
    private val viewModel: TravelLogViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Retrieve arguments passed from the previous screen
        val viuUid = arguments?.getString("viuUid") ?: ""
        val viuName = arguments?.getString("viuName") ?: "Viu"

        return ComposeView(requireContext()).apply {
            // Dispose the Composition when the View's Lifecycle is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                TravelLogScreen(
                    viuUid = viuUid,
                    viuName = viuName,
                    viewModel = viewModel,
                    onBack = {
                        parentFragmentManager.popBackStack()
                    }
                )
            }
        }
    }

    // --- HIDE BOTTOM NAV LOGIC ---
    override fun onResume() {
        super.onResume()
        // Hide Bottom Nav when this screen opens
        requireActivity().findViewById<View>(R.id.bottom_nav_compose_view)?.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Show Bottom Nav again when leaving this screen
        requireActivity().findViewById<View>(R.id.bottom_nav_compose_view)?.visibility = View.VISIBLE
    }

    companion object {
        fun newInstance(viuUid: String, viuName: String): TravelLogFragment {
            val fragment = TravelLogFragment()
            val args = Bundle()
            args.putString("viuUid", viuUid)
            args.putString("viuName", viuName)
            fragment.arguments = args
            return fragment
        }
    }
}