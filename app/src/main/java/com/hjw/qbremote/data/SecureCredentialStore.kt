package com.hjw.qbremote.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureCredentialStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREF_FILE_NAME_V2, Context.MODE_PRIVATE)

    init {
        migrateLegacyEncryptedPrefsIfNeeded(
            context = appContext,
            targetPrefs = prefs,
            encryptor = ::encryptValue,
        )
    }

    fun savePassword(password: String) {
        prefs.edit().putString(KEY_PASSWORD, encryptValue(password)).apply()
    }

    fun getPassword(): String {
        return decryptStoredValue(prefs.getString(KEY_PASSWORD, null))
    }

    fun clearPassword() {
        prefs.edit().remove(KEY_PASSWORD).apply()
    }

    fun savePasswordForProfile(profileId: String, password: String) {
        if (profileId.isBlank()) return
        prefs.edit().putString(profilePasswordKey(profileId), encryptValue(password)).apply()
    }

    fun getPasswordForProfile(profileId: String): String {
        if (profileId.isBlank()) return ""
        return decryptStoredValue(prefs.getString(profilePasswordKey(profileId), null))
    }

    fun removePasswordForProfile(profileId: String) {
        if (profileId.isBlank()) return
        prefs.edit().remove(profilePasswordKey(profileId)).apply()
    }

    private fun profilePasswordKey(profileId: String): String {
        return "${KEY_PASSWORD_PREFIX}${profileId.trim()}"
    }

    private fun encryptValue(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val payload = ByteBuffer.allocate(4 + iv.size + encryptedBytes.size).apply {
            putInt(iv.size)
            put(iv)
            put(encryptedBytes)
        }.array()
        return "${VALUE_PREFIX}${Base64.encodeToString(payload, Base64.NO_WRAP)}"
    }

    private fun decryptStoredValue(storedValue: String?): String {
        val encoded = storedValue?.trim().orEmpty()
        if (encoded.isEmpty()) return ""
        return runCatching {
            // Legacy migration compatibility: writers always produce v2:-prefixed values,
            // so an unprefixed value is pre-encryption plaintext returned as-is.
            if (!encoded.startsWith(VALUE_PREFIX)) return@runCatching encoded
            val payload = Base64.decode(encoded.removePrefix(VALUE_PREFIX), Base64.NO_WRAP)
            val buffer = ByteBuffer.wrap(payload)
            val ivSize = buffer.int
            require(ivSize in 12..32) { "Invalid IV size." }
            val iv = ByteArray(ivSize).also { buffer.get(it) }
            val encryptedBytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
            )
            cipher.doFinal(encryptedBytes).toString(Charsets.UTF_8)
        }.getOrDefault("")
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) return existingKey

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE,
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    companion object {
        private const val PREF_FILE_NAME_V2 = "qb_secure_credentials_v2"
        private const val KEY_PASSWORD = "password"
        private const val KEY_PASSWORD_PREFIX = "password_profile_"
        private const val KEY_ALIAS = "qbremote_secure_credentials_key"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val VALUE_PREFIX = "v2:"
    }
}
