package com.example.life.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.life.data.local.JournalEntry
import com.example.life.data.local.JournalRepository
import com.example.life.data.local.LifeDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JournalViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: JournalRepository

    val allEntries: StateFlow<List<JournalEntry>>

    init {
        val database = LifeDatabase.getDatabase(application)
        repository = JournalRepository(database.journalDao())
        allEntries = repository.allEntries.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun addEntry(title: String, content: String, key: String) {
        viewModelScope.launch {
            repository.insert(title, content, key)
        }
    }

    fun updateEntry(id: Long, title: String, content: String, key: String) {
        viewModelScope.launch {
            repository.update(id, title, content, key)
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }
    
    fun verifyKey(verificationString: String, key: String): Boolean {
        return repository.verifyKey(verificationString, key)
    }
    
    fun decryptContent(encryptedContent: String, key: String): String {
        return repository.decryptContent(encryptedContent, key)
    }
}
