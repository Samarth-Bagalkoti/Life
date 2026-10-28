package com.example.life.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.life.data.local.CheckInRepository
import com.example.life.data.local.LifeDatabase
import com.example.life.data.local.SettingsManager
import com.example.life.data.model.CheckIn
import com.example.life.data.model.Mood
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

data class DailyInsight(
    val dateLabel: String,
    val averageMoodScore: Double,
    val commonMood: Mood,
    val moonPhase: MoonPhase,
    val moonIllumination: Int,
    val timestamp: Long,
    val checkIns: List<CheckIn>
)

class InsightsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CheckInRepository
    private val settingsManager = SettingsManager(application)

    val dailyInsights: StateFlow<List<DailyInsight>>

    init {
        val checkInDao = LifeDatabase.getDatabase(application).checkInDao()
        repository = CheckInRepository(checkInDao)

        dailyInsights = repository.allCheckIns
            .map { checkIns ->
                processInsights(checkIns)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    private fun processInsights(checkIns: List<CheckIn>): List<DailyInsight> {
        if (checkIns.isEmpty()) return emptyList()

        val calendar = Calendar.getInstance()
        val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

        // Group by day (ignoring time)
        val grouped = checkIns.groupBy { checkIn ->
            calendar.time = checkIn.timestamp
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }

        return grouped.mapNotNull { (dayTimestamp, dailyCheckIns) ->
            if (dailyCheckIns.isEmpty()) return@mapNotNull null
            
            val validScores = dailyCheckIns.map { it.mood.score }
            val avgScore = if (validScores.isNotEmpty()) validScores.average() else 3.0
            
            val commonMood = dailyCheckIns.groupBy { it.mood }
                .maxByOrNull { it.value.size }?.key ?: Mood.Okay
            
            val moonPhase = try {
                MoonPhaseUtil.getMoonPhase(
                    Date(dayTimestamp),
                    settingsManager.newMoonReferenceDate,
                    settingsManager.fullMoonReferenceDate
                )
            } catch (e: Exception) {
                MoonPhase.NEW_MOON
            }
            
            val moonIllumination = try {
                MoonPhaseUtil.getMoonIllumination(
                    Date(dayTimestamp),
                    settingsManager.newMoonReferenceDate,
                    settingsManager.fullMoonReferenceDate
                )
            } catch (e: Exception) {
                0
            }

            DailyInsight(
                dateLabel = dateFormatter.format(Date(dayTimestamp)),
                averageMoodScore = avgScore,
                commonMood = commonMood,
                moonPhase = moonPhase,
                moonIllumination = moonIllumination,
                timestamp = dayTimestamp,
                checkIns = dailyCheckIns.sortedBy { it.timestamp }
            )
        }.sortedByDescending { it.timestamp }
    }
}
