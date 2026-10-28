package com.example.life.ui.checkin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.life.data.local.CheckInRepository
import com.example.life.data.local.LifeDatabase
import com.example.life.data.model.CheckIn
import com.example.life.data.model.Mood
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CheckInViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CheckInRepository

    val checkIns: StateFlow<List<CheckIn>>

    init {
        val checkInDao = LifeDatabase.getDatabase(application).checkInDao()
        repository = CheckInRepository(checkInDao)
        checkIns = repository.allCheckIns.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun addCheckIn(situation: String, mood: Mood, reason: String?, nextAction: String) {
        viewModelScope.launch {
            repository.insert(situation, mood, reason, nextAction)
        }
    }
}
