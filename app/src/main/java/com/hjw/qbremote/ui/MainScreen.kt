package com.hjw.qbremote.ui
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.hjw.qbremote.data.AppLanguage
import com.hjw.qbremote.data.AppTheme
import com.hjw.qbremote.R
import com.hjw.qbremote.data.ChartSortMode
import com.hjw.qbremote.data.ConnectionSettings
import com.hjw.qbremote.data.ServerProfile
import com.hjw.qbremote.data.model.AddTorrentFile
import com.hjw.qbremote.data.model.TorrentFileInfo
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.data.model.TorrentProperties
import com.hjw.qbremote.data.model.TransferInfo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.net.URI

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

private val PanelShape = RoundedCornerShape(20.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = androidx.compose.material3.rememberDrawerState(
        initialValue = androidx.compose.material3.DrawerValue.Closed
    )
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val density = LocalDensity.current

    var currentPage by remember { mutableStateOf(AppPage.DASHBOARD) }
    var previousPage by remember { mutableStateOf(AppPage.DASHBOARD) }
    var showAddTorrentSheet by rememberSaveable { mutableStateOf(false) }
    var showServerProfileSheet by rememberSaveable { mutableStateOf(false) }
    var selectedTorrentIdentity by rememberSaveable { mutableStateOf("") }
    var showTorrentSortMenu by remember { mutableStateOf(false) }
    var showTorrentSearchBar by rememberSaveable { mutableStateOf(false) }
    var torrentSearchQuery by rememberSaveable { mutableStateOf("") }
    var torrentListSortOption by rememberSaveable { mutableStateOf(TorrentListSortOption.ADDED_TIME) }
    var torrentListSortDescending by rememberSaveable { mutableStateOf(true) }
    var sortScrollRequestId by remember { mutableIntStateOf(0) }
    var startMotion by remember { mutableStateOf(false) }
    val openDrawerDescription = stringResource(R.string.menu_open_drawer)
    val backDescription = stringResource(R.string.back)
    val manageServersDescription = stringResource(R.string.menu_manage_servers)
    val sortDescription = stringResource(R.string.menu_sort)
    val searchDescription = stringResource(R.string.menu_search)
    val addTorrentDescription = stringResource(R.string.menu_add_torrent)
    val localContext = LocalContext.current
    val contentReveal by animateFloatAsState(
        targetValue = if (startMotion) 1f else 0f,
        animationSpec = tween(
            durationMillis = 700,
            delayMillis = 80,
            easing = FastOutSlowInEasing,
        ),
        label = "contentReveal",
    )
    val appBackgroundGradient = if (state.settings.appTheme == AppTheme.DARK) {
        DarkBackgroundGradient
    } else {
        LightBackgroundGradient
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

    LaunchedEffect(Unit) {
        startMotion = true
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
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
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
                    darkTheme = state.settings.appTheme == AppTheme.DARK,
                    onThemeChange = { enabled ->
                        viewModel.updateAppTheme(if (enabled) AppTheme.DARK else AppTheme.LIGHT)
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
                }
                .background(appBackgroundGradient),
        ) {
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
                            TopBrandTitle(
                                onDoubleTap = {
                                    scrollToTopOfCurrentPage(animated = true)
                                },
                            )
                        },
                        actions = {
                            when (currentPage) {
                                AppPage.DASHBOARD -> {
                                    TextButton(onClick = { openSettings() }) {
                                        Text(
                                            stringResource(R.string.menu_settings),
                                            color = MaterialTheme.colorScheme.onBackground,
                                        )
                                    }
                                    TextButton(
                                        modifier = Modifier.semantics {
                                            contentDescription = manageServersDescription
                                        },
                                        onClick = {
                                            showServerProfileSheet = true
                                        },
                                    ) {
                                        Text("+", color = MaterialTheme.colorScheme.onBackground, fontSize = 22.sp)
                                    }
                                }

                                AppPage.SETTINGS -> {
                                    TextButton(
                                        onClick = {
                                            if (state.connected) viewModel.refresh() else viewModel.connect()
                                        },
                                    ) {
                                        Text(
                                            text = if (state.connected) {
                                                if (state.isRefreshing) {
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
                                            if (state.connected) viewModel.refresh() else viewModel.connect()
                                        },
                                    ) {
                                        Text(
                                            text = if (state.connected) {
                                                if (state.isRefreshing) {
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
                LazyColumn(
                    state = contentListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = contentReveal
                            translationY = (1f - contentReveal) * 36f
                        }
                        .padding(innerPadding),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    when (currentPage) {
                        AppPage.DASHBOARD -> {
                            if (state.connected) {
                                item {
                                    ServerOverviewCard(
                                        serverVersion = state.serverVersion,
                                        transferInfo = state.transferInfo,
                                        torrents = state.torrents,
                                        torrentCount = state.torrents.size,
                                        showTotals = state.settings.showSpeedTotals,
                                        isRefreshing = state.isRefreshing,
                                        onRefresh = viewModel::refresh,
                                        onOpenTorrentList = ::openTorrentList,
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
                                    item {
                                        OutlinedTextField(
                                            value = torrentSearchQuery,
                                            onValueChange = { torrentSearchQuery = it },
                                            modifier = Modifier.fillMaxWidth(),
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

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 72.dp)
                    .height(56.dp)
                    .zIndex(2f)
                    .pointerInput(contentListState) {
                        detectTapGestures(
                            onDoubleTap = {
                                scrollToTopOfCurrentPage(animated = true)
                            },
                        )
                    },
            )

            if (showServerProfileSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showServerProfileSheet = false },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
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
private fun AddTorrentSheet(
    context: Context,
    categoryOptions: List<String>,
    tagOptions: List<String>,
    pathOptions: List<String>,
    onCancel: () -> Unit,
    onAdd: (
        urls: String,
        files: List<AddTorrentFile>,
        autoTmm: Boolean,
        category: String,
        tags: String,
        savePath: String,
        paused: Boolean,
        skipChecking: Boolean,
        sequentialDownload: Boolean,
        firstLastPiecePrio: Boolean,
        uploadLimitKb: String,
        downloadLimitKb: String,
    ) -> Unit,
) {
    var urls by remember { mutableStateOf("") }
    var selectedFiles by remember { mutableStateOf(listOf<AddTorrentFile>()) }
    var autoTmm by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var savePath by remember { mutableStateOf("") }
    var paused by remember { mutableStateOf(false) }
    var skipChecking by remember { mutableStateOf(false) }
    var sequentialDownload by remember { mutableStateOf(true) }
    var firstLastPiecePrio by remember { mutableStateOf(false) }
    var uploadLimitKb by remember { mutableStateOf("") }
    var downloadLimitKb by remember { mutableStateOf("") }
    val canAdd = urls.trim().isNotBlank() || selectedFiles.isNotEmpty()
    val suggestedCategoryOptions = remember(categoryOptions) {
        categoryOptions
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }
    val suggestedPathOptions = remember(pathOptions) {
        pathOptions
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    val pickTorrentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNullOrEmpty()) return@rememberLauncherForActivityResult
        val newFiles = uris.mapNotNull { readTorrentFile(context, it) }
        if (newFiles.isNotEmpty()) {
            selectedFiles = (selectedFiles + newFiles).distinctBy { file ->
                "${file.name}|${file.bytes.size}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 700.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.add_torrent_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.torrent_links_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(R.string.torrent_links_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = urls,
                    onValueChange = { urls = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("magnet:?xt=...") },
                    minLines = 2,
                    maxLines = 4,
                )
            }
        }

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.torrent_files_title),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    TextButton(
                        onClick = { pickTorrentLauncher.launch(arrayOf("*/*")) },
                    ) {
                        Text("+")
                    }
                }
                if (selectedFiles.isEmpty()) {
                    Text(
                        text = stringResource(R.string.torrent_files_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    selectedFiles.take(5).forEach { file ->
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (selectedFiles.size > 5) {
                        Text(
                            text = pluralStringResource(
                                id = R.plurals.more_files_count,
                                count = selectedFiles.size - 5,
                                selectedFiles.size - 5,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SettingSwitchRow(
                    title = stringResource(R.string.auto_torrent_management),
                    checked = autoTmm,
                    onCheckedChange = { autoTmm = it },
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.add_category_label)) },
                    placeholder = { Text(stringResource(R.string.leave_empty_hint)) },
                    singleLine = true,
                )
                if (suggestedCategoryOptions.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(suggestedCategoryOptions, key = { it }) { option ->
                            val selected = category.equals(option, ignoreCase = true)
                            TorrentMetaChip(
                                text = option,
                                containerColor = if (selected) Color(0xFF5D7CFF) else Color(0xFF4D4D4D),
                                contentColor = Color(0xFFEAF0FF),
                                onClick = { category = option },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.add_tags_label)) },
                    placeholder = { Text(stringResource(R.string.tags_split_hint)) },
                    singleLine = true,
                )
                if (tagOptions.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(tagOptions, key = { it }) { option ->
                            val selected = parseTags(tags).any { it.equals(option, ignoreCase = true) }
                            TorrentMetaChip(
                                text = option,
                                containerColor = if (selected) Color(0xFF5D7CFF) else Color(0xFF4D4D4D),
                                contentColor = Color(0xFFEAF0FF),
                                onClick = { tags = toggleTag(tags, option) },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = savePath,
                    onValueChange = { savePath = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.save_path_manual_label)) },
                    placeholder = { Text("/mnt/usb2_2-1/download") },
                    singleLine = true,
                )
                if (suggestedPathOptions.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(suggestedPathOptions, key = { it }) { option ->
                            val selected = savePath.equals(option, ignoreCase = true)
                            TorrentMetaChip(
                                text = option,
                                containerColor = if (selected) Color(0xFF5D7CFF) else Color(0xFF4D4D4D),
                                contentColor = Color(0xFFEAF0FF),
                                onClick = { savePath = option },
                            )
                        }
                    }
                }
                SettingSwitchRow(
                    title = stringResource(R.string.pause_after_add),
                    checked = paused,
                    onCheckedChange = { paused = it },
                )
            }
        }

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SettingSwitchRow(
                    title = stringResource(R.string.skip_hash_check),
                    checked = skipChecking,
                    onCheckedChange = { skipChecking = it },
                )
                SettingSwitchRow(
                    title = stringResource(R.string.sequential_download),
                    checked = sequentialDownload,
                    onCheckedChange = { sequentialDownload = it },
                )
                SettingSwitchRow(
                    title = stringResource(R.string.first_last_piece_prio),
                    checked = firstLastPiecePrio,
                    onCheckedChange = { firstLastPiecePrio = it },
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uploadLimitKb,
                        onValueChange = { uploadLimitKb = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.upload_limit_kb_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(
                        value = downloadLimitKb,
                        onValueChange = { downloadLimitKb = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.download_limit_kb_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
            TextButton(
                enabled = canAdd,
                onClick = {
                    onAdd(
                        urls,
                        selectedFiles,
                        autoTmm,
                        category,
                        tags,
                        savePath,
                        paused,
                        skipChecking,
                        sequentialDownload,
                        firstLastPiecePrio,
                        uploadLimitKb,
                        downloadLimitKb,
                    )
                },
            ) {
                Text(stringResource(R.string.add))
            }
        }
    }
}

@Composable
private fun ServerProfileSheet(
    currentSettings: ConnectionSettings,
    profiles: List<ServerProfile>,
    activeProfileId: String?,
    onSwitchProfile: (String) -> Unit,
    onAddProfile: (
        name: String,
        host: String,
        port: String,
        useHttps: Boolean,
        username: String,
        password: String,
        refreshSeconds: String,
    ) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf(currentSettings.host) }
    var port by remember { mutableStateOf(if (currentSettings.port <= 0) "8080" else currentSettings.port.toString()) }
    var useHttps by remember { mutableStateOf(currentSettings.useHttps) }
    var username by remember { mutableStateOf(currentSettings.username) }
    var password by remember { mutableStateOf(currentSettings.password) }
    var refreshSeconds by remember { mutableStateOf(currentSettings.refreshSeconds.toString()) }

    val canAdd = host.trim().isNotBlank() && username.trim().isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 760.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.server_manage_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        if (profiles.isEmpty()) {
            Text(
                text = stringResource(R.string.no_saved_servers),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            profiles.forEach { profile ->
                val active = profile.id == activeProfileId
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSwitchProfile(profile.id) },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    ),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (active) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        }
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = buildServerAddressText(
                                    ConnectionSettings(
                                        host = profile.host,
                                        port = profile.port,
                                        useHttps = profile.useHttps,
                                    )
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = if (active) {
                                stringResource(R.string.server_current)
                            } else {
                                stringResource(R.string.server_switch)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        Text(
            text = stringResource(R.string.add_server_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.server_name_optional_label)) },
            singleLine = true,
        )
        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.connection_host_label)) },
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.connection_port_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            OutlinedTextField(
                value = refreshSeconds,
                onValueChange = { refreshSeconds = it },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.connection_refresh_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
        }
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.connection_username_label)) },
            singleLine = true,
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.connection_password_label)) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.connection_https_label))
            Switch(
                checked = useHttps,
                onCheckedChange = { useHttps = it },
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
            TextButton(
                enabled = canAdd,
                onClick = {
                    onAddProfile(
                        name.trim(),
                        host.trim(),
                        port.trim(),
                        useHttps,
                        username.trim(),
                        password,
                        refreshSeconds.trim(),
                    )
                },
            ) {
                Text(stringResource(R.string.save_and_connect))
            }
        }
    }
}

private fun readTorrentFile(context: Context, uri: Uri): AddTorrentFile? {
    return runCatching {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val name = readDisplayName(context, uri).ifBlank { "upload.torrent" }
        AddTorrentFile(name = name, bytes = bytes)
    }.getOrNull()
}

private fun readDisplayName(context: Context, uri: Uri): String {
    val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
    return runCatching {
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex).orEmpty() else ""
        }.orEmpty()
    }.getOrDefault("")
}

@Composable
private fun DrawerThemeItem(
    darkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.menu_theme),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (darkTheme) {
                    stringResource(R.string.theme_dark)
                } else {
                    stringResource(R.string.theme_light)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = darkTheme,
            onCheckedChange = onThemeChange,
        )
    }
}

