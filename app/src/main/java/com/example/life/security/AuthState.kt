package com.example.life.security

sealed class AuthState {
    object Unregistered : AuthState()
    object Locked : AuthState()
    object Authenticated : AuthState()
}
