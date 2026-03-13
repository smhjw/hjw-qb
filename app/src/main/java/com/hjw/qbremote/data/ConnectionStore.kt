package com.hjw.qbremote.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hjw.qbremote.data.model.CountryPeerSnapshot
import com.hjw.qbremote.data.model.CountryUploadRecord
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.data.model.TransferInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.net.URI
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "qb_connection")

data class ConnectionSettings(
    val host: String = "",
    val port: Int = 8080,
    val useHttps: Boolean = false,
    val username: String = "admin",
    val password: String = "",
    val refreshSeconds: Int = 5,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val appTheme: AppTheme = AppTheme.DARK,
    val customBackgroundImagePath: String = "",
    val customBackgroundToneIsLight: Boolean = false,
    val showSpeedTotals: Boolean = true,
    val enableServerGrouping: Boolean = true,
    val showChartPanel: Boolean = true,
    val showCountryFlowCard: Boolean = true,
    val showUploadDistributionCard: Boolean = true,
    val showCategoryDistributionCard: Boolean = true,
    val dashboardCardOrder: String = "country_flow,category_share,daily_upload",
    val chartShowSiteName: Boolean = true,
    val chartSortMode: ChartSortMode = ChartSortMode.TOTAL_SPEED,
    val deleteFilesDefault: Boolean = true,
    val deleteFilesWhenNoSeeders: Boolean = false,
    val homeTorrentEntryHintDismissed: Boolean = false,
    val hasSeenDashboardHideHint: Boolean = false,
    val hasSeenDashboardHiddenSnack: Boolean = false,
) {
    fun baseUrl(): String {
        return baseUrlCandidates().first()
    }

    fun baseUrlCandidates(): List<String> {
        val rawInput = host.trim()
        require(rawInput.isNotBlank()) { "Host cannot be empty." }

        val hasExplicitScheme = rawInput.contains("://")
        val normalizedInput = if (hasExplicitScheme) rawInput else "http://$rawInput"

        val parsedUri = runCatching { URI(normalizedInput) }.getOrElse {
            throw IllegalArgumentException(
                "Host format is invalid. Use host, host:port, or http(s)://host[:port]."
            )
        }

        val parsedHost = parsedUri.host?.takeIf { it.isNotBlank() }
            ?: parsedUri.rawAuthority
                ?.substringAfterLast('@')
                ?.substringBefore(':')
                ?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException(
                "Host format is invalid. Use host, host:port, or http(s)://host[:port]."
            )

        val scheme = if (hasExplicitScheme) {
            val parsedScheme = parsedUri.scheme?.lowercase()
            val validatedScheme = parsedScheme
                ?: throw IllegalArgumentException("Only http/https is supported.")
            if (validatedScheme != "http" && validatedScheme != "https") {
                throw IllegalArgumentException("Only http/https is supported.")
            }
            validatedScheme
        } else {
            if (useHttps) "https" else "http"
        }

        val hostForUrl = if (parsedHost.contains(':') && !parsedHost.startsWith("[")) {
            "[$parsedHost]"
        } else {
            parsedHost
        }

        val rawPath = parsedUri.rawPath.orEmpty().trim()
        val normalizedPath = if (rawPath.isBlank() || rawPath == "/") {
            ""
        } else {
            rawPath.trimEnd('/')
        }
        val pathForUrl = if (normalizedPath.isNotEmpty() && !normalizedPath.startsWith('/')) {
            "/$normalizedPath"
        } else {
            normalizedPath
        }

        val explicitPort = parsedUri.port.takeIf { it in 1..65535 }
        val schemeDefaultPort = if (scheme == "https") 443 else 80
        val configuredPort = port.takeIf { it in 1..65535 } ?: schemeDefaultPort
        val primaryPort = explicitPort ?: configuredPort
        val primaryUrl = "$scheme://$hostForUrl:$primaryPort$pathForUrl/"

        if (!hasExplicitScheme || explicitPort != null || configuredPort == schemeDefaultPort) {
            return listOf(primaryUrl)
        }

        val fallbackUrl = "$scheme://$hostForUrl:$schemeDefaultPort$pathForUrl/"
        return listOf(primaryUrl, fallbackUrl).distinct()
    }
}

