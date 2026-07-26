package com.hjw.qbremote.ui

internal data class DashboardReorderHoldReleaseResult(
    val nextHeldProfileId: String? = null,
    val profileIdToRefreshImmediately: String? = null,
)

internal fun shouldSkipRefreshForDashboardReorderHold(
    heldProfileId: String?,
    holdAllProfiles: Boolean,
    profileId: String,
): Boolean {
    if (holdAllProfiles) return true
    val normalizedHeldProfileId = heldProfileId?.trim().orEmpty()
    val normalizedProfileId = profileId.trim()
    return normalizedHeldProfileId.isNotBlank() &&
        normalizedProfileId.isNotBlank() &&
        normalizedHeldProfileId == normalizedProfileId
}

internal fun releaseDashboardReorderHold(
    state: MainUiState,
): DashboardReorderHoldReleaseResult {
    val heldProfileId = state.dashboardRefreshHoldProfileId?.trim().orEmpty()
    if (heldProfileId.isBlank()) return DashboardReorderHoldReleaseResult()
    return DashboardReorderHoldReleaseResult(
        nextHeldProfileId = null,
        profileIdToRefreshImmediately = heldProfileId,
    )
}
