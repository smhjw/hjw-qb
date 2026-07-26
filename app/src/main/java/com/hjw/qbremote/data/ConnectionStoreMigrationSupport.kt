package com.hjw.qbremote.data

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hjw.qbremote.data.ConnectionPreferenceKeys as Keys

internal fun legacyGlobalChartSettingKeys(): List<Preferences.Key<*>> = listOf(
    stringPreferencesKey("dashboard_card_order"),
    stringPreferencesKey("chart_sort_mode"),
    booleanPreferencesKey("show_speed_totals"),
    booleanPreferencesKey("enable_server_grouping"),
    booleanPreferencesKey("show_chart_panel"),
    booleanPreferencesKey("show_country_flow_card"),
    booleanPreferencesKey("show_upload_distribution_card"),
    booleanPreferencesKey("show_category_distribution_card"),
    booleanPreferencesKey("chart_show_site_name"),
)

internal fun removeLegacyGlobalChartSettings(target: MutablePreferences) {
    legacyGlobalChartSettingKeys().forEach { key -> target.remove(key) }
}

internal fun resolveMigratedDashboardHints(
    existingStackHint: Boolean,
    existingSwipeHint: Boolean,
    existingCardHint: Boolean,
    preferences: Preferences,
): Triple<Boolean, Boolean, Boolean>? {
    if (existingStackHint && existingSwipeHint && existingCardHint) return null

    val perServerPreferences = parseServerDashboardPreferences(preferences[Keys.ServerDashboardPreferencesJson]).values
    val migratedStackHint = existingStackHint || perServerPreferences.any { it.hasSeenStackReorderHint }
    val migratedSwipeHint = existingSwipeHint || perServerPreferences.any { it.hasSeenDashboardSwipeHint }
    val migratedCardHint = existingCardHint || perServerPreferences.any { it.hasSeenDashboardCardHint }
    if (
        migratedStackHint == existingStackHint &&
        migratedSwipeHint == existingSwipeHint &&
        migratedCardHint == existingCardHint
    ) {
        return null
    }

    return Triple(migratedStackHint, migratedSwipeHint, migratedCardHint)
}

internal fun buildDefaultProfileFromLegacyPreferences(pref: Preferences): ServerProfile? {
    val host = pref[Keys.Host].orEmpty().trim()
    if (host.isBlank()) return null

    return ServerProfile(
        id = generateProfileId(),
        name = buildProfileName(
            requestedName = "",
            host = host,
            index = 1,
        ),
        backendType = runCatching {
            enumValueOf<ServerBackendType>(pref[Keys.ServerBackendType].orEmpty())
        }.getOrDefault(ServerBackendType.QBITTORRENT),
        host = host,
        port = (pref[Keys.Port] ?: 8080).coerceIn(1, 65535),
        useHttps = pref[Keys.UseHttps] ?: false,
        username = pref[Keys.Username] ?: "admin",
        refreshSeconds = (pref[Keys.RefreshSeconds] ?: 5).coerceIn(5, 120),
    )
}
