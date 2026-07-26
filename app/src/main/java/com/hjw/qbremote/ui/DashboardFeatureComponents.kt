package com.hjw.qbremote.ui
import android.Manifest
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.hjw.qbremote.data.AppLanguage
import com.hjw.qbremote.data.AppTheme
import com.hjw.qbremote.R
import com.hjw.qbremote.data.CachedDashboardServerSnapshot
import com.hjw.qbremote.data.ConnectionSettings
import com.hjw.qbremote.data.ServerBackendType
import com.hjw.qbremote.data.ServerCapabilities
import com.hjw.qbremote.data.ServerDashboardPreferences
import com.hjw.qbremote.data.ServerProfile
import com.hjw.qbremote.data.defaultCapabilitiesFor
import com.hjw.qbremote.data.model.AddTorrentFile
import com.hjw.qbremote.data.model.TorrentFileInfo
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.data.model.TorrentProperties
import com.hjw.qbremote.data.model.TorrentTracker
import com.hjw.qbremote.data.model.TransferInfo
import com.hjw.qbremote.ui.theme.qbGlassCardColors
import com.hjw.qbremote.ui.theme.qbGlassChipColor
import com.hjw.qbremote.ui.theme.qbGlassOutlineColor
import com.hjw.qbremote.ui.theme.qbGlassStrongContainerColor
import com.hjw.qbremote.ui.theme.qbGlassSubtleContainerColor
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.net.URI
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun ServerOverviewCard(
    overviewTitle: String,
    backendType: ServerBackendType,
    serverProfiles: List<ServerProfile>,
    activeProfileId: String?,
    serverVersion: String,
    transferInfo: TransferInfo,
    torrents: List<TorrentInfo>,
    torrentCount: Int,
    showTotals: Boolean,
    showEntryHint: Boolean,
    onCardClick: (() -> Unit)?,
    onDismissEntryHint: () -> Unit,
    onOpenTorrentList: () -> Unit,
    onSwitchServerProfile: (String) -> Unit,
    onEditServerProfile: (String) -> Unit,
    onRequestDeleteServerProfile: (String) -> Unit,
    swipeActionsEnabled: Boolean,
    showSwipeHint: Boolean,
    onDismissSwipeHint: () -> Unit,
) {
    val stateSummary = remember(torrents) { buildDashboardStateSummary(torrents) }
    val activeProfile = remember(serverProfiles, activeProfileId) {
        serverProfiles.firstOrNull { it.id == activeProfileId }
    }
    val uploadLimitText = formatRateLimit(
        value = transferInfo.uploadRateLimit,
        unlimitedLabel = stringResource(R.string.limit_unlimited),
    )
    val downloadLimitText = formatRateLimit(
        value = transferInfo.downloadRateLimit,
        unlimitedLabel = stringResource(R.string.limit_unlimited),
    )
    val pausedTotal = stateSummary.pausedUploadCount + stateSummary.pausedDownloadCount
    val statusUploadingLabel = stringResource(R.string.status_uploading)
    val statusDownloadingLabel = stringResource(R.string.status_downloading)
    val statusPausedLabel = stringResource(R.string.status_paused)
    val statusErrorLabel = stringResource(R.string.status_error)
    val statusCheckingLabel = stringResource(R.string.status_checking)
    val statusWaitingLabel = stringResource(R.string.status_waiting)
    val statusTotalLabel = stringResource(R.string.status_total_torrents)
    val statusPills = remember(
        stateSummary,
        torrentCount,
        statusUploadingLabel,
        statusDownloadingLabel,
        statusPausedLabel,
        statusErrorLabel,
        statusCheckingLabel,
        statusWaitingLabel,
        statusTotalLabel,
    ) {
        listOf(
            DashboardStatusPillItem(
                label = statusUploadingLabel,
                count = stateSummary.uploadingCount,
                accentColor = Color(0xFF3BBA6F),
            ),
            DashboardStatusPillItem(
                label = statusDownloadingLabel,
                count = stateSummary.downloadingCount,
                accentColor = Color(0xFF3990FF),
            ),
            DashboardStatusPillItem(
                label = statusPausedLabel,
                count = pausedTotal,
                accentColor = Color(0xFF8D98A8),
            ),
            DashboardStatusPillItem(
                label = statusErrorLabel,
                count = stateSummary.errorCount,
                accentColor = Color(0xFFE1493D),
            ),
            DashboardStatusPillItem(
                label = statusCheckingLabel,
                count = stateSummary.checkingCount,
                accentColor = Color(0xFFE1A22B),
            ),
            DashboardStatusPillItem(
                label = statusWaitingLabel,
                count = stateSummary.waitingCount,
                accentColor = Color(0xFFA674E8),
            ),
            DashboardStatusPillItem(
                label = statusTotalLabel,
                count = torrentCount,
                accentColor = Color(0xFF11A9B5),
            ),
        )
    }
    val actionWidth = 104.dp
    val actionWidthPx = with(LocalDensity.current) { 104.dp.toPx() }
    var revealOffset by rememberSaveable(activeProfileId) { mutableFloatStateOf(0f) }
    val animatedRevealOffset by animateFloatAsState(
        targetValue = revealOffset,
        label = "serverOverviewRevealOffset",
    )
    LaunchedEffect(activeProfile?.id, swipeActionsEnabled) {
        revealOffset = 0f
    }
    val showActionRail = swipeActionsEnabled &&
        activeProfile != null &&
        animatedRevealOffset <= -(actionWidthPx * 0.42f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PanelShape),
    ) {
        if (showActionRail) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(actionWidth),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ServerOverviewActionButton(
                        iconRes = R.drawable.ic_action_edit,
                        description = stringResource(R.string.edit),
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = {
                            revealOffset = 0f
                            activeProfile?.id?.let(onEditServerProfile)
                        },
                    )
                    ServerOverviewActionButton(
                        iconRes = R.drawable.ic_action_delete,
                        description = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error,
                        onClick = {
                            revealOffset = 0f
                            activeProfile?.id?.let(onRequestDeleteServerProfile)
                        },
                    )
                }
            }
        }

        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = animatedRevealOffset }
                .pointerInput(activeProfile?.id) {
                    if (activeProfile == null || !swipeActionsEnabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            if (showSwipeHint) {
                                onDismissSwipeHint()
                            }
                            revealOffset = (revealOffset + dragAmount).coerceIn(-actionWidthPx, 0f)
                        },
                        onDragEnd = {
                            revealOffset = if (revealOffset <= -(actionWidthPx * 0.45f)) {
                                -actionWidthPx
                            } else {
                                0f
                            }
                        },
                    )
                }
                .then(
                    if (onCardClick != null) {
                        Modifier.clickable {
                            if (revealOffset < 0f) {
                                revealOffset = 0f
                            } else {
                                onCardClick()
                            }
                        }
                    } else {
                        Modifier
                    }
                ),
            shape = PanelShape,
            border = BorderStroke(1.dp, qbGlassOutlineColor(defaultAlpha = 0.28f)),
            colors = qbGlassCardColors(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(id = backendOverviewIconRes(backendType)),
                        contentDescription = overviewTitle,
                        modifier = Modifier.size(34.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = overviewTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.server_version_fmt, serverVersion),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onOpenTorrentList) {
                        Text(
                            text = stringResource(R.string.dashboard_open_torrents),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                if (serverProfiles.size > 1) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        items(serverProfiles, key = { it.id }) { profile ->
                            ServerProfileSummaryCard(
                                profile = profile,
                                active = profile.id == activeProfileId,
                                addressText = buildServerAddressText(
                                    ConnectionSettings(
                                        host = profile.host,
                                        port = profile.port,
                                        useHttps = profile.useHttps,
                                    ),
                                ),
                                summaryText = if (profile.id == activeProfileId) {
                                    stringResource(
                                        R.string.server_summary_speed_fmt,
                                        formatSpeed(transferInfo.uploadSpeed),
                                        formatSpeed(transferInfo.downloadSpeed),
                                    )
                                } else {
                                    stringResource(R.string.server_profile_saved)
                                },
                                onSwitch = {
                                    revealOffset = 0f
                                    onSwitchServerProfile(profile.id)
                                },
                                onEdit = {
                                    revealOffset = 0f
                                    onEditServerProfile(profile.id)
                                },
                                onDelete = {
                                    revealOffset = 0f
                                    onRequestDeleteServerProfile(profile.id)
                                },
                            )
                        }
                    }
                }

                if (showEntryHint) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        DashboardEntryHintBubble(
                            text = stringResource(R.string.dashboard_open_torrents_hint),
                            dismissDescription = stringResource(R.string.dismiss_hint),
                            onDismiss = onDismissEntryHint,
                        )
                    }
                }
                if (showSwipeHint) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        DashboardEntryHintBubble(
                            text = stringResource(R.string.dashboard_swipe_hint),
                            dismissDescription = stringResource(R.string.dismiss_hint),
                            onDismiss = onDismissSwipeHint,
                        )
                    }
                }

                DashboardSecondaryStatsBlock(
                    uploadSpeedText = formatSpeed(transferInfo.uploadSpeed),
                    downloadSpeedText = formatSpeed(transferInfo.downloadSpeed),
                    uploadLimitText = uploadLimitText,
                    downloadLimitText = downloadLimitText,
                    showTotals = showTotals,
                    totalDownloadedText = formatBytes(transferInfo.downloadedTotal),
                    totalUploadedText = formatBytes(transferInfo.uploadedTotal),
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    items(statusPills, key = { it.label }) { pill ->
                        DashboardStatusPill(
                            label = pill.label,
                            count = pill.count,
                            accentColor = pill.accentColor,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun MultiServerDashboardSection(
    aggregate: DashboardAggregateState,
    snapshots: List<CachedDashboardServerSnapshot>,
    draggingProfileId: String?,
    settlingProfileId: String?,
    draggingOffsetY: () -> Float,
    settlingOffsetY: () -> Float,
    draggingTargetIndex: Int,
    dragSession: VerticalReorderSession<String>?,
    showReorderHint: Boolean,
    onDismissReorderHint: () -> Unit,
    onStartDrag: (String) -> Unit,
    onDragDelta: (String, Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onOpenServerDashboard: (String) -> Unit,
    onOpenGlobalSpeedLimits: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DashboardAggregateOverviewCard(
            aggregate = aggregate,
            onOpenGlobalSpeedLimits = onOpenGlobalSpeedLimits,
        )
        WalletServerCardStack(
            snapshots = snapshots,
            draggingProfileId = draggingProfileId,
            settlingProfileId = settlingProfileId,
            draggingOffsetY = draggingOffsetY,
            settlingOffsetY = settlingOffsetY,
            draggingTargetIndex = draggingTargetIndex,
            dragSession = dragSession,
            showReorderHint = showReorderHint,
            onDismissReorderHint = onDismissReorderHint,
            onStartDrag = onStartDrag,
            onDragDelta = onDragDelta,
            onDragEnd = onDragEnd,
            onDragCancel = onDragCancel,
            onOpenServerDashboard = onOpenServerDashboard,
        )
    }
}

@Composable
private fun DashboardAggregateOverviewCard(
    aggregate: DashboardAggregateState,
    onOpenGlobalSpeedLimits: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = PanelShape,
        border = BorderStroke(1.dp, qbGlassOutlineColor()),
        colors = qbGlassCardColors(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.dashboard_server_speed_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.dashboard_server_speed_online_count,
                        aggregate.totalServerCount,
                        aggregate.totalServerCount,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AggregateSpeedTextLine(
                label = stringResource(R.string.sort_upload_speed),
                value = formatSpeed(aggregate.transferInfo.uploadSpeed),
                accent = Color(0xFF3BBA6F),
            )
            AggregateSpeedTextLine(
                label = stringResource(R.string.sort_download_speed),
                value = formatSpeed(aggregate.transferInfo.downloadSpeed),
                accent = Color(0xFF3990FF),
            )
            InlineRealtimeSpeedChart(
                aggregate = aggregate,
                modifier = Modifier.padding(top = 2.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onOpenGlobalSpeedLimits) {
                    Text(
                        text = stringResource(R.string.global_speed_limit_action),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun AggregateSpeedTextLine(
    label: String,
    value: String,
    accent: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(accent, RoundedCornerShape(99.dp)),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun WalletServerCardStack(
    snapshots: List<CachedDashboardServerSnapshot>,
    draggingProfileId: String?,
    settlingProfileId: String?,
    draggingOffsetY: () -> Float,
    settlingOffsetY: () -> Float,
    draggingTargetIndex: Int,
    dragSession: VerticalReorderSession<String>?,
    showReorderHint: Boolean,
    onDismissReorderHint: () -> Unit,
    onStartDrag: (String) -> Unit,
    onDragDelta: (String, Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onOpenServerDashboard: (String) -> Unit,
) {
    val orderedSnapshots = snapshots
    val paletteIndexByProfileId = remember(orderedSnapshots) {
        orderedSnapshots
            .map { it.profileId }
            .distinct()
            .sorted()
            .mapIndexed { index, profileId -> profileId to index }
            .toMap()
    }
    val serverStackGestureKey = remember(orderedSnapshots) {
        orderedSnapshots.joinToString(separator = "|") { it.profileId }
    }
    val stackHeight = if (orderedSnapshots.isEmpty()) {
        0.dp
    } else {
        HomeServerStackExpandedCardHeight + HomeServerStackExposedHeight * (orderedSnapshots.size - 1)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.server_stack_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.server_stack_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (showReorderHint) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    DashboardEntryHintBubble(
                        text = stringResource(R.string.server_stack_reorder_hint),
                        dismissDescription = stringResource(R.string.dismiss_hint),
                        onDismiss = onDismissReorderHint,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(stackHeight),
        ) {
            orderedSnapshots.withIndex().toList().asReversed().forEach { (index, snapshot) ->
                key(snapshot.profileId) {
                    val isSettlingCard = settlingProfileId == snapshot.profileId
                    WalletServerStackCard(
                        snapshot = snapshot,
                        gestureKey = serverStackGestureKey,
                        stackedIndex = index,
                        paletteIndex = paletteIndexByProfileId[snapshot.profileId] ?: index,
                        cardHeight = HomeServerStackExpandedCardHeight,
                        collapsedCardHeight = HomeServerStackCollapsedCardHeight,
                        exposedHeight = HomeServerStackExposedHeight,
                        selected = index == 0,
                        stackCount = orderedSnapshots.size,
                        isDragging = draggingProfileId == snapshot.profileId && !isSettlingCard,
                        isSettling = isSettlingCard,
                        dragOffsetY = { if (draggingProfileId == snapshot.profileId && !isSettlingCard) draggingOffsetY() else 0f },
                        settlingOffsetY = { if (isSettlingCard) settlingOffsetY() else 0f },
                        siblingOffsetY = calculateServerStackSiblingOffset(
                            profileId = snapshot.profileId,
                            draggingProfileId = draggingProfileId,
                            draggingTargetIndex = draggingTargetIndex,
                            dragSession = dragSession,
                        ),
                        animateSiblingOffset = dragSession != null,
                        onDragStart = { onStartDrag(snapshot.profileId) },
                        onDragDelta = { deltaY -> onDragDelta(snapshot.profileId, deltaY) },
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel,
                        onClick = { onOpenServerDashboard(snapshot.profileId) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun WalletServerStackCard(
    snapshot: CachedDashboardServerSnapshot,
    gestureKey: String,
    stackedIndex: Int,
    paletteIndex: Int,
    cardHeight: Dp,
    collapsedCardHeight: Dp,
    exposedHeight: Dp,
    selected: Boolean,
    stackCount: Int,
    isDragging: Boolean,
    isSettling: Boolean,
    dragOffsetY: () -> Float,
    settlingOffsetY: () -> Float,
    siblingOffsetY: Float,
    animateSiblingOffset: Boolean,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onClick: () -> Unit,
) {
    val palette = remember(snapshot.profileId, paletteIndex) {
        walletCardPalette(paletteIndex)
    }
    val stateLabel = if (snapshot.isStale) {
        stringResource(R.string.server_snapshot_stale_state)
    } else {
        stringResource(R.string.server_snapshot_live_state)
    }
    val exposedStepPx = with(LocalDensity.current) { exposedHeight.toPx() }
    val cornerShape = RoundedCornerShape(24.dp)
    val collapsedShape = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp,
    )
    val uploadLimitText = formatRateLimit(
        value = snapshot.transferInfo.uploadRateLimit,
        unlimitedLabel = stringResource(R.string.limit_unlimited),
    )
    val downloadLimitText = formatRateLimit(
        value = snapshot.transferInfo.downloadRateLimit,
        unlimitedLabel = stringResource(R.string.limit_unlimited),
    )
    val serverNameTextStyle = MaterialTheme.typography.titleMedium.copy(
        fontSize = 18.sp,
        lineHeight = 22.sp,
    )
    val clickSuppressionThresholdPx = with(LocalDensity.current) {
        ServerCardClickSuppressionDragThreshold.toPx()
    }
    val presentation = remember(selected, isDragging, isSettling) {
        resolveWalletServerStackCardPresentation(
            selected = selected,
            isDragging = isDragging,
            isSettling = isSettling,
        )
    }
    val cardShape = if (presentation.showExpandedLayout) cornerShape else collapsedShape
    var isPressed by remember(snapshot.profileId) { mutableStateOf(false) }
    var lastDragFinishedAt by remember(snapshot.profileId) { mutableLongStateOf(0L) }
    var dragDistanceSinceStart by remember(snapshot.profileId) { mutableFloatStateOf(0f) }
    val latestOnDragStart by rememberUpdatedState(onDragStart)
    val latestOnDragDelta by rememberUpdatedState(onDragDelta)
    val latestOnDragEnd by rememberUpdatedState(onDragEnd)
    val latestOnDragCancel by rememberUpdatedState(onDragCancel)
    val latestOnClick by rememberUpdatedState(onClick)
    val animatedSiblingOffset by animateFloatAsState(
        targetValue = siblingOffsetY,
        animationSpec = if (animateSiblingOffset) {
            ReorderSiblingOffsetAnimationSpec
        } else {
            snap()
        },
        label = "walletServerSiblingOffset",
    )
    val baseTranslationY = if (selected) {
        (stackCount - 1) * exposedStepPx
    } else {
        (stackCount - 1 - stackedIndex) * exposedStepPx
    }
    val displayHeight = if (presentation.showExpandedLayout) cardHeight else collapsedCardHeight
    val pressedScale by animateFloatAsState(
        targetValue = if (isPressed && !isDragging) 0.985f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "walletServerPressedScale",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(displayHeight)
            .graphicsLayer {
                translationY = baseTranslationY +
                    if (isDragging || isSettling) 0f else animatedSiblingOffset
                shadowElevation = 0f
            }
            .pointerInput(snapshot.profileId, gestureKey) {
                if (stackCount < 2) return@pointerInput
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        isPressed = false
                        dragDistanceSinceStart = 0f
                        latestOnDragStart()
                    },
                    onDragEnd = {
                        isPressed = false
                        lastDragFinishedAt = resolveServerCardClickSuppressionTimestamp(
                            dragDistanceSinceStart = dragDistanceSinceStart,
                            clickSuppressionThresholdPx = clickSuppressionThresholdPx,
                            currentTimeMillis = SystemClock.elapsedRealtime(),
                        )
                        latestOnDragEnd()
                    },
                    onDragCancel = {
                        isPressed = false
                        lastDragFinishedAt = resolveServerCardClickSuppressionTimestamp(
                            dragDistanceSinceStart = dragDistanceSinceStart,
                            clickSuppressionThresholdPx = clickSuppressionThresholdPx,
                            currentTimeMillis = SystemClock.elapsedRealtime(),
                        )
                        latestOnDragCancel()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragDistanceSinceStart += kotlin.math.abs(dragAmount.y)
                        latestOnDragDelta(dragAmount.y)
                    },
                )
            }
            .pointerInput(snapshot.profileId, gestureKey, lastDragFinishedAt) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try {
                            tryAwaitRelease()
                        } finally {
                            isPressed = false
                        }
                    },
                    onTap = {
                        if (
                            shouldSuppressServerCardClick(
                                lastDragFinishedAt = lastDragFinishedAt,
                                currentTimeMillis = SystemClock.elapsedRealtime(),
                            )
                        ) {
                            return@detectTapGestures
                        }
                        isPressed = false
                        latestOnClick()
                    }
                )
            }
            .zIndex(
                if (isDragging || isSettling) {
                    (stackCount + 1).toFloat()
                } else {
                    (stackCount - stackedIndex).toFloat()
                },
            ),
    ) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = when {
                        isDragging -> dragOffsetY()
                        isSettling -> settlingOffsetY()
                        else -> 0f
                    }
                    scaleX = when {
                        isDragging || isSettling -> 1f
                        presentation.showExpandedLayout -> 1f
                        else -> 0.992f
                    } * pressedScale
                    scaleY = when {
                        isDragging || isSettling -> 1f
                        presentation.showExpandedLayout -> 1f
                        else -> 0.992f
                    } * pressedScale
                    shadowElevation = when {
                        isDragging || isSettling -> 0f
                        selected -> ReorderSelectedShadow
                        else -> ReorderCollapsedShadow
                    }
                    shape = cardShape
                    clip = true
                },
            shape = cardShape,
            border = BorderStroke(
                if (isDragging || isSettling) 2.dp else 1.dp,
                if (isDragging || isSettling) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.90f)
                } else {
                    Color.White.copy(alpha = presentation.borderAlpha)
                },
            ),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = palette.background,
                    shape = cardShape,
                )
                .padding(
                    horizontal = presentation.horizontalPadding,
                    vertical = presentation.verticalPadding,
                ),
        ) {
            if (presentation.showExpandedLayout) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = snapshot.profileName.ifBlank { backendLabel(snapshot.backendType) },
                                style = serverNameTextStyle,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = backendLabel(snapshot.backendType),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.82f),
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = stateLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.92f),
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(
                                    R.string.server_summary_speed_fmt,
                                    formatSpeed(snapshot.transferInfo.uploadSpeed),
                                    formatSpeed(snapshot.transferInfo.downloadSpeed),
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.accent,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.End,
                            )
                        }
                    }
                    
                    WalletCardMetricLine(
                        label = stringResource(R.string.sort_total_uploaded),
                        value = formatBytes(snapshot.transferInfo.uploadedTotal),
                    )
                    WalletCardMetricLine(
                        label = stringResource(R.string.sort_total_downloaded),
                        value = formatBytes(snapshot.transferInfo.downloadedTotal),
                    )
                    WalletCardMetricLine(
                        label = stringResource(R.string.upload_limit_kb_label),
                        value = uploadLimitText,
                    )
                    WalletCardMetricLine(
                        label = stringResource(R.string.download_limit_kb_label),
                        value = downloadLimitText,
                    )
                    WalletCardMetricLine(
                        label = stringResource(R.string.free_space_label),
                        value = formatBytes(snapshot.transferInfo.freeSpaceOnDisk),
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = snapshot.profileName.ifBlank { backendLabel(snapshot.backendType) },
                        style = serverNameTextStyle,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = stateLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.92f),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        Text(
                            text = stringResource(
                                R.string.server_summary_speed_fmt,
                                formatSpeed(snapshot.transferInfo.uploadSpeed),
                                formatSpeed(snapshot.transferInfo.downloadSpeed),
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.accent,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
private fun WalletCardMetricLine(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.76f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
internal fun DashboardManagementEmptyCard(
    onOpenManager: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = PanelShape,
        border = BorderStroke(1.dp, qbGlassOutlineColor()),
        colors = qbGlassCardColors(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.dashboard_all_cards_hidden_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.dashboard_all_cards_hidden_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onOpenManager) {
                Text(stringResource(R.string.dashboard_manage_cards_action))
            }
        }
    }
}

@Composable
internal fun PageRestorePlaceholder() {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, qbGlassOutlineColor(defaultAlpha = 0.28f)),
        colors = qbGlassCardColors(),
    ) {
        Text(
            text = stringResource(R.string.loading),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
internal fun ServerDashboardSkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ServerDashboardSkeletonCard(
            titleWidthFraction = 0.34f,
            bodyHeight = 132.dp,
        )
        ServerDashboardSkeletonCard(
            titleWidthFraction = 0.42f,
            bodyHeight = 182.dp,
        )
        ServerDashboardSkeletonCard(
            titleWidthFraction = 0.28f,
            bodyHeight = 168.dp,
        )
    }
}

@Composable
private fun ServerDashboardSkeletonCard(
    titleWidthFraction: Float,
    bodyHeight: Dp,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = PanelShape,
        border = BorderStroke(1.dp, qbGlassOutlineColor()),
        colors = qbGlassCardColors(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(titleWidthFraction)
                    .height(16.dp)
                    .background(
                        color = qbGlassSubtleContainerColor(),
                        shape = RoundedCornerShape(999.dp),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bodyHeight)
                    .background(
                        color = qbGlassStrongContainerColor(),
                        shape = RoundedCornerShape(18.dp),
                    ),
            )
        }
    }
}

@Composable
internal fun ServerDashboardCardManagerSheet(
    availableCards: List<DashboardChartCard>,
    preferences: ServerDashboardPreferences,
    onToggleCard: (DashboardChartCard, Boolean) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.dashboard_manage_cards_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        availableCards.forEach { card ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = dashboardChartCardLabel(card),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(
                    checked = preferences.visibleCards.contains(card.storageKey),
                    onCheckedChange = { checked -> onToggleCard(card, checked) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onReset) {
                Text(stringResource(R.string.dashboard_manage_cards_reset))
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    }
}

private data class WalletCardPalette(
    val background: Brush,
    val accent: Color,
)

private fun walletCardPalette(
    stackedIndex: Int,
): WalletCardPalette {
    val palettes = listOf(
        WalletCardPalette(
            background = Brush.linearGradient(
                listOf(
                    Color(0xFF16213F),
                    Color(0xFF3458B8),
                    Color(0xFF5E89FF),
                ),
            ),
            accent = Color(0xFFEAF2FF),
        ),
        WalletCardPalette(
            background = Brush.linearGradient(
                listOf(
                    Color(0xFF3A1C0F),
                    Color(0xFF9B5223),
                    Color(0xFFE79A45),
                ),
            ),
            accent = Color(0xFFFFF3E7),
        ),
        WalletCardPalette(
            background = Brush.linearGradient(
                listOf(
                    Color(0xFF112C26),
                    Color(0xFF24755D),
                    Color(0xFF52C79B),
                ),
            ),
            accent = Color(0xFFE9FFF5),
        ),
        WalletCardPalette(
            background = Brush.linearGradient(
                listOf(
                    Color(0xFF2B183B),
                    Color(0xFF7A46B1),
                    Color(0xFFB97DFF),
                ),
            ),
            accent = Color(0xFFF6EEFF),
        ),
        WalletCardPalette(
            background = Brush.linearGradient(
                listOf(
                    Color(0xFF381520),
                    Color(0xFF9F4062),
                    Color(0xFFE785A7),
                ),
            ),
            accent = Color(0xFFFFEEF5),
        ),
        WalletCardPalette(
            background = Brush.linearGradient(
                listOf(
                    Color(0xFF13293F),
                    Color(0xFF2280A6),
                    Color(0xFF6FD2F7),
                ),
            ),
            accent = Color(0xFFEAFBFF),
        ),
    )
    return palettes[stackedIndex % palettes.size]
}

@Composable
private fun backendLabel(backendType: ServerBackendType): String {
    return when (backendType) {
        ServerBackendType.QBITTORRENT -> stringResource(R.string.backend_qbittorrent)
        ServerBackendType.TRANSMISSION -> stringResource(R.string.backend_transmission)
    }
}

private fun backendOverviewIconRes(backendType: ServerBackendType): Int {
    return when (backendType) {
        ServerBackendType.QBITTORRENT -> R.drawable.ic_backend_qbittorrent
        ServerBackendType.TRANSMISSION -> R.drawable.ic_backend_transmission
    }
}

@Composable
private fun ServerOverviewActionButton(
    iconRes: Int,
    description: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .semantics { contentDescription = description }
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
    }
}


@Composable
private fun ServerProfileSummaryCard(
    profile: ServerProfile,
    active: Boolean,
    addressText: String,
    summaryText: String,
    onSwitch: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.width(214.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (active) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
            } else {
                qbGlassOutlineColor(defaultAlpha = 0.28f)
            },
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (active) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f)
            } else {
                qbGlassSubtleContainerColor()
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = addressText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (active) {
                        stringResource(R.string.server_profile_active)
                    } else {
                        stringResource(R.string.server_profile_tap_to_connect)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Text(
                text = summaryText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onSwitch) {
                    Text(
                        text = if (active) {
                            stringResource(R.string.dashboard_open_torrents)
                        } else {
                            stringResource(R.string.connect)
                        },
                    )
                }
                Row {
                    TextButton(onClick = onEdit) { Text(stringResource(R.string.edit)) }
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
                }
            }
        }
    }
}