data class ServerProfile(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val useHttps: Boolean,
    val username: String,
    val refreshSeconds: Int,
)

data class ServerProfilesState(
    val profiles: List<ServerProfile> = emptyList(),
    val activeProfileId: String? = null,
)

data class DailyUploadTrackingSnapshot(
    val date: String = "",
    val baselineByTorrent: Map<String, Long> = emptyMap(),
    val lastSeenByTorrent: Map<String, Long> = emptyMap(),
)

data class DailyCountryUploadTrackingSnapshot(
    val date: String = "",
    val totalsByCountry: Map<String, Long> = emptyMap(),
    val peerSnapshots: Map<String, CountryPeerSnapshot> = emptyMap(),
    val lastSeenByTorrent: Map<String, Long> = emptyMap(),
    val recentSamples: List<CountryDistributionSample> = emptyList(),
)

data class CountryDistributionSample(
    val sampledTotals: Map<String, Long> = emptyMap(),
    val peerCountsByCountry: Map<String, Long> = emptyMap(),
)

data class DashboardCacheSnapshot(
    val transferInfo: TransferInfo = TransferInfo(),
    val torrents: List<TorrentInfo> = emptyList(),
    val dailyTagUploadDate: String = "",
    val dailyTagUploadStats: List<CachedDailyTagUploadStat> = emptyList(),
    val dailyCountryUploadDate: String = "",
    val dailyCountryUploadStats: List<CountryUploadRecord> = emptyList(),
)

data class CachedDailyTagUploadStat(
    val tag: String = "",
    val uploadedBytes: Long = 0L,
    val torrentCount: Int = 0,
    val isNoTag: Boolean = false,
)

enum class ChartSortMode {
    TOTAL_SPEED,
    DOWNLOAD_SPEED,
    UPLOAD_SPEED,
    TORRENT_COUNT,
}

enum class AppLanguage {
    SYSTEM,
    ZH_CN,
    EN,
}

enum class AppTheme {
    DARK,
    LIGHT,
    CUSTOM,
}

class ConnectionStore(private val context: Context) {
    private val secureCredentials = SecureCredentialStore(context)
    private val gson = Gson()
    private val serverProfileListType = object : TypeToken<List<ServerProfile>>() {}.type
    private val dailyUploadTrackingMapType =
        object : TypeToken<Map<String, DailyUploadTrackingSnapshot>>() {}.type
    private val dailyCountryUploadTrackingMapType =
        object : TypeToken<Map<String, DailyCountryUploadTrackingSnapshot>>() {}.type
    private val dashboardCacheMapType =
        object : TypeToken<Map<String, DashboardCacheSnapshot>>() {}.type

    private object Keys {
        val Host = stringPreferencesKey("host")
        val Port = intPreferencesKey("port")
        val UseHttps = booleanPreferencesKey("use_https")
        val Username = stringPreferencesKey("username")
        val PasswordLegacy = stringPreferencesKey("password")
        val RefreshSeconds = intPreferencesKey("refresh_seconds")
        val AppLanguage = stringPreferencesKey("app_language")
        val AppTheme = stringPreferencesKey("app_theme")
        val CustomBackgroundImagePath = stringPreferencesKey("custom_background_image_path")
        val CustomBackgroundToneIsLight = booleanPreferencesKey("custom_background_tone_is_light")
        val ShowSpeedTotals = booleanPreferencesKey("show_speed_totals")
        val EnableServerGrouping = booleanPreferencesKey("enable_server_grouping")
        val ShowChartPanel = booleanPreferencesKey("show_chart_panel")
        val ShowCountryFlowCard = booleanPreferencesKey("show_country_flow_card")
        val ShowUploadDistributionCard = booleanPreferencesKey("show_upload_distribution_card")
        val ShowCategoryDistributionCard = booleanPreferencesKey("show_category_distribution_card")
        val DashboardCardOrder = stringPreferencesKey("dashboard_card_order")
        val ChartShowSiteName = booleanPreferencesKey("chart_show_site_name")
        val ChartSortMode = stringPreferencesKey("chart_sort_mode")
        val DeleteFilesDefault = booleanPreferencesKey("delete_files_default")
        val DeleteFilesWhenNoSeeders = booleanPreferencesKey("delete_files_when_no_seeders")
        val HomeTorrentEntryHintDismissed = booleanPreferencesKey("home_torrent_entry_hint_dismissed")
        val HasSeenDashboardHideHint = booleanPreferencesKey("has_seen_dashboard_hide_hint")
        val HasSeenDashboardHiddenSnack = booleanPreferencesKey("has_seen_dashboard_hidden_snack")
        val ServerProfilesJson = stringPreferencesKey("server_profiles_json")
        val ActiveServerProfileId = stringPreferencesKey("active_server_profile_id")
        val DailyUploadTrackingJson = stringPreferencesKey("daily_upload_tracking_json")
        val DailyCountryUploadTrackingJson = stringPreferencesKey("daily_country_upload_tracking_json")
        val DashboardCacheJson = stringPreferencesKey("dashboard_cache_json")
    }

