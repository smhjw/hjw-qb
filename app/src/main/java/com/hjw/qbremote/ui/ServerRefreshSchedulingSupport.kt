package com.hjw.qbremote.ui

import com.hjw.qbremote.data.ServerProfile

internal fun selectDueServerRefreshProfileIds(
    profiles: List<ServerProfile>,
    nextRefreshAtByProfileId: Map<String, Long>,
    inFlightProfileIds: Set<String>,
    heldProfileId: String?,
    holdAllProfiles: Boolean,
    now: Long,
): List<String> {
    return profiles.mapNotNull { profile ->
        val dueAt = nextRefreshAtByProfileId[profile.id] ?: 0L
        if (
            shouldSkipRefreshForDashboardReorderHold(
                heldProfileId = heldProfileId,
                holdAllProfiles = holdAllProfiles,
                profileId = profile.id,
            )
        ) {
            return@mapNotNull null
        }
        if (now >= dueAt && profile.id !in inFlightProfileIds) profile.id else null
    }
}

internal fun nextServerRefreshDueAt(now: Long, refreshSeconds: Int): Long {
    return now + refreshSeconds.coerceIn(5, 120) * 1_000L
}
