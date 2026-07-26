package com.hjw.qbremote.ui
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hjw.qbremote.R
import com.hjw.qbremote.data.defaultCapabilitiesFor
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.ui.theme.qbGlassStrongContainerColor
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.math.abs
import kotlin.math.roundToInt

private data class PageAnimationState(
    val page: AppPage,
    val dashboardSessionKey: String = "",
    val themeSignature: String = "",
)

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalAnimationApi::class,
)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = androidx.compose.material3.rememberDrawerState(
        initialValue = androidx.compose.material3.DrawerValue.Closed
    )
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    var currentPage by rememberSaveable { mutableStateOf(AppPage.DASHBOARD) }
    var previousPage by rememberSaveable { mutableStateOf(AppPage.DASHBOARD) }
    var showAddTorrentSheet by rememberSaveable { mutableStateOf(false) }
    var showServerProfileSheet by rememberSaveable { mutableStateOf(false) }
    var serverSheetEditingProfileId by rememberSaveable { mutableStateOf("") }
    var pendingDeleteProfileId by rememberSaveable { mutableStateOf("") }
    var pendingTorrentExportHash by rememberSaveable { mutableStateOf("") }
    var pendingTorrentExportName by rememberSaveable { mutableStateOf("") }
    var selectedTorrentIdentity by rememberSaveable { mutableStateOf("") }
    var pendingTorrentReturnIdentity by rememberSaveable { mutableStateOf("") }
    var showDashboardCardManagerSheet by rememberSaveable { mutableStateOf(false) }
    var showTorrentSortMenu by remember { mutableStateOf(false) }
    var showTorrentSearchBar by rememberSaveable { mutableStateOf(false) }
    var sortScrollRequestId by remember { mutableIntStateOf(0) }
    val addTorrentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val serverProfileSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dashboardCardManagerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val localContext = LocalContext.current
    val exportTorrentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-bittorrent"),
    ) { uri ->
        val exportHash = pendingTorrentExportHash
        pendingTorrentExportHash = ""
        pendingTorrentExportName = ""
        if (uri == null || exportHash.isBlank()) return@rememberLauncherForActivityResult
        viewModel.exportTorrentFile(exportHash) { bytes ->
            runCatching {
                localContext.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(bytes)
                } ?: error("Unable to open export target.")
            }.onSuccess {
                scope.launch {
                    snackbarHostState.showSnackbar(localContext.getString(R.string.detail_export_success))
                }
            }.onFailure {
                scope.launch {
                    snackbarHostState.showSnackbar(localContext.getString(R.string.detail_export_failed))
                }
            }
        }
    }
    val pageThemeSignature = remember(
        state.settings.appTheme,
        state.settings.customBackgroundToneIsLight,
        state.settings.customBackgroundImagePath,
    ) {
        buildPageThemeSignature(
            appTheme = state.settings.appTheme,
            customBackgroundToneIsLight = state.settings.customBackgroundToneIsLight,
            customBackgroundImagePath = state.settings.customBackgroundImagePath,
        )
    }
    val torrentListDisplayState by viewModel.torrentListDisplayState.collectAsStateWithLifecycle()
    val serverDashboardDisplay by viewModel.serverDashboardDisplayState.collectAsStateWithLifecycle()
    val torrentListFilterState = torrentListDisplayState.torrentListFilterState
    val torrentListBaseSnapshot = torrentListDisplayState.torrentListBaseSnapshot
    val visibleTorrentItems = torrentListDisplayState.visibleTorrentItems
    val torrentPlacementContextKey = remember(currentPage, state.activeServerProfileId) {
        "${currentPage.name}:${state.activeServerProfileId.orEmpty()}"
    }
    val visibleTorrentItemKeys = remember(visibleTorrentItems) {
        visibleTorrentItems.map { item -> item.torrent.hash.ifBlank { item.identityKey } }
    }
    var previousVisibleTorrentItemKeys by remember { mutableStateOf<List<String>>(emptyList()) }
    var previousTorrentPlacementContextKey by remember { mutableStateOf<String?>(null) }
    val comparablePreviousTorrentItemKeys = remember(
        torrentPlacementContextKey,
        previousTorrentPlacementContextKey,
        previousVisibleTorrentItemKeys,
    ) {
        if (previousTorrentPlacementContextKey == torrentPlacementContextKey) {
            previousVisibleTorrentItemKeys
        } else {
            emptyList()
        }
    }
    val animateTorrentPlacement = remember(
        comparablePreviousTorrentItemKeys,
        visibleTorrentItemKeys,
    ) {
        shouldAnimateTorrentPlacement(
            previousKeys = comparablePreviousTorrentItemKeys,
            currentKeys = visibleTorrentItemKeys,
        )
    }
    LaunchedEffect(torrentPlacementContextKey, visibleTorrentItemKeys) {
        previousTorrentPlacementContextKey = torrentPlacementContextKey
        previousVisibleTorrentItemKeys = visibleTorrentItemKeys.toList()
    }
    val crossSeedCounts = torrentListBaseSnapshot.crossSeedCounts
    val activePendingProfileId = remember(state.activeServerProfileId) {
        state.activeServerProfileId.orEmpty()
    }
    fun isPendingAction(hash: String): Boolean {
        if (activePendingProfileId.isBlank()) return false
        return state.pendingActionKeys.contains(buildPendingActionKey(activePendingProfileId, hash))
    }
    val categoryOptionsForAdd = remember(state.categoryOptions) {
        buildSortedDistinctTrimmedStrings(state.categoryOptions)
    }
    val tagOptionsForAdd = remember(state.tagOptions) {
        buildSortedDistinctTrimmedStrings(state.tagOptions)
    }
    val pathOptionsForAdd = remember(state.torrents) {
        buildSortedDistinctTrimmedStrings(state.torrents.map { torrent -> torrent.savePath })
    }
    val selectedTorrent = remember(torrentListBaseSnapshot.torrents, selectedTorrentIdentity) {
        torrentListBaseSnapshot.torrents.firstOrNull { torrentIdentityKey(it) == selectedTorrentIdentity }
    }
    val showHomeAggregateDashboard = state.serverProfiles.isNotEmpty()
    val showServerStackReorderUi = state.serverProfiles.size > 1
    val dashboardServerSnapshotIds = remember(state.dashboardServerSnapshots) {
        state.dashboardServerSnapshots.map { it.profileId }
    }
    var localDashboardServerProfileOrder by remember {
        mutableStateOf(dashboardServerSnapshotIds)
    }
    LaunchedEffect(dashboardServerSnapshotIds) {
        localDashboardServerProfileOrder = reconcileReorderableItemOrder(
            currentOrder = localDashboardServerProfileOrder,
            availableItems = dashboardServerSnapshotIds,
        )
    }
    val orderedDashboardServerSnapshots = remember(
        state.dashboardServerSnapshots,
        localDashboardServerProfileOrder,
    ) {
        orderDashboardServerSnapshots(
            snapshots = state.dashboardServerSnapshots,
            orderedProfileIds = localDashboardServerProfileOrder,
        )
    }
    val selectedDashboardProfileId = state.selectedDashboardProfileId
        ?: state.activeServerProfileId
        ?: state.serverProfiles.firstOrNull()?.id
    val serverDashboardSessionKey = remember(
        selectedDashboardProfileId,
        state.dashboardSessionToken,
    ) {
        "${selectedDashboardProfileId.orEmpty()}:${state.dashboardSessionToken}"
    }
    val selectedDashboardSnapshot = remember(
        orderedDashboardServerSnapshots,
        selectedDashboardProfileId,
    ) {
        orderedDashboardServerSnapshots.firstOrNull { it.profileId == selectedDashboardProfileId }
    }
    val selectedServerProfile = remember(state.serverProfiles, selectedDashboardProfileId) {
        state.serverProfiles.firstOrNull { it.id == selectedDashboardProfileId }
    }
    val pendingDeleteProfile = remember(pendingDeleteProfileId, state.serverProfiles) {
        state.serverProfiles.firstOrNull { it.id == pendingDeleteProfileId }
    }
    val selectedDashboardBackendType = serverDashboardDisplay.backendType
    val serverDashboardCapabilities = remember(selectedDashboardBackendType) {
        defaultCapabilitiesFor(selectedDashboardBackendType)
    }
    val serverDashboardVersion = serverDashboardDisplay.serverVersion
    val serverDashboardTransferInfo = serverDashboardDisplay.transferInfo
    val serverDashboardTorrents = serverDashboardDisplay.torrents
    val serverDashboardTorrentCount = serverDashboardDisplay.torrentCount
    val serverDashboardShowContent = serverDashboardDisplay.hasContent
    val availableDashboardCards = serverDashboardDisplay.availableCards
    val currentDashboardPreferences = serverDashboardDisplay.resolvedPreferences
    val showServerStackHint = showServerStackReorderUi &&
        !state.settings.hasSeenServerStackReorderHint
    val showDashboardSwipeHint = selectedServerProfile != null &&
        !state.settings.hasSeenServerDashboardSwipeHint
    val showDashboardCardHint = selectedServerProfile != null &&
        !state.settings.hasSeenServerDashboardCardHint
    val dashboardListState = rememberLazyListState()
    val serverDashboardListState = rememberLazyListState()
    val torrentListState = rememberLazyListState()
    val torrentDetailListState = rememberLazyListState()
    val settingsListState = rememberLazyListState()
    var localDashboardCardOrder by remember(selectedServerProfile?.id, selectedDashboardBackendType) {
        mutableStateOf(parseDashboardCardOrder(currentDashboardPreferences.cardOrder, availableDashboardCards))
    }
    var localVisibleDashboardCardKeys by remember(selectedServerProfile?.id, selectedDashboardBackendType) {
        mutableStateOf(currentDashboardPreferences.visibleCards.toSet())
    }
    val visibleDashboardCards = remember(
        localDashboardCardOrder,
        localVisibleDashboardCardKeys,
    ) {
        localDashboardCardOrder.filter { card ->
            card.storageKey in localVisibleDashboardCardKeys
        }
    }
    val displayVisibleDashboardCards = remember(visibleDashboardCards) {
        buildDashboardDisplayCards(visibleDashboardCards)
    }
    val dashboardDragGestureKey = remember(displayVisibleDashboardCards) {
        displayVisibleDashboardCards.joinToString(separator = "|") { it.ownerKey }
    }
    val displayDashboardPreferences = remember(
        currentDashboardPreferences,
        localDashboardCardOrder,
        localVisibleDashboardCardKeys,
        visibleDashboardCards,
    ) {
        currentDashboardPreferences.copy(
            cardOrder = serializeDashboardCardOrder(localDashboardCardOrder, availableDashboardCards),
            visibleCards = visibleDashboardCards.map { it.storageKey },
        )
    }
    val dashboardReorder = remember { VerticalReorderUiState<DashboardDisplayCardItem>() }
    var dashboardDropJob by remember { mutableStateOf<Job?>(null) }
    var dashboardLockedCardHeights by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var revealedDashboardHideCardKey by remember(selectedServerProfile?.id, currentPage) {
        mutableStateOf<String?>(null)
    }
    val dashboardCardHeights = remember { mutableStateMapOf<String, Int>() }
    val serverReorder = remember { VerticalReorderUiState<String>() }
    var serverDropJob by remember { mutableStateOf<Job?>(null) }
    val pageListScrollEnabled = shouldEnablePageListScroll(
        draggingServerProfileId = serverReorder.draggingItem,
        draggingDashboardCard = dashboardReorder.draggingItem,
        settlingServerProfileId = serverReorder.settlingItem,
        settlingDashboardCard = dashboardReorder.settlingItem,
    )

    fun listStateForPage(page: AppPage): LazyListState {
        return when (page) {
            AppPage.DASHBOARD -> dashboardListState
            AppPage.SERVER_DASHBOARD -> serverDashboardListState
            AppPage.TORRENT_LIST -> torrentListState
            AppPage.TORRENT_DETAIL -> torrentDetailListState
            AppPage.SETTINGS -> settingsListState
        }
    }

    fun closeDrawer(action: () -> Unit) {
        action()
        scope.launch { drawerState.close() }
    }

    fun openSettings() {
        if (currentPage != AppPage.SETTINGS) {
            previousPage = currentPage
        }
        currentPage = AppPage.SETTINGS
    }

    fun openServerProfileSheet(editingProfileId: String? = null) {
        serverSheetEditingProfileId = editingProfileId.orEmpty()
        showServerProfileSheet = true
    }

    fun requestDeleteServerProfile(profileId: String) {
        pendingDeleteProfileId = profileId
    }

    fun copyToClipboard(value: String, successMessageRes: Int) {
        val text = value.trim()
        if (text.isBlank()) return
        clipboardManager.setText(AnnotatedString(text))
        scope.launch {
            snackbarHostState.showSnackbar(localContext.getString(successMessageRes))
        }
    }

    fun requestTorrentExport(hash: String, torrentName: String) {
        val normalizedHash = hash.trim()
        if (normalizedHash.isBlank()) return
        pendingTorrentExportHash = normalizedHash
        pendingTorrentExportName = buildTorrentExportFileName(
            torrentName = torrentName,
            hash = normalizedHash,
        )
        exportTorrentLauncher.launch(pendingTorrentExportName)
    }

    fun openTorrentList() {
        if (currentPage != AppPage.TORRENT_LIST) {
            previousPage = currentPage
        }
        currentPage = AppPage.TORRENT_LIST
    }

    fun openTorrentListFromDashboard() {
        if (!state.settings.homeTorrentEntryHintDismissed) {
            viewModel.dismissHomeTorrentEntryHint()
        }
        openTorrentList()
    }

    fun openServerDashboard(profileId: String) {
        if (profileId.isBlank()) return
        if (currentPage != AppPage.SERVER_DASHBOARD) {
            previousPage = currentPage
        }
        currentPage = AppPage.SERVER_DASHBOARD
        viewModel.switchServerProfile(profileId)
    }

    fun openTorrentDetail(torrent: TorrentInfo) {
        val torrentIdentity = torrentIdentityKey(torrent)
        selectedTorrentIdentity = torrentIdentity
        pendingTorrentReturnIdentity = if (currentPage == AppPage.TORRENT_LIST) {
            torrentIdentity
        } else {
            ""
        }
        if (currentPage != AppPage.TORRENT_DETAIL) {
            previousPage = currentPage
        }
        currentPage = AppPage.TORRENT_DETAIL
    }

    fun backToPreviousPage() {
        val nextPage = if (previousPage == currentPage) AppPage.DASHBOARD else previousPage
        if (currentPage == AppPage.TORRENT_DETAIL && nextPage != AppPage.TORRENT_LIST) {
            pendingTorrentReturnIdentity = ""
        }
        currentPage = nextPage
    }

    fun requestScrollToFirstTorrentAfterSort() {
        sortScrollRequestId += 1
    }

    fun persistDashboardCardOrderIfChanged(
        nextOrder: List<DashboardChartCard>,
        rollbackOrder: List<DashboardChartCard> = nextOrder,
    ) {
        val profileId = selectedServerProfile?.id ?: return
        val serialized = serializeDashboardCardOrder(nextOrder, availableDashboardCards)
        if (serialized != currentDashboardPreferences.cardOrder) {
            viewModel.updateServerDashboardCardOrder(profileId, nextOrder) { success ->
                if (!success && state.activeServerProfileId == profileId) {
                    localDashboardCardOrder = rollbackOrder
                }
            }
        }
    }

    fun startDashboardCardDrag(card: DashboardDisplayCardItem) {
        val visibleCards = displayVisibleDashboardCards
        val startIndex = visibleCards.indexOf(card)
        if (startIndex < 0) return
        revealedDashboardHideCardKey = null
        val itemSpacingPx = with(density) { DashboardCardSpacing.toPx() }
        val defaultCardHeightPx = with(density) { 180.dp.toPx() }
        val slotHeights = visibleCards.map { dashboardCard ->
            dashboardCardHeights[dashboardCard.ownerKey]?.toFloat() ?: defaultCardHeightPx
        }
        val slotTops = buildList {
            var currentTop = 0f
            visibleCards.forEachIndexed { index, _ ->
                add(currentTop)
                currentTop += slotHeights[index] + itemSpacingPx
            }
        }
        val edgeSlackPx = with(density) { 24.dp.toPx() }
        dashboardDropJob?.cancel()
        dashboardDropJob = null
        val session = buildVerticalReorderSession(
            items = visibleCards,
            startIndex = startIndex,
            slotTops = slotTops,
            slotHeights = slotHeights,
            edgeSlackPx = edgeSlackPx,
        )
        Snapshot.withMutableSnapshot {
            dashboardLockedCardHeights = visibleCards.associate { dashboardCard ->
                val measuredHeight = dashboardCardHeights[dashboardCard.ownerKey] ?: defaultCardHeightPx.roundToInt()
                dashboardCard.ownerKey to measuredHeight
            }
            dashboardReorder.start(session = session, item = card)
        }
        selectedServerProfile?.id?.let(viewModel::setDashboardReorderHold)
    }

    fun updateDashboardCardDrag(card: DashboardDisplayCardItem, deltaY: Float) {
        dashboardReorder.applyDelta(item = card, deltaY = deltaY)
    }

    fun finishDashboardCardDrag(commit: Boolean) {
        if (dashboardReorder.draggingItem == null) return
        val dragState = dashboardReorder.snapshotDragState() ?: return
        val draggedCard = dragState.item
        val dragSession = dragState.session
        val previousOrder = localDashboardCardOrder
        val finalTargetIndex = resolveVerticalReorderFinalTargetIndex(
            state = dragState,
            commit = commit,
        )
        val finalOffsetY = resolveVerticalReorderRestingOffset(
            state = dragState,
            commit = commit,
        )
        val nextOrder = if (commit) {
            reorderDashboardCardOrderForDisplay(
                order = localDashboardCardOrder,
                displayCards = dragSession.items,
                owner = draggedCard.owner,
                targetIndex = finalTargetIndex,
            )
        } else {
            previousOrder
        }
        dashboardDropJob?.cancel()
        dashboardReorder.beginSettle(finalTargetIndex)
        dashboardDropJob = scope.launch {
            val startOffsetY = dashboardReorder.offsetY
            if (abs(startOffsetY - finalOffsetY) > 0.5f) {
                val animatable = Animatable(startOffsetY)
                animatable.animateTo(
                    targetValue = finalOffsetY,
                    animationSpec = ReorderSettleAnimationSpec,
                ) {
                    dashboardReorder.updateSettleOffset(value)
                }
            }
            val shouldPersistOrder = commit && nextOrder != previousOrder
            Snapshot.withMutableSnapshot {
                if (shouldPersistOrder) {
                    localDashboardCardOrder = nextOrder
                }
                dashboardReorder.clear()
                dashboardLockedCardHeights = emptyMap()
                dashboardDropJob = null
            }
            if (shouldPersistOrder) {
                persistDashboardCardOrderIfChanged(nextOrder, previousOrder)
            }
            viewModel.setDashboardReorderHold(null)
        }
    }

    fun endDashboardCardDrag() = finishDashboardCardDrag(commit = true)

    fun cancelDashboardCardDrag() = finishDashboardCardDrag(commit = false)

    fun hideDashboardCard(card: DashboardDisplayCardItem) {
        val profileId = selectedServerProfile?.id ?: return
        val previousVisibleKeys = localVisibleDashboardCardKeys
        val nextVisibleKeys = applyDashboardDisplayCardVisibility(
            visibleKeys = previousVisibleKeys,
            displayCard = card,
            visible = false,
        )
        localVisibleDashboardCardKeys = nextVisibleKeys
        revealedDashboardHideCardKey = null
        viewModel.updateServerDashboardCardsVisibility(
            profileId = profileId,
            cards = card.representedCards,
            visible = false,
        ) { success ->
            if (!success && state.activeServerProfileId == profileId) {
                localVisibleDashboardCardKeys = previousVisibleKeys
            }
        }
    }

    fun startServerStackDrag(profileId: String) {
        val orderedIds = orderedDashboardServerSnapshots.map { it.profileId }
        val startIndex = orderedIds.indexOf(profileId)
        if (startIndex < 0 || orderedIds.size < 2) return
        val exposedStepPx = with(density) { HomeServerStackExposedHeight.toPx() }
        val edgeSlackPx = with(density) { 24.dp.toPx() }
        serverDropJob?.cancel()
        serverDropJob = null
        val session = buildHomeServerStackReorderSession(
            orderedProfileIds = orderedIds,
            startIndex = startIndex,
            exposedStepPx = exposedStepPx,
            edgeSlackPx = edgeSlackPx,
        )
        serverReorder.start(session = session, item = profileId)
        viewModel.setServerStackReorderHold(true)
    }

    fun updateServerStackDrag(profileId: String, deltaY: Float) {
        serverReorder.applyDelta(item = profileId, deltaY = deltaY)
    }

    fun finishServerStackDrag(commit: Boolean) {
        if (serverReorder.draggingItem == null) return
        val dragState = serverReorder.snapshotDragState() ?: return
        val dropPlan = resolveHomeServerStackDropPlan(
            state = dragState,
            commit = commit,
        )
        serverDropJob?.cancel()
        serverReorder.beginSettle(dropPlan.finalTargetIndex)
        serverDropJob = scope.launch {
            val startOffsetY = serverReorder.offsetY
            if (abs(startOffsetY - dropPlan.finalOffsetY) > 0.5f) {
                val animatable = Animatable(startOffsetY)
                animatable.animateTo(
                    targetValue = dropPlan.finalOffsetY,
                    animationSpec = ReorderSettleAnimationSpec,
                ) {
                    serverReorder.updateSettleOffset(value)
                }
            }
            Snapshot.withMutableSnapshot {
                if (dropPlan.shouldCommitReorder) {
                    localDashboardServerProfileOrder = dropPlan.reorderedIds
                }
                serverReorder.clear()
                serverDropJob = null
            }
            viewModel.setServerStackReorderHold(false)
            if (dropPlan.shouldCommitReorder) {
                viewModel.reorderServerProfiles(dropPlan.reorderedIds)
            }
        }
    }

    fun endServerStackDrag() = finishServerStackDrag(commit = true)

    fun cancelServerStackDrag() = finishServerStackDrag(commit = false)

    fun scrollToTopOfCurrentPage(animated: Boolean) {
        scope.launch {
            val targetListState = listStateForPage(currentPage)
            if (animated) {
                targetListState.animateScrollToItem(0)
            } else {
                targetListState.scrollToItem(0)
            }
        }
    }

    LaunchedEffect(sortScrollRequestId) {
        if (sortScrollRequestId <= 0) return@LaunchedEffect
        if (currentPage != AppPage.TORRENT_LIST) return@LaunchedEffect
        val targetIndex = if (showTorrentSearchBar && visibleTorrentItems.isNotEmpty()) 1 else 0
        torrentListState.scrollToItem(targetIndex)
        // Guard against LazyList position restore after data reordering.
        yield()
        torrentListState.scrollToItem(targetIndex)
    }

    LaunchedEffect(
        currentPage,
        pendingTorrentReturnIdentity,
        visibleTorrentItems,
        showTorrentSearchBar,
    ) {
        if (currentPage != AppPage.TORRENT_LIST) return@LaunchedEffect
        val anchorIdentity = pendingTorrentReturnIdentity.ifBlank { return@LaunchedEffect }
        val targetListIndex = visibleTorrentItems.indexOfFirst { it.identityKey == anchorIdentity }
        pendingTorrentReturnIdentity = ""
        if (targetListIndex < 0) return@LaunchedEffect
        val targetIndex = targetListIndex + if (showTorrentSearchBar) 1 else 0
        torrentListState.scrollToItem(targetIndex)
        yield()
        torrentListState.scrollToItem(targetIndex)
    }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message.resolve(localContext))
        viewModel.dismissError()
    }

    LaunchedEffect(currentDashboardPreferences.cardOrder, selectedServerProfile?.id, selectedDashboardBackendType) {
        localDashboardCardOrder = parseDashboardCardOrder(
            currentDashboardPreferences.cardOrder,
            availableDashboardCards,
        )
    }

    LaunchedEffect(currentDashboardPreferences.visibleCards, selectedServerProfile?.id, selectedDashboardBackendType) {
        localVisibleDashboardCardKeys = currentDashboardPreferences.visibleCards.toSet()
    }

    LaunchedEffect(revealedDashboardHideCardKey, selectedServerProfile?.id, currentPage) {
        val currentKey = revealedDashboardHideCardKey ?: return@LaunchedEffect
        if (currentPage != AppPage.SERVER_DASHBOARD) return@LaunchedEffect
        delay(5_000)
        if (revealedDashboardHideCardKey == currentKey) {
            revealedDashboardHideCardKey = null
        }
    }

    LaunchedEffect(currentPage) {
        if (currentPage != AppPage.DASHBOARD) {
            serverDropJob?.cancel()
            serverDropJob = null
            serverReorder.clear()
        }
        if (currentPage != AppPage.SERVER_DASHBOARD) {
            showDashboardCardManagerSheet = false
            revealedDashboardHideCardKey = null
            dashboardDropJob?.cancel()
            dashboardDropJob = null
            dashboardReorder.clear()
            dashboardLockedCardHeights = emptyMap()
            viewModel.setDashboardReorderHold(null)
        }
    }

    LaunchedEffect(currentPage, selectedTorrent?.hash) {
        if (currentPage != AppPage.TORRENT_LIST) {
            showTorrentSortMenu = false
        }
        val hash = selectedTorrent?.hash.orEmpty()
        val refreshScene = when (currentPage) {
            AppPage.DASHBOARD -> RefreshScene.DASHBOARD
            AppPage.SERVER_DASHBOARD -> RefreshScene.SERVER
            AppPage.TORRENT_LIST -> RefreshScene.SERVER
            AppPage.TORRENT_DETAIL -> RefreshScene.TORRENT_DETAIL
            AppPage.SETTINGS -> RefreshScene.SETTINGS
        }
        viewModel.updateRefreshScene(refreshScene)
        if (currentPage == AppPage.TORRENT_DETAIL && hash.isNotBlank()) {
            viewModel.loadTorrentDetail(hash)
        }
    }

    LaunchedEffect(currentPage, serverDashboardSessionKey) {
        if (currentPage == AppPage.SERVER_DASHBOARD) {
            serverDashboardListState.scrollToItem(0)
        }
    }

    BackHandler(enabled = currentPage != AppPage.DASHBOARD) {
        backToPreviousPage()
    }

    androidx.compose.material3.ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            androidx.compose.material3.ModalDrawerSheet(
                modifier = Modifier
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                drawerContainerColor = qbGlassStrongContainerColor(),
                drawerContentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                DrawerThemeItem(
                    settings = state.settings,
                    onThemeChange = { theme ->
                        closeDrawer { viewModel.updateAppTheme(theme) }
                    },
                    onApplyCustomTheme = { imagePath, toneIsLight ->
                        closeDrawer {
                            viewModel.applyCustomThemeBackground(imagePath, toneIsLight)
                        }
                    },
                )
            }
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentPage) {
                    val edgeWidthPx = with(density) { 36.dp.toPx() }
                    val triggerDistancePx = with(density) { 90.dp.toPx() }
                    var trackingFromEdge = false
                    var dragDistance = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            trackingFromEdge = offset.x <= edgeWidthPx
                            dragDistance = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            if (!trackingFromEdge) return@detectHorizontalDragGestures
                            if (dragAmount > 0f) {
                                dragDistance += dragAmount
                            }
                            if (dragDistance >= triggerDistancePx && currentPage != AppPage.DASHBOARD) {
                                backToPreviousPage()
                                trackingFromEdge = false
                                dragDistance = 0f
                            }
                            change.consume()
                        },
                        onDragEnd = {
                            trackingFromEdge = false
                            dragDistance = 0f
                        },
                        onDragCancel = {
                            trackingFromEdge = false
                            dragDistance = 0f
                        },
                    )
                },
        ) {
            MainScreenBackdrop(
                appTheme = state.settings.appTheme,
                customBackgroundAvailable = state.customBackgroundAvailable,
                customBackgroundImagePath = state.settings.customBackgroundImagePath,
                customBackgroundToneIsLight = state.settings.customBackgroundToneIsLight,
            )
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                ),
                topBar = {
                    MainScreenTopBar(
                        currentPage = currentPage,
                        connected = state.connected,
                        isManualRefreshing = state.isManualRefreshing,
                        sortOption = torrentListFilterState.sortOption,
                        sortDescending = torrentListFilterState.descending,
                        showSearchBar = showTorrentSearchBar,
                        showSortMenu = showTorrentSortMenu,
                        onSortMenuVisibilityChange = { visible -> showTorrentSortMenu = visible },
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onBack = { backToPreviousPage() },
                        onScrollToTop = { scrollToTopOfCurrentPage(animated = true) },
                        onOpenServerSheet = { openServerProfileSheet() },
                        onOpenSettings = { openSettings() },
                        onOpenCardManager = {
                            viewModel.markServerDashboardCardHintSeen()
                            showDashboardCardManagerSheet = true
                        },
                        onRefreshOrConnect = {
                            if (state.connected) viewModel.refresh(manual = true) else viewModel.connect()
                        },
                        onSortOptionSelected = { option ->
                            viewModel.updateTorrentListSortOption(option)
                            showTorrentSortMenu = false
                            requestScrollToFirstTorrentAfterSort()
                        },
                        onSortDirectionSelected = { descending ->
                            viewModel.updateTorrentListSortDirection(descending)
                            showTorrentSortMenu = false
                            requestScrollToFirstTorrentAfterSort()
                        },
                        onToggleSearchBar = {
                            showTorrentSearchBar = !showTorrentSearchBar
                            if (!showTorrentSearchBar) {
                                viewModel.updateTorrentSearchQuery("")
                            }
                            scope.launch {
                                torrentListState.scrollToItem(0)
                            }
                        },
                        onOpenAddTorrent = {
                            viewModel.loadGlobalSelectionOptions()
                            showAddTorrentSheet = true
                        },
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { innerPadding ->
                val placeholderFlags = resolveMainScreenPlaceholderFlags(
                    state = state,
                    selectedTorrentPresent = selectedTorrent != null,
                    selectedTorrentIdentity = selectedTorrentIdentity,
                    selectedServerProfilePresent = selectedServerProfile != null,
                    selectedDashboardSnapshotPresent = selectedDashboardSnapshot != null,
                    showHomeAggregateDashboard = showHomeAggregateDashboard,
                )
                val contentModifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))

                val contentList: @Composable (AppPage) -> Unit = { page ->
                    LazyColumn(
                        state = listStateForPage(page),
                        modifier = contentModifier,
                        userScrollEnabled = pageListScrollEnabled,
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        when (page) {
                            AppPage.DASHBOARD -> dashboardHomePageContent(
                                state = state,
                                dashboardServerSnapshots = orderedDashboardServerSnapshots,
                                showDashboardSnapshot = placeholderFlags.showDashboardSnapshot,
                                showHomeAggregateDashboard = showHomeAggregateDashboard,
                                showDashboardSkeleton = placeholderFlags.showDashboardSkeleton,
                                showServerStackHint = showServerStackHint,
                                draggingProfileId = serverReorder.activeItem,
                                settlingProfileId = serverReorder.settlingItem,
                                draggingOffsetY = { if (serverReorder.settlingItem != null) 0f else serverReorder.offsetY },
                                settlingOffsetY = { if (serverReorder.settlingItem != null) serverReorder.offsetY else 0f },
                                draggingTargetIndex = serverReorder.targetIndex,
                                dragSession = serverReorder.session,
                                onDismissReorderHint = viewModel::markServerStackReorderHintSeen,
                                onStartServerStackDrag = { profileId ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.markServerStackReorderHintSeen()
                                    startServerStackDrag(profileId)
                                },
                                onDragServerStack = ::updateServerStackDrag,
                                onEndServerStackDrag = ::endServerStackDrag,
                                onCancelServerStackDrag = ::cancelServerStackDrag,
                                onOpenServerDashboard = ::openServerDashboard,
                                onOpenGlobalSpeedLimits = viewModel::openGlobalSpeedLimitDialog,
                                onDismissHomeTorrentEntryHint = viewModel::dismissHomeTorrentEntryHint,
                                onOpenTorrentList = ::openTorrentListFromDashboard,
                                onSwitchServerProfile = viewModel::switchServerProfile,
                                onEditServerProfile = { profileId -> openServerProfileSheet(profileId) },
                                onRequestDeleteServerProfile = ::requestDeleteServerProfile,
                                onOpenConnection = ::openSettings,
                            )

                            AppPage.SERVER_DASHBOARD -> serverDashboardRootPageContent(
                                sessionKey = serverDashboardSessionKey,
                                selectedServerProfile = selectedServerProfile,
                                showContent = serverDashboardShowContent,
                                showDashboardCardHint = showDashboardCardHint,
                                showDashboardSwipeHint = showDashboardSwipeHint,
                                showSkeleton = placeholderFlags.showServerDashboardSkeleton,
                                showRestorePlaceholder = placeholderFlags.showRestorePlaceholder,
                                selectedDashboardBackendType = selectedDashboardBackendType,
                                serverDashboardCapabilities = serverDashboardCapabilities,
                                serverDashboardDisplay = serverDashboardDisplay,
                                serverDashboardVersion = serverDashboardVersion,
                                serverDashboardTransferInfo = serverDashboardTransferInfo,
                                serverDashboardTorrents = serverDashboardTorrents,
                                serverDashboardTorrentCount = serverDashboardTorrentCount,
                                displayVisibleDashboardCards = displayVisibleDashboardCards,
                                dashboardDragGestureKey = dashboardDragGestureKey,
                                draggingDashboardCard = dashboardReorder.activeItem,
                                settlingDashboardCard = dashboardReorder.settlingItem,
                                draggingDashboardOffsetY = { dashboardReorder.offsetY },
                                settlingDashboardOffsetY = { dashboardReorder.offsetY },
                                draggingDashboardTargetIndex = dashboardReorder.targetIndex,
                                draggingDashboardSession = dashboardReorder.session,
                                revealedDashboardHideCardKey = revealedDashboardHideCardKey,
                                dashboardCardHeights = dashboardCardHeights,
                                dashboardLockedCardHeights = dashboardLockedCardHeights,
                                onRevealHideCard = { card -> revealedDashboardHideCardKey = card.ownerKey },
                                onHideCard = ::hideDashboardCard,
                                onStartCardDrag = { card ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    startDashboardCardDrag(card)
                                },
                                onDragCard = ::updateDashboardCardDrag,
                                onEndCardDrag = ::endDashboardCardDrag,
                                onCancelCardDrag = ::cancelDashboardCardDrag,
                                onOpenManager = {
                                    viewModel.markServerDashboardCardHintSeen()
                                    showDashboardCardManagerSheet = true
                                },
                                onOpenTorrentList = ::openTorrentListFromDashboard,
                                onSwitchServerProfile = viewModel::switchServerProfile,
                                onEditServerProfile = { profileId -> openServerProfileSheet(profileId) },
                                onRequestDeleteServerProfile = ::requestDeleteServerProfile,
                                onDismissHomeTorrentEntryHint = viewModel::dismissHomeTorrentEntryHint,
                                onMarkDashboardSwipeHintSeen = viewModel::markServerDashboardSwipeHintSeen,
                                onMarkDashboardCardHintSeen = viewModel::markServerDashboardCardHintSeen,
                                onOpenConnection = ::openSettings,
                            )

                            AppPage.TORRENT_LIST -> {
                                torrentListPageContent(
                                    showContent = placeholderFlags.showTorrentListContent,
                                    showRestorePlaceholder = placeholderFlags.showRestorePlaceholder,
                                    showSearchBar = showTorrentSearchBar,
                                    animatePlacement = animateTorrentPlacement,
                                    searchQuery = torrentListFilterState.query,
                                    onSearchQueryChange = viewModel::updateTorrentSearchQuery,
                                    filterState = torrentListFilterState,
                                    onStateFilterChange = viewModel::updateTorrentListStateFilter,
                                    onCategoryFilterChange = viewModel::updateTorrentListCategoryFilter,
                                    onTagFilterChange = viewModel::updateTorrentListTagFilter,
                                    categoryOptions = state.categoryOptions,
                                    tagOptions = state.tagOptions,
                                    visibleItems = visibleTorrentItems,
                                    isPendingAction = ::isPendingAction,
                                    onOpenDetails = ::openTorrentDetail,
                                    onOpenConnection = { openSettings() },
                                )
                            }

                            AppPage.TORRENT_DETAIL -> torrentDetailPageContent(
                                selectedTorrent = selectedTorrent,
                                selectedTorrentIdentity = selectedTorrentIdentity,
                                showRestorePlaceholder = placeholderFlags.showTorrentDetailRestorePlaceholder,
                                crossSeedCounts = crossSeedCounts,
                                state = state,
                                isPendingAction = ::isPendingAction,
                                onCopyHash = { hash ->
                                    copyToClipboard(hash, R.string.detail_hash_copied)
                                },
                                onCopyMagnet = { magnetUri ->
                                                    copyToClipboard(magnetUri, R.string.detail_magnet_copied)
                                },
                                onExportTorrent = { hash, name ->
                                                    requestTorrentExport(
                                                        hash = hash,
                                                        torrentName = name,
                                                    )
                                },
                                onPauseTorrent = viewModel::pauseTorrent,
                                onResumeTorrent = viewModel::resumeTorrent,
                                onDeleteTorrent = viewModel::deleteTorrent,
                                onRenameTorrent = viewModel::renameTorrent,
                                onSetTorrentLocation = viewModel::setTorrentLocation,
                                onSetTorrentCategory = viewModel::setTorrentCategory,
                                onSetTorrentTags = viewModel::setTorrentTags,
                                onSetTorrentSpeedLimit = viewModel::setTorrentSpeedLimit,
                                onSetTorrentShareRatio = viewModel::setTorrentShareRatio,
                                onReannounceTorrent = viewModel::reannounceTorrent,
                                onRecheckTorrent = viewModel::recheckTorrent,
                                onCopyTracker = { tracker ->
                                    copyToClipboard(tracker.url, R.string.detail_tracker_copied)
                                },
                                onEditTracker = { hash, tracker, newUrl ->
                                    viewModel.editTracker(hash, tracker, newUrl)
                                },
                                onDeleteTracker = { hash, tracker ->
                                    viewModel.removeTracker(hash, tracker)
                                },
                            )

                            AppPage.SETTINGS -> settingsRootPageContent(
                                state = state,
                                onAppLanguageChange = viewModel::updateAppLanguage,
                                onDeleteFilesWhenNoSeedersChange = viewModel::updateDeleteFilesWhenNoSeeders,
                                onDeleteFilesDefaultChange = viewModel::updateDeleteFilesDefault,
                                onCompletionNotificationsChange = viewModel::updateCompletionNotificationsEnabled,
                                onBackendTypeChange = viewModel::updateServerBackendType,
                                onHostChange = viewModel::updateHost,
                                onPortChange = viewModel::updatePort,
                                onHttpsChange = viewModel::updateUseHttps,
                                onUserChange = viewModel::updateUsername,
                                onPasswordChange = viewModel::updatePassword,
                                onRefreshSecondsChange = viewModel::updateRefreshSeconds,
                                onConnect = {
                                    viewModel.connect()
                                    currentPage = AppPage.DASHBOARD
                                },
                            )
                        }
                    }
                }

                val animatedPageTarget = remember(currentPage, serverDashboardSessionKey, pageThemeSignature) {
                    PageAnimationState(
                        page = currentPage,
                        dashboardSessionKey = if (currentPage == AppPage.SERVER_DASHBOARD) {
                            serverDashboardSessionKey
                        } else {
                            ""
                        },
                        themeSignature = pageThemeSignature,
                    )
                }
                val animatedPageContent: @Composable () -> Unit = {
                    AnimatedContent(
                        targetState = animatedPageTarget,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(durationMillis = 180)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = 140))
                        },
                        label = "pageAnimation",
                    ) { pageState ->
                        key(pageState.page, pageState.dashboardSessionKey, pageState.themeSignature) {
                            contentList(pageState.page)
                        }
                    }
                }

                if (
                    (currentPage == AppPage.DASHBOARD || currentPage == AppPage.SERVER_DASHBOARD) &&
                    (state.connected || state.serverProfiles.isNotEmpty())
                ) {
                    val pullRefreshState = rememberPullRefreshState(
                        refreshing = state.isManualRefreshing,
                        onRefresh = { viewModel.refresh(manual = true) },
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pullRefresh(pullRefreshState),
                    ) {
                        animatedPageContent()
                        PullRefreshIndicator(
                            refreshing = state.isManualRefreshing,
                            state = pullRefreshState,
                            modifier = Modifier.align(Alignment.TopCenter),
                            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    animatedPageContent()
                }
            }

            MainScreenSheets(
                showServerProfileSheet = showServerProfileSheet,
                serverProfileSheetState = serverProfileSheetState,
                serverProfiles = state.serverProfiles,
                activeServerProfileId = state.activeServerProfileId,
                serverSheetEditingProfileId = serverSheetEditingProfileId,
                onDismissServerProfileSheet = {
                    showServerProfileSheet = false
                    serverSheetEditingProfileId = ""
                },
                onSwitchServerProfile = { profileId ->
                    viewModel.switchServerProfile(profileId)
                    showServerProfileSheet = false
                    serverSheetEditingProfileId = ""
                    currentPage = AppPage.DASHBOARD
                },
                onAddServerProfile = { name, backendType, host, port, useHttps, username, password, refreshSeconds ->
                    viewModel.addServerProfile(
                        name = name,
                        backendType = backendType,
                        host = host,
                        port = port,
                        useHttps = useHttps,
                        username = username,
                        password = password,
                        refreshSeconds = refreshSeconds,
                    )
                    showServerProfileSheet = false
                    serverSheetEditingProfileId = ""
                    currentPage = AppPage.DASHBOARD
                },
                onUpdateServerProfile = { profileId, name, backendType, host, port, useHttps, username, password, refreshSeconds ->
                    viewModel.updateServerProfile(
                        profileId = profileId,
                        name = name,
                        backendType = backendType,
                        host = host,
                        port = port,
                        useHttps = useHttps,
                        username = username,
                        password = password,
                        refreshSeconds = refreshSeconds,
                    )
                    showServerProfileSheet = false
                    serverSheetEditingProfileId = ""
                },
                onRequestDeleteServerProfile = ::requestDeleteServerProfile,
                showAddTorrentSheet = showAddTorrentSheet,
                addTorrentSheetState = addTorrentSheetState,
                addTorrentCapabilities = state.activeCapabilities,
                categoryOptionsForAdd = categoryOptionsForAdd,
                tagOptionsForAdd = tagOptionsForAdd,
                pathOptionsForAdd = pathOptionsForAdd,
                addTorrentInitialUrls = state.sharedTorrentInput?.urls.orEmpty(),
                onDismissAddTorrentSheet = { showAddTorrentSheet = false },
                onCancelAddTorrent = {
                    showAddTorrentSheet = false
                    viewModel.clearSharedMagnetUrl()
                },
                onAddTorrent = { urls, files, autoTmm, category, tags, savePath, paused, skipChecking, sequential, firstLast, upKb, dlKb ->
                    viewModel.addTorrent(
                        urls = urls,
                        files = files,
                        autoTmm = autoTmm,
                        category = category,
                        tags = tags,
                        savePath = savePath,
                        paused = paused,
                        skipChecking = skipChecking,
                        sequentialDownload = sequential,
                        firstLastPiecePrio = firstLast,
                        uploadLimitKb = upKb,
                        downloadLimitKb = dlKb,
                    )
                    showAddTorrentSheet = false
                },
                showDashboardCardManagerSheet = showDashboardCardManagerSheet,
                dashboardCardManagerSheetState = dashboardCardManagerSheetState,
                cardManagerProfileId = selectedServerProfile?.id,
                availableDashboardCards = availableDashboardCards,
                displayDashboardPreferences = displayDashboardPreferences,
                onToggleDashboardCard = { currentProfileId, card, visible ->
                    val previousVisibleKeys = localVisibleDashboardCardKeys
                    localVisibleDashboardCardKeys = if (visible) {
                        previousVisibleKeys + card.storageKey
                    } else {
                        previousVisibleKeys - card.storageKey
                    }
                    viewModel.updateServerDashboardCardVisibility(
                        currentProfileId,
                        card,
                        visible,
                    ) { success ->
                        if (!success && currentProfileId == state.activeServerProfileId) {
                            localVisibleDashboardCardKeys = previousVisibleKeys
                        }
                    }
                },
                onResetDashboardCards = { currentProfileId ->
                    val previousOrder = localDashboardCardOrder
                    val previousVisibleKeys = localVisibleDashboardCardKeys
                    val defaults = defaultServerDashboardPreferencesForBackend(selectedDashboardBackendType)
                    localDashboardCardOrder = parseDashboardCardOrder(
                        defaults.cardOrder,
                        availableDashboardCards,
                    )
                    localVisibleDashboardCardKeys = defaults.visibleCards.toSet()
                    viewModel.resetServerDashboardPreferences(currentProfileId) { success ->
                        if (!success && currentProfileId == state.activeServerProfileId) {
                            localDashboardCardOrder = previousOrder
                            localVisibleDashboardCardKeys = previousVisibleKeys
                        }
                    }
                },
                onDismissCardManagerSheet = { showDashboardCardManagerSheet = false },
            )

            MainScreenDialogs(
                pendingDeleteProfile = pendingDeleteProfile,
                activeServerProfileId = state.activeServerProfileId,
                onDismissDeleteProfile = { pendingDeleteProfileId = "" },
                onConfirmDeleteProfile = { profile ->
                    if (
                        currentPage == AppPage.SERVER_DASHBOARD &&
                        profile.id == state.activeServerProfileId
                    ) {
                        currentPage = AppPage.DASHBOARD
                    }
                    viewModel.deleteServerProfile(profile.id)
                    pendingDeleteProfileId = ""
                    showServerProfileSheet = false
                    serverSheetEditingProfileId = ""
                },
                pendingBackendRepair = state.pendingBackendRepair,
                onConfirmPendingBackendRepair = viewModel::confirmPendingBackendRepair,
                onDismissPendingBackendRepair = viewModel::dismissPendingBackendRepair,
                globalSpeedLimitDialogVisible = state.globalSpeedLimitDialogVisible,
                serverProfiles = state.serverProfiles,
                globalSpeedLimitProfileId = state.globalSpeedLimitProfileId,
                globalSpeedLimits = state.globalSpeedLimits,
                globalSpeedLimitLoading = state.globalSpeedLimitLoading,
                globalSpeedLimitSaving = state.globalSpeedLimitSaving,
                globalSpeedLimitLoadFailed = state.globalSpeedLimitLoadFailed,
                onGlobalSpeedLimitProfileSelected = viewModel::selectGlobalSpeedLimitProfile,
                onRetryGlobalSpeedLimitLoad = viewModel::retryGlobalSpeedLimitLoad,
                onDismissGlobalSpeedLimitDialog = viewModel::dismissGlobalSpeedLimitDialog,
                onSaveGlobalSpeedLimits = viewModel::saveGlobalSpeedLimits,
            )

        }
    }
}
