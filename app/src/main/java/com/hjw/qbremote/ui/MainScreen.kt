package com.hjw.qbremote.ui
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.hjw.qbremote.data.AppLanguage
import com.hjw.qbremote.data.AppTheme
import com.hjw.qbremote.R
import com.hjw.qbremote.data.ChartSortMode
import com.hjw.qbremote.data.ConnectionSettings
import com.hjw.qbremote.data.ServerProfile
import com.hjw.qbremote.data.model.AddTorrentFile
import com.hjw.qbremote.data.model.CountryUploadRecord
import com.hjw.qbremote.data.model.TorrentFileInfo
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.data.model.TorrentProperties
import com.hjw.qbremote.data.model.TransferInfo
import com.hjw.qbremote.ui.theme.qbGlassCardColors
import com.hjw.qbremote.ui.theme.qbGlassChipColor
import com.hjw.qbremote.ui.theme.qbGlassOutlineColor
import com.hjw.qbremote.ui.theme.qbGlassStrongContainerColor
import com.hjw.qbremote.ui.theme.qbGlassSubtleContainerColor
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.net.URI
import java.util.Locale
import kotlin.math.roundToInt

private data class SiteChartEntry(
    val site: String,
    val torrentCount: Int,
    val downloadSpeed: Long,
    val uploadSpeed: Long,
    val totalSpeed: Long,
)

private data class DashboardStateSummary(
    val uploadingCount: Int = 0,
    val downloadingCount: Int = 0,
    val pausedUploadCount: Int = 0,
    val pausedDownloadCount: Int = 0,
    val errorCount: Int = 0,
    val checkingCount: Int = 0,
    val waitingCount: Int = 0,
)

private data class DashboardStatusPillItem(
    val label: String,
    val count: Int,
    val accentColor: Color,
)

private data class DashboardDragSession(
    val visibleCards: List<DashboardChartCard>,
    val indexByCard: Map<DashboardChartCard, Int>,
    val startIndex: Int,
    val swapTriggerPx: Float,
    val shiftDistancePx: Float,
    val maxUpwardOffset: Float,
    val maxDownwardOffset: Float,
)

data class PieLegendEntry(
    val label: String,
    val value: Long,
    val valueText: String,
)

private enum class AppPage {
    DASHBOARD,
    TORRENT_LIST,
    TORRENT_DETAIL,
    SETTINGS,
}

private enum class TorrentListSortOption {
    ADDED_TIME,
    UPLOAD_SPEED,
    DOWNLOAD_SPEED,
    SHARE_RATIO,
    TOTAL_UPLOADED,
    TOTAL_DOWNLOADED,
    TORRENT_SIZE,
    ACTIVITY_TIME,
    SEEDERS,
    LEECHERS,
    CROSS_SEED_COUNT,
}

enum class DashboardChartCard(
    val storageKey: String,
) {
    COUNTRY_FLOW("country_flow"),
    CATEGORY_SHARE("category_share"),
    DAILY_UPLOAD("daily_upload"),
}

