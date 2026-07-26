package com.hjw.qbremote.data

import java.net.URI

internal fun isLikelyPrivateHost(host: String): Boolean {
    val normalized = host.trim().removePrefix("[").removeSuffix("]").lowercase()
    if (normalized.isBlank()) return false
    if (normalized == "localhost") return true
    if (
        normalized.endsWith(".local") ||
        normalized.endsWith(".lan") ||
        normalized.endsWith(".internal") ||
        normalized.endsWith(".home.arpa")
    ) {
        return true
    }
    if (normalized.contains(':')) {
        return normalized == "::1" ||
            normalized.startsWith("fe80:") ||
            normalized.startsWith("fc") ||
            normalized.startsWith("fd")
    }
    val octets = parseIpv4Octets(normalized) ?: return false
    return when {
        octets[0] == 127 -> true
        octets[0] == 10 -> true
        octets[0] == 172 && octets[1] in 16..31 -> true
        octets[0] == 192 && octets[1] == 168 -> true
        octets[0] == 169 && octets[1] == 254 -> true
        else -> false
    }
}

internal fun connectionUsesInsecurePublicEndpoint(settings: ConnectionSettings): Boolean {
    val baseUrl = try {
        settings.baseUrl()
    } catch (_: IllegalArgumentException) {
        return false
    }
    val parsedUri = runCatching { URI(baseUrl) }.getOrNull() ?: return false
    if (!parsedUri.scheme.equals("http", ignoreCase = true)) return false
    val host = parsedUri.host?.takeIf { it.isNotBlank() } ?: return false
    return !isLikelyPrivateHost(host)
}

private fun parseIpv4Octets(host: String): IntArray? {
    val parts = host.split('.')
    if (parts.size != 4) return null
    val octets = IntArray(parts.size)
    parts.forEachIndexed { index, part ->
        if (part.isEmpty() || part.length > 3 || part.any { it !in '0'..'9' }) return null
        val value = part.toInt()
        if (value > 255) return null
        octets[index] = value
    }
    return octets
}
