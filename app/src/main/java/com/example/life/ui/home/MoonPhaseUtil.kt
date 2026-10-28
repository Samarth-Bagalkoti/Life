package com.example.life.ui.home

import java.util.Calendar
import java.util.Date
import kotlin.math.floor

enum class MoonPhase(val label: String, val icon: String) {
    NEW_MOON("New Moon", "🌑"),
    WAXING_CRESCENT("Waxing Crescent", "🌒"),
    FIRST_QUARTER("First Quarter", "🌓"),
    WAXING_GIBBOUS("Waxing Gibbous", "🌔"),
    FULL_MOON("Full Moon", "🌕"),
    WANING_GIBBOUS("Waning Gibbous", "🌖"),
    LAST_QUARTER("Last Quarter", "🌗"),
    WANING_CRESCENT("Waning Crescent", "🌘")
}

object MoonPhaseUtil {
    private const val LUNAR_MONTH_DAYS = 29.530588
    private const val LUNAR_MONTH_MILLIS = (LUNAR_MONTH_DAYS * 24 * 60 * 60 * 1000).toLong()

    // Default reference dates for India: Sept 11, 2026 (New Moon) and Sept 26, 2026 (Full Moon)
    private val DEFAULT_NEW_MOON_REF: Long by lazy {
        Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 11, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun getMoonIllumination(date: Date, newMoonRef: Long, fullMoonRef: Long): Int {
        val anchor = when {
            newMoonRef > 0 -> newMoonRef
            fullMoonRef > 0 -> fullMoonRef - (LUNAR_MONTH_MILLIS / 2)
            else -> DEFAULT_NEW_MOON_REF
        }

        val diff = date.time - anchor
        var phasePercent = (diff.toDouble() / LUNAR_MONTH_MILLIS)
        phasePercent -= Math.floor(phasePercent)
        if (phasePercent < 0) phasePercent += 1.0

        // phasePercent 0 is New Moon, 0.5 is Full Moon
        // Illumination %: 0% at New Moon (0 and 1.0), 100% at Full Moon (0.5)
        val illumination = if (phasePercent <= 0.5) {
            phasePercent * 2
        } else {
            (1.0 - phasePercent) * 2
        }
        
        return (illumination * 100).toInt().coerceIn(0, 100)
    }

    fun getMoonPhase(date: Date, newMoonRef: Long, fullMoonRef: Long): MoonPhase {
        // Use user-provided anchors if available, otherwise use defaults
        val anchor = when {
            newMoonRef > 0 -> newMoonRef
            fullMoonRef > 0 -> fullMoonRef - (LUNAR_MONTH_MILLIS / 2)
            else -> DEFAULT_NEW_MOON_REF
        }

        val diff = date.time - anchor
        // Calculate phase based on the lunar cycle
        var phasePercent = (diff.toDouble() / LUNAR_MONTH_MILLIS)
        phasePercent -= floor(phasePercent)
        if (phasePercent < 0) phasePercent += 1.0

        return when {
            (phasePercent < 0.0625 || phasePercent >= 0.9375) -> MoonPhase.NEW_MOON
            phasePercent < 0.1875 -> MoonPhase.WAXING_CRESCENT
            phasePercent < 0.3125 -> MoonPhase.FIRST_QUARTER
            phasePercent < 0.4375 -> MoonPhase.WAXING_GIBBOUS
            phasePercent < 0.5625 -> MoonPhase.FULL_MOON
            phasePercent < 0.6875 -> MoonPhase.WANING_GIBBOUS
            phasePercent < 0.8125 -> MoonPhase.LAST_QUARTER
            else -> MoonPhase.WANING_CRESCENT
        }
    }
}
