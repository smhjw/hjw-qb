package com.hjw.qbremote.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.hjw.qbremote.data.ConnectionPreferenceKeys as Keys
import com.hjw.qbremote.notifications.mergeProfileScopedCompletionStates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Without a corruption handler a damaged preferences file would make every read
// throw CorruptionException forever - the app could never start again. Losing the
// cached settings once and starting clean is strictly better than a crash loop.
private val Context.dataStore by preferencesDataStore(
    name = "qb_connection",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

class ConnectionStore(internal val context: Context) {
    private val secureCredentials = SecureCredentialStore(context)
    private val gson = connectionStoreGson

    private val uiPreferencePatchLock = Mutex()
    private var pendingUiPreferencePatch: UiPreferencePatch? = null
    private val uiPreferenceWriterLock = Mutex()

    val settingsFlow: Flow<ConnectionSettings> = context.dataStore.data.map { pref ->
        val activeProfileId = pref[Keys.ActiveServerProfileId].orEmpty()
        pref.toSettings(resolvePassword(activeProfileId))
    }.distinctUntilChanged()

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
    }.distinctUntilChanged()

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
            applyConnectionSettingsSnapshot(
                target = target,
                settings = settings,
                resolvedActiveProfileId = resolvedActiveProfileId,
                profilesJson = gson.toJson(profiles),
            )
        }
    }

    suspend fun saveUiPreferences(patch: UiPreferencePatch) {
        uiPreferencePatchLock.withLock {
            pendingUiPreferencePatch = pendingUiPreferencePatch
                ?.let { mergeUiPreferencePatches(it, patch) }
                ?: patch
        }
        uiPreferenceWriterLock.withLock {
            val toWrite = uiPreferencePatchLock.withLock {
                pendingUiPreferencePatch.also { pendingUiPreferencePatch = null }
            } ?: return
            context.dataStore.edit { target -> applyUiPreferencePatch(target, toWrite) }
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
            target[Keys.ServerBackendType] = profile.backendType.name
            target[Keys.RefreshSeconds] = profile.refreshSeconds
        }

        saveServerDashboardPreferences(
            profileId = id,
            preferences = defaultServerDashboardPreferences(settings),
        )

        return profile
    }

    suspend fun cleanupLegacyGlobalChartSettingsIfNeeded() {
        val pref = context.dataStore.data.first()
        val legacyKeysPresent = legacyGlobalChartSettingKeys().any { key -> pref.contains(key) }
        if (!legacyKeysPresent) return

        context.dataStore.edit { target ->
            removeLegacyGlobalChartSettings(target)
        }
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
            serverBackendType = profile.backendType,
            refreshSeconds = profile.refreshSeconds,
        )

        secureCredentials.savePassword(password)

        context.dataStore.edit { target ->
            target[Keys.ActiveServerProfileId] = profile.id
            target[Keys.Host] = profile.host
            target[Keys.Port] = profile.port
            target[Keys.UseHttps] = profile.useHttps
            target[Keys.Username] = profile.username
            target[Keys.ServerBackendType] = profile.backendType.name
            target[Keys.RefreshSeconds] = profile.refreshSeconds
        }

        return switched
    }

    suspend fun updateServerProfile(
        profileId: String,
        name: String,
        settings: ConnectionSettings,
        passwordOverride: String? = null,
    ): ServerProfile {
        require(profileId.isNotBlank()) { "Invalid server profile id." }
        val pref = context.dataStore.data.first()
        val profiles = parseProfiles(pref[Keys.ServerProfilesJson]).toMutableList()
        val index = profiles.indexOfFirst { it.id == profileId }
        require(index >= 0) { "Server profile not found." }

        val updatedProfile = settings.toServerProfile(
            id = profileId,
            name = buildProfileName(
                requestedName = name,
                host = settings.host,
                index = index + 1,
            ),
        )
        profiles[index] = updatedProfile

        val activeProfileId = pref[Keys.ActiveServerProfileId].orEmpty()
        if (passwordOverride != null) {
            secureCredentials.savePasswordForProfile(profileId, passwordOverride)
            if (activeProfileId == profileId) {
                secureCredentials.savePassword(passwordOverride)
            }
        }

        context.dataStore.edit { target ->
            target[Keys.ServerProfilesJson] = gson.toJson(profiles)
            if (activeProfileId == profileId) {
                target[Keys.Host] = updatedProfile.host
                target[Keys.Port] = updatedProfile.port
                target[Keys.UseHttps] = updatedProfile.useHttps
                target[Keys.Username] = updatedProfile.username
                target[Keys.ServerBackendType] = updatedProfile.backendType.name
                target[Keys.RefreshSeconds] = updatedProfile.refreshSeconds
            }
        }

        return updatedProfile
    }

    suspend fun reorderServerProfiles(profileIds: List<String>): List<ServerProfile> {
        val normalizedIds = profileIds.map { it.trim() }.filter { it.isNotBlank() }
        if (normalizedIds.isEmpty()) return serverProfilesFlow.first().profiles
        val pref = context.dataStore.data.first()
        val currentProfiles = parseProfiles(pref[Keys.ServerProfilesJson])
        if (currentProfiles.isEmpty()) return emptyList()
        val profilesById = currentProfiles.associateBy { it.id }
        val reordered = buildList<ServerProfile> {
            normalizedIds.forEach { id ->
                profilesById[id]?.let(::add)
            }
            currentProfiles.forEach { profile ->
                if (none { existing -> existing.id == profile.id }) add(profile)
            }
        }
        context.dataStore.edit { target ->
            target[Keys.ServerProfilesJson] = gson.toJson(reordered)
        }
        return reordered
    }

    suspend fun deleteServerProfile(profileId: String): DeleteServerProfileResult {
        require(profileId.isNotBlank()) { "Invalid server profile id." }
        val pref = context.dataStore.data.first()
        val profiles = parseProfiles(pref[Keys.ServerProfilesJson]).toMutableList()
        val index = profiles.indexOfFirst { it.id == profileId }
        require(index >= 0) { "Server profile not found." }

        val removed = profiles.removeAt(index)
        val currentActiveProfileId = pref[Keys.ActiveServerProfileId].orEmpty()
        val deletingActiveProfile = currentActiveProfileId == profileId

        secureCredentials.removePasswordForProfile(profileId)
        removeServerDashboardPreferences(profileId)

        var nextActiveProfileId: String? = currentActiveProfileId.takeIf { it.isNotBlank() && it != profileId }
        var nextSettings: ConnectionSettings? = null

        if (deletingActiveProfile) {
            val nextProfile = profiles.firstOrNull()
            if (nextProfile != null) {
                nextActiveProfileId = nextProfile.id
                val password = resolvePassword(nextProfile.id)
                secureCredentials.savePassword(password)
                nextSettings = pref.toSettings(password).copy(
                    host = nextProfile.host,
                    port = nextProfile.port,
                    useHttps = nextProfile.useHttps,
                    username = nextProfile.username,
                    password = password,
                    serverBackendType = nextProfile.backendType,
                    refreshSeconds = nextProfile.refreshSeconds,
                )
            } else {
                secureCredentials.clearPassword()
                nextActiveProfileId = null
                nextSettings = pref.toSettings("").copy(
                    host = "",
                    port = 8080,
                    useHttps = false,
                    username = "admin",
                    password = "",
                    serverBackendType = ServerBackendType.QBITTORRENT,
                    refreshSeconds = 5,
                )
            }
        }

        context.dataStore.edit { target ->
            target[Keys.ServerProfilesJson] = gson.toJson(profiles)
            val dailyUploadSnapshots = parseDailyUploadTrackingSnapshots(target[Keys.DailyUploadTrackingJson]).toMutableMap()
            dailyUploadSnapshots.remove("profile:$profileId")
            target[Keys.DailyUploadTrackingJson] = gson.toJson(dailyUploadSnapshots)

            val dailyCountrySnapshots = parseDailyCountryUploadTrackingSnapshots(
                target[Keys.DailyCountryUploadTrackingJson]
            ).toMutableMap()
            dailyCountrySnapshots.remove("profile:$profileId")
            target[Keys.DailyCountryUploadTrackingJson] = gson.toJson(dailyCountrySnapshots)

            val dashboardCaches = parseDashboardCacheSnapshots(target[Keys.DashboardCacheJson]).toMutableMap()
            dashboardCaches.remove("profile:$profileId")
            target[Keys.DashboardCacheJson] = gson.toJson(dashboardCaches)

            val dashboardServerSnapshots = parseDashboardServerSnapshots(
                target[Keys.DashboardServerSnapshotsJson]
            ).toMutableMap()
            dashboardServerSnapshots.remove(profileId)
            target[Keys.DashboardServerSnapshotsJson] = gson.toJson(dashboardServerSnapshots)

            if (deletingActiveProfile) {
                if (nextActiveProfileId.isNullOrBlank()) {
                    target.remove(Keys.ActiveServerProfileId)
                    target[Keys.Host] = ""
                    target[Keys.Port] = 8080
                    target[Keys.UseHttps] = false
                    target[Keys.Username] = "admin"
                    target[Keys.ServerBackendType] = ServerBackendType.QBITTORRENT.name
                    target[Keys.RefreshSeconds] = 5
                } else {
                    val nextProfile = profiles.first { it.id == nextActiveProfileId }
                    target[Keys.ActiveServerProfileId] = nextActiveProfileId
                    target[Keys.Host] = nextProfile.host
                    target[Keys.Port] = nextProfile.port
                    target[Keys.UseHttps] = nextProfile.useHttps
                    target[Keys.Username] = nextProfile.username
                    target[Keys.ServerBackendType] = nextProfile.backendType.name
                    target[Keys.RefreshSeconds] = nextProfile.refreshSeconds
                }
            }
            target.remove(Keys.HomeAggregateSpeedHistoryJson)
        }

        return DeleteServerProfileResult(
            deletedProfileId = removed.id,
            activeProfileId = nextActiveProfileId,
            settings = nextSettings,
        )
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
            val updated = upsertNormalizedEntryIfChanged(
                entries = snapshots,
                rawKey = scopeKey,
                value = snapshot.normalized(),
            ) ?: return@edit
            target[Keys.DailyUploadTrackingJson] = gson.toJson(updated)
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
            val updated = upsertNormalizedEntryIfChanged(
                entries = snapshots,
                rawKey = scopeKey,
                value = snapshot.normalized(),
            ) ?: return@edit
            target[Keys.DailyCountryUploadTrackingJson] = gson.toJson(updated)
        }
    }

    suspend fun loadCompletionNotificationStates(): Map<String, String> {
        val pref = context.dataStore.data.first()
        return parseCompletionNotificationStates(pref[Keys.CompletionNotificationStatesJson])
    }

    suspend fun saveCompletionNotificationStates(states: Map<String, String>) {
        context.dataStore.edit { target ->
            if (states.isEmpty()) {
                target.remove(Keys.CompletionNotificationStatesJson)
            } else {
                target[Keys.CompletionNotificationStatesJson] = gson.toJson(states)
            }
        }
    }

    suspend fun saveCompletionNotificationStatesForProfile(
        profileId: String,
        states: Map<String, String>,
    ) {
        val normalizedProfileId = profileId.trim()
        if (normalizedProfileId.isBlank()) return
        context.dataStore.edit { target ->
            val persisted = parseCompletionNotificationStates(target[Keys.CompletionNotificationStatesJson])
            val updated = mergeProfileScopedCompletionStates(
                persisted = persisted,
                profileId = normalizedProfileId,
                states = states,
            ) ?: return@edit
            target[Keys.CompletionNotificationStatesJson] = gson.toJson(updated)
        }
    }

    suspend fun loadDashboardCacheSnapshot(scopeKey: String): DashboardCacheSnapshot? {
        if (scopeKey.isBlank()) return null
        val pref = context.dataStore.data.first()
        val raw = pref[Keys.DashboardCacheJson]
        if (isOversizedDashboardPersistencePayload(raw)) {
            context.dataStore.edit { target ->
                target.remove(Keys.DashboardCacheJson)
            }
            return null
        }
        val snapshots = parseDashboardCacheSnapshots(raw)
        return snapshots[scopeKey]
    }

    suspend fun saveDashboardCacheSnapshot(
        scopeKey: String,
        snapshot: DashboardCacheSnapshot,
    ) {
        if (scopeKey.isBlank()) return
        context.dataStore.edit { target ->
            val snapshots = parseDashboardCacheSnapshots(target[Keys.DashboardCacheJson]).toMutableMap()
            val updated = upsertNormalizedEntryIfChanged(
                entries = snapshots,
                rawKey = scopeKey,
                value = sanitizeDashboardCacheForPersistence(snapshot).normalized(),
            ) ?: return@edit
            target[Keys.DashboardCacheJson] = gson.toJson(updated)
        }
    }

    suspend fun loadDashboardServerSnapshots(): Map<String, CachedDashboardServerSnapshot> {
        val pref = context.dataStore.data.first()
        val raw = pref[Keys.DashboardServerSnapshotsJson]
        if (isOversizedDashboardPersistencePayload(raw)) {
            context.dataStore.edit { target ->
                target.remove(Keys.DashboardServerSnapshotsJson)
            }
            return emptyMap()
        }
        return parseDashboardServerSnapshots(raw).mapValues { (_, snapshot) ->
            sanitizeDashboardServerSnapshotForPersistence(snapshot)
        }
    }

    suspend fun loadHomeAggregateSpeedHistorySnapshot(scopeKey: String): HomeAggregateSpeedHistorySnapshot? {
        val normalizedScopeKey = scopeKey.trim()
        if (normalizedScopeKey.isBlank()) return null
        val pref = context.dataStore.data.first()
        val snapshot = parseHomeAggregateSpeedHistorySnapshot(pref[Keys.HomeAggregateSpeedHistoryJson])
        return if (snapshot.scopeKey == normalizedScopeKey) snapshot else null
    }

    suspend fun saveHomeAggregateSpeedHistorySnapshot(
        scopeKey: String,
        snapshot: HomeAggregateSpeedHistorySnapshot,
    ) {
        context.dataStore.edit { target ->
            val normalized = normalizeHomeAggregateSpeedHistoryForPersistence(
                scopeKey = scopeKey,
                snapshot = snapshot,
            )
            if (normalized == null) {
                if (target.contains(Keys.HomeAggregateSpeedHistoryJson)) {
                    target.remove(Keys.HomeAggregateSpeedHistoryJson)
                }
            } else if (parseHomeAggregateSpeedHistorySnapshot(target[Keys.HomeAggregateSpeedHistoryJson]) != normalized) {
                target[Keys.HomeAggregateSpeedHistoryJson] = gson.toJson(normalized)
            }
        }
    }

    suspend fun loadServerDashboardPreferences(): Map<String, ServerDashboardPreferences> {
        val pref = context.dataStore.data.first()
        return parseServerDashboardPreferences(pref[Keys.ServerDashboardPreferencesJson])
    }

    suspend fun loadServerDashboardPreferences(profileId: String): ServerDashboardPreferences? {
        if (profileId.isBlank()) return null
        return loadServerDashboardPreferences()[profileId.trim()]
    }

    suspend fun saveServerDashboardPreferences(
        profileId: String,
        preferences: ServerDashboardPreferences,
    ) {
        val normalizedProfileId = profileId.trim()
        if (normalizedProfileId.isBlank()) return
        context.dataStore.edit { target ->
            val preferencesById = parseServerDashboardPreferences(target[Keys.ServerDashboardPreferencesJson]).toMutableMap()
            preferencesById[normalizedProfileId] = preferences.normalized()
            target[Keys.ServerDashboardPreferencesJson] = gson.toJson(preferencesById)
        }
    }

    suspend fun updateServerDashboardPreferences(
        profileId: String,
        fallbackSettings: ConnectionSettings,
        update: (ServerDashboardPreferences) -> ServerDashboardPreferences,
    ): ServerDashboardPreferences {
        val current = loadServerDashboardPreferences(profileId)
            ?: defaultServerDashboardPreferences(fallbackSettings)
        val next = update(current).normalized()
        saveServerDashboardPreferences(profileId, next)
        return next
    }

    suspend fun removeServerDashboardPreferences(profileId: String) {
        if (profileId.isBlank()) return
        context.dataStore.edit { target ->
            val preferencesById = parseServerDashboardPreferences(target[Keys.ServerDashboardPreferencesJson]).toMutableMap()
            preferencesById.remove(profileId.trim())
            target[Keys.ServerDashboardPreferencesJson] = gson.toJson(preferencesById)
        }
    }

    suspend fun saveDashboardServerSnapshot(snapshot: CachedDashboardServerSnapshot) {
        val profileId = snapshot.profileId.trim()
        if (profileId.isBlank()) return
        context.dataStore.edit { target ->
            val snapshots = parseDashboardServerSnapshots(target[Keys.DashboardServerSnapshotsJson]).toMutableMap()
            val updated = upsertNormalizedEntryIfChanged(
                entries = snapshots,
                rawKey = profileId,
                value = sanitizeDashboardServerSnapshotForPersistence(snapshot).normalized(),
            ) ?: return@edit
            target[Keys.DashboardServerSnapshotsJson] = gson.toJson(updated)
        }
    }

    suspend fun removeDashboardServerSnapshot(profileId: String) {
        if (profileId.isBlank()) return
        context.dataStore.edit { target ->
            val snapshots = parseDashboardServerSnapshots(target[Keys.DashboardServerSnapshotsJson]).toMutableMap()
            snapshots.remove(profileId)
            target[Keys.DashboardServerSnapshotsJson] = gson.toJson(snapshots)
        }
    }

    suspend fun loadSettingsForProfile(profileId: String): ConnectionSettings? {
        if (profileId.isBlank()) return null
        val pref = context.dataStore.data.first()
        val profile = parseProfiles(pref[Keys.ServerProfilesJson]).firstOrNull { it.id == profileId } ?: return null
        return pref.toSettings(resolvePassword(profileId)).copy(
            host = profile.host,
            port = profile.port,
            useHttps = profile.useHttps,
            username = profile.username,
            serverBackendType = profile.backendType,
            refreshSeconds = profile.refreshSeconds,
        )
    }

    suspend fun migrateLegacyPasswordIfNeeded() {
        val prefBefore = context.dataStore.data.first()
        val legacy = prefBefore[Keys.PasswordLegacy].orEmpty()
        if (legacy.isNotBlank()) {
            secureCredentials.savePassword(legacy)
            context.dataStore.edit { it.remove(Keys.PasswordLegacy) }
        }

        ensureDefaultServerProfileIfMissing()
        migrateLegacyDashboardHintsIfNeeded()
        cleanupDeprecatedDashboardTrendHistoryIfNeeded()
    }

    private suspend fun ensureDefaultServerProfileIfMissing() {
        val pref = context.dataStore.data.first()
        val profiles = parseProfiles(pref[Keys.ServerProfilesJson]).toMutableList()
        if (profiles.isNotEmpty()) return

        val profile = buildDefaultProfileFromLegacyPreferences(pref) ?: return
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

    // Falling back to the global password when a profile has none is intentional
    // compatibility with the single-server era; both credentials belong to the same user.
    private fun resolvePassword(profileId: String): String {
        return if (profileId.isBlank()) {
            secureCredentials.getPassword()
        } else {
            secureCredentials.getPasswordForProfile(profileId).ifBlank {
                secureCredentials.getPassword()
            }
        }
    }

    private suspend fun migrateLegacyDashboardHintsIfNeeded() {
        val pref = context.dataStore.data.first()
        val migratedHints = resolveMigratedDashboardHints(
            existingStackHint = pref[Keys.HasSeenServerStackReorderHint] ?: false,
            existingSwipeHint = pref[Keys.HasSeenServerDashboardSwipeHint] ?: false,
            existingCardHint = pref[Keys.HasSeenServerDashboardCardHint] ?: false,
            preferences = pref,
        ) ?: return

        context.dataStore.edit { target ->
            target[Keys.HasSeenServerStackReorderHint] = migratedHints.first
            target[Keys.HasSeenServerDashboardSwipeHint] = migratedHints.second
            target[Keys.HasSeenServerDashboardCardHint] = migratedHints.third
        }
    }

    private suspend fun cleanupDeprecatedDashboardTrendHistoryIfNeeded() {
        val pref = context.dataStore.data.first()
        if (!pref.contains(Keys.DeprecatedDashboardTrendHistoryJson)) return
        context.dataStore.edit { target ->
            target.remove(Keys.DeprecatedDashboardTrendHistoryJson)
        }
    }
}

internal fun applyConnectionSettingsSnapshot(
    target: MutablePreferences,
    settings: ConnectionSettings,
    resolvedActiveProfileId: String,
    profilesJson: String,
) {
    target[Keys.Host] = settings.host
    target[Keys.Port] = settings.port
    target[Keys.UseHttps] = settings.useHttps
    target[Keys.Username] = settings.username
    target[Keys.ServerBackendType] = settings.serverBackendType.name
    target[Keys.RefreshSeconds] = settings.refreshSeconds
    target.remove(Keys.PasswordLegacy)
    if (resolvedActiveProfileId.isNotBlank()) {
        target[Keys.ActiveServerProfileId] = resolvedActiveProfileId
    }
    target[Keys.ServerProfilesJson] = profilesJson
}

