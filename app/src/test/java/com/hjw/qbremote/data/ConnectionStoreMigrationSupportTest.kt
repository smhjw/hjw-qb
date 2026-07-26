package com.hjw.qbremote.data

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hjw.qbremote.data.ConnectionPreferenceKeys as Keys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionStoreMigrationSupportTest {
    @Test
    fun `legacy chart setting key names stay frozen`() {
        assertEquals(
            listOf(
                "dashboard_card_order",
                "chart_sort_mode",
                "show_speed_totals",
                "enable_server_grouping",
                "show_chart_panel",
                "show_country_flow_card",
                "show_upload_distribution_card",
                "show_category_distribution_card",
                "chart_show_site_name",
            ),
            legacyGlobalChartSettingKeys().map { it.name },
        )
    }

    @Test
    fun `remove legacy chart settings keeps unrelated keys`() {
        val target = mutablePreferencesOf()
        target[stringPreferencesKey("dashboard_card_order")] = "a,b"
        target[stringPreferencesKey("chart_sort_mode")] = "speed"
        target[booleanPreferencesKey("show_speed_totals")] = true
        target[booleanPreferencesKey("enable_server_grouping")] = true
        target[booleanPreferencesKey("show_chart_panel")] = true
        target[booleanPreferencesKey("show_country_flow_card")] = true
        target[booleanPreferencesKey("show_upload_distribution_card")] = true
        target[booleanPreferencesKey("show_category_distribution_card")] = true
        target[booleanPreferencesKey("chart_show_site_name")] = true
        target[Keys.Host] = "nas.local"

        removeLegacyGlobalChartSettings(target)

        assertEquals(setOf<Preferences.Key<*>>(Keys.Host), target.asMap().keys)
    }

    @Test
    fun `all hints already seen returns null`() {
        assertNull(
            resolveMigratedDashboardHints(
                existingStackHint = true,
                existingSwipeHint = true,
                existingCardHint = true,
                preferences = mutablePreferencesOf(),
            )
        )
    }

    @Test
    fun `nothing to migrate returns null`() {
        assertNull(
            resolveMigratedDashboardHints(
                existingStackHint = false,
                existingSwipeHint = false,
                existingCardHint = false,
                preferences = mutablePreferencesOf(),
            )
        )
    }

    @Test
    fun `per server hints migrate into missing global hints`() {
        val pref = mutablePreferencesOf()
        pref[Keys.ServerDashboardPreferencesJson] = connectionStoreGson.toJson(
            mapOf(
                "p1" to ServerDashboardPreferences(hasSeenDashboardSwipeHint = true),
                "p2" to ServerDashboardPreferences(hasSeenDashboardCardHint = true),
            )
        )

        assertEquals(
            Triple(true, true, true),
            resolveMigratedDashboardHints(
                existingStackHint = true,
                existingSwipeHint = false,
                existingCardHint = false,
                preferences = pref,
            )
        )
    }

    @Test
    fun `blank legacy host produces no default profile`() {
        assertNull(buildDefaultProfileFromLegacyPreferences(mutablePreferencesOf()))

        val blankHost = mutablePreferencesOf()
        blankHost[Keys.Host] = "   "
        assertNull(buildDefaultProfileFromLegacyPreferences(blankHost))
    }

    @Test
    fun `legacy preferences build default profile with fallbacks and clamps`() {
        val pref = mutablePreferencesOf()
        pref[Keys.Host] = " 192.168.1.5 "
        pref[Keys.Port] = 99999
        pref[Keys.RefreshSeconds] = 1
        pref[Keys.ServerBackendType] = "GARBAGE"

        val profile = requireNotNull(buildDefaultProfileFromLegacyPreferences(pref))

        assertTrue(profile.id.isNotBlank())
        assertEquals("192.168.1.5", profile.host)
        assertEquals("192.168.1.5", profile.name)
        assertEquals(65535, profile.port)
        assertEquals(5, profile.refreshSeconds)
        assertEquals("admin", profile.username)
        assertEquals(ServerBackendType.QBITTORRENT, profile.backendType)
        assertEquals(false, profile.useHttps)
    }
}