@Composable
internal fun DashboardEntryHintBubble(
    text: String,
    dismissDescription: String,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .background(
                color = qbGlassChipColor(),
                shape = RoundedCornerShape(14.dp),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Box(
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .semantics { contentDescription = dismissDescription }
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_action_close),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
            )
        }
    }
}

@Composable
private fun DashboardSecondaryStatsBlock(
    uploadSpeedText: String,
    downloadSpeedText: String,
    uploadLimitText: String,
    downloadLimitText: String,
    showTotals: Boolean,
    totalDownloadedText: String,
    totalUploadedText: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DashboardSpeedMetricPanel(
            title = stringResource(R.string.upload),
            directionGlyph = "↑",
            speedText = uploadSpeedText,
            limitText = uploadLimitText,
            totalText = totalUploadedText,
            showTotal = showTotals,
            accentColor = Color(0xFF2B73F5),
        )
        DashboardSpeedMetricPanel(
            title = stringResource(R.string.download),
            directionGlyph = "↓",
            speedText = downloadSpeedText,
            limitText = downloadLimitText,
            totalText = totalDownloadedText,
            showTotal = showTotals,
            accentColor = Color(0xFF08A3AE),
        )
    }
}

private data class SpeedDisplayParts(
    val value: String,
    val unit: String,
)

@Composable
private fun RowScope.DashboardSpeedMetricPanel(
    title: String,
    directionGlyph: String,
    speedText: String,
    limitText: String,
    totalText: String,
    showTotal: Boolean,
    accentColor: Color,
) {
    val speedParts = remember(speedText) { splitSpeedDisplayParts(speedText) }
    val speedDescription = if (directionGlyph == "↑") {
        stringResource(R.string.global_up_fmt, speedText)
    } else {
        stringResource(R.string.global_down_fmt, speedText)
    }
    val totalDescription = if (showTotal) {
        if (directionGlyph == "↑") {
            stringResource(R.string.global_total_up_fmt, totalText)
        } else {
            stringResource(R.string.global_total_down_fmt, totalText)
        }
    } else {
        ""
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .semantics {
                contentDescription = listOf(speedDescription, totalDescription)
                    .filter { it.isNotBlank() }
                    .joinToString("，")
            }
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.56f),
                shape = RoundedCornerShape(10.dp),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = directionGlyph,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(2.dp)
                .background(accentColor, RoundedCornerShape(99.dp)),
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = speedParts.value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
            )
            if (speedParts.unit.isNotBlank()) {
                Text(
                    text = speedParts.unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.limit_value_fmt, limitText),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (showTotal) {
            Text(
                text = stringResource(R.string.total_value_fmt, totalText),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f),
            )
        }
    }
}

private fun splitSpeedDisplayParts(speedText: String): SpeedDisplayParts {
    val normalized = speedText.trim()
    val splitIndex = normalized.lastIndexOf(' ')
    return if (splitIndex in 1 until normalized.lastIndex) {
        SpeedDisplayParts(
            value = normalized.substring(0, splitIndex),
            unit = normalized.substring(splitIndex + 1),
        )
    } else {
        SpeedDisplayParts(value = normalized, unit = "")
    }
}

@Composable
private fun DashboardStatusPill(
    label: String,
    count: Int,
    accentColor: Color,
) {
    Box(
        modifier = Modifier
            .background(
                color = qbGlassSubtleContainerColor(),
                shape = RoundedCornerShape(999.dp),
            )
            .border(
                width = 1.dp,
                color = qbGlassOutlineColor(defaultAlpha = 0.24f),
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(accentColor, RoundedCornerShape(99.dp)),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun formatRateLimit(value: Long, unlimitedLabel: String): String {
    return if (value <= 0L) {
        unlimitedLabel
    } else {
        formatSpeed(value)
    }
}







