package com.hjw.qbremote.data

import androidx.datastore.preferences.core.Preferences
import com.hjw.qbremote.data.ConnectionPreferenceKeys as Keys
import java.util.UUID

internal fun ConnectionSettings.toServerProfile(
    id: String,
    name: String,
): ServerProfile {
    return ServerProfile(
        id = id,
        name = name,
        backendType = serverBackendType,
        host = host.trim(),
        port = port.coerceIn(1, 65535),
        useHttps = useHttps,
        username = username.trim().ifBlank { "admin" },
        refreshSeconds = refreshSeconds.coerceIn(5, 120),
    )
}

internal fun Preferences.toSettings(securePassword: String): ConnectionSettings {
    return ConnectionSettings(
        host = this[Keys.Host] ?: "",
        port = this[Keys.Port] ?: 8080,
        useHttps = this[Keys.UseHttps] ?: false,
        username = this[Keys.Username] ?: "admin",
        password = securePassword,
        serverBackendType = runCatching {
            enumValueOf<ServerBackendType>(this[Keys.ServerBackendType].orEmpty())
        }.getOrDefault(ServerBackendType.QBITTORRENT),
        refreshSeconds = (this[Keys.RefreshSeconds] ?: 5).coerceIn(5, 120),
        appLanguage = runCatching {
            enumValueOf<AppLanguage>(this[Keys.AppLanguage].orEmpty())
        }.getOrDefault(AppLanguage.SYSTEM),
        appTheme = runCatching {
            enumValueOf<AppTheme>(this[Keys.AppTheme].orEmpty())
        }.getOrDefault(AppTheme.DARK),
        customBackgroundImagePath = this[Keys.CustomBackgroundImagePath].orEmpty(),
        customBackgroundToneIsLight = this[Keys.CustomBackgroundToneIsLight] ?: false,
        deleteFilesDefault = this[Keys.DeleteFilesDefault] ?: true,
        deleteFilesWhenNoSeeders = this[Keys.DeleteFilesWhenNoSeeders] ?: false,
        completionNotificationsEnabled = this[Keys.CompletionNotificationsEnabled] ?: false,
        homeTorrentEntryHintDismissed = this[Keys.HomeTorrentEntryHintDismissed] ?: false,
        hasSeenDashboardHideHint = this[Keys.HasSeenDashboardHideHint] ?: false,
        hasSeenDashboardHiddenSnack = this[Keys.HasSeenDashboardHiddenSnack] ?: false,
        hasSeenServerStackReorderHint = this[Keys.HasSeenServerStackReorderHint] ?: false,
        hasSeenServerDashboardSwipeHint = this[Keys.HasSeenServerDashboardSwipeHint] ?: false,
        hasSeenServerDashboardCardHint = this[Keys.HasSeenServerDashboardCardHint] ?: false,
    )
}

internal fun buildProfileName(
    requestedName: String,
    host: String,
    index: Int,
): String {
    val trimmedName = requestedName.trim()
    if (trimmedName.isNotBlank()) return trimmedName
    val fallbackHost = host.trim().ifBlank { "Server $index" }
    return fallbackHost
}

internal fun generateProfileId(): String = UUID.randomUUID().toString()