    val settingsFlow: Flow<ConnectionSettings> = context.dataStore.data.map { pref ->
        val activeProfileId = pref[Keys.ActiveServerProfileId].orEmpty()
        pref.toSettings(resolvePassword(activeProfileId))
    }

    val serverProfilesFlow: Flow<ServerProfilesState> = context.dataStore.data.map { pref ->
        val profiles = parseProfiles(pref[Keys.ServerProfilesJson])
        val storedActive = pref[Keys.ActiveServerProfileId].orEmpty()
        val active = when {
            storedActive.isNotBlank() && profiles.any { it.id == storedActive } -> storedActive
            profiles.isNotEmpty() -> profiles.first().id
            else -> null
        }
        ServerProfilesState(
            profiles = profiles,
            activeProfileId = active,
        )
    }

    suspend fun save(settings: ConnectionSettings) {
        val pref = context.dataStore.data.first()
        val activeProfileId = pref[Keys.ActiveServerProfileId].orEmpty()
        val profiles = parseProfiles(pref[Keys.ServerProfilesJson]).toMutableList()
        var resolvedActiveProfileId = activeProfileId

        if (resolvedActiveProfileId.isBlank() && settings.host.trim().isNotBlank()) {
            val newProfileId = generateProfileId()
            val newProfile = settings.toServerProfile(
                id = newProfileId,
                name = buildProfileName(
                    requestedName = "",
                    host = settings.host,
                    index = profiles.size + 1,
                )
            )
            profiles += newProfile
            resolvedActiveProfileId = newProfileId
        }

        if (resolvedActiveProfileId.isNotBlank()) {
            val index = profiles.indexOfFirst { it.id == resolvedActiveProfileId }
            if (index >= 0) {
                val current = profiles[index]
                profiles[index] = settings.toServerProfile(
                    id = current.id,
                    name = current.name,
                )
            } else if (settings.host.trim().isNotBlank()) {
                profiles += settings.toServerProfile(
                    id = resolvedActiveProfileId,
                    name = buildProfileName(
                        requestedName = "",
                        host = settings.host,
                        index = profiles.size + 1,
                    )
                )
            }
        }

        secureCredentials.savePassword(settings.password)
        if (resolvedActiveProfileId.isNotBlank()) {
            secureCredentials.savePasswordForProfile(resolvedActiveProfileId, settings.password)
        }

        context.dataStore.edit { target ->
            target[Keys.Host] = settings.host
            target[Keys.Port] = settings.port
            target[Keys.UseHttps] = settings.useHttps
            target[Keys.Username] = settings.username
            target[Keys.RefreshSeconds] = settings.refreshSeconds
            target[Keys.AppLanguage] = settings.appLanguage.name
            target[Keys.AppTheme] = settings.appTheme.name
            target[Keys.CustomBackgroundImagePath] = settings.customBackgroundImagePath
            target[Keys.CustomBackgroundToneIsLight] = settings.customBackgroundToneIsLight
            target[Keys.ShowSpeedTotals] = settings.showSpeedTotals
            target[Keys.EnableServerGrouping] = settings.enableServerGrouping
            target[Keys.ShowChartPanel] = settings.showChartPanel
            target[Keys.ShowCountryFlowCard] = settings.showCountryFlowCard
            target[Keys.ShowUploadDistributionCard] = settings.showUploadDistributionCard
            target[Keys.ShowCategoryDistributionCard] = settings.showCategoryDistributionCard
            target[Keys.DashboardCardOrder] = settings.dashboardCardOrder
            target[Keys.ChartShowSiteName] = settings.chartShowSiteName
            target[Keys.ChartSortMode] = settings.chartSortMode.name
            target[Keys.DeleteFilesDefault] = settings.deleteFilesDefault
            target[Keys.DeleteFilesWhenNoSeeders] = settings.deleteFilesWhenNoSeeders
            target[Keys.HomeTorrentEntryHintDismissed] = settings.homeTorrentEntryHintDismissed
            target[Keys.HasSeenDashboardHideHint] = settings.hasSeenDashboardHideHint
            target[Keys.HasSeenDashboardHiddenSnack] = settings.hasSeenDashboardHiddenSnack
            target.remove(Keys.PasswordLegacy)
            if (resolvedActiveProfileId.isNotBlank()) {
                target[Keys.ActiveServerProfileId] = resolvedActiveProfileId
            }
            target[Keys.ServerProfilesJson] = gson.toJson(profiles)
        }
    }

