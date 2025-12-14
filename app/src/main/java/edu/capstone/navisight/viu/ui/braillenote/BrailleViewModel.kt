package edu.capstone.navisight.viu.ui.braillenote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import edu.capstone.navisight.viu.domain.braileUseCase.DeleteNoteUseCase
import edu.capstone.navisight.viu.domain.braileUseCase.GetNotesUseCase
import edu.capstone.navisight.viu.domain.braileUseCase.SaveNoteUseCase
import edu.capstone.navisight.viu.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BrailleViewModel(
    private val getNotesUseCase: GetNotesUseCase,
    private val saveNoteUseCase: SaveNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase
) : ViewModel() {

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes

    init {
        loadNotes()
    }

    private fun loadNotes() {
        viewModelScope.launch {
            getNotesUseCase().collect { noteList ->
                _notes.value = noteList
            }
        }
    }

    fun saveNote(content: String) {
        if (content.isBlank()) return

        viewModelScope.launch {
            val newNote = Note(
                content = content,
                title = "Note",
                timestamp = System.currentTimeMillis()
            )
            saveNoteUseCase(newNote)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            deleteNoteUseCase(note)
        }
    }
}

class BrailleViewModelFactory(
    private val getNotesUseCase: GetNotesUseCase,
    private val saveNoteUseCase: SaveNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BrailleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BrailleViewModel(getNotesUseCase, saveNoteUseCase, deleteNoteUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}