package com.hjw.qbremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hjw.qbremote.R
import com.hjw.qbremote.data.CachedDashboardServerSnapshot
import com.hjw.qbremote.data.ServerBackendType

internal fun LazyListScope.dashboardHomePageContent(
    state: MainUiState,
    dashboardServerSnapshots: List<CachedDashboardServerSnapshot>,
    showDashboardSnapshot: Boolean,
    showHomeAggregateDashboard: Boolean,
    showDashboardSkeleton: Boolean,
    showServerStackHint: Boolean,
    draggingProfileId: String?,
    settlingProfileId: String?,
    draggingOffsetY: () -> Float,
    settlingOffsetY: () -> Float,
    draggingTargetIndex: Int,
    dragSession: VerticalReorderSession<String>?,
    onDismissReorderHint: () -> Unit,
    onStartServerStackDrag: (String) -> Unit,
    onDragServerStack: (String, Float) -> Unit,
    onEndServerStackDrag: () -> Unit,
    onCancelServerStackDrag: () -> Unit,
    onOpenServerDashboard: (String) -> Unit,
    onOpenGlobalSpeedLimits: () -> Unit,
    onDismissHomeTorrentEntryHint: () -> Unit,
    onOpenTorrentList: () -> Unit,
    onSwitchServerProfile: (String) -> Unit,
    onEditServerProfile: (String) -> Unit,
    onRequestDeleteServerProfile: (String) -> Unit,
    onOpenConnection: () -> Unit,
) {
    when {
        showDashboardSnapshot -> {
            item {
                if (showHomeAggregateDashboard) {
                    MultiServerDashboardSection(
                        aggregate = state.dashboardAggregate,
                        snapshots = dashboardServerSnapshots,
                        draggingProfileId = draggingProfileId,
                        settlingProfileId = settlingProfileId,
                        draggingOffsetY = draggingOffsetY,
                        settlingOffsetY = settlingOffsetY,
                        draggingTargetIndex = draggingTargetIndex,
                        dragSession = dragSession,
                        showReorderHint = showServerStackHint,
                        onDismissReorderHint = onDismissReorderHint,
                        onStartDrag = onStartServerStackDrag,
                        onDragDelta = onDragServerStack,
                        onDragEnd = onEndServerStackDrag,
                        onDragCancel = onCancelServerStackDrag,
                        onOpenServerDashboard = onOpenServerDashboard,
                        onOpenGlobalSpeedLimits = onOpenGlobalSpeedLimits,
                    )
                } else {
                    val activeProfileId = state.activeServerProfileId
                        ?: state.serverProfiles.firstOrNull()?.id
                    val activeBackendType = state.serverProfiles
                        .firstOrNull { profile -> profile.id == activeProfileId }
                        ?.backendType
                        ?: state.settings.serverBackendType
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ServerOverviewCard(
                            overviewTitle = when (activeBackendType) {
                                ServerBackendType.TRANSMISSION -> "Transmission"
                                ServerBackendType.QBITTORRENT -> stringResource(R.string.brand_qbittorrent)
                            },
                            serverProfiles = state.serverProfiles,
                            activeProfileId = state.activeServerProfileId,
                            serverVersion = state.serverVersion,
                            transferInfo = state.transferInfo,
                            torrents = state.torrents,
                            torrentCount = maxOf(state.torrents.size, state.transferInfo.totalTorrentCount),
                            showTotals = true,
                            showEntryHint = !state.settings.homeTorrentEntryHintDismissed,
                            backendType = activeBackendType,
                            onCardClick = {
                                activeProfileId?.let(onOpenServerDashboard)
                            },
                            onDismissEntryHint = onDismissHomeTorrentEntryHint,
                            onOpenTorrentList = onOpenTorrentList,
                            onSwitchServerProfile = onSwitchServerProfile,
                            onEditServerProfile = onEditServerProfile,
                            onRequestDeleteServerProfile = onRequestDeleteServerProfile,
                            swipeActionsEnabled = false,
                            showSwipeHint = false,
                            onDismissSwipeHint = {},
                        )
                    }
                }
            }
        }

        showDashboardSkeleton -> {
            item {
                DashboardHomeSkeleton(showCharts = false)
            }
        }

        else -> {
            item {
                NeedConnectionCard(onOpenConnection = onOpenConnection)
            }
        }
    }
}

