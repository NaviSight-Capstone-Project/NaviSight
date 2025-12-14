package edu.capstone.navisight.viu.domain.braileUseCase

import edu.capstone.navisight.viu.data.repository.NoteRepository
import edu.capstone.navisight.viu.model.Note

class DeleteNoteUseCase(private val repository: NoteRepository) {
    suspend operator fun invoke(note: Note) {
        repository.deleteNote(note)
    }
}