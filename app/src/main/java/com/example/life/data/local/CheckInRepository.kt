package com.example.life.data.local

import com.example.life.data.model.CheckIn
import com.example.life.data.model.Mood
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date

class CheckInRepository(private val checkInDao: CheckInDao) {

    val allCheckIns: Flow<List<CheckIn>> = checkInDao.getAllCheckIns().map { entities ->
        entities.mapNotNull { entity ->
            try {
                val mood = Mood.entries.find { it.name.equals(entity.mood, ignoreCase = true) } 
                    ?: Mood.Okay // Fallback
                
                CheckIn(
                    id = entity.id.toString(),
                    timestamp = Date(entity.timestamp),
                    situation = entity.situation,
                    mood = mood,
                    reason = entity.reason,
                    nextAction = entity.nextAction
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun insert(situation: String, mood: Mood, reason: String?, nextAction: String) {
        val entity = CheckInEntity(
            timestamp = System.currentTimeMillis(),
            situation = situation,
            mood = mood.name,
            reason = reason,
            nextAction = nextAction
        )
        checkInDao.insert(entity)
    }
}