    suspend fun addServerProfile(name: String, settings: ConnectionSettings): ServerProfile {
        val pref = context.dataStore.data.first()
        val profiles = parseProfiles(pref[Keys.ServerProfilesJson]).toMutableList()
        val id = generateProfileId()
        val profile = settings.toServerProfile(
            id = id,
            name = buildProfileName(
                requestedName = name,
                host = settings.host,
                index = profiles.size + 1,
            ),
        )
        profiles += profile

        secureCredentials.savePasswordForProfile(id, settings.password)
        secureCredentials.savePassword(settings.password)

        context.dataStore.edit { target ->
            target[Keys.ServerProfilesJson] = gson.toJson(profiles)
            target[Keys.ActiveServerProfileId] = id
            target[Keys.Host] = profile.host
            target[Keys.Port] = profile.port
            target[Keys.UseHttps] = profile.useHttps
            target[Keys.Username] = profile.username
            target[Keys.RefreshSeconds] = profile.refreshSeconds
        }

        return profile
    }

    suspend fun switchToServerProfile(profileId: String): ConnectionSettings {
        require(profileId.isNotBlank()) { "Invalid server profile id." }
        val pref = context.dataStore.data.first()
        val profiles = parseProfiles(pref[Keys.ServerProfilesJson])
        val profile = profiles.firstOrNull { it.id == profileId }
            ?: throw IllegalArgumentException("Server profile not found.")

        val password = resolvePassword(profileId)
        val currentSettings = pref.toSettings(password)
        val switched = currentSettings.copy(
            host = profile.host,
            port = profile.port,
            useHttps = profile.useHttps,
            username = profile.username,
            password = password,
            refreshSeconds = profile.refreshSeconds,
        )

        secureCredentials.savePassword(password)

        context.dataStore.edit { target ->
            target[Keys.ActiveServerProfileId] = profile.id
            target[Keys.Host] = profile.host
            target[Keys.Port] = profile.port
            target[Keys.UseHttps] = profile.useHttps
            target[Keys.Username] = profile.username
            target[Keys.RefreshSeconds] = profile.refreshSeconds
        }

        return switched
    }

    suspend fun loadDailyUploadTrackingSnapshot(scopeKey: String): DailyUploadTrackingSnapshot? {
        if (scopeKey.isBlank()) return null
        val pref = context.dataStore.data.first()
        val snapshots = parseDailyUploadTrackingSnapshots(pref[Keys.DailyUploadTrackingJson])
        return snapshots[scopeKey]
    }

    suspend fun saveDailyUploadTrackingSnapshot(
        scopeKey: String,
        snapshot: DailyUploadTrackingSnapshot,
    ) {
        if (scopeKey.isBlank()) return
        context.dataStore.edit { target ->
            val snapshots = parseDailyUploadTrackingSnapshots(target[Keys.DailyUploadTrackingJson]).toMutableMap()
            snapshots[scopeKey] = snapshot.normalized()
            target[Keys.DailyUploadTrackingJson] = gson.toJson(snapshots)
        }
    }