@Composable
private fun TopBrandTitle(
    onDoubleTap: () -> Unit = {},
) {
    Text(
        text = buildAnnotatedString {
            pushStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                ),
            )
            append("qb")
            pop()
            pushStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.92f),
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            append("remote")
            pop()
        },
        style = MaterialTheme.typography.titleMedium.copy(
            letterSpacing = 0.sp,
        ),
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = {
                    onDoubleTap()
                },
            )
        },
        maxLines = 1,
    )
}

@Composable
private fun NeedConnectionCard(
    onOpenConnection: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = PanelShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.connect_first_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = onOpenConnection,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                    ),
                ) {
                    Text(stringResource(R.string.go_to_settings))
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
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f),
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
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
private fun SettingsPanelCard(
    content: @Composable () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = { content() },
        )
    }
}

@Composable
private fun SettingsPageContent(
    settings: ConnectionSettings,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onShowSpeedTotalsChange: (Boolean) -> Unit,
    onEnableServerGroupingChange: (Boolean) -> Unit,
    onDeleteFilesWhenNoSeedersChange: (Boolean) -> Unit,
    onDeleteFilesDefaultChange: (Boolean) -> Unit,
) {
    var showLanguageMenu by remember { mutableStateOf(false) }
    var pendingAppLanguage by rememberSaveable { mutableStateOf(settings.appLanguage) }

    LaunchedEffect(settings.appLanguage) {
        pendingAppLanguage = settings.appLanguage
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SettingsPanelCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_language),
                    modifier = Modifier.weight(1f),
                )
                Box {
                    TextButton(onClick = { showLanguageMenu = true }) {
                        Text(appLanguageLabel(pendingAppLanguage))
                    }
                    DropdownMenu(
                        expanded = showLanguageMenu,
                        onDismissRequest = { showLanguageMenu = false },
                    ) {
                        AppLanguage.entries.forEach { language ->
                            DropdownMenuItem(
                                text = { Text(appLanguageLabel(language)) },
                                onClick = {
                                    pendingAppLanguage = language
                                    showLanguageMenu = false
                                },
                            )
                        }
                    }
                }
                TextButton(
                    enabled = pendingAppLanguage != settings.appLanguage,
                    onClick = { onAppLanguageChange(pendingAppLanguage) },
                ) {
                    Text(stringResource(R.string.settings_language_save))
                }
            }
            Text(
                text = stringResource(R.string.settings_language_apply_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingSwitchRow(
                title = stringResource(R.string.settings_show_speed_totals),
                checked = settings.showSpeedTotals,
                onCheckedChange = onShowSpeedTotalsChange,
            )
            SettingSwitchRow(
                title = stringResource(R.string.settings_enable_server_grouping),
                checked = settings.enableServerGrouping,
                onCheckedChange = onEnableServerGroupingChange,
            )
        }
        SettingsPanelCard {
            SettingSwitchRow(
                title = stringResource(R.string.settings_delete_when_no_seeders),
                checked = settings.deleteFilesWhenNoSeeders,
                onCheckedChange = onDeleteFilesWhenNoSeedersChange,
            )
            SettingSwitchRow(
                title = stringResource(R.string.settings_delete_by_default),
                checked = settings.deleteFilesDefault,
                onCheckedChange = onDeleteFilesDefaultChange,
            )
        }
    }
}

