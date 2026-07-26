@file:Suppress("DEPRECATION")

package com.hjw.qbremote.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

internal fun migrateLegacyEncryptedPrefsIfNeeded(
    context: Context,
    targetPrefs: SharedPreferences,
    encryptor: (String) -> String,
) {
    migrateLegacyPrefs(
        targetPrefs = targetPrefs,
        encryptor = encryptor,
        legacyPrefsProvider = {
            runCatching {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    LEGACY_PREF_FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            }.getOrNull()
        },
    )
}

internal fun migrateLegacyPrefs(
    targetPrefs: SharedPreferences,
    encryptor: (String) -> String,
    legacyPrefsProvider: () -> SharedPreferences?,
) {
    if (targetPrefs.getBoolean(KEY_LEGACY_MIGRATED, false)) return

    val legacyPrefs = legacyPrefsProvider()

    if (legacyPrefs == null) {
        targetPrefs.edit().putBoolean(KEY_LEGACY_MIGRATED, true).apply()
        return
    }

    val legacyValues = legacyPrefs.all
        .mapNotNull { (key, value) ->
            (value as? String)?.let { key to it }
        }

    val editor = targetPrefs.edit()
    legacyValues.forEach { (key, value) ->
        editor.putString(key, encryptor(value))
    }
    editor.putBoolean(KEY_LEGACY_MIGRATED, true).apply()

    if (legacyValues.isNotEmpty()) {
        legacyPrefs.edit().clear().apply()
    }
}

private const val LEGACY_PREF_FILE_NAME = "qb_secure_credentials"
private const val KEY_LEGACY_MIGRATED = "_legacy_migrated_v2"