val PanelShape = RoundedCornerShape(20.dp)
private val DarkBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF060A12),
        Color(0xFF0B1422),
        Color(0xFF08131E),
        Color(0xFF060A12),
    ),
)
private val LightBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFF5FAFF),
        Color(0xFFEAF3FC),
        Color(0xFFE4F0F9),
        Color(0xFFF6FAFF),
    ),
)
val DashboardPiePalette = listOf(
    Color(0xFF4C8DFF),
    Color(0xFF33BC84),
    Color(0xFFF3A53C),
    Color(0xFFA77AF2),
    Color(0xFFEF6D5E),
    Color(0xFF19B1C3),
    Color(0xFF8F9FB7),
    Color(0xFFFFCF5C),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = androidx.compose.material3.rememberDrawerState(
        initialValue = androidx.compose.material3.DrawerValue.Closed
    )
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    var currentPage by remember { mutableStateOf(AppPage.DASHBOARD) }
    var previousPage by remember { mutableStateOf(AppPage.DASHBOARD) }
    var showAddTorrentSheet by rememberSaveable { mutableStateOf(false) }
    var showServerProfileSheet by rememberSaveable { mutableStateOf(false) }
    var selectedTorrentIdentity by rememberSaveable { mutableStateOf("") }
    var showTorrentSortMenu by remember { mutableStateOf(false) }
    var showTorrentSearchBar by rememberSaveable { mutableStateOf(false) }
    var torrentSearchQuery by rememberSaveable { mutableStateOf("") }
    var torrentListSortOption by rememberSaveable { mutableStateOf(TorrentListSortOption.UPLOAD_SPEED) }
    var torrentListSortDescending by rememberSaveable { mutableStateOf(true) }
    var sortScrollRequestId by remember { mutableIntStateOf(0) }
    val addTorrentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val openDrawerDescription = stringResource(R.string.menu_open_drawer)
    val backDescription = stringResource(R.string.back)
    val manageServersDescription = stringResource(R.string.menu_manage_servers)
    val sortDescription = stringResource(R.string.menu_sort)
    val searchDescription = stringResource(R.string.menu_search)
    val addTorrentDescription = stringResource(R.string.menu_add_torrent)
    val localContext = LocalContext.current
    val backgroundTargetSizePx = remember(
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        density,
    ) {
        val widthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }
        val heightPx = with(density) { configuration.screenHeightDp.dp.roundToPx() }
        (maxOf(widthPx, heightPx) * 1.18f).roundToInt().coerceAtLeast(1)
    }
    val appBackgroundBrush = when (state.settings.appTheme) {
        AppTheme.DARK -> DarkBackgroundGradient
        AppTheme.LIGHT -> LightBackgroundGradient
        AppTheme.CUSTOM -> DarkBackgroundGradient
    }
    val customBackgroundState = rememberCustomBackgroundImageState(
        path = state.settings.customBackgroundImagePath,
        targetMaxDimensionPx = backgroundTargetSizePx,
    )
    val customBackgroundImage = customBackgroundState.image
    val showCustomBackgroundImage = state.settings.appTheme == AppTheme.CUSTOM && customBackgroundImage != null
    val customBackgroundScrim = if (state.settings.customBackgroundToneIsLight) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.18f)
    }
    val crossSeedCounts = remember(state.torrents) {
        buildCrossSeedCountMap(state.torrents)
    }
    val filteredTorrents = remember(state.torrents, torrentSearchQuery) {
        val query = torrentSearchQuery.trim()
        if (query.isBlank()) {
            state.torrents
        } else {
            state.torrents.filter { torrent ->
                matchesTorrentSearch(torrent = torrent, query = query)
            }
        }
    }
    val visibleTorrents = remember(
        filteredTorrents,
        torrentListSortOption,
        torrentListSortDescending,
        crossSeedCounts,
    ) {
        sortTorrentList(
            torrents = filteredTorrents,
            sortOption = torrentListSortOption,
            descending = torrentListSortDescending,
            crossSeedCounts = crossSeedCounts,
        )
    }
    val categoryOptionsForAdd = remember(state.categoryOptions) {
        state.categoryOptions
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
    val tagOptionsForAdd = remember(state.tagOptions) {
        state.tagOptions
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
    val pathOptionsForAdd = remember(state.torrents) {
        state.torrents
            .map { it.savePath.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
    val selectedTorrent = remember(state.torrents, selectedTorrentIdentity) {
        state.torrents.firstOrNull { torrentIdentityKey(it) == selectedTorrentIdentity }
    }
    val contentListState = rememberLazyListState()
    var dashboardCardOrder by remember { mutableStateOf(parseDashboardCardOrder(state.settings.dashboardCardOrder)) }
    var draggingDashboardCard by remember { mutableStateOf<DashboardChartCard?>(null) }
    var draggingDashboardOffsetY by remember { mutableFloatStateOf(0f) }
    var draggingDashboardTargetIndex by remember { mutableIntStateOf(-1) }
    var draggingDashboardSession by remember { mutableStateOf<DashboardDragSession?>(null) }
    val dashboardCardHeights = remember { mutableStateMapOf<DashboardChartCard, Int>() }

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

    fun openTorrentDetail(torrent: TorrentInfo) {
        selectedTorrentIdentity = torrentIdentityKey(torrent)
        if (currentPage != AppPage.TORRENT_DETAIL) {
            previousPage = currentPage
        }
        currentPage = AppPage.TORRENT_DETAIL
    }

    fun backToPreviousPage() {
        currentPage = if (previousPage == currentPage) AppPage.DASHBOARD else previousPage
    }

    fun requestScrollToFirstTorrentAfterSort() {
        sortScrollRequestId += 1
    }

    fun persistDashboardCardOrderIfChanged(nextOrder: List<DashboardChartCard>) {
        val serialized = serializeDashboardCardOrder(nextOrder)
        if (serialized != state.settings.dashboardCardOrder) {
            viewModel.updateDashboardCardOrder(serialized)
        }
    }

    fun visibleDashboardCards(): List<DashboardChartCard> {
        return dashboardCardOrder.filter { candidate ->
            when (candidate) {
                DashboardChartCard.COUNTRY_FLOW -> state.settings.showCountryFlowCard
                DashboardChartCard.CATEGORY_SHARE -> state.settings.showCategoryDistributionCard
                DashboardChartCard.DAILY_UPLOAD -> state.settings.showUploadDistributionCard
            }
        }
    }

    fun startDashboardCardDrag(card: DashboardChartCard) {
        val visibleCards = visibleDashboardCards()
        val startIndex = visibleCards.indexOf(card)
        if (startIndex < 0) return
        val currentCardHeight = dashboardCardHeights[card]?.toFloat()
            ?: with(density) { 180.dp.toPx() }
        val minSwapTriggerPx = with(density) { 56.dp.toPx() }
        val maxSwapTriggerPx = with(density) { 88.dp.toPx() }
        val spacingPx = with(density) { 10.dp.toPx() }
        val edgeSlackPx = with(density) { 24.dp.toPx() }
        val swapTriggerPx = (currentCardHeight * 0.34f).coerceIn(minSwapTriggerPx, maxSwapTriggerPx)
        draggingDashboardCard = card
        draggingDashboardOffsetY = 0f
        draggingDashboardTargetIndex = startIndex
        draggingDashboardSession = DashboardDragSession(
            visibleCards = visibleCards,
            indexByCard = visibleCards.mapIndexed { index, dashboardCard -> dashboardCard to index }.toMap(),
            startIndex = startIndex,
            swapTriggerPx = swapTriggerPx,
            shiftDistancePx = currentCardHeight + spacingPx,
            maxUpwardOffset = -(startIndex * swapTriggerPx + edgeSlackPx),
            maxDownwardOffset = ((visibleCards.lastIndex - startIndex) * swapTriggerPx) + edgeSlackPx,
        )
    }

    fun updateDashboardCardDrag(card: DashboardChartCard, deltaY: Float) {
        val dragSession = draggingDashboardSession ?: return
        if (draggingDashboardCard != card) return
        draggingDashboardOffsetY = (draggingDashboardOffsetY + deltaY)
            .coerceIn(dragSession.maxUpwardOffset, dragSession.maxDownwardOffset)

        val offsetSteps = when {
            draggingDashboardOffsetY > 0f -> (draggingDashboardOffsetY / dragSession.swapTriggerPx).toInt()
            draggingDashboardOffsetY < 0f -> -((-draggingDashboardOffsetY / dragSession.swapTriggerPx).toInt())
            else -> 0
        }
        draggingDashboardTargetIndex = (dragSession.startIndex + offsetSteps)
            .coerceIn(0, dragSession.visibleCards.lastIndex)
    }

    fun endDashboardCardDrag() {
        val draggedCard = draggingDashboardCard ?: return
        val dragSession = draggingDashboardSession
        if (dragSession != null && draggingDashboardTargetIndex >= 0) {
            dashboardCardOrder = reorderDashboardCardOrder(
                order = dashboardCardOrder,
                visibleCards = dragSession.visibleCards,
                card = draggedCard,
                targetIndex = draggingDashboardTargetIndex,
            )
        }
        draggingDashboardCard = null
        draggingDashboardOffsetY = 0f
        draggingDashboardTargetIndex = -1
        draggingDashboardSession = null
        persistDashboardCardOrderIfChanged(dashboardCardOrder)
    }

    fun scrollToTopOfCurrentPage(animated: Boolean) {
        scope.launch {
            if (animated) {
                contentListState.animateScrollToItem(0)
            } else {
                contentListState.scrollToItem(0)
            }
        }
    }

    LaunchedEffect(sortScrollRequestId) {
        if (sortScrollRequestId <= 0) return@LaunchedEffect
        if (currentPage != AppPage.TORRENT_LIST) return@LaunchedEffect
        val targetIndex = if (showTorrentSearchBar && visibleTorrents.isNotEmpty()) 1 else 0
        contentListState.scrollToItem(targetIndex)
        // Guard against LazyList position restore after data reordering.
        yield()
        contentListState.scrollToItem(targetIndex)
    }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.dismissError()
    }

    LaunchedEffect(
        currentPage,
        state.connected,
        state.settings.showChartPanel,
        state.settings.showCountryFlowCard,
        state.settings.showUploadDistributionCard,
        state.settings.showCategoryDistributionCard,
        state.settings.hasSeenDashboardHideHint,
    ) {
        if (
            currentPage == AppPage.DASHBOARD &&
            state.connected &&
                state.settings.showChartPanel &&
                (
                    state.settings.showCountryFlowCard ||
                    state.settings.showUploadDistributionCard ||
                    state.settings.showCategoryDistributionCard
                ) &&
            !state.settings.hasSeenDashboardHideHint
        ) {
            snackbarHostState.showSnackbar(localContext.getString(R.string.dashboard_country_hide_hint))
            viewModel.markDashboardHideHintSeen()
        }
    }

    LaunchedEffect(state.settings.dashboardCardOrder) {
        dashboardCardOrder = parseDashboardCardOrder(state.settings.dashboardCardOrder)
    }

    LaunchedEffect(currentPage, selectedTorrent?.hash) {
        if (currentPage != AppPage.TORRENT_LIST) {
            showTorrentSortMenu = false
            showTorrentSearchBar = false
            torrentSearchQuery = ""
        }
        val hash = selectedTorrent?.hash.orEmpty()
        val refreshScene = when (currentPage) {
            AppPage.DASHBOARD -> RefreshScene.DASHBOARD
            AppPage.TORRENT_LIST -> RefreshScene.DASHBOARD
            AppPage.TORRENT_DETAIL -> RefreshScene.TORRENT_DETAIL
            AppPage.SETTINGS -> RefreshScene.SETTINGS
        }
        viewModel.updateRefreshScene(refreshScene)
        if (currentPage == AppPage.TORRENT_DETAIL && hash.isNotBlank()) {
            viewModel.loadTorrentDetail(hash)
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
            if (showCustomBackgroundImage) {
                customBackgroundImage?.let { image ->
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(customBackgroundScrim),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(appBackgroundBrush),
                )
            }
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                        ),
                        navigationIcon = {
                            TextButton(
                                modifier = Modifier.semantics {
                                    contentDescription = if (currentPage == AppPage.DASHBOARD) {
                                        openDrawerDescription
                                    } else {
                                        backDescription
                                    }
                                },
                                onClick = {
                                    if (currentPage == AppPage.DASHBOARD) {
                                        scope.launch { drawerState.open() }
                                    } else {
                                        backToPreviousPage()
                                    }
                                },
                            ) {
                                Text(
                                    text = if (currentPage == AppPage.DASHBOARD) "≡" else "←",
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                        },
                        title = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(currentPage) {
                                        detectTapGestures(
                                            onDoubleTap = {
                                                scrollToTopOfCurrentPage(animated = true)
                                            },
                                        )
                                    },
                            ) {
                                TopBrandTitle()
                            }
                        },
                        actions = {
                            when (currentPage) {
                                AppPage.DASHBOARD -> {
                                    Row(
                                        modifier = Modifier.padding(end = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        TextButton(
                                            modifier = Modifier.semantics {
                                                contentDescription = manageServersDescription
                                            },
                                            onClick = { showServerProfileSheet = true },
                                        ) {
                                            Text(
                                                text = stringResource(R.string.menu_servers),
                                                color = MaterialTheme.colorScheme.onBackground,
                                            )
                                        }
                                        TextButton(onClick = { openSettings() }) {
                                            Text(
                                                text = stringResource(R.string.menu_settings),
                                                color = MaterialTheme.colorScheme.onBackground,
                                            )
                                        }
                                    }
                                }

                                AppPage.SETTINGS -> {
                                    TextButton(
                                        onClick = {
                                            if (state.connected) viewModel.refresh(manual = true) else viewModel.connect()
                                        },
                                    ) {
                                        Text(
                                            text = if (state.connected) {
                                                if (state.isManualRefreshing) {
                                                    stringResource(R.string.refreshing)
                                                } else {
                                                    stringResource(R.string.refresh)
                                                }
                                            } else {
                                                stringResource(R.string.connect)
                                            },
                                            color = MaterialTheme.colorScheme.onBackground,
                                        )
                                    }
                                }

                                AppPage.TORRENT_LIST -> {
                                    Box {
                                        TextButton(
                                            modifier = Modifier.semantics {
                                                contentDescription = sortDescription
                                            },
                                            onClick = { showTorrentSortMenu = true },
                                        ) {
                                            Text(stringResource(R.string.menu_sort), color = MaterialTheme.colorScheme.onBackground)
                                        }
                                        DropdownMenu(
                                            expanded = showTorrentSortMenu,
                                            onDismissRequest = { showTorrentSortMenu = false },
                                        ) {
                                            TorrentListSortOption.entries.forEach { option ->
                                                val isSelected = option == torrentListSortOption
                                                DropdownMenuItem(
                                                    text = {
                                                        val prefix = if (isSelected) "✓ " else ""
                                                        Text("$prefix${torrentListSortLabel(option)}")
                                                    },
                                                    onClick = {
                                                        torrentListSortOption = option
                                                        showTorrentSortMenu = false
                                                        requestScrollToFirstTorrentAfterSort()
                                                    },
                                                )
                                            }
                                            HorizontalDivider()
                                            DropdownMenuItem(
                                                    text = {
                                                        val prefix = if (torrentListSortDescending) "✓ " else ""
                                                        Text("${prefix}${stringResource(R.string.sort_descending)}")
                                                    },
                                                onClick = {
                                                    torrentListSortDescending = true
                                                    showTorrentSortMenu = false
                                                    requestScrollToFirstTorrentAfterSort()
                                                },
                                            )
                                            DropdownMenuItem(
                                                    text = {
                                                        val prefix = if (!torrentListSortDescending) "✓ " else ""
                                                        Text("${prefix}${stringResource(R.string.sort_ascending)}")
                                                    },
                                                onClick = {
                                                    torrentListSortDescending = false
                                                    showTorrentSortMenu = false
                                                    requestScrollToFirstTorrentAfterSort()
                                                },
                                            )
                                        }
                                    }
                                    TextButton(
                                        modifier = Modifier.semantics {
                                            contentDescription = searchDescription
                                        },
                                        onClick = {
                                            showTorrentSearchBar = !showTorrentSearchBar
                                            if (!showTorrentSearchBar) {
                                                torrentSearchQuery = ""
                                            }
                                            scope.launch {
                                                contentListState.scrollToItem(0)
                                            }
                                        },
                                    ) {
                                        Text(
                                            text = if (showTorrentSearchBar) {
                                                stringResource(R.string.menu_collapse)
                                            } else {
                                                stringResource(R.string.menu_search)
                                            },
                                            color = MaterialTheme.colorScheme.onBackground,
                                        )
                                    }
                                    TextButton(
                                        modifier = Modifier.semantics {
                                            contentDescription = addTorrentDescription
                                        },
                                        onClick = {
                                            viewModel.loadGlobalSelectionOptions()
                                            showAddTorrentSheet = true
                                        },
                                    ) {
                                        Text(
                                            text = "+",
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontSize = 22.sp,
                                        )
                                    }
                                }

                                else -> {
                                    TextButton(
                                        onClick = {
                                            if (state.connected) viewModel.refresh(manual = true) else viewModel.connect()
                                        },
                                    ) {
                                        Text(
                                            text = if (state.isConnecting) {
                                                stringResource(R.string.connecting)
                                            } else if (state.connected) {
                                                if (state.isManualRefreshing) {
                                                    stringResource(R.string.refreshing)
                                                } else {
                                                    stringResource(R.string.refresh)
                                                }
                                            } else {
                                                stringResource(R.string.connect)
                                            },
                                            color = MaterialTheme.colorScheme.onBackground,
                                        )
                                    }
                                }
                            }
                        },
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { innerPadding ->
                val hasSavedConnection = remember(state.settings.host, state.settings.username) {
                    state.settings.host.trim().isNotBlank() && state.settings.username.trim().isNotBlank()
                }
                val showDashboardSnapshot = state.connected || state.hasDashboardSnapshot
                val showDashboardSkeleton = !showDashboardSnapshot &&
                    (state.isConnecting || (!state.dashboardCacheHydrated && hasSavedConnection))
                val contentModifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)

                val contentList: @Composable () -> Unit = {
                    LazyColumn(
                        state = contentListState,
                        modifier = contentModifier,
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        when (currentPage) {
                            AppPage.DASHBOARD -> {
                                if (showDashboardSnapshot) {
                                    item {
                                        ServerOverviewCard(
                                            serverVersion = state.serverVersion,
                                            transferInfo = state.transferInfo,
                                            torrents = state.torrents,
                                            torrentCount = state.torrents.size,
                                            showTotals = state.settings.showSpeedTotals,
                                            showEntryHint = !state.settings.homeTorrentEntryHintDismissed,
                                            onDismissEntryHint = viewModel::dismissHomeTorrentEntryHint,
                                            onOpenTorrentList = ::openTorrentListFromDashboard,
                                        )
                                    }
                                    if (state.settings.showChartPanel) {
                                        item(key = "dashboard_chart_region") {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                            ) {
                                                dashboardCardOrder
                                                    .filter { card ->
                                                        when (card) {
                                                            DashboardChartCard.COUNTRY_FLOW -> state.settings.showCountryFlowCard
                                                            DashboardChartCard.CATEGORY_SHARE -> state.settings.showCategoryDistributionCard
                                                            DashboardChartCard.DAILY_UPLOAD -> state.settings.showUploadDistributionCard
                                                        }
                                                    }
                                                    .forEach { card ->
                                                        key(card) {
                                                            ReorderableDashboardCard(
                                                                card = card,
                                                                isDragging = draggingDashboardCard == card,
                                                                dragOffsetY = if (draggingDashboardCard == card) {
                                                                    draggingDashboardOffsetY
                                                                } else {
                                                                    0f
                                                                },
                                                                siblingOffsetY = calculateDashboardSiblingOffset(
                                                                    card = card,
                                                                    draggingCard = draggingDashboardCard,
                                                                    draggingTargetIndex = draggingDashboardTargetIndex,
                                                                    dragSession = draggingDashboardSession,
                                                                ),
                                                                onDragStart = {
                                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                    startDashboardCardDrag(card)
                                                                },
                                                                onDragDelta = { deltaY -> updateDashboardCardDrag(card, deltaY) },
                                                                onDragEnd = { endDashboardCardDrag() },
                                                                onMeasured = { height -> dashboardCardHeights[card] = height },
                                                            ) {
                                                                when (card) {
                                                                    DashboardChartCard.COUNTRY_FLOW -> {
                                                                        CountryFlowMapCard(
                                                                            stats = state.dailyCountryUploadStats,
                                                                            onHide = {
                                                                                viewModel.updateShowCountryFlowCard(false)
                                                                                if (!state.settings.hasSeenDashboardHiddenSnack) {
                                                                                    scope.launch {
                                                                                        snackbarHostState.showSnackbar(
                                                                                            localContext.getString(
                                                                                                R.string.dashboard_country_hidden_snack
                                                                                            )
                                                                                        )
                                                                                    }
                                                                                    viewModel.markDashboardHiddenSnackSeen()
                                                                                }
                                                                            },
                                                                        )
                                                                    }

                                                                    DashboardChartCard.CATEGORY_SHARE -> {
                                                                        CategorySharePieCard(
                                                                            torrents = state.torrents,
                                                                            onHide = { viewModel.updateShowCategoryDistributionCard(false) },
                                                                        )
                                                                    }

                                                                    DashboardChartCard.DAILY_UPLOAD -> {
                                                                        DailyTagUploadPieCard(
                                                                            date = state.dailyTagUploadDate,
                                                                            stats = state.dailyTagUploadStats,
                                                                            onHide = { viewModel.updateShowUploadDistributionCard(false) },
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                            }
                                        }
                                    }
                                } else if (showDashboardSkeleton) {
                                    item {
                                        DashboardHomeSkeleton(
                                            showCharts = state.settings.showChartPanel,
                                        )
                                    }
                                } else {
                                    item {
                                        NeedConnectionCard(
                                            onOpenConnection = { openSettings() },
                                        )
                                    }
                                }
                            }

                            AppPage.TORRENT_LIST -> {
                                if (state.connected) {
                                    if (showTorrentSearchBar) {
                                        stickyHeader(key = "torrent_search_bar") {
                                            OutlinedTextField(
                                                value = torrentSearchQuery,
                                                onValueChange = { torrentSearchQuery = it },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(qbGlassStrongContainerColor())
                                                    .padding(bottom = 8.dp),
                                                label = { Text(stringResource(R.string.search_torrent_label)) },
                                                placeholder = { Text(stringResource(R.string.search_torrent_placeholder)) },
                                                singleLine = true,
                                            )
                                        }
                                    }
                                    items(
                                        items = visibleTorrents,
                                        key = { it.hash.ifBlank { it.name } },
                                    ) { torrent ->
                                        TorrentCard(
                                            torrent = torrent,
                                            crossSeedCount = crossSeedCounts[torrentIdentityKey(torrent)] ?: 0,
                                            isPending = state.pendingHashes.contains(torrent.hash),
                                            onOpenDetails = { openTorrentDetail(torrent) },
                                        )
                                    }
                                    if (visibleTorrents.isEmpty()) {
                                        item {
                                            Text(
                                                text = stringResource(R.string.no_torrent_data),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodyMedium,
                                            )
                                        }
                                    }
                                } else {
                                    item {
                                        NeedConnectionCard(
                                            onOpenConnection = { openSettings() },
                                        )
                                    }
                                }
                            }

                            AppPage.TORRENT_DETAIL -> {
                                val torrent = selectedTorrent
                                if (torrent == null) {
                                    item {
                                        Text(
                                            text = stringResource(R.string.torrent_detail_not_found),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                } else {
                                    item {
                                        TorrentOperationDetailCard(
                                            torrent = torrent,
                                            crossSeedCount = crossSeedCounts[torrentIdentityKey(torrent)] ?: 0,
                                            isPending = state.pendingHashes.contains(torrent.hash),
                                            detailLoading = state.detailLoading && state.detailHash == torrent.hash,
                                            detailProperties = if (state.detailHash == torrent.hash) state.detailProperties else null,
                                            detailFiles = if (state.detailHash == torrent.hash) state.detailFiles else emptyList(),
                                            detailTrackers = if (state.detailHash == torrent.hash) state.detailTrackers else emptyList(),
                                            categoryOptions = state.categoryOptions,
                                            tagOptions = state.tagOptions,
                                            deleteFilesDefault = state.settings.deleteFilesDefault,
                                            deleteFilesWhenNoSeeders = state.settings.deleteFilesWhenNoSeeders,
                                            onPause = { viewModel.pauseTorrent(torrent.hash) },
                                            onResume = { viewModel.resumeTorrent(torrent.hash) },
                                            onDelete = { deleteFiles ->
                                                viewModel.deleteTorrent(torrent.hash, deleteFiles)
                                            },
                                            onRename = { viewModel.renameTorrent(torrent.hash, it) },
                                            onSetLocation = { viewModel.setTorrentLocation(torrent.hash, it) },
                                            onSetCategory = { viewModel.setTorrentCategory(torrent.hash, it) },
                                            onSetTags = { oldTags, newTags ->
                                                viewModel.setTorrentTags(torrent.hash, oldTags, newTags)
                                            },
                                            onSetSpeedLimit = { dl, up ->
                                                viewModel.setTorrentSpeedLimit(torrent.hash, dl, up)
                                            },
                                            onSetShareRatio = { ratio ->
                                                viewModel.setTorrentShareRatio(torrent.hash, ratio)
                                            },
                                        )
                                    }
                                }
                            }

                            AppPage.SETTINGS -> {
                                item {
                                    SettingsPageContent(
                                        settings = state.settings,
                                        onAppLanguageChange = viewModel::updateAppLanguage,
                                        onShowSpeedTotalsChange = viewModel::updateShowSpeedTotals,
                                        onEnableServerGroupingChange = viewModel::updateEnableServerGrouping,
                                        onShowChartPanelChange = viewModel::updateShowChartPanel,
                                        onShowCountryFlowCardChange = viewModel::updateShowCountryFlowCard,
                                        onShowUploadDistributionCardChange = viewModel::updateShowUploadDistributionCard,
                                        onShowCategoryDistributionCardChange = viewModel::updateShowCategoryDistributionCard,
                                        onDeleteFilesWhenNoSeedersChange = viewModel::updateDeleteFilesWhenNoSeeders,
                                        onDeleteFilesDefaultChange = viewModel::updateDeleteFilesDefault,
                                    )
                                }
                                item {
                                    ConnectionCard(
                                        state = state,
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
                    }
                }

                if (currentPage == AppPage.DASHBOARD && state.connected) {
                    val pullRefreshState = rememberPullRefreshState(
                        refreshing = state.isManualRefreshing,
                        onRefresh = { viewModel.refresh(manual = true) },
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pullRefresh(pullRefreshState),
                    ) {
                        contentList()
                        PullRefreshIndicator(
                            refreshing = state.isManualRefreshing,
                            state = pullRefreshState,
                            modifier = Modifier.align(Alignment.TopCenter),
                            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    contentList()
                }
            }

            if (showServerProfileSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showServerProfileSheet = false },
                    containerColor = qbGlassStrongContainerColor(),
                    shape = PanelShape,
                ) {
                    ServerProfileSheet(
                        currentSettings = state.settings,
                        profiles = state.serverProfiles,
                        activeProfileId = state.activeServerProfileId,
                        onSwitchProfile = { profileId ->
                            viewModel.switchServerProfile(profileId)
                            showServerProfileSheet = false
                            currentPage = AppPage.DASHBOARD
                        },
                        onAddProfile = { name, host, port, useHttps, username, password, refreshSeconds ->
                            viewModel.addServerProfile(
                                name = name,
                                host = host,
                                port = port,
                                useHttps = useHttps,
                                username = username,
                                password = password,
                                refreshSeconds = refreshSeconds,
                            )
                            showServerProfileSheet = false
                            currentPage = AppPage.DASHBOARD
                        },
                        onCancel = { showServerProfileSheet = false },
                    )
                }
            }

            if (showAddTorrentSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showAddTorrentSheet = false },
                    sheetState = addTorrentSheetState,
                    containerColor = qbGlassStrongContainerColor(),
                    shape = PanelShape,
                ) {
                    AddTorrentSheet(
                        context = localContext,
                        categoryOptions = categoryOptionsForAdd,
                        tagOptions = tagOptionsForAdd,
                        pathOptions = pathOptionsForAdd,
                        onCancel = { showAddTorrentSheet = false },
                        onAdd = { urls, files, autoTmm, category, tags, savePath, paused, skipChecking, sequential, firstLast, upKb, dlKb ->
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
                    )
                }
            }

        }
    }
}


@Composable
private fun CrossSeedDetailSummaryCard(
    sourceName: String,
    count: Int,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        colors = CardDefaults.outlinedCardColors(
            containerColor = qbGlassChipColor(),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.cross_seed_detail_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (sourceName.isNotBlank()) {
                Text(
                    text = stringResource(R.string.cross_seed_detail_source_fmt, sourceName),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = stringResource(R.string.cross_seed_detail_count_fmt, count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CrossSeedDetailCard(torrent: TorrentInfo) {
    val effectiveState = effectiveTorrentState(torrent)
    val stateLabel = localizedTorrentStateLabel(effectiveState)
    val categoryText = normalizeCategoryLabel(
        category = torrent.category,
        noCategoryText = stringResource(R.string.no_category),
    )
    val tagsText = compactTagsLabel(
        tags = torrent.tags,
        noTagsText = stringResource(R.string.no_tags),
    )
    val savePathText = torrent.savePath.ifBlank { "-" }
    val siteText = trackerSiteName(
        tracker = torrent.tracker,
        unknownLabel = stringResource(R.string.site_unknown),
    )
    val stateStyle = torrentStateStyle(effectiveState)

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, qbGlassOutlineColor(defaultAlpha = 0.3f)),
        colors = CardDefaults.outlinedCardColors(
            containerColor = qbGlassSubtleContainerColor(),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = torrent.name,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.added_fmt, formatAddedOn(torrent.addedOn)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = siteText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    item {
                        TorrentMetaChip(
                            text = tagsText,
                            containerColor = Color(0xFF0B8F6F),
                            contentColor = Color(0xFFE1FFF4),
                        )
                    }
                    item {
                        TorrentStateTag(
                            label = stateLabel,
                            style = stateStyle,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.last_activity_fmt, formatActiveAgo(torrent.lastActivity)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            TorrentInfoCell(
                text = stringResource(
                    R.string.torrent_speed_fmt,
                    formatSpeed(torrent.uploadSpeed),
                    formatSpeed(torrent.downloadSpeed),
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TorrentInfoCell(
                    text = stringResource(
                        R.string.torrent_uploaded_downloaded_fmt,
                        formatBytes(torrent.uploaded),
                        formatBytes(torrent.downloaded),
                    ),
                    modifier = Modifier.weight(1f),
                )
                TorrentInfoCell(
                    text = stringResource(R.string.torrent_size_fmt, formatBytes(torrent.size)),
                    modifier = Modifier.weight(1f),
                )
            }

            TorrentInfoCell(
                text = stringResource(R.string.torrent_category_fmt, categoryText),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TorrentInfoCell(
                    text = stringResource(R.string.torrent_ratio_fmt, formatRatio(torrent.ratio)),
                    modifier = Modifier.weight(1f),
                )
                TorrentInfoCell(
                    text = stringResource(R.string.torrent_seed_count_fmt, torrent.seeders, torrent.numComplete),
                    modifier = Modifier.weight(1f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TorrentInfoCell(
                    text = stringResource(R.string.torrent_peer_count_fmt, torrent.leechers, torrent.numIncomplete),
                    modifier = Modifier.weight(1f),
                )
                TorrentInfoCell(
                    text = stringResource(R.string.cross_seed_detail_site_fmt, siteText),
                    modifier = Modifier.weight(1f),
                )
            }

            TorrentInfoCell(
                text = stringResource(R.string.torrent_save_path_fmt, savePathText),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ServerOverviewCard(
    serverVersion: String,
    transferInfo: TransferInfo,
    torrents: List<TorrentInfo>,
    torrentCount: Int,
    showTotals: Boolean,
    showEntryHint: Boolean,
    onDismissEntryHint: () -> Unit,
    onOpenTorrentList: () -> Unit,
) {
    val stateSummary = remember(torrents) { buildDashboardStateSummary(torrents) }
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

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenTorrentList() },
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
                    painter = painterResource(id = R.drawable.ic_qbremote_foreground),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(34.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_name),
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

@Composable
private fun TagChartPanelCard(
    entries: List<SiteChartEntry>,
    chartSortMode: ChartSortMode,
    showSiteName: Boolean,
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
            Text(
                text = stringResource(R.string.chart_panel_title_fmt, chartSortModeLabel(chartSortMode)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold,
            )

            if (entries.isEmpty()) {
                Text(
                    text = stringResource(R.string.chart_no_data),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                return@Column
            }

            val topEntries = entries.take(10)
            val maxMetric = topEntries.maxOfOrNull { chartMetric(it, chartSortMode) }?.coerceAtLeast(1L) ?: 1L
            topEntries.forEachIndexed { index, entry ->
                val metric = chartMetric(entry, chartSortMode)
                val label = if (showSiteName) entry.site else "#${index + 1}"
                val metricText = chartMetricText(entry, chartSortMode)
                val progress = (metric.toFloat() / maxMetric.toFloat()).coerceIn(0f, 1f)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.weight(0.45f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Column(modifier = Modifier.weight(0.55f)) {
                        Text(text = metricText, style = MaterialTheme.typography.labelMedium)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardEntryHintBubble(
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
        Text(
            text = "×",
            modifier = Modifier
                .semantics { contentDescription = dismissDescription }
                .clickable(onClick = onDismiss)
                .padding(horizontal = 2.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
            fontWeight = FontWeight.Bold,
        )
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

private fun buildDashboardStateSummary(torrents: List<TorrentInfo>): DashboardStateSummary {
    if (torrents.isEmpty()) return DashboardStateSummary()

    var uploading = 0
    var downloading = 0
    var pausedUpload = 0
    var pausedDownload = 0
    var error = 0
    var checking = 0
    var waiting = 0

    torrents.forEach { torrent ->
        when (normalizeTorrentState(effectiveTorrentState(torrent))) {
            "uploading", "forcedup", "stalledup" -> uploading++
            "downloading", "forceddl", "stalleddl", "metadl", "forcedmetadl", "allocating", "moving" -> downloading++
            "pausedup", "stoppedup" -> pausedUpload++
            "pauseddl", "stoppeddl" -> pausedDownload++
            "error", "missingfiles" -> error++
            "checkingdl", "checkingup", "checkingresumedata" -> checking++
            "queueddl", "queuedup" -> waiting++
        }
    }

    return DashboardStateSummary(
        uploadingCount = uploading,
        downloadingCount = downloading,
        pausedUploadCount = pausedUpload,
        pausedDownloadCount = pausedDownload,
        errorCount = error,
        checkingCount = checking,
        waitingCount = waiting,
    )
}

private fun formatRateLimit(value: Long, unlimitedLabel: String): String {
    return if (value <= 0L) {
        unlimitedLabel
    } else {
        formatSpeed(value)
    }
}

private data class CrossSeedGroupKey(
    val savePath: String,
    val size: Long,
    val uniqueIdentity: String = "",
)

private fun torrentIdentityKey(torrent: TorrentInfo): String {
    return torrent.hash.ifBlank {
        "${torrent.name}|${torrent.addedOn}|${torrent.savePath}|${torrent.size}"
    }
}

private fun buildCrossSeedCountMap(torrents: List<TorrentInfo>): Map<String, Int> {
    val grouped = torrents.groupBy { crossSeedGroupKey(it) }
    val result = mutableMapOf<String, Int>()

    torrents.forEach { torrent ->
        val key = crossSeedGroupKey(torrent)
        val groupCount = grouped[key]?.size ?: 1
        result[torrentIdentityKey(torrent)] = (groupCount - 1).coerceAtLeast(0)
    }
    return result
}

private fun crossSeedGroupKey(torrent: TorrentInfo): CrossSeedGroupKey {
    val normalizedPath = torrent.savePath.trim().lowercase()
    val normalizedSize = torrent.size.coerceAtLeast(0L)
    if (normalizedPath.isBlank() || normalizedSize <= 0L) {
        return CrossSeedGroupKey(
            savePath = "__invalid__",
            size = -1L,
            uniqueIdentity = torrent.hash.ifBlank { torrentIdentityKey(torrent) },
        )
    }
    return CrossSeedGroupKey(
        savePath = normalizedPath,
        size = normalizedSize,
    )
}

private fun sortTorrentList(
    torrents: List<TorrentInfo>,
    sortOption: TorrentListSortOption,
    descending: Boolean,
    crossSeedCounts: Map<String, Int>,
): List<TorrentInfo> {
    val comparator = when (sortOption) {
        TorrentListSortOption.ADDED_TIME ->
            compareBy<TorrentInfo> { it.addedOn }
        TorrentListSortOption.UPLOAD_SPEED ->
            compareBy<TorrentInfo> { it.uploadSpeed }
                .thenBy { it.addedOn }
        TorrentListSortOption.DOWNLOAD_SPEED ->
            compareBy<TorrentInfo> { it.downloadSpeed }
                .thenBy { it.addedOn }
        TorrentListSortOption.SHARE_RATIO ->
            compareBy<TorrentInfo> { it.ratio }
                .thenBy { it.addedOn }
        TorrentListSortOption.TOTAL_UPLOADED ->
            compareBy<TorrentInfo> { it.uploaded }
                .thenBy { it.addedOn }
        TorrentListSortOption.TOTAL_DOWNLOADED ->
            compareBy<TorrentInfo> { it.downloaded }
                .thenBy { it.addedOn }
        TorrentListSortOption.TORRENT_SIZE ->
            compareBy<TorrentInfo> { it.size }
                .thenBy { it.addedOn }
        TorrentListSortOption.ACTIVITY_TIME ->
            compareBy<TorrentInfo> { it.lastActivity }
                .thenBy { it.addedOn }
        TorrentListSortOption.SEEDERS ->
            compareBy<TorrentInfo> { it.seeders }
                .thenBy { it.addedOn }
        TorrentListSortOption.LEECHERS ->
            compareBy<TorrentInfo> { it.leechers }
                .thenBy { it.addedOn }
        TorrentListSortOption.CROSS_SEED_COUNT ->
            compareBy<TorrentInfo> { crossSeedCounts[torrentIdentityKey(it)] ?: 0 }
                .thenBy { it.addedOn }
    }
    val finalComparator = if (descending) comparator.reversed() else comparator
    return torrents.sortedWith(finalComparator)
}

@Composable
private fun torrentListSortLabel(option: TorrentListSortOption): String {
    return when (option) {
        TorrentListSortOption.ADDED_TIME -> stringResource(R.string.sort_added_time)
        TorrentListSortOption.UPLOAD_SPEED -> stringResource(R.string.sort_upload_speed)
        TorrentListSortOption.DOWNLOAD_SPEED -> stringResource(R.string.sort_download_speed)
        TorrentListSortOption.SHARE_RATIO -> stringResource(R.string.sort_share_ratio)
        TorrentListSortOption.TOTAL_UPLOADED -> stringResource(R.string.sort_total_uploaded)
        TorrentListSortOption.TOTAL_DOWNLOADED -> stringResource(R.string.sort_total_downloaded)
        TorrentListSortOption.TORRENT_SIZE -> stringResource(R.string.sort_torrent_size)
        TorrentListSortOption.ACTIVITY_TIME -> stringResource(R.string.sort_activity_time)
        TorrentListSortOption.SEEDERS -> stringResource(R.string.sort_seeders)
        TorrentListSortOption.LEECHERS -> stringResource(R.string.sort_leechers)
        TorrentListSortOption.CROSS_SEED_COUNT -> stringResource(R.string.sort_cross_seed_count)
    }
}

private fun buildTagChartEntries(
    torrents: List<TorrentInfo>,
    mode: ChartSortMode,
    noTagLabel: String,
): List<SiteChartEntry> {
    val grouped = mutableMapOf<String, MutableList<TorrentInfo>>()
    torrents.forEach { torrent ->
        val tags = torrent.tags
            .split(',', ';', '|')
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "-" && !it.equals("null", ignoreCase = true) }
            .ifEmpty { listOf(noTagLabel) }
        tags.forEach { tag ->
            grouped.getOrPut(tag) { mutableListOf() }.add(torrent)
        }
    }

    return grouped.map { (tag, list) ->
        val down = list.sumOf { it.downloadSpeed }
        val up = list.sumOf { it.uploadSpeed }
        SiteChartEntry(
            site = tag,
            torrentCount = list.size,
            downloadSpeed = down,
            uploadSpeed = up,
            totalSpeed = down + up,
        )
    }.sortedByDescending { chartMetric(it, mode) }
}

private fun chartMetric(entry: SiteChartEntry, mode: ChartSortMode): Long {
    return when (mode) {
        ChartSortMode.TOTAL_SPEED -> entry.totalSpeed
        ChartSortMode.DOWNLOAD_SPEED -> entry.downloadSpeed
        ChartSortMode.UPLOAD_SPEED -> entry.uploadSpeed
        ChartSortMode.TORRENT_COUNT -> entry.torrentCount.toLong()
    }
}

@Composable
private fun chartMetricText(entry: SiteChartEntry, mode: ChartSortMode): String {
    return when (mode) {
        ChartSortMode.TOTAL_SPEED -> stringResource(
            R.string.chart_metric_total_fmt,
            formatSpeed(entry.totalSpeed)
        )
        ChartSortMode.DOWNLOAD_SPEED -> stringResource(
            R.string.chart_metric_down_fmt,
            formatSpeed(entry.downloadSpeed)
        )
        ChartSortMode.UPLOAD_SPEED -> stringResource(
            R.string.chart_metric_up_fmt,
            formatSpeed(entry.uploadSpeed)
        )
        ChartSortMode.TORRENT_COUNT -> stringResource(
            R.string.chart_metric_torrents_fmt,
            entry.torrentCount
        )
    }
}

private fun trackerSiteName(tracker: String, unknownLabel: String): String {
    val trimmed = tracker.trim()
    if (trimmed.isBlank()) return unknownLabel

    return runCatching {
        URI(trimmed).host.orEmpty().ifBlank { unknownLabel }
    }.getOrElse {
        trimmed
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore('/')
            .ifBlank { unknownLabel }
    }
}

private fun parseDashboardCardOrder(raw: String): List<DashboardChartCard> {
    val parsed = raw
        .split(',')
        .mapNotNull { token ->
            DashboardChartCard.entries.firstOrNull { it.storageKey == token.trim() }
        }
        .distinct()
        .toMutableList()
    DashboardChartCard.entries.forEach { card ->
        if (!parsed.contains(card)) {
            parsed += card
        }
    }
    return parsed
}

private fun serializeDashboardCardOrder(order: List<DashboardChartCard>): String {
    return parseDashboardCardOrder(order.joinToString(",") { it.storageKey })
        .joinToString(",") { it.storageKey }
}

private fun swapDashboardCardOrder(
    order: List<DashboardChartCard>,
    first: DashboardChartCard,
    second: DashboardChartCard,
): List<DashboardChartCard> {
    val mutable = order.toMutableList()
    val firstIndex = mutable.indexOf(first)
    val secondIndex = mutable.indexOf(second)
    if (firstIndex < 0 || secondIndex < 0) return order
    mutable[firstIndex] = second
    mutable[secondIndex] = first
    return mutable
}

private fun reorderDashboardCardOrder(
    order: List<DashboardChartCard>,
    visibleCards: List<DashboardChartCard>,
    card: DashboardChartCard,
    targetIndex: Int,
): List<DashboardChartCard> {
    val currentVisibleIndex = visibleCards.indexOf(card)
    if (currentVisibleIndex < 0 || targetIndex !in visibleCards.indices || currentVisibleIndex == targetIndex) {
        return order
    }

    val reorderedVisibleCards = visibleCards.toMutableList().apply {
        remove(card)
        add(targetIndex, card)
    }
    val visibleCardSet = visibleCards.toSet()
    var visibleCursor = 0

    return order.map { existingCard ->
        if (existingCard in visibleCardSet) {
            reorderedVisibleCards[visibleCursor++]
        } else {
            existingCard
        }
    }
}

private fun dashboardTargetBoundaryOffset(
    visibleCards: List<DashboardChartCard>,
    startIndex: Int,
    targetIndex: Int,
    cardHeights: Map<DashboardChartCard, Int>,
    itemSpacingPx: Float,
): Float {
    if (startIndex == targetIndex) return 0f
    val draggedCard = visibleCards.getOrNull(startIndex) ?: return 0f
    val draggedHeight = cardHeights[draggedCard]?.toFloat() ?: 0f
    var boundary = 0f

    return if (targetIndex > startIndex) {
        for (index in startIndex until targetIndex) {
            val nextCard = visibleCards[index + 1]
            val nextHeight = cardHeights[nextCard]?.toFloat() ?: draggedHeight
            boundary += itemSpacingPx + (nextHeight * 0.5f)
            if (index < targetIndex - 1) {
                boundary += nextHeight * 0.5f
            }
        }
        boundary
    } else {
        for (index in startIndex downTo (targetIndex + 1)) {
            val previousCard = visibleCards[index - 1]
            val previousHeight = cardHeights[previousCard]?.toFloat() ?: draggedHeight
            boundary -= itemSpacingPx + (previousHeight * 0.5f)
            if (index > targetIndex + 1) {
                boundary -= previousHeight * 0.5f
            }
        }
        boundary
    }
}

private fun calculateDashboardSiblingOffset(
    card: DashboardChartCard,
    draggingCard: DashboardChartCard?,
    draggingTargetIndex: Int,
    dragSession: DashboardDragSession?,
): Float {
    if (draggingCard == null || draggingCard == card || dragSession == null) return 0f
    val draggingIndex = dragSession.startIndex
    val targetIndex = draggingTargetIndex
    val cardIndex = dragSession.indexByCard[card] ?: return 0f
    if (draggingIndex < 0 || targetIndex < 0 || cardIndex < 0) return 0f

    val shiftDistance = dragSession.shiftDistancePx
    if (shiftDistance <= 0f) return 0f

    return when {
        targetIndex > draggingIndex && cardIndex in (draggingIndex + 1)..targetIndex -> -shiftDistance
        targetIndex < draggingIndex && cardIndex in targetIndex until draggingIndex -> shiftDistance
        else -> 0f
    }
}







