package com.hjw.qbremote.data

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacySecureCredentialMigrationTest {
    @Test
    fun `legacy string values are re-encrypted into target and legacy is cleared`() {
        val target = FakeSharedPreferences()
        val legacy = FakeSharedPreferences()
        legacy.values["password"] = "hunter2"
        legacy.values["profile_password_p1"] = "abc"

        migrateLegacyPrefs(
            targetPrefs = target,
            encryptor = { "enc($it)" },
            legacyPrefsProvider = { legacy },
        )

        assertEquals("enc(hunter2)", target.values["password"])
        assertEquals("enc(abc)", target.values["profile_password_p1"])
        assertEquals(true, target.values["_legacy_migrated_v2"])
        assertTrue(legacy.values.isEmpty())
    }

    @Test
    fun `unavailable legacy prefs only set the migration flag`() {
        val target = FakeSharedPreferences()

        migrateLegacyPrefs(
            targetPrefs = target,
            encryptor = { "enc($it)" },
            legacyPrefsProvider = { null },
        )

        assertEquals(mapOf<String, Any?>("_legacy_migrated_v2" to true), target.values)
    }

    @Test
    fun `already migrated target never invokes legacy provider`() {
        val target = FakeSharedPreferences()
        target.values["_legacy_migrated_v2"] = true
        var providerCalls = 0

        migrateLegacyPrefs(
            targetPrefs = target,
            encryptor = { it },
            legacyPrefsProvider = {
                providerCalls++
                FakeSharedPreferences()
            },
        )

        assertEquals(0, providerCalls)
        assertEquals(mapOf<String, Any?>("_legacy_migrated_v2" to true), target.values)
    }

    @Test
    fun `non string legacy values are ignored`() {
        val target = FakeSharedPreferences()
        val legacy = FakeSharedPreferences()
        legacy.values["password"] = "hunter2"
        legacy.values["attempts"] = 3
        legacy.values["flag"] = true

        migrateLegacyPrefs(
            targetPrefs = target,
            encryptor = { "enc($it)" },
            legacyPrefsProvider = { legacy },
        )

        assertEquals("enc(hunter2)", target.values["password"])
        assertFalse(target.values.containsKey("attempts"))
        assertFalse(target.values.containsKey("flag"))
        assertEquals(true, target.values["_legacy_migrated_v2"])
        assertTrue(legacy.values.isEmpty())
    }
}

private class FakeSharedPreferences : SharedPreferences {
    val values = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = values.toMutableMap()

    override fun getString(key: String?, defValue: String?): String? =
        values[key] as? String ?: defValue

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        @Suppress("UNCHECKED_CAST")
        return values[key] as? MutableSet<String> ?: defValues
    }

    override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue

    override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        values[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor(this)

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit
}

private class FakeEditor(private val prefs: FakeSharedPreferences) : SharedPreferences.Editor {
    private val pending = mutableMapOf<String, Any?>()
    private val removals = mutableSetOf<String>()
    private var clearRequested = false

    override fun putString(key: String?, value: String?): SharedPreferences.Editor =
        also { pending[requireNotNull(key)] = value }

    override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor =
        also { pending[requireNotNull(key)] = values }

    override fun putInt(key: String?, value: Int): SharedPreferences.Editor =
        also { pending[requireNotNull(key)] = value }

    override fun putLong(key: String?, value: Long): SharedPreferences.Editor =
        also { pending[requireNotNull(key)] = value }

    override fun putFloat(key: String?, value: Float): SharedPreferences.Editor =
        also { pending[requireNotNull(key)] = value }

    override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor =
        also { pending[requireNotNull(key)] = value }

    override fun remove(key: String?): SharedPreferences.Editor =
        also { removals += requireNotNull(key) }

    override fun clear(): SharedPreferences.Editor = also { clearRequested = true }

    override fun commit(): Boolean {
        applyChanges()
        return true
    }

    override fun apply() {
        applyChanges()
    }

    private fun applyChanges() {
        if (clearRequested) prefs.values.clear()
        removals.forEach { prefs.values.remove(it) }
        pending.forEach { (key, value) -> prefs.values[key] = value }
    }
}
