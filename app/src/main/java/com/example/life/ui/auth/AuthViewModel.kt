package com.example.life.ui.auth

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import com.example.life.security.AuthSecurityManager
import com.example.life.security.AuthState
import com.example.life.security.BiometricAuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val securityManager = AuthSecurityManager(application)
    private val biometricManager = BiometricAuthManager(application)

    private val _authState = MutableStateFlow<AuthState>(
        if (securityManager.isRegistered()) AuthState.Locked else AuthState.Unregistered
    )
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val isBiometricAvailable: Boolean
        get() = biometricManager.canAuthenticate()

    val isBiometricEnabled: Boolean
        get() = securityManager.isBiometricEnabled()

    val username: String
        get() = securityManager.getUsername()

    fun clearError() {
        _errorMessage.value = null
    }

    fun register(username: String, pinOrPassword: String, confirmPin: String, enableBiometric: Boolean) {
        if (username.isBlank()) {
            _errorMessage.value = "Please enter your name/username"
            return
        }
        if (pinOrPassword.isBlank() || pinOrPassword.length < 4) {
            _errorMessage.value = "PIN/Password must be at least 4 characters"
            return
        }
        if (pinOrPassword != confirmPin) {
            _errorMessage.value = "PIN/Passwords do not match"
            return
        }

        val success = securityManager.register(username, pinOrPassword, enableBiometric)
        if (success) {
            _errorMessage.value = null
            _authState.value = AuthState.Authenticated
        } else {
            _errorMessage.value = "Failed to create local account"
        }
    }

    fun login(pinOrPassword: String) {
        if (pinOrPassword.isBlank()) {
            _errorMessage.value = "Please enter your PIN/Password"
            return
        }

        if (securityManager.verifyCredentials(pinOrPassword)) {
            _errorMessage.value = null
            _authState.value = AuthState.Authenticated
        } else {
            _errorMessage.value = "Incorrect PIN/Password"
        }
    }

    fun triggerBiometricLogin(activity: FragmentActivity) {
        if (!isBiometricAvailable || !isBiometricEnabled) return

        biometricManager.showBiometricPrompt(
            activity = activity,
            onSuccess = {
                _errorMessage.value = null
                _authState.value = AuthState.Authenticated
            },
            onError = { error ->
                _errorMessage.value = error
            }
        )
    }

    fun lock() {
        if (securityManager.isRegistered()) {
            _authState.value = AuthState.Locked
        }
    }

    fun toggleBiometricSetting(enabled: Boolean) {
        securityManager.setBiometricEnabled(enabled)
    }

    fun resetAccount() {
        securityManager.clearAllData()
        _authState.value = AuthState.Unregistered
    }
}