@Composable
private fun ConnectionCard(
    state: MainUiState,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onHttpsChange: (Boolean) -> Unit,
    onUserChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRefreshSecondsChange: (String) -> Unit,
    onConnect: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = PanelShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.connection_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.settings.host,
                onValueChange = onHostChange,
                singleLine = true,
                label = { Text(stringResource(R.string.connection_host_label)) },
                placeholder = { Text(stringResource(R.string.connection_host_hint)) },
                shape = RoundedCornerShape(14.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.width(120.dp),
                    value = if (state.settings.port == 0) "" else state.settings.port.toString(),
                    onValueChange = onPortChange,
                    singleLine = true,
                    label = { Text(stringResource(R.string.connection_port_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                )

                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = state.settings.refreshSeconds.toString(),
                    onValueChange = onRefreshSecondsChange,
                    singleLine = true,
                    label = { Text(stringResource(R.string.connection_refresh_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                )
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.settings.username,
                onValueChange = onUserChange,
                singleLine = true,
                label = { Text(stringResource(R.string.connection_username_label)) },
                shape = RoundedCornerShape(14.dp),
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.settings.password,
                onValueChange = onPasswordChange,
                singleLine = true,
                label = { Text(stringResource(R.string.connection_password_label)) },
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(14.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.connection_https_label))
                Switch(
                    checked = state.settings.useHttps,
                    onCheckedChange = onHttpsChange,
                    modifier = Modifier.padding(start = 6.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = onConnect,
                        enabled = !state.isConnecting,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp),
                            ),
                    ) {
                        Text(
                            if (state.isConnecting) {
                                stringResource(R.string.connecting)
                            } else {
                                stringResource(R.string.connect)
                            }
                        )
                    }
                }
            }
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
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.86f),
        ),
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
                TextButton(onClick = onRefresh) {
                    Text(
                        if (isRefreshing) {
                            stringResource(R.string.refreshing)
                        } else {
                            stringResource(R.string.refresh)
                        }
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.86f),
        ),
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
private fun TorrentCard(
    torrent: TorrentInfo,
    crossSeedCount: Int,
    isPending: Boolean,
    onOpenDetails: () -> Unit,
) {
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
    val activeAgoText = formatActiveAgo(torrent.lastActivity)
    val addedOnText = formatAddedOn(torrent.addedOn)
    val savePathText = torrent.savePath.ifBlank { "-" }
    val stateStyle = torrentStateStyle(effectiveState)

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isPending) { onOpenDetails() },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, stateStyle.borderColor.copy(alpha = 0.58f)),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.86f),
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
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = torrent.name,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 17.sp,
                )
            }

            TorrentMetaHeaderRow(
                tagsText = tagsText,
                crossSeedCount = crossSeedCount,
                stateLabel = stateLabel,
                stateStyle = stateStyle,
                addedOnText = addedOnText,
                activeAgoText = activeAgoText,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                LinearProgressIndicator(
                    progress = { torrent.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.weight(1f),
                    color = stateStyle.progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text(
                    text = formatPercent(torrent.progress),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = stateStyle.progressColor,
                )
            }

            TorrentQuickStatsRow(
                torrent = torrent,
                categoryText = categoryText,
                savePathText = savePathText,
                minHeight = 96.dp,
            )

        }
    }
}

