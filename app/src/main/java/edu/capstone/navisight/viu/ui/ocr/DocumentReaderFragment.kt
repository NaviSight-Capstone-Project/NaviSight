package edu.capstone.navisight.viu.ui.ocr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import edu.capstone.navisight.common.TextToSpeechHelper

class DocumentReaderFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                DocumentReaderScreen(
                    onNavigateBack = {
                        TextToSpeechHelper.speak(context, "Returning back to camera. Please wait.")
                        parentFragmentManager.popBackStack()
                    }
                )
            }
        }
    }
}