    suspend fun loadDailyCountryUploadTrackingSnapshot(scopeKey: String): DailyCountryUploadTrackingSnapshot? {
        if (scopeKey.isBlank()) return null
        val pref = context.dataStore.data.first()
        val snapshots = parseDailyCountryUploadTrackingSnapshots(pref[Keys.DailyCountryUploadTrackingJson])
        return snapshots[scopeKey]
    }

    suspend fun saveDailyCountryUploadTrackingSnapshot(
        scopeKey: String,
        snapshot: DailyCountryUploadTrackingSnapshot,
    ) {
        if (scopeKey.isBlank()) return
        context.dataStore.edit { target ->
            val snapshots = parseDailyCountryUploadTrackingSnapshots(
                target[Keys.DailyCountryUploadTrackingJson]
            ).toMutableMap()
            snapshots[scopeKey] = snapshot.normalized()
            target[Keys.DailyCountryUploadTrackingJson] = gson.toJson(snapshots)
        }
    }

    suspend fun loadDashboardCacheSnapshot(scopeKey: String): DashboardCacheSnapshot? {
        if (scopeKey.isBlank()) return null
        val pref = context.dataStore.data.first()
        val snapshots = parseDashboardCacheSnapshots(pref[Keys.DashboardCacheJson])
        return snapshots[scopeKey]
    }

    suspend fun saveDashboardCacheSnapshot(
        scopeKey: String,
        snapshot: DashboardCacheSnapshot,
    ) {
        if (scopeKey.isBlank()) return
        context.dataStore.edit { target ->
            val snapshots = parseDashboardCacheSnapshots(target[Keys.DashboardCacheJson]).toMutableMap()
            snapshots[scopeKey] = snapshot.normalized()
            target[Keys.DashboardCacheJson] = gson.toJson(snapshots)
        }
    }

    suspend fun migrateLegacyPasswordIfNeeded() {
        val prefBefore = context.dataStore.data.first()
        val legacy = prefBefore[Keys.PasswordLegacy].orEmpty()
        if (legacy.isNotBlank()) {
            secureCredentials.savePassword(legacy)
            context.dataStore.edit { it.remove(Keys.PasswordLegacy) }
        }

        ensureDefaultServerProfileIfMissing()
    }

    private suspend fun ensureDefaultServerProfileIfMissing() {
        val pref = context.dataStore.data.first()
        val profiles = parseProfiles(pref[Keys.ServerProfilesJson]).toMutableList()
        if (profiles.isNotEmpty()) return

        val host = pref[Keys.Host].orEmpty().trim()
        if (host.isBlank()) return

        val profile = ServerProfile(
            id = generateProfileId(),
            name = buildProfileName(
                requestedName = "",
                host = host,
                index = 1,
            ),
            host = host,
            port = (pref[Keys.Port] ?: 8080).coerceIn(1, 65535),
            useHttps = pref[Keys.UseHttps] ?: false,
            username = pref[Keys.Username] ?: "admin",
            refreshSeconds = (pref[Keys.RefreshSeconds] ?: 5).coerceIn(5, 120),
        )
        profiles += profile

        val password = secureCredentials.getPassword()
        if (password.isNotBlank()) {
            secureCredentials.savePasswordForProfile(profile.id, password)
        }

        context.dataStore.edit { target ->
            target[Keys.ServerProfilesJson] = gson.toJson(profiles)
            target[Keys.ActiveServerProfileId] = profile.id
        }
    }

    private fun parseProfiles(raw: String?): List<ServerProfile> {
        val text = raw.orEmpty().trim()
        if (text.isBlank()) return emptyList()
        return runCatching {
            gson.fromJson<List<ServerProfile>>(text, serverProfileListType)
                .orEmpty()
                .mapNotNull { profile ->
                    val id = profile.id.trim()
                    val host = profile.host.trim()
                    if (id.isBlank() || host.isBlank()) {
                        null
                    } else {
                        profile.copy(
                            name = profile.name.trim().ifBlank {
                                buildProfileName("", host, 0)
                            },
                            host = host,
                            port = profile.port.coerceIn(1, 65535),
                            username = profile.username.ifBlank { "admin" },
                            refreshSeconds = profile.refreshSeconds.coerceIn(5, 120),
                        )
                    }
                }
                .distinctBy { it.id }
        }.getOrDefault(emptyList())
    }

    private fun parseDailyUploadTrackingSnapshots(raw: String?): Map<String, DailyUploadTrackingSnapshot> {
        val text = raw.orEmpty().trim()
        if (text.isBlank()) return emptyMap()
        return runCatching {
            gson.fromJson<Map<String, DailyUploadTrackingSnapshot>>(text, dailyUploadTrackingMapType)
                .orEmpty()
                .mapKeys { it.key.trim() }
                .filterKeys { it.isNotBlank() }
                .mapValues { (_, snapshot) -> snapshot.normalized() }
        }.getOrDefault(emptyMap())
    }

    private fun parseDailyCountryUploadTrackingSnapshots(raw: String?): Map<String, DailyCountryUploadTrackingSnapshot> {
        val text = raw.orEmpty().trim()
        if (text.isBlank()) return emptyMap()
        return runCatching {
            gson.fromJson<Map<String, DailyCountryUploadTrackingSnapshot>>(text, dailyCountryUploadTrackingMapType)
                .orEmpty()
                .mapKeys { it.key.trim() }
                .filterKeys { it.isNotBlank() }
                .mapValues { (_, snapshot) -> snapshot.normalized() }
        }.getOrDefault(emptyMap())
    }

    private fun parseDashboardCacheSnapshots(raw: String?): Map<String, DashboardCacheSnapshot> {
        val text = raw.orEmpty().trim()
        if (text.isBlank()) return emptyMap()
        return runCatching {
            gson.fromJson<Map<String, DashboardCacheSnapshot>>(text, dashboardCacheMapType)
                .orEmpty()
                .mapKeys { it.key.trim() }
                .filterKeys { it.isNotBlank() }
                .mapValues { (_, snapshot) -> snapshot.normalized() }
        }.getOrDefault(emptyMap())
    }

    private fun resolvePassword(profileId: String): String {
        return if (profileId.isBlank()) {
            secureCredentials.getPassword()
        } else {
            secureCredentials.getPasswordForProfile(profileId).ifBlank {
                secureCredentials.getPassword()
            }
        }
    }

    private fun buildProfileName(
        requestedName: String,
        host: String,
        index: Int,
    ): String {
        val trimmedName = requestedName.trim()
        if (trimmedName.isNotBlank()) return trimmedName
        val fallbackHost = host.trim().ifBlank { "Server $index" }
        return fallbackHost
    }

    private fun generateProfileId(): String = UUID.randomUUID().toString()

    private fun DailyUploadTrackingSnapshot.normalized(): DailyUploadTrackingSnapshot {
        return copy(
            date = date.trim(),
            baselineByTorrent = baselineByTorrent
                .filterKeys { it.isNotBlank() }
                .mapValues { (_, value) -> value.coerceAtLeast(0L) },
            lastSeenByTorrent = lastSeenByTorrent
                .filterKeys { it.isNotBlank() }
                .mapValues { (_, value) -> value.coerceAtLeast(0L) },
        )
    }

    private fun DailyCountryUploadTrackingSnapshot.normalized(): DailyCountryUploadTrackingSnapshot {
        return copy(
            date = date.trim(),
            totalsByCountry = totalsByCountry
                .mapKeys { it.key.trim().uppercase() }
                .filterKeys { it.isNotBlank() }
                .mapValues { (_, value) -> value.coerceAtLeast(0L) },
            peerSnapshots = peerSnapshots
                .filterKeys { it.isNotBlank() }
                .mapValues { (_, snapshot) ->
                    snapshot.copy(
                        key = snapshot.key.trim(),
                        peerAddress = snapshot.peerAddress.trim(),
                        countryCode = snapshot.countryCode.trim().uppercase(),
                        countryName = snapshot.countryName.trim(),
                        uploadedBytes = snapshot.uploadedBytes.coerceAtLeast(0L),
                    )
                },
            lastSeenByTorrent = lastSeenByTorrent
                .filterKeys { it.isNotBlank() }
                .mapValues { (_, value) -> value.coerceAtLeast(0L) },
            recentSamples = recentSamples.map { sample ->
                sample.copy(
                    sampledTotals = sample.sampledTotals
                        .mapKeys { it.key.trim().uppercase() }
                        .filterKeys { it.isNotBlank() }
                        .mapValues { (_, value) -> value.coerceAtLeast(0L) },
                    peerCountsByCountry = sample.peerCountsByCountry
                        .mapKeys { it.key.trim().uppercase() }
                        .filterKeys { it.isNotBlank() }
                        .mapValues { (_, value) -> value.coerceAtLeast(0L) },
                )
            },
        )
    }

    private fun DashboardCacheSnapshot.normalized(): DashboardCacheSnapshot {
        return copy(
            dailyTagUploadDate = dailyTagUploadDate.trim(),
            dailyTagUploadStats = dailyTagUploadStats.map { it.copy(tag = it.tag.trim()) },
            dailyCountryUploadDate = dailyCountryUploadDate.trim(),
            dailyCountryUploadStats = dailyCountryUploadStats.map { record ->
                record.copy(
                    countryCode = record.countryCode.trim().uppercase(),
                    countryName = record.countryName.trim(),
                    uploadedBytes = record.uploadedBytes.coerceAtLeast(0L),
                )
            },
        )
    }

    private fun ConnectionSettings.toServerProfile(
        id: String,
        name: String,
    ): ServerProfile {
        return ServerProfile(
            id = id,
            name = name,
            host = host.trim(),
            port = port.coerceIn(1, 65535),
            useHttps = useHttps,
            username = username.trim().ifBlank { "admin" },
            refreshSeconds = refreshSeconds.coerceIn(5, 120),
        )
    }

    private fun Preferences.toSettings(securePassword: String): ConnectionSettings {
        return ConnectionSettings(
            host = this[Keys.Host] ?: "",
            port = this[Keys.Port] ?: 8080,
            useHttps = this[Keys.UseHttps] ?: false,
            username = this[Keys.Username] ?: "admin",
            password = securePassword,
            refreshSeconds = (this[Keys.RefreshSeconds] ?: 5).coerceIn(5, 120),
            appLanguage = runCatching {
                enumValueOf<AppLanguage>(this[Keys.AppLanguage].orEmpty())
            }.getOrDefault(AppLanguage.SYSTEM),
            appTheme = runCatching {
                enumValueOf<AppTheme>(this[Keys.AppTheme].orEmpty())
            }.getOrDefault(AppTheme.DARK),
            customBackgroundImagePath = this[Keys.CustomBackgroundImagePath].orEmpty(),
            customBackgroundToneIsLight = this[Keys.CustomBackgroundToneIsLight] ?: false,
            showSpeedTotals = this[Keys.ShowSpeedTotals] ?: true,
            enableServerGrouping = this[Keys.EnableServerGrouping] ?: true,
            showChartPanel = this[Keys.ShowChartPanel] ?: true,
            showCountryFlowCard = this[Keys.ShowCountryFlowCard] ?: true,
            showUploadDistributionCard = this[Keys.ShowUploadDistributionCard] ?: true,
            showCategoryDistributionCard = this[Keys.ShowCategoryDistributionCard] ?: true,
            dashboardCardOrder = this[Keys.DashboardCardOrder] ?: "country_flow,category_share,daily_upload",
            chartShowSiteName = this[Keys.ChartShowSiteName] ?: true,
            chartSortMode = runCatching {
                enumValueOf<ChartSortMode>(this[Keys.ChartSortMode].orEmpty())
            }.getOrDefault(ChartSortMode.TOTAL_SPEED),
            deleteFilesDefault = this[Keys.DeleteFilesDefault] ?: true,
            deleteFilesWhenNoSeeders = this[Keys.DeleteFilesWhenNoSeeders] ?: false,
            homeTorrentEntryHintDismissed = this[Keys.HomeTorrentEntryHintDismissed] ?: false,
            hasSeenDashboardHideHint = this[Keys.HasSeenDashboardHideHint] ?: false,
            hasSeenDashboardHiddenSnack = this[Keys.HasSeenDashboardHiddenSnack] ?: false,
        )
    }
}
