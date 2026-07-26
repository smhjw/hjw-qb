package com.hjw.qbremote.ui

import com.hjw.qbremote.data.BackendConnectionError
import com.hjw.qbremote.data.ServerBackendType

internal fun userFacingConnectionMessage(error: Throwable): String {
    return when (error) {
        is BackendConnectionError.WrongBackend -> {
            "服务器类型不匹配，目标看起来是 ${backendDisplayName(error.detected)}。"
        }

        is BackendConnectionError.RpcPathNotFound -> {
            if (error.failureSummary.isBlank()) {
                "Transmission RPC 路径未找到。"
            } else {
                "Transmission RPC 路径未找到。${error.failureSummary}"
            }
        }

        is BackendConnectionError.AuthFailed -> "${backendDisplayName(error.backendType)} 认证失败。"
        else -> error.message?.takeIf { it.isNotBlank() } ?: "刷新失败"
    }
}

internal fun backendDisplayName(type: ServerBackendType): String {
    return when (type) {
        ServerBackendType.QBITTORRENT -> "qBittorrent"
        ServerBackendType.TRANSMISSION -> "Transmission"
    }
}

internal fun shouldSuppressRefreshError(message: String?): Boolean {
    val normalized = message?.trim()?.lowercase().orEmpty()
    if (normalized.isBlank()) return false
    return normalized.contains("unable to resolve host") ||
        normalized.contains("no address associated with hostname")
}
