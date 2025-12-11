package edu.capstone.navisight.viu.domain.braileUseCase

import edu.capstone.navisight.viu.data.repository.NoteRepository
import edu.capstone.navisight.viu.model.Note
import kotlinx.coroutines.flow.Flow

class GetNotesUseCase(private val repository: NoteRepository) {
    operator fun invoke(): Flow<List<Note>> {
        return repository.getAllNotes()
    }
}