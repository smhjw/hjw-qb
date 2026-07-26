package com.hjw.qbremote.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.hjw.qbremote.R
import com.hjw.qbremote.data.ServerBackendType
import com.hjw.qbremote.ui.theme.qbGlassSubtleContainerColor
import com.hjw.qbremote.data.ServerCapabilities
import com.hjw.qbremote.data.ServerProfile
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.data.model.TransferInfo

@Composable
internal fun ServerDashboardPageContent(
    selectedServerProfile: ServerProfile?,
    showContent: Boolean,
    showDashboardCardHint: Boolean,
    showDashboardSwipeHint: Boolean,
    showSkeleton: Boolean,
    showRestorePlaceholder: Boolean,
    selectedDashboardBackendType: ServerBackendType,
    serverDashboardCapabilities: ServerCapabilities,
    serverDashboardDisplay: ServerDashboardDisplayState,
    serverDashboardVersion: String,
    serverDashboardTransferInfo: TransferInfo,
    serverDashboardTorrents: List<TorrentInfo>,
    serverDashboardTorrentCount: Int,
    displayVisibleDashboardCards: List<DashboardDisplayCardItem>,
    dashboardDragGestureKey: String,
    draggingDashboardCard: DashboardDisplayCardItem?,
    settlingDashboardCard: DashboardDisplayCardItem?,
    draggingDashboardOffsetY: () -> Float,
    settlingDashboardOffsetY: () -> Float,
    draggingDashboardTargetIndex: Int,
    draggingDashboardSession: VerticalReorderSession<DashboardDisplayCardItem>?,
    revealedDashboardHideCardKey: String?,
    dashboardCardHeights: MutableMap<String, Int>,
    dashboardLockedCardHeights: Map<String, Int>,
    onRevealHideCard: (DashboardDisplayCardItem) -> Unit,
    onHideCard: (DashboardDisplayCardItem) -> Unit,
    onStartCardDrag: (DashboardDisplayCardItem) -> Unit,
    onDragCard: (DashboardDisplayCardItem, Float) -> Unit,
    onEndCardDrag: () -> Unit,
    onCancelCardDrag: () -> Unit,
    onOpenManager: () -> Unit,
    onOpenTorrentList: () -> Unit,
    onSwitchServerProfile: (String) -> Unit,
    onEditServerProfile: (String) -> Unit,
    onRequestDeleteServerProfile: (String) -> Unit,
    onDismissHomeTorrentEntryHint: () -> Unit,
    onMarkDashboardSwipeHintSeen: () -> Unit,
    onMarkDashboardCardHintSeen: () -> Unit,
    onOpenConnection: () -> Unit,
) {
    if (selectedServerProfile != null && showContent) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(120)),
                exit = fadeOut(animationSpec = tween(90)),
            ) {
                ServerOverviewCard(
                    overviewTitle = when (selectedServerProfile.backendType) {
                        ServerBackendType.TRANSMISSION -> "Transmission"
                        ServerBackendType.QBITTORRENT -> stringResource(R.string.brand_qbittorrent)
                    },
                    backendType = selectedServerProfile.backendType,
                    serverProfiles = listOf(selectedServerProfile),
                    activeProfileId = selectedServerProfile.id,
                    serverVersion = serverDashboardVersion,
                    transferInfo = serverDashboardTransferInfo,
                    torrents = serverDashboardTorrents,
                    torrentCount = serverDashboardTorrentCount,
                    showTotals = true,
                    showEntryHint = false,
                    onCardClick = null,
                    onDismissEntryHint = onDismissHomeTorrentEntryHint,
                    onOpenTorrentList = onOpenTorrentList,
                    onSwitchServerProfile = onSwitchServerProfile,
                    onEditServerProfile = onEditServerProfile,
                    onRequestDeleteServerProfile = onRequestDeleteServerProfile,
                    swipeActionsEnabled = true,
                    showSwipeHint = showDashboardSwipeHint,
                    onDismissSwipeHint = onMarkDashboardSwipeHintSeen,
                )
            }

            if (showDashboardCardHint) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    DashboardEntryHintBubble(
                        text = stringResource(R.string.dashboard_chart_reorder_hint),
                        dismissDescription = stringResource(R.string.dismiss_hint),
                        onDismiss = onMarkDashboardCardHintSeen,
                    )
                }
            }

            if (displayVisibleDashboardCards.isEmpty()) {
                DashboardManagementEmptyCard(
                    onOpenManager = onOpenManager,
                )
            } else {
                displayVisibleDashboardCards.forEach { displayCard ->
                    key(displayCard.ownerKey) {
                        ReorderableDashboardCard(
                            card = displayCard.owner,
                            gestureKey = dashboardDragGestureKey,
                            isDragging = draggingDashboardCard == displayCard && settlingDashboardCard != displayCard,
                            isSettling = settlingDashboardCard == displayCard,
                            dragOffsetY = { if (draggingDashboardCard == displayCard) draggingDashboardOffsetY() else 0f },
                            settlingOffsetY = { if (settlingDashboardCard == displayCard) settlingDashboardOffsetY() else 0f },
                            siblingOffsetY = calculateDashboardSiblingOffset(
                                card = displayCard,
                                draggingCard = draggingDashboardCard,
                                draggingTargetIndex = draggingDashboardTargetIndex,
                                dragSession = draggingDashboardSession,
                            ),
                            animateSiblingOffset = draggingDashboardSession != null,
                            lockedHeightPx = dashboardLockedCardHeights[displayCard.ownerKey],
                            onDragStart = {
                                onStartCardDrag(displayCard)
                                onMarkDashboardCardHintSeen()
                            },
                            onDragDelta = { deltaY -> onDragCard(displayCard, deltaY) },
                            onDragEnd = onEndCardDrag,
                            onDragCancel = onCancelCardDrag,
                            onMeasured = { height ->
                                if (dashboardLockedCardHeights.isEmpty()) {
                                    dashboardCardHeights[displayCard.ownerKey] = height
                                }
                            },
                        ) {
                            ServerDashboardCardRenderer(
                                displayCard = displayCard,
                                backendType = selectedDashboardBackendType,
                                capabilities = serverDashboardCapabilities,
                                displayState = serverDashboardDisplay,
                                tagUploadDate = serverDashboardDisplay.tagUploadDate,
                                revealedDashboardHideCardKey = revealedDashboardHideCardKey,
                                onRevealHideCard = onRevealHideCard,
                                onHideCard = onHideCard,
                            )
                        }
                    }
                }
            }
        }
    } else if (showSkeleton || showRestorePlaceholder) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(120)),
            exit = fadeOut(animationSpec = tween(90)),
        ) {
            ServerDashboardSkeleton()
        }
    } else {
        NeedConnectionCard(
            onOpenConnection = onOpenConnection,
        )
    }
}

@Composable
private fun ServerDashboardCardRenderer(
    displayCard: DashboardDisplayCardItem,
    backendType: ServerBackendType,
    capabilities: ServerCapabilities,
    displayState: ServerDashboardDisplayState,
    tagUploadDate: String,
    revealedDashboardHideCardKey: String?,
    onRevealHideCard: (DashboardDisplayCardItem) -> Unit,
    onHideCard: (DashboardDisplayCardItem) -> Unit,
) {
    val showHideButton = revealedDashboardHideCardKey == displayCard.ownerKey
    val onRevealHide = { onRevealHideCard(displayCard) }
    val onHide = { onHideCard(displayCard) }
    when (displayCard.owner) {
        DashboardChartCard.COUNTRY_FLOW -> ServerDashboardCountryFlowCard(
            capabilities = capabilities,
            displayState = displayState,
            showHideButton = showHideButton,
            onRevealHide = onRevealHide,
            onHide = onHide,
        )

        DashboardChartCard.CATEGORY_SHARE -> ServerDashboardCategoryShareCard(
            backendType = backendType,
            capabilities = capabilities,
            displayState = displayState,
            showHideButton = showHideButton,
            onRevealHide = onRevealHide,
            onHide = onHide,
        )

        DashboardChartCard.DAILY_UPLOAD -> ServerDashboardPieCard(
            title = if (tagUploadDate.isNotBlank()) {
                stringResource(R.string.dashboard_upload_title_with_date, tagUploadDate)
            } else {
                stringResource(R.string.dashboard_upload_title)
            },
            entries = displayState.dailyUploadEntries,
            emptyText = stringResource(R.string.dashboard_daily_tag_upload_empty),
            showHideButton = showHideButton,
            onRevealHide = onRevealHide,
            onHide = onHide,
        )

        DashboardChartCard.TAG_UPLOAD -> ServerDashboardPieCard(
            title = stringResource(R.string.dashboard_tag_upload_share_title),
            entries = displayState.dailyUploadEntries,
            emptyText = stringResource(R.string.dashboard_daily_tag_upload_empty),
            showHideButton = showHideButton,
            onRevealHide = onRevealHide,
            onHide = onHide,
            compactLegendRows = true,
        )

        DashboardChartCard.TORRENT_STATE -> ServerDashboardPieCard(
            title = stringResource(R.string.dashboard_torrent_state_share_title),
            entries = displayState.transmissionStateEntries,
            emptyText = stringResource(R.string.chart_no_data),
            showHideButton = showHideButton,
            onRevealHide = onRevealHide,
            onHide = onHide,
        )

        DashboardChartCard.TRACKER_SITE -> ServerDashboardPieCard(
            title = stringResource(R.string.dashboard_tracker_site_share_title),
            entries = displayState.trackerSitePieEntries,
            emptyText = stringResource(R.string.chart_no_data),
            showHideButton = showHideButton,
            onRevealHide = onRevealHide,
            onHide = onHide,
        )

        DashboardChartCard.SIZE_DISTRIBUTION -> ServerDashboardPieCard(
            title = stringResource(R.string.dashboard_size_distribution_title),
            entries = buildSizeDistributionEntries(displayState.torrents),
            emptyText = stringResource(R.string.chart_no_data),
            showHideButton = showHideButton,
            onRevealHide = onRevealHide,
            onHide = onHide,
        )

        DashboardChartCard.SHARE_RATIO_DISTRIBUTION -> ServerDashboardPieCard(
            title = stringResource(R.string.dashboard_share_ratio_distribution_title),
            entries = buildShareRatioDistributionEntries(displayState.torrents),
            emptyText = stringResource(R.string.chart_no_data),
            showHideButton = showHideButton,
            onRevealHide = onRevealHide,
            onHide = onHide,
        )
    }
}

private fun buildShareRatioDistributionEntries(
    torrents: List<TorrentInfo>,
): List<PieLegendSeedEntry> {
    val display = buildShareRatioDistributionDisplay(torrents)
    if (display.totalCount == 0) return emptyList()
    val bucketLabels = listOf("< 1", "1 - 2", "2 - 5", ">= 5")
    return display.bucketCounts.mapIndexedNotNull { index, count ->
        count.takeIf { it > 0 }?.let {
            PieLegendSeedEntry(
                label = LegendLabelSpec.Raw(bucketLabels[index]),
                value = it.toLong(),
                valueKind = LegendValueKind.TORRENT_COUNT,
            )
        }
    }
}

private fun buildSizeDistributionEntries(
    torrents: List<TorrentInfo>,
): List<PieLegendSeedEntry> {
    if (torrents.isEmpty()) return emptyList()
    val gib = 1024L * 1024L * 1024L
    val bucketLabels = listOf("< 1 GB", "1 - 10 GB", "10 - 50 GB", ">= 50 GB")
    val bucketCounts = listOf(
        torrents.count { it.size < gib },
        torrents.count { it.size >= gib && it.size < 10 * gib },
        torrents.count { it.size >= 10 * gib && it.size < 50 * gib },
        torrents.count { it.size >= 50 * gib },
    )
    return bucketCounts.mapIndexedNotNull { index, count ->
        count.takeIf { it > 0 }?.let {
            PieLegendSeedEntry(
                label = LegendLabelSpec.Raw(bucketLabels[index]),
                value = it.toLong(),
                valueKind = LegendValueKind.TORRENT_COUNT,
            )
        }
    }
}

@Composable
private fun ServerDashboardCountryFlowCard(
    capabilities: ServerCapabilities,
    displayState: ServerDashboardDisplayState,
    showHideButton: Boolean,
    onRevealHide: () -> Unit,
    onHide: () -> Unit,
) {
    CountryFlowMapCard(
        stats = if (capabilities.supportsCountryDistribution) {
            displayState.countryUploadStats
        } else {
            emptyList()
        },
        showHideButton = showHideButton,
        onRevealHide = onRevealHide,
        onHide = onHide,
    )
}

@Composable
private fun ServerDashboardCategoryShareCard(
    backendType: ServerBackendType,
    capabilities: ServerCapabilities,
    displayState: ServerDashboardDisplayState,
    showHideButton: Boolean,
    onRevealHide: () -> Unit,
    onHide: () -> Unit,
) {
    DashboardHorizontalBarChartCard(
        title = stringResource(R.string.dashboard_category_share_title),
        entries = if (backendType == ServerBackendType.TRANSMISSION) {
            displayState.transmissionLabelBarEntries
        } else if (capabilities.supportsCategories) {
            displayState.categoryShareBarEntries
        } else {
            emptyList()
        },
        emptyText = stringResource(R.string.chart_no_data),
        showHideButton = showHideButton,
        onRevealHide = onRevealHide,
        onHide = onHide,
    )
}

@Composable
private fun ServerDashboardPieCard(
    title: String,
    entries: List<PieLegendSeedEntry>,
    emptyText: String,
    showHideButton: Boolean,
    onRevealHide: () -> Unit,
    onHide: () -> Unit,
    compactLegendRows: Boolean = false,
) {
    DashboardSeedPieCard(
        title = title,
        entries = entries,
        emptyText = emptyText,
        showHideButton = showHideButton,
        onRevealHide = onRevealHide,
        onHide = onHide,
        shareColor = MaterialTheme.colorScheme.primary,
        compactLegendRows = compactLegendRows,
    )
}

@Composable
private fun ServerDashboardTrackerSiteCard(
    displayState: ServerDashboardDisplayState,
    showHideButton: Boolean,
    onRevealHide: () -> Unit,
    onHide: () -> Unit,
) {
    DashboardHorizontalBarChartCard(
        title = stringResource(R.string.dashboard_tracker_site_share_title),
        entries = displayState.trackerSiteBarEntries,
        emptyText = stringResource(R.string.chart_no_data),
        showHideButton = showHideButton,
        onRevealHide = onRevealHide,
        onHide = onHide,
    )
}



