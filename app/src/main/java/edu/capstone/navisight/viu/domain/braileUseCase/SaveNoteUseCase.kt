package edu.capstone.navisight.viu.domain.braileUseCase

import edu.capstone.navisight.viu.data.repository.NoteRepository
import edu.capstone.navisight.viu.model.Note

class SaveNoteUseCase(private val repository: NoteRepository) {
    suspend operator fun invoke(note: Note) {
        repository.saveNote(note)
    }
}