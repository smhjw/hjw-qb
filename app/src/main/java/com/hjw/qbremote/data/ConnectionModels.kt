package com.hjw.qbremote.data

import com.hjw.qbremote.data.model.CountryPeerSnapshot
import com.hjw.qbremote.data.model.CountryPeerCountRecord
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.data.model.TransferInfo
import java.net.URI

data class ConnectionSettings(
    val host: String = "",
    val port: Int = 8080,
    val useHttps: Boolean = false,
    val username: String = "admin",
    val password: String = "",
    val serverBackendType: ServerBackendType = ServerBackendType.QBITTORRENT,
    val refreshSeconds: Int = 5,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val appTheme: AppTheme = AppTheme.DARK,
    val customBackgroundImagePath: String = "",
    val customBackgroundToneIsLight: Boolean = false,
    val deleteFilesDefault: Boolean = true,
    val deleteFilesWhenNoSeeders: Boolean = false,
    val completionNotificationsEnabled: Boolean = false,
    val homeTorrentEntryHintDismissed: Boolean = false,
    val hasSeenDashboardHideHint: Boolean = false,
    val hasSeenDashboardHiddenSnack: Boolean = false,
    val hasSeenServerStackReorderHint: Boolean = false,
    val hasSeenServerDashboardSwipeHint: Boolean = false,
    val hasSeenServerDashboardCardHint: Boolean = false,
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
    val backendType: ServerBackendType,
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

data class DeleteServerProfileResult(
    val deletedProfileId: String,
    val activeProfileId: String? = null,
    val settings: ConnectionSettings? = null,
)

data class DailyUploadTrackingSnapshot(
    val date: String = "",
    val totalsByTag: Map<String, Long> = emptyMap(),
    val countedTagsByTorrent: Map<String, List<String>> = emptyMap(),
    val lastSeenByTorrent: Map<String, Long> = emptyMap(),
)

data class DailyCountryUploadTrackingSnapshot(
    val date: String = "",
    val totalsByCountry: Map<String, Long> = emptyMap(),
    val peerSnapshots: Map<String, CountryPeerSnapshot> = emptyMap(),
    val lastSeenByTorrent: Map<String, Long> = emptyMap(),
)

data class HomeSpeedHistoryPoint(
    val timestamp: Long = 0L,
    val uploadSpeed: Long = 0L,
    val downloadSpeed: Long = 0L,
    val onlineServerCount: Int = 0,
)

data class HomeAggregateSpeedHistorySnapshot(
    val scopeKey: String = "",
    val points: List<HomeSpeedHistoryPoint> = emptyList(),
)

data class DashboardCacheSnapshot(
    val transferInfo: TransferInfo = TransferInfo(),
    val torrents: List<TorrentInfo> = emptyList(),
    val dailyTagUploadDate: String = "",
    val dailyTagUploadStats: List<CachedDailyTagUploadStat> = emptyList(),
    val dailyCountryUploadDate: String = "",
    val dailyCountryUploadStats: List<CountryPeerCountRecord> = emptyList(),
)

data class DashboardServerSnapshotStore(
    val snapshots: Map<String, CachedDashboardServerSnapshot> = emptyMap(),
)

data class CachedDailyTagUploadStat(
    val tag: String = "",
    val uploadedBytes: Long = 0L,
    val torrentCount: Int = 0,
    val isNoTag: Boolean = false,
)

data class ServerDashboardPreferences(
    val visibleCards: List<String> = listOf(
        "country_flow",
        "category_share",
        "daily_upload",
        "tracker_site",
        "share_ratio_distribution",
    ),
    val cardOrder: String = "country_flow,category_share,daily_upload,tracker_site,share_ratio_distribution",
    val hasSeenStackReorderHint: Boolean = false,
    val hasSeenDashboardSwipeHint: Boolean = false,
    val hasSeenDashboardCardHint: Boolean = false,
)

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
