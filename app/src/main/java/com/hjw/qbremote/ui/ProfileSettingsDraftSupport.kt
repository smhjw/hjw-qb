package com.hjw.qbremote.ui

import com.hjw.qbremote.data.ConnectionSettings
import com.hjw.qbremote.data.ServerBackendType

internal fun buildProfileSettingsDraft(
    baseSettings: ConnectionSettings,
    backendType: ServerBackendType,
    host: String,
    port: String,
    useHttps: Boolean,
    username: String,
    password: String,
    refreshSeconds: String,
): ConnectionSettings {
    val normalizedHost = host.trim()
    val parsed = parseHostInputHints(normalizedHost)
    val defaultPort = when (backendType) {
        ServerBackendType.QBITTORRENT -> 8080
        ServerBackendType.TRANSMISSION -> 9091
    }
    val resolvedPort = parsed?.port ?: (port.toIntOrNull() ?: defaultPort)
    val resolvedUseHttps = parsed?.useHttps ?: useHttps
    val nextSettings = baseSettings.copy(
        host = normalizedHost,
        port = resolvedPort.coerceIn(1, 65535),
        useHttps = resolvedUseHttps,
        username = username.trim(),
        password = password,
        serverBackendType = backendType,
        refreshSeconds = (refreshSeconds.toIntOrNull() ?: 5).coerceIn(5, 120),
    )
    require(nextSettings.host.isNotBlank()) { "主机不能为空" }
    require(nextSettings.username.isNotBlank()) { "用户名不能为空" }
    return nextSettings
}