@Composable
private fun TorrentOperationDetailCard(
    torrent: TorrentInfo,
    crossSeedCount: Int,
    isPending: Boolean,
    detailLoading: Boolean,
    detailProperties: TorrentProperties?,
    detailFiles: List<TorrentFileInfo>,
    detailTrackers: List<com.hjw.qbremote.data.model.TorrentTracker>,
    categoryOptions: List<String>,
    tagOptions: List<String>,
    deleteFilesDefault: Boolean,
    deleteFilesWhenNoSeeders: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: (Boolean) -> Unit,
    onRename: (String) -> Unit,
    onSetLocation: (String) -> Unit,
    onSetCategory: (String) -> Unit,
    onSetTags: (String, String) -> Unit,
    onSetSpeedLimit: (String, String) -> Unit,
    onSetShareRatio: (String) -> Unit,
) {
    var showDeleteDialog by remember(torrent.hash) { mutableStateOf(false) }
    var deleteFilesChecked by remember(torrent.hash) { mutableStateOf(false) }

    var renameText by remember(torrent.hash) { mutableStateOf(torrent.name) }
    var locationText by remember(torrent.hash) {
        mutableStateOf(detailProperties?.savePath?.takeIf { it.isNotBlank() } ?: torrent.savePath)
    }
    var categoryTextInput by remember(torrent.hash) { mutableStateOf(torrent.category) }
    var tagsTextInput by remember(torrent.hash) { mutableStateOf(torrent.tags) }
    var downloadLimitText by remember(torrent.hash) { mutableStateOf("") }
    var uploadLimitText by remember(torrent.hash) { mutableStateOf("") }
    var ratioText by remember(torrent.hash) { mutableStateOf(formatRatio(torrent.ratio)) }

    var selectedTab by remember(torrent.hash) { mutableIntStateOf(0) }

    LaunchedEffect(torrent.hash, detailProperties?.downloadLimit, detailProperties?.uploadLimit) {
        val dl = detailProperties?.downloadLimit ?: 0L
        val up = detailProperties?.uploadLimit ?: 0L
        downloadLimitText = if (dl > 0L) (dl / 1024L).toString() else ""
        uploadLimitText = if (up > 0L) (up / 1024L).toString() else ""
    }
    LaunchedEffect(torrent.hash, detailProperties?.shareRatio) {
        val ratio = detailProperties?.shareRatio
        if (ratio != null && ratio >= 0.0 && ratio.isFinite()) {
            ratioText = formatRatio(ratio)
        }
    }

    val effectiveState = effectiveTorrentState(torrent)
    val paused = isPausedState(effectiveState)
    val stateLabel = localizedTorrentStateLabel(effectiveState)
    val stateStyle = torrentStateStyle(effectiveState)
    val tagsText = compactTagsLabel(
        tags = torrent.tags,
        noTagsText = stringResource(R.string.no_tags),
    )
    val addedOnText = formatAddedOn(torrent.addedOn)
    val activeAgoText = formatActiveAgo(torrent.lastActivity)
    val categoryText = normalizeCategoryLabel(
        category = torrent.category,
        noCategoryText = stringResource(R.string.no_category),
    )
    val savePathText = torrent.savePath.ifBlank { "-" }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.42f)),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = torrent.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            TabRow(selectedTabIndex = selectedTab) {
                listOf(
                    stringResource(R.string.tab_info),
                    stringResource(R.string.tab_trackers),
                    stringResource(R.string.tab_peers),
                    stringResource(R.string.tab_files),
                ).forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        TorrentMetaHeaderRow(
                            tagsText = tagsText,
                            crossSeedCount = crossSeedCount,
                            stateLabel = stateLabel,
                            stateStyle = stateStyle,
                            addedOnText = addedOnText,
                            activeAgoText = activeAgoText,
                        )
                        TorrentInfoCell(
                            text = formatPercent(torrent.progress),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LinearProgressIndicator(
                            progress = { torrent.progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = stateStyle.progressColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))

                        TorrentQuickStatsRow(
                            torrent = torrent,
                            categoryText = categoryText,
                            savePathText = savePathText,
                            minHeight = 84.dp,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))

                        Text(
                            stringResource(R.string.detail_section_name),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ActionInputRow(
                            label = stringResource(R.string.detail_new_name_label),
                            value = renameText,
                            onValueChange = { renameText = it },
                            actionText = stringResource(R.string.detail_action_change),
                            enabled = !isPending,
                            onAction = { onRename(renameText.trim()) },
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
                        Text(
                            stringResource(R.string.detail_section_path),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.detail_set_path_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ActionInputRow(
                            label = stringResource(R.string.detail_save_path_label),
                            value = locationText,
                            onValueChange = { locationText = it },
                            actionText = stringResource(R.string.detail_action_change),
                            enabled = !isPending,
                            onAction = { onSetLocation(locationText.trim()) },
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
                        Text(
                            stringResource(R.string.detail_section_category),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (categoryOptions.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                items(categoryOptions, key = { it }) { option ->
                                    TorrentMetaChip(
                                        text = option,
                                        containerColor = if (option == categoryTextInput) Color(0xFF5D7CFF) else Color(0xFF4D4D4D),
                                        contentColor = Color(0xFFEAF0FF),
                                        onClick = { categoryTextInput = option },
                                    )
                                }
                            }
                        }
                        ActionInputRow(
                            label = stringResource(R.string.detail_category_label),
                            value = categoryTextInput,
                            onValueChange = { categoryTextInput = it },
                            actionText = stringResource(R.string.detail_action_change),
                            enabled = !isPending,
                            onAction = { onSetCategory(categoryTextInput.trim()) },
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
                        Text(
                            stringResource(R.string.detail_section_tags),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (tagOptions.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                items(tagOptions, key = { it }) { option ->
                                    val selected = parseTags(tagsTextInput).contains(option)
                                    TorrentMetaChip(
                                        text = option,
                                        containerColor = if (selected) Color(0xFF5D7CFF) else Color(0xFF4D4D4D),
                                        contentColor = Color(0xFFEAF0FF),
                                        onClick = { tagsTextInput = toggleTag(tagsTextInput, option) },
                                    )
                                }
                            }
                        }
                        ActionInputRow(
                            label = stringResource(R.string.detail_tags_label),
                            value = tagsTextInput,
                            onValueChange = { tagsTextInput = it },
                            actionText = stringResource(R.string.detail_action_change),
                            enabled = !isPending,
                            onAction = { onSetTags(torrent.tags, tagsTextInput.trim()) },
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
                        Text(
                            stringResource(R.string.detail_section_speed_limit),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = downloadLimitText,
                                onValueChange = { downloadLimitText = it },
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(R.string.detail_download_kb_label)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                enabled = !isPending,
                            )
                            OutlinedTextField(
                                value = uploadLimitText,
                                onValueChange = { uploadLimitText = it },
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(R.string.detail_upload_kb_label)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                enabled = !isPending,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = { onSetSpeedLimit(downloadLimitText, uploadLimitText) }, enabled = !isPending) {
                                Text(stringResource(R.string.detail_action_apply))
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
                        Text(
                            stringResource(R.string.detail_section_ratio),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ActionInputRow(
                            label = stringResource(R.string.detail_ratio_label),
                            value = ratioText,
                            onValueChange = { ratioText = it },
                            actionText = stringResource(R.string.detail_action_apply),
                            enabled = !isPending,
                            onAction = { onSetShareRatio(ratioText.trim()) },
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(
                                onClick = {
                                    if (paused) onResume() else onPause()
                                },
                                enabled = !isPending,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), RoundedCornerShape(8.dp)),
                            ) {
                                Text(
                                    if (paused) {
                                        stringResource(R.string.resume)
                                    } else {
                                        stringResource(R.string.pause)
                                    }
                                )
                            }
                            TextButton(
                                onClick = {
                                    deleteFilesChecked = deleteFilesDefault ||
                                        (deleteFilesWhenNoSeeders && torrent.seeders <= 0)
                                    showDeleteDialog = true
                                },
                                enabled = !isPending,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.14f), RoundedCornerShape(8.dp)),
                            ) {
                                Text(stringResource(R.string.delete))
                            }
                        }
                    }
                }

                1 -> {
                    if (detailLoading) {
                        Text(
                            stringResource(R.string.loading),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TorrentMetaChip(
                            text = "🌐 ${detailTrackers.size}",
                            containerColor = Color(0xFF6C3FD3),
                            contentColor = Color.White,
                        )
                    }
                    if (detailTrackers.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_tracker_info),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        detailTrackers.forEach { tracker ->
                            TrackerInfoCard(tracker = tracker)
                        }
                    }
                }

                2 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TorrentInfoCell(
                            text = stringResource(R.string.torrent_seed_count_fmt, torrent.seeders, torrent.numComplete),
                            modifier = Modifier.weight(1f),
                        )
                        TorrentInfoCell(
                            text = stringResource(R.string.torrent_peer_count_fmt, torrent.leechers, torrent.numIncomplete),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TorrentInfoCell(
                            text = stringResource(R.string.torrent_cross_seed_chip_fmt, crossSeedCount),
                            modifier = Modifier.weight(1f),
                        )
                        TorrentInfoCell(
                            text = stringResource(R.string.torrent_ratio_fmt, formatRatio(torrent.ratio)),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    TorrentInfoCell(
                        text = stringResource(
                            R.string.recent_activity_fmt,
                            formatActiveAgo(torrent.lastActivity),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                3 -> {
                    if (detailLoading) {
                        Text(
                            stringResource(R.string.loading_files),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else if (detailFiles.isEmpty()) {
                        Text(
                            stringResource(R.string.no_file_details),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        detailFiles.take(120).forEach { file ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = file.name,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = formatBytes(file.size),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = formatPercent(file.progress),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = PanelShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text(stringResource(R.string.delete_torrent_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.delete_torrent_desc))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = deleteFilesChecked,
                            onCheckedChange = { deleteFilesChecked = it },
                        )
                        Text(stringResource(R.string.delete_files))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(deleteFilesChecked)
                    },
                    enabled = !isPending,
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ActionInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    actionText: String,
    enabled: Boolean,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            label = { Text(label) },
            enabled = enabled,
        )
        TextButton(
            onClick = onAction,
            enabled = enabled,
            modifier = Modifier.background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                shape = RoundedCornerShape(8.dp),
            ),
        ) {
            Text(actionText)
        }
    }
}

@Composable
private fun TrackerInfoCard(tracker: com.hjw.qbremote.data.model.TorrentTracker) {
    val status = trackerStatusLabel(tracker.status)
    val statusColor = trackerStatusColor(tracker.status)
    val message = tracker.message.trim().ifBlank {
        stringResource(R.string.tracker_message_ok)
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TorrentMetaChip(
                    text = status,
                    containerColor = statusColor.copy(alpha = 0.22f),
                    contentColor = statusColor,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = tracker.url.ifBlank { "-" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.tracker_stats_fmt,
                    tracker.numPeers,
                    tracker.numSeeds,
                    tracker.numLeeches,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun trackerStatusLabel(status: Int): String {
    return when (status) {
        0 -> stringResource(R.string.tracker_status_disabled)
        1 -> stringResource(R.string.tracker_status_not_contacted)
        2 -> stringResource(R.string.tracker_status_working)
        3 -> stringResource(R.string.tracker_status_updating)
        4 -> stringResource(R.string.tracker_status_not_working)
        else -> stringResource(R.string.state_unknown)
    }
}

private fun trackerStatusColor(status: Int): Color {
    return when (status) {
        0 -> Color(0xFF9E9E9E)
        1 -> Color(0xFF90A4AE)
        2 -> Color(0xFF4CAF50)
        3 -> Color(0xFFFFC107)
        4 -> Color(0xFFE53935)
        else -> Color(0xFF607D8B)
    }
}

private fun parseTags(input: String): List<String> {
    return input
        .split(',', ';', '|')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

private fun matchesTorrentSearch(torrent: TorrentInfo, query: String): Boolean {
    val normalizedQuery = query.trim().lowercase()
    if (normalizedQuery.isBlank()) return true

    return listOf(
        torrent.name,
        torrent.hash,
        torrent.category,
        torrent.tags,
        torrent.savePath,
        torrent.tracker,
    ).any { field ->
        field.lowercase().contains(normalizedQuery)
    }
}

private fun toggleTag(current: String, option: String): String {
    val tags = parseTags(current).toMutableList()
    val idx = tags.indexOfFirst { it.equals(option, ignoreCase = false) }
    if (idx >= 0) {
        tags.removeAt(idx)
    } else {
        tags.add(option)
    }
    return tags.joinToString(",")
}

private data class TorrentStateStyle(
    val borderColor: Color,
    val progressColor: Color,
    val tagContainer: Color,
    val tagContent: Color,
)

@Composable
private fun torrentStateStyle(state: String): TorrentStateStyle {
    val normalized = normalizeTorrentState(state)
    val base = when (normalized) {
        "error", "missingfiles" -> Color(0xFFD32F2F)
        "downloading", "stalleddl", "forceddl" -> Color(0xFF1E88E5)
        "uploading", "stalledup", "forcedup" -> Color(0xFF2E7D32)
        "pauseddl", "pausedup", "stoppeddl", "stoppedup" -> Color(0xFF6D6D6D)
        "queueddl", "queuedup", "checkingdl", "checkingup", "checkingresumedata", "metadl", "forcedmetadl", "allocating", "moving" -> Color(0xFFF9A825)
        else -> Color(0xFF607D8B)
    }
    return TorrentStateStyle(
        borderColor = base,
        progressColor = base,
        tagContainer = base.copy(alpha = 0.20f),
        tagContent = base,
    )
}

@Composable
private fun TorrentStateTag(
    label: String,
    style: TorrentStateStyle,
) {
    Box(
        modifier = Modifier
            .background(style.tagContainer, RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(
            text = label,
            color = style.tagContent,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TorrentMetaChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .background(containerColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TorrentInfoCell(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
                shape = RoundedCornerShape(7.dp),
            )
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TorrentQuickStatsRow(
    torrent: TorrentInfo,
    categoryText: String,
    savePathText: String,
    minHeight: Dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(0.24f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = "▲ ${formatSpeed(torrent.uploadSpeed)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF6E8DFF),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "▼ ${formatSpeed(torrent.downloadSpeed)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFF5B95),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(
            modifier = Modifier.weight(0.44f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = "📤 ${formatBytes(torrent.uploaded)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "📥 ${formatBytes(torrent.downloaded)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "🏷️ $categoryText",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "📁 $savePathText",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(
            modifier = Modifier.weight(0.32f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = "⚖ ${formatRatio(torrent.ratio)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "💾 ${formatBytes(torrent.size)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "🌱 ${torrent.seeders}/${torrent.numComplete}  👥 ${torrent.leechers}/${torrent.numIncomplete}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(999.dp),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
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

@Composable
private fun SettingsDialog(
    settings: ConnectionSettings,
    onDismiss: () -> Unit,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onShowSpeedTotalsChange: (Boolean) -> Unit,
    onEnableServerGroupingChange: (Boolean) -> Unit,
    onShowChartPanelChange: (Boolean) -> Unit,
    onChartShowSiteNameChange: (Boolean) -> Unit,
    onChartSortModeChange: (ChartSortMode) -> Unit,
    onDeleteFilesWhenNoSeedersChange: (Boolean) -> Unit,
    onDeleteFilesDefaultChange: (Boolean) -> Unit,
) {
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showChartSortMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = PanelShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(stringResource(R.string.settings_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_language),
                        modifier = Modifier.weight(1f),
                    )
                    Box {
                        TextButton(onClick = { showLanguageMenu = true }) {
                            Text(appLanguageLabel(settings.appLanguage))
                        }
                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false },
                        ) {
                            AppLanguage.entries.forEach { language ->
                                DropdownMenuItem(
                                    text = { Text(appLanguageLabel(language)) },
                                    onClick = {
                                        onAppLanguageChange(language)
                                        showLanguageMenu = false
                                    },
                                )
                            }
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.settings_language_apply_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SettingSwitchRow(
                    title = stringResource(R.string.settings_show_speed_totals),
                    checked = settings.showSpeedTotals,
                    onCheckedChange = onShowSpeedTotalsChange,
                )
                SettingSwitchRow(
                    title = stringResource(R.string.settings_enable_server_grouping),
                    checked = settings.enableServerGrouping,
                    onCheckedChange = onEnableServerGroupingChange,
                )
                SettingSwitchRow(
                    title = stringResource(R.string.settings_show_chart_panel),
                    checked = settings.showChartPanel,
                    onCheckedChange = onShowChartPanelChange,
                )
                SettingSwitchRow(
                    title = stringResource(R.string.settings_show_site_name),
                    checked = settings.chartShowSiteName,
                    onCheckedChange = onChartShowSiteNameChange,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_chart_sort_mode),
                        modifier = Modifier.weight(1f),
                    )
                    Box {
                        TextButton(onClick = { showChartSortMenu = true }) {
                            Text(chartSortModeLabel(settings.chartSortMode))
                        }
                        DropdownMenu(
                            expanded = showChartSortMenu,
                            onDismissRequest = { showChartSortMenu = false },
                        ) {
                            ChartSortMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(chartSortModeLabel(mode)) },
                                    onClick = {
                                        onChartSortModeChange(mode)
                                        showChartSortMenu = false
                                    },
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                SettingSwitchRow(
                    title = stringResource(R.string.settings_delete_when_no_seeders),
                    checked = settings.deleteFilesWhenNoSeeders,
                    onCheckedChange = onDeleteFilesWhenNoSeedersChange,
                )
                SettingSwitchRow(
                    title = stringResource(R.string.settings_delete_by_default),
                    checked = settings.deleteFilesDefault,
                    onCheckedChange = onDeleteFilesDefaultChange,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.done))
            }
        },
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun localizedTorrentStateLabel(state: String): String {
    return when (normalizeTorrentState(state)) {
        "downloading", "stalleddl" -> stringResource(R.string.state_downloading)
        "uploading", "stalledup" -> stringResource(R.string.state_seeding)
        "pauseddl", "pausedup" -> stringResource(R.string.state_paused)
        "error", "missingfiles" -> stringResource(R.string.state_error)
        "queueddl", "queuedup" -> stringResource(R.string.state_queued)
        "metadl", "forcedmetadl" -> stringResource(R.string.state_metadata)
        "checkingdl", "checkingup", "checkingresumedata" -> stringResource(R.string.state_checking)
        "allocating", "moving" -> stringResource(R.string.state_preparing)
        "stoppeddl", "stoppedup" -> stringResource(R.string.state_stopped)
        "forceddl", "forcedup" -> stringResource(R.string.state_forced)
        "unknown", "" -> stringResource(R.string.state_unknown)
        else -> stringResource(R.string.state_unknown)
    }
}

private fun isPausedState(state: String): Boolean {
    return normalizeTorrentState(state) in setOf("pauseddl", "pausedup", "stoppeddl", "stoppedup")
}

private fun isActiveTransferState(state: String): Boolean {
    return normalizeTorrentState(state) in setOf(
        "downloading", "forceddl", "stalleddl", "metadl", "forcedmetadl",
        "checkingdl", "queueddl", "allocating", "moving", "checkingresumedata",
        "uploading", "forcedup", "stalledup", "checkingup", "queuedup"
    )
}

private fun effectiveTorrentState(torrent: TorrentInfo): String {
    val normalized = normalizeTorrentState(torrent.state)
    if (normalized.isNotBlank() && normalized != "unknown") return normalized
    if (torrent.uploadSpeed > 0L) return "uploading"
    if (torrent.downloadSpeed > 0L) return "downloading"
    if (torrent.progress >= 1f && (torrent.uploaded > 0L || torrent.downloaded > 0L || torrent.size > 0L)) {
        return "stalledup"
    }
    if (torrent.progress > 0f || torrent.downloaded > 0L) return "stalleddl"
    return if (normalized.isBlank()) "unknown" else normalized
}

private fun normalizeTorrentState(state: String): String {
    return state.trim().lowercase()
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

@Composable
private fun TorrentMetaHeaderRow(
    tagsText: String,
    crossSeedCount: Int,
    stateLabel: String,
    stateStyle: TorrentStateStyle,
    addedOnText: String,
    activeAgoText: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
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
                TorrentMetaChip(
                    text = stringResource(R.string.torrent_cross_seed_chip_fmt, crossSeedCount),
                    containerColor = Color(0xFF1F7AE0),
                    contentColor = Color(0xFFE4F0FF),
                )
            }
            item {
                TorrentStateTag(
                    label = stateLabel,
                    style = stateStyle,
                )
            }
        }

        Column(
            modifier = Modifier.widthIn(max = 170.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            TorrentTimestampLabel(
                iconResId = R.drawable.ic_meta_added,
                markerColor = Color(0xFF2D74F7),
                text = addedOnText,
            )
            TorrentTimestampLabel(
                iconResId = R.drawable.ic_meta_active,
                markerColor = Color(0xFF0C9FA9),
                text = activeAgoText,
            )
        }
    }
}

@Composable
private fun TorrentTimestampLabel(
    iconResId: Int,
    markerColor: Color,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color = markerColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(3.dp),
                )
                .border(
                    width = 1.dp,
                    color = markerColor.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(3.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                modifier = Modifier.size(8.dp),
                tint = markerColor,
            )
        }

        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun normalizeCategoryLabel(category: String, noCategoryText: String): String {
    val normalized = category.trim()
    if (normalized.isBlank()) return noCategoryText
    if (normalized == "-" || normalized.equals("null", ignoreCase = true)) return noCategoryText
    return normalized
}

private fun compactTagsLabel(tags: String, noTagsText: String): String {
    val normalizedTags = tags
        .split(',', ';', '|')
        .map { it.trim() }
        .filter { it.isNotBlank() && it != "-" && !it.equals("null", ignoreCase = true) }

    if (normalizedTags.isEmpty()) return noTagsText
    if (normalizedTags.size <= 2) return normalizedTags.joinToString(",")

    val preview = normalizedTags.take(2).joinToString(",")
    return "$preview +${normalizedTags.size - 2}"
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

@Composable
private fun chartSortModeLabel(mode: ChartSortMode): String {
    return when (mode) {
        ChartSortMode.TOTAL_SPEED -> stringResource(R.string.chart_sort_total_speed)
        ChartSortMode.DOWNLOAD_SPEED -> stringResource(R.string.chart_sort_download_speed)
        ChartSortMode.UPLOAD_SPEED -> stringResource(R.string.chart_sort_upload_speed)
        ChartSortMode.TORRENT_COUNT -> stringResource(R.string.chart_sort_torrent_count)
    }
}

@Composable
private fun appLanguageLabel(language: AppLanguage): String {
    return when (language) {
        AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
        AppLanguage.ZH_CN -> stringResource(R.string.settings_language_zh_cn)
        AppLanguage.EN -> stringResource(R.string.settings_language_en)
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

private fun buildServerAddressText(settings: ConnectionSettings): String {
    val host = settings.host.trim().ifBlank { "-" }
    if (host.startsWith("http://", ignoreCase = true) || host.startsWith("https://", ignoreCase = true)) {
        return host
    }
    val scheme = if (settings.useHttps) "https" else "http"
    return "$scheme://$host:${settings.port}"
}







