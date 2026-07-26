package com.hjw.qbremote.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

internal val connectionStoreGson = Gson()

private val serverProfileListType = object : TypeToken<List<ServerProfile>>() {}.type
private val dailyUploadTrackingMapType =
    object : TypeToken<Map<String, DailyUploadTrackingSnapshot>>() {}.type
private val dailyCountryUploadTrackingMapType =
    object : TypeToken<Map<String, DailyCountryUploadTrackingSnapshot>>() {}.type
private val dashboardCacheMapType =
    object : TypeToken<Map<String, DashboardCacheSnapshot>>() {}.type
private val dashboardServerSnapshotMapType =
    object : TypeToken<Map<String, CachedDashboardServerSnapshot>>() {}.type
private val serverDashboardPreferencesMapType =
    object : TypeToken<Map<String, ServerDashboardPreferences>>() {}.type
private val completionNotificationStatesType =
    object : TypeToken<Map<String, String>>() {}.type

internal fun parseProfiles(raw: String?): List<ServerProfile> {
    val text = raw.orEmpty().trim()
    if (text.isBlank()) return emptyList()
    return runCatching {
        connectionStoreGson.fromJson(text, com.google.gson.JsonArray::class.java)
            ?.mapNotNull { element ->
                val obj = element?.asJsonObject ?: return@mapNotNull null
                val id = obj.get("id")?.asString.orEmpty().trim()
                val host = obj.get("host")?.asString.orEmpty().trim()
                if (id.isBlank() || host.isBlank()) return@mapNotNull null
                val backendType = runCatching {
                    enumValueOf<ServerBackendType>(
                        obj.get("backendType")?.asString.orEmpty().ifBlank {
                            ServerBackendType.QBITTORRENT.name
                        }
                    )
                }.getOrDefault(ServerBackendType.QBITTORRENT)
                ServerProfile(
                    id = id,
                    name = obj.get("name")?.asString.orEmpty().trim().ifBlank {
                        buildProfileName("", host, 0)
                    },
                    backendType = backendType,
                    host = host,
                    port = (obj.get("port")?.asInt ?: 8080).coerceIn(1, 65535),
                    useHttps = obj.get("useHttps")?.asBoolean ?: false,
                    username = obj.get("username")?.asString.orEmpty().ifBlank { "admin" },
                    refreshSeconds = (obj.get("refreshSeconds")?.asInt ?: 5).coerceIn(5, 120),
                )
            }
            .orEmpty()
            .distinctBy { it.id }
    }.getOrDefault(emptyList())
}

internal fun parseDailyUploadTrackingSnapshots(raw: String?): Map<String, DailyUploadTrackingSnapshot> {
    val text = raw.orEmpty().trim()
    if (text.isBlank()) return emptyMap()
    return runCatching {
        connectionStoreGson.fromJson<Map<String, DailyUploadTrackingSnapshot>>(text, dailyUploadTrackingMapType)
            .orEmpty()
            .mapKeys { it.key.trim() }
            .filterKeys { it.isNotBlank() }
            .mapValues { (_, snapshot) -> snapshot.normalized() }
    }.getOrDefault(emptyMap())
}

internal fun parseDailyCountryUploadTrackingSnapshots(raw: String?): Map<String, DailyCountryUploadTrackingSnapshot> {
    val text = raw.orEmpty().trim()
    if (text.isBlank()) return emptyMap()
    return runCatching {
        connectionStoreGson.fromJson<Map<String, DailyCountryUploadTrackingSnapshot>>(text, dailyCountryUploadTrackingMapType)
            .orEmpty()
            .mapKeys { it.key.trim() }
            .filterKeys { it.isNotBlank() }
            .mapValues { (_, snapshot) -> snapshot.normalized() }
    }.getOrDefault(emptyMap())
}

internal fun parseCompletionNotificationStates(raw: String?): Map<String, String> {
    if (raw.isNullOrBlank()) return emptyMap()
    return runCatching {
        connectionStoreGson.fromJson<Map<String, String>>(raw, completionNotificationStatesType)
            ?: emptyMap()
    }.getOrDefault(emptyMap())
}

internal fun parseDashboardCacheSnapshots(raw: String?): Map<String, DashboardCacheSnapshot> {
    val text = raw.orEmpty().trim()
    if (text.isBlank()) return emptyMap()
    return runCatching {
        connectionStoreGson.fromJson<Map<String, DashboardCacheSnapshot>>(text, dashboardCacheMapType)
            .orEmpty()
            .mapKeys { it.key.trim() }
            .filterKeys { it.isNotBlank() }
            .mapValues { (_, snapshot) -> snapshot.normalized() }
    }.getOrDefault(emptyMap())
}

internal fun parseHomeAggregateSpeedHistorySnapshot(raw: String?): HomeAggregateSpeedHistorySnapshot {
    val text = raw.orEmpty().trim()
    if (text.isBlank()) return HomeAggregateSpeedHistorySnapshot()
    return runCatching {
        connectionStoreGson.fromJson(text, HomeAggregateSpeedHistorySnapshot::class.java)
            ?.normalized()
    }.getOrNull() ?: HomeAggregateSpeedHistorySnapshot()
}

internal fun parseServerDashboardPreferences(raw: String?): Map<String, ServerDashboardPreferences> {
    val text = raw.orEmpty().trim()
    if (text.isBlank()) return emptyMap()
    return runCatching {
        connectionStoreGson.fromJson<Map<String, ServerDashboardPreferences>>(text, serverDashboardPreferencesMapType)
            .orEmpty()
            .mapKeys { it.key.trim() }
            .filterKeys { it.isNotBlank() }
            .mapValues { (_, preferences) -> preferences.normalized() }
    }.getOrDefault(emptyMap())
}

internal fun parseDashboardServerSnapshots(raw: String?): Map<String, CachedDashboardServerSnapshot> {
    val text = raw.orEmpty().trim()
    if (text.isBlank()) return emptyMap()
    return runCatching {
        connectionStoreGson.fromJson<Map<String, CachedDashboardServerSnapshot>>(text, dashboardServerSnapshotMapType)
            .orEmpty()
            .mapKeys { it.key.trim() }
            .filterKeys { it.isNotBlank() }
            .mapValues { (_, snapshot) -> snapshot.normalized() }
    }.getOrDefault(emptyMap())
}
