package edu.capstone.navisight.viu.ui.braillenote

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import edu.capstone.navisight.viu.data.local.AppDatabase
import edu.capstone.navisight.viu.data.repository.NoteRepository
import edu.capstone.navisight.viu.domain.braileUseCase.GetNotesUseCase
import edu.capstone.navisight.viu.domain.braileUseCase.SaveNoteUseCase

class BrailleNoteFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val database = AppDatabase.getDatabase(requireContext())
        val repository = NoteRepository(database.noteDao())
        val getNotesUseCase = GetNotesUseCase(repository)
        val saveNoteUseCase = SaveNoteUseCase(repository)

        val factory = BrailleViewModelFactory(getNotesUseCase, saveNoteUseCase)
        val viewModel = ViewModelProvider(this, factory)[BrailleViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    Surface {
                        var isReadingMode by remember { mutableStateOf(false) }

                        if (isReadingMode) {
                            BackHandler {
                                isReadingMode = false
                            }
                            SavedNotesScreen(
                                viewModel = viewModel,
                                onBack = { isReadingMode = false }
                            )
                        } else {
                            BrailleNoteScreen(
                                viewModel = viewModel,
                                onNavigateToSaved = { isReadingMode = true },
                                onNavigateBack = {
                                    parentFragmentManager.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}