package com.example.life.ui.home

import android.app.AppOpsManager
import android.app.Application
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val appIcon: Drawable?,
    val usageDuration: Long, // Millis
    val percentage: Float
)

data class DailyUsage(
    val dateLabel: String,
    val totalTime: Long, // Millis
    val timestamp: Long
)

data class ScreenTimeState(
    val hasPermission: Boolean = false,
    val totalTimeToday: Long = 0L,
    val dailyHistory: List<DailyUsage> = emptyList(),
    val appUsageList: List<AppUsageInfo> = emptyList(),
    val selectedDayTimestamp: Long = 0L,
    val isLoading: Boolean = false
)

class ScreenTimeViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ScreenTimeState())
    val state: StateFlow<ScreenTimeState> = _state.asStateFlow()

    private val usageStatsManager = application.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager = application.packageManager

    fun refreshData() {
        viewModelScope.launch(Dispatchers.IO) {
            val hasPermission = checkUsagePermission()
            if (!hasPermission) {
                _state.value = ScreenTimeState(hasPermission = false)
                return@launch
            }

            _state.value = _state.value.copy(isLoading = true, hasPermission = true)

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val todayStart = calendar.timeInMillis
            val now = System.currentTimeMillis()

            val history = mutableListOf<DailyUsage>()
            val tempCal = Calendar.getInstance()
            tempCal.timeInMillis = todayStart

            // Last 7 days history
            for (i in 0..6) {
                val dayStart = tempCal.timeInMillis
                val dayEnd = if (i == 0) now else dayStart + 24 * 60 * 60 * 1000
                
                val dailyApps = getAppUsageForPeriod(dayStart, dayEnd)
                val totalTime = dailyApps.sumOf { it.usageDuration }
                
                val label = if (i == 0) "Today" else {
                    val day = tempCal.get(Calendar.DAY_OF_MONTH)
                    val month = tempCal.get(Calendar.MONTH) + 1
                    "$day/$month"
                }

                history.add(DailyUsage(label, totalTime, dayStart))
                tempCal.add(Calendar.DAY_OF_YEAR, -1)
            }

            val todayApps = getAppUsageForPeriod(todayStart, now)
            val totalToday = todayApps.sumOf { it.usageDuration }

            _state.value = _state.value.copy(
                totalTimeToday = totalToday,
                dailyHistory = history.reversed(),
                appUsageList = todayApps,
                selectedDayTimestamp = todayStart,
                isLoading = false
            )
        }
    }

    fun selectDay(timestamp: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val start = timestamp
            val end = if (isToday(start)) System.currentTimeMillis() else start + 24 * 60 * 60 * 1000
            
            val apps = getAppUsageForPeriod(start, end)
            _state.value = _state.value.copy(
                appUsageList = apps,
                selectedDayTimestamp = timestamp
            )
        }
    }

    private fun isToday(timestamp: Long): Boolean {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        return timestamp == today.timeInMillis
    }

    private fun getAppUsageForPeriod(start: Long, end: Long): List<AppUsageInfo> {
        val events = usageStatsManager.queryEvents(start, end)
        val statsMap = mutableMapOf<String, Long>()
        val lastStartTime = mutableMapOf<String, Long>()
        
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    lastStartTime[event.packageName] = event.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val startTime = lastStartTime[event.packageName]
                    if (startTime != null) {
                        val duration = event.timeStamp - startTime
                        if (duration > 0) {
                            statsMap[event.packageName] = (statsMap[event.packageName] ?: 0L) + duration
                        }
                        lastStartTime.remove(event.packageName)
                    }
                }
            }
        }
        
        // Handle apps that are still in foreground at the 'end' timestamp
        lastStartTime.forEach { (packageName, startTime) ->
            if (end > startTime) {
                val duration = end - startTime
                statsMap[packageName] = (statsMap[packageName] ?: 0L) + duration
            }
        }

        val totalTime = statsMap.values.sum().coerceAtLeast(1L)
        
        return statsMap.mapNotNull { (packageName, duration) ->
            if (duration <= 0) return@mapNotNull null
            
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                // Filter out system apps that are usually not considered "user screen time" or are noisy
                // (Optional, but usually better for accuracy against Digital Wellbeing)
                
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                val appIcon = try {
                    packageManager.getApplicationIcon(appInfo)
                } catch (e: Exception) {
                    null
                }
                
                AppUsageInfo(
                    packageName = packageName,
                    appName = appName,
                    appIcon = appIcon,
                    usageDuration = duration,
                    percentage = (duration.toFloat() / totalTime) * 100f
                )
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.usageDuration }.take(30)
    }

    private fun checkUsagePermission(): Boolean {
        val appOps = getApplication<Application>().getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            getApplication<Application>().packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
