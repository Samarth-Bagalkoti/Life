package com.example.life.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.life.data.local.LifeDatabase
import com.example.life.data.local.NoteEntity
import com.example.life.data.local.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository

    val allNotes: StateFlow<List<NoteEntity>>

    init {
        val noteDao = LifeDatabase.getDatabase(application).noteDao()
        repository = NoteRepository(noteDao)
        allNotes = repository.allNotes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun addNote(title: String, content: String, key: String) {
        viewModelScope.launch {
            repository.insert(title, content, key)
        }
    }

    fun updateNote(id: Long, title: String, content: String, key: String) {
        viewModelScope.launch {
            repository.updateWithNewKey(id, title, content, key)
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.delete(note)
        }
    }

    fun verifyKey(verificationString: String, key: String): Boolean {
        return repository.verifyKey(verificationString, key)
    }

    fun decryptContent(encryptedContent: String, key: String): String {
        return repository.decryptContent(encryptedContent, key)
    }
}
