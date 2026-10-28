package com.example.life.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class InactivityLockManager(
    private val onLockRequired: () -> Unit
) : DefaultLifecycleObserver {

    private var backgroundTimestamp: Long = 0
    var lockTimeoutMillis: Long = 0 // 0 means lock immediately

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (backgroundTimestamp != 0L) {
            val elapsed = System.currentTimeMillis() - backgroundTimestamp
            if (elapsed >= lockTimeoutMillis) {
                onLockRequired()
            }
            backgroundTimestamp = 0
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        backgroundTimestamp = System.currentTimeMillis()
    }
}
