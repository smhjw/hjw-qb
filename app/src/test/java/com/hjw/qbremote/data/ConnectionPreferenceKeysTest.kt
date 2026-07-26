package com.hjw.qbremote.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionPreferenceKeysTest {
    @Test
    fun persistedKeyNamesRemainCompatible() {
        assertEquals("host", ConnectionPreferenceKeys.Host.name)
        assertEquals("password", ConnectionPreferenceKeys.PasswordLegacy.name)
        assertEquals("server_profiles_json", ConnectionPreferenceKeys.ServerProfilesJson.name)
        assertEquals("active_server_profile_id", ConnectionPreferenceKeys.ActiveServerProfileId.name)
        assertEquals("dashboard_cache_json", ConnectionPreferenceKeys.DashboardCacheJson.name)
        assertEquals(
            "dashboard_server_snapshots_json",
            ConnectionPreferenceKeys.DashboardServerSnapshotsJson.name,
        )
        assertEquals(
            "server_dashboard_preferences_json",
            ConnectionPreferenceKeys.ServerDashboardPreferencesJson.name,
        )
    }
}
