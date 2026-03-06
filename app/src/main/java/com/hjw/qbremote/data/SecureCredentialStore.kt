package com.hjw.qbremote.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureCredentialStore(context: Context) {
    private val prefs = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREF_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun savePassword(password: String) {
        prefs.edit().putString(KEY_PASSWORD, password).apply()
    }

    fun getPassword(): String {
        return prefs.getString(KEY_PASSWORD, "").orEmpty()
    }

    fun savePasswordForProfile(profileId: String, password: String) {
        if (profileId.isBlank()) return
        prefs.edit().putString(profilePasswordKey(profileId), password).apply()
    }

    fun getPasswordForProfile(profileId: String): String {
        if (profileId.isBlank()) return ""
        return prefs.getString(profilePasswordKey(profileId), "").orEmpty()
    }

    private fun profilePasswordKey(profileId: String): String {
        return "${KEY_PASSWORD_PREFIX}${profileId.trim()}"
    }

    companion object {
        private const val PREF_FILE_NAME = "qb_secure_credentials"
        private const val KEY_PASSWORD = "password"
        private const val KEY_PASSWORD_PREFIX = "password_profile_"
    }
}
