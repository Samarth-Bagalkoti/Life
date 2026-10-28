package com.example.life.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class AuthSecurityManager(context: Context) {
    private val TAG = "AuthSecurityManager"

    private val sharedPreferences: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "secure_life_auth_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create EncryptedSharedPreferences: ${e.message}. Falling back to plain SharedPreferences.")
            // Fallback for corrupted/invalidated KeyStore keys
            context.getSharedPreferences("secure_life_auth_prefs", Context.MODE_PRIVATE)
        }
    }

    fun isRegistered(): Boolean {
        return try {
            sharedPreferences.getBoolean(KEY_IS_REGISTERED, false)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading isRegistered: ${e.message}")
            false
        }
    }

    fun getUsername(): String {
        return sharedPreferences.getString(KEY_USERNAME, "") ?: ""
    }

    fun isBiometricEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun register(username: String, pinOrPassword: String, enableBiometric: Boolean): Boolean {
        if (username.isBlank() || pinOrPassword.isBlank()) return false

        return try {
            val salt = generateSalt()
            val passwordHash = hashPassword(pinOrPassword.toCharArray(), salt)
            val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)

            sharedPreferences.edit()
                .putBoolean(KEY_IS_REGISTERED, true)
                .putString(KEY_USERNAME, username.trim())
                .putString(KEY_PASSWORD_HASH, passwordHash)
                .putString(KEY_SALT, saltBase64)
                .putBoolean(KEY_BIOMETRIC_ENABLED, enableBiometric)
                .apply()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error during registration: ${e.message}")
            false
        }
    }

    fun verifyCredentials(pinOrPassword: String): Boolean {
        if (!isRegistered()) return false

        return try {
            val storedHash = sharedPreferences.getString(KEY_PASSWORD_HASH, null) ?: return false
            val storedSaltBase64 = sharedPreferences.getString(KEY_SALT, null) ?: return false
            val salt = Base64.decode(storedSaltBase64, Base64.NO_WRAP)

            val computedHash = hashPassword(pinOrPassword.toCharArray(), salt)
            storedHash == computedHash
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying credentials: ${e.message}")
            false
        }
    }

    fun clearAllData() {
        sharedPreferences.edit().clear().apply()
    }

    private fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

    private fun hashPassword(password: CharArray, salt: ByteArray): String {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = skf.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    companion object {
        private const val KEY_IS_REGISTERED = "is_registered"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_SALT = "salt"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"

        private const val PBKDF2_ITERATIONS = 100_000
        private const val KEY_LENGTH_BITS = 256
    }
}
