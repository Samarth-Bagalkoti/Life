package com.example.life.data.local

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("life_settings", Context.MODE_PRIVATE)

    var remindersEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMINDERS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_REMINDERS_ENABLED, value).apply()

    var newMoonReferenceDate: Long
        get() = prefs.getLong(KEY_NEW_MOON_DATE, 0L)
        set(value) = prefs.edit().putLong(KEY_NEW_MOON_DATE, value).apply()

    var fullMoonReferenceDate: Long
        get() = prefs.getLong(KEY_FULL_MOON_DATE, 0L)
        set(value) = prefs.edit().putLong(KEY_FULL_MOON_DATE, value).apply()

    var autoLockTimeoutSeconds: Int
        get() = prefs.getInt(KEY_AUTO_LOCK_TIMEOUT, 0) // 0 means immediately
        set(value) = prefs.edit().putInt(KEY_AUTO_LOCK_TIMEOUT, value).apply()

    companion object {
        private const val KEY_REMINDERS_ENABLED = "reminders_enabled"
        private const val KEY_NEW_MOON_DATE = "new_moon_date"
        private const val KEY_FULL_MOON_DATE = "full_moon_date"
        private const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout"
    }
}
