package com.hjw.qbremote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hjw.qbremote.data.AppLanguage
import com.hjw.qbremote.data.AppTheme
import com.hjw.qbremote.data.ChartSortMode
import com.hjw.qbremote.data.ConnectionSettings
import com.hjw.qbremote.data.ConnectionStore
import com.hjw.qbremote.data.DailyUploadTrackingSnapshot
import com.hjw.qbremote.data.QbRepository
import com.hjw.qbremote.data.ServerProfile
import com.hjw.qbremote.data.model.AddTorrentFile
import com.hjw.qbremote.data.model.AddTorrentRequest
import com.hjw.qbremote.data.model.TorrentFileInfo
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.data.model.TorrentProperties
import com.hjw.qbremote.data.model.TorrentTracker
import com.hjw.qbremote.data.model.TransferInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

enum class RefreshScene {
    DASHBOARD,
    TORRENT_DETAIL,
    SETTINGS,
}

data class DailyTagUploadStat(
    val tag: String,
    val uploadedBytes: Long,
    val torrentCount: Int,
    val isNoTag: Boolean = false,
)

data class MainUiState(
    val settings: ConnectionSettings = ConnectionSettings(),
    val serverProfiles: List<ServerProfile> = emptyList(),
    val activeServerProfileId: String? = null,
    val isConnecting: Boolean = false,
    val isManualRefreshing: Boolean = false,
    val connected: Boolean = false,
    val serverVersion: String = "-",
    val transferInfo: TransferInfo = TransferInfo(),
    val torrents: List<TorrentInfo> = emptyList(),
    val detailHash: String = "",
    val detailLoading: Boolean = false,
    val detailProperties: TorrentProperties? = null,
    val detailFiles: List<TorrentFileInfo> = emptyList(),
    val detailTrackers: List<TorrentTracker> = emptyList(),
    val categoryOptions: List<String> = emptyList(),
    val tagOptions: List<String> = emptyList(),
    val dailyTagUploadDate: String = "",
    val dailyTagUploadStats: List<DailyTagUploadStat> = emptyList(),
    val refreshScene: RefreshScene = RefreshScene.DASHBOARD,
    val pendingHashes: Set<String> = emptySet(),
    val errorMessage: String? = null,
)

class MainViewModel(
    private val connectionStore: ConnectionStore,
    private val repository: QbRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null
    private var hourlyBoundaryRefreshJob: Job? = null
    private var autoConnectAttempted = false
    private var isRefreshInProgress = false
    private var dailyUploadTrackingScopeKey: String? = null
    private var dailyUploadBaselineDate: LocalDate? = null
    private val dailyUploadBaselineByTorrent = mutableMapOf<String, Long>()
    private val dailyUploadLastSeenByTorrent = mutableMapOf<String, Long>()

    init {
        viewModelScope.launch {
            connectionStore.migrateLegacyPasswordIfNeeded()
            launch {
                connectionStore.settingsFlow.collect { settings ->
                    _uiState.update { current -> current.copy(settings = settings) }
                    autoConnectIfNeeded(settings)
                }
            }
            launch {
                connectionStore.serverProfilesFlow.collect { profilesState ->
                    _uiState.update { current ->
                        current.copy(
                            serverProfiles = profilesState.profiles,
                            activeServerProfileId = profilesState.activeProfileId,
                        )
                    }
                }
            }
        }
    }

    fun updateHost(value: String) = updateSettings { current ->
        val parsed = parseHostInputHints(value)
        current.copy(
            host = value,
            port = parsed?.port ?: current.port,
            useHttps = parsed?.useHttps ?: current.useHttps,
        )
    }
    fun updatePort(value: String) = updateSettings { it.copy(port = value.toIntOrNull() ?: 0) }
    fun updateUseHttps(value: Boolean) = updateSettings { it.copy(useHttps = value) }
    fun updateUsername(value: String) = updateSettings { it.copy(username = value) }
    fun updatePassword(value: String) = updateSettings { it.copy(password = value) }
    fun updateRefreshSeconds(value: String) {
        val sec = value.toIntOrNull()?.coerceIn(5, 120) ?: 5
        updateSettings { it.copy(refreshSeconds = sec) }
    }

    fun updateShowSpeedTotals(value: Boolean) = updateAndPersistSettings {
        it.copy(showSpeedTotals = value)
    }

    fun updateAppLanguage(value: AppLanguage) = updateAndPersistSettings {
        it.copy(appLanguage = value)
    }

    fun updateAppTheme(value: AppTheme) = updateAndPersistSettings {
        it.copy(appTheme = value)
    }

    fun updateEnableServerGrouping(value: Boolean) = updateAndPersistSettings {
        it.copy(enableServerGrouping = value)
    }

    fun updateShowChartPanel(value: Boolean) = updateAndPersistSettings {
        it.copy(showChartPanel = value)
    }

    fun updateChartShowSiteName(value: Boolean) = updateAndPersistSettings {
        it.copy(chartShowSiteName = value)
    }

    fun updateChartSortMode(value: ChartSortMode) = updateAndPersistSettings {
        it.copy(chartSortMode = value)
    }

    fun updateDeleteFilesDefault(value: Boolean) = updateAndPersistSettings {
        it.copy(deleteFilesDefault = value)
    }

    fun updateDeleteFilesWhenNoSeeders(value: Boolean) = updateAndPersistSettings {
        it.copy(deleteFilesWhenNoSeeders = value)
    }

    fun dismissHomeTorrentEntryHint() = updateAndPersistSettings {
        if (it.homeTorrentEntryHintDismissed) {
            it
        } else {
            it.copy(homeTorrentEntryHintDismissed = true)
        }
    }

    fun updateRefreshScene(scene: RefreshScene) {
        _uiState.update { current ->
            if (current.refreshScene == scene) current else current.copy(refreshScene = scene)
        }
    }

    fun connect() {
        connectInternal(persistSettings = true, showErrorOnFailure = true)
    }

    fun addServerProfile(
        name: String,
        host: String,
        port: String,
        useHttps: Boolean,
        username: String,
        password: String,
        refreshSeconds: String,
    ) {
        viewModelScope.launch {
            runCatching {
                val current = _uiState.value.settings
                val normalizedHost = host.trim()
                val parsed = parseHostInputHints(normalizedHost)
                val resolvedPort = parsed?.port ?: (port.toIntOrNull() ?: 8080)
                val resolvedUseHttps = parsed?.useHttps ?: useHttps
                val nextSettings = current.copy(
                    host = normalizedHost,
                    port = resolvedPort.coerceIn(1, 65535),
                    useHttps = resolvedUseHttps,
                    username = username.trim(),
                    password = password,
                    refreshSeconds = (refreshSeconds.toIntOrNull() ?: 5).coerceIn(5, 120),
                )
                require(nextSettings.host.isNotBlank()) { "主机不能为空" }
                require(nextSettings.username.isNotBlank()) { "用户名不能为空" }

                val profile = connectionStore.addServerProfile(name = name, settings = nextSettings)
                val switched = connectionStore.switchToServerProfile(profile.id)
                _uiState.update {
                    it.copy(
                        settings = switched,
                        connected = false,
                        serverVersion = "-",
                        transferInfo = TransferInfo(),
                        torrents = emptyList(),
                        dailyTagUploadDate = "",
                        dailyTagUploadStats = emptyList(),
                    )
                }
            }.onSuccess {
                resetDailyUploadTrackingState()
                connectInternal(persistSettings = false, showErrorOnFailure = true)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "添加服务器失败")
                }
            }
        }
    }

    fun switchServerProfile(profileId: String) {
        if (profileId.isBlank()) return
        viewModelScope.launch {
            runCatching {
                val switched = connectionStore.switchToServerProfile(profileId)
                _uiState.update {
                    it.copy(
                        settings = switched,
                        connected = false,
                        serverVersion = "-",
                        transferInfo = TransferInfo(),
                        torrents = emptyList(),
                        dailyTagUploadDate = "",
                        dailyTagUploadStats = emptyList(),
                        detailHash = "",
                        detailProperties = null,
                        detailFiles = emptyList(),
                        detailTrackers = emptyList(),
                    )
                }
            }.onSuccess {
                resetDailyUploadTrackingState()
                connectInternal(persistSettings = false, showErrorOnFailure = true)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "切换服务器失败")
                }
            }
        }
    }

    private fun autoConnectIfNeeded(settings: ConnectionSettings) {
        if (autoConnectAttempted) return
        if (settings.host.isBlank() || settings.username.isBlank()) return
        autoConnectAttempted = true
        connectInternal(persistSettings = false, showErrorOnFailure = false)
    }

    private fun connectInternal(
        persistSettings: Boolean,
        showErrorOnFailure: Boolean,
    ) {
        if (_uiState.value.isConnecting) return
        viewModelScope.launch {
            resetDailyUploadTrackingState()
            _uiState.update { it.copy(isConnecting = true, errorMessage = null) }
            val settings = _uiState.value.settings
            if (persistSettings) {
                connectionStore.save(settings)
            }

            repository.connect(settings)
                .onSuccess {
                    _uiState.update { it.copy(isConnecting = false, connected = true) }
                    refreshServerVersion()
                    refresh()
                    loadGlobalSelectionOptions()
                    startAutoRefresh()
                    startHourlyBoundaryRefresh()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            connected = false,
                            errorMessage = if (showErrorOnFailure) {
                                error.message ?: "Connection failed."
                            } else {
                                null
                            }
                        )
                    }
                    autoRefreshJob?.cancel()
                    hourlyBoundaryRefreshJob?.cancel()
                }
        }
    }

    fun refresh(manual: Boolean = false) {
        if (isRefreshInProgress) return
        isRefreshInProgress = true
        viewModelScope.launch {
            val refreshScene = _uiState.value.refreshScene
            val detailHash = _uiState.value.detailHash
            val shouldRefreshDetail = refreshScene == RefreshScene.TORRENT_DETAIL && detailHash.isNotBlank()

            try {
                if (manual) {
                    _uiState.update {
                        it.copy(
                            isManualRefreshing = true,
                            errorMessage = null,
                        )
                    }
                }

                val dashboardResult = repository.fetchDashboard()
                val dashboardData = dashboardResult.getOrNull()
                if (dashboardData != null) {
                    val (date, dailyStats) = buildDailyTagUploadStats(dashboardData.torrents)
                    _uiState.update {
                        it.copy(
                            transferInfo = dashboardData.transferInfo,
                            torrents = dashboardData.torrents,
                            dailyTagUploadDate = date,
                            dailyTagUploadStats = dailyStats,
                        )
                    }
                    if (shouldRefreshDetail) {
                        refreshDetailSnapshot(detailHash)
                    }
                } else {
                    val error = dashboardResult.exceptionOrNull()
                    _uiState.update {
                        val message = error?.message
                        if (shouldSuppressRefreshError(message)) {
                            it
                        } else {
                            it.copy(
                                errorMessage = message ?: "Refresh failed."
                            )
                        }
                    }
                }
            } finally {
                isRefreshInProgress = false
                if (manual) {
                    _uiState.update {
                        if (it.isManualRefreshing) {
                            it.copy(isManualRefreshing = false)
                        } else {
                            it
                        }
                    }
                }
            }
        }
    }

    fun pauseTorrent(hash: String) = runTorrentAction(hash) {
        repository.pauseTorrent(hash).getOrThrow()
    }

    fun resumeTorrent(hash: String) = runTorrentAction(hash) {
        repository.resumeTorrent(hash).getOrThrow()
    }

    fun deleteTorrent(hash: String, deleteFiles: Boolean) = runTorrentAction(hash) {
        repository.deleteTorrent(hash, deleteFiles).getOrThrow()
    }

    fun loadTorrentDetail(hash: String) {
        if (hash.isBlank()) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    detailHash = hash,
                    detailLoading = true,
                    errorMessage = null,
                )
            }
            repository.fetchTorrentDetail(hash)
                .onSuccess { detail ->
                    val trackers = repository.fetchTorrentTrackers(hash).getOrElse { emptyList() }
                    val categoryOptions = repository.fetchCategoryOptions().getOrElse { emptyList() }
                    val tagOptions = repository.fetchTagOptions().getOrElse { emptyList() }
                    _uiState.update {
                        it.copy(
                            detailLoading = false,
                            detailProperties = detail.properties,
                            detailFiles = detail.files,
                            detailTrackers = trackers,
                            categoryOptions = categoryOptions,
                            tagOptions = tagOptions,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            detailLoading = false,
                            detailProperties = null,
                            detailFiles = emptyList(),
                            detailTrackers = emptyList(),
                            errorMessage = error.message ?: "加载种子详情失败",
                        )
                    }
                }
        }
    }
    fun renameTorrent(hash: String, newName: String) = runDetailAction(hash) {
        repository.renameTorrent(hash, newName).getOrThrow()
    }

    fun setTorrentLocation(hash: String, location: String) = runDetailAction(hash) {
        repository.setTorrentLocation(hash, location).getOrThrow()
    }

    fun setTorrentCategory(hash: String, category: String) = runDetailAction(hash) {
        repository.setTorrentCategory(hash, category).getOrThrow()
    }

    fun setTorrentTags(hash: String, oldTags: String, newTags: String) = runDetailAction(hash) {
        repository.setTorrentTags(hash, oldTags, newTags).getOrThrow()
    }

    fun setTorrentSpeedLimit(hash: String, downloadLimitKb: String, uploadLimitKb: String) = runDetailAction(hash) {
        val dl = parseLimitKbToBytes(downloadLimitKb)
        val up = parseLimitKbToBytes(uploadLimitKb)
        repository.setTorrentSpeedLimit(hash, dl, up).getOrThrow()
    }

    fun setTorrentShareRatio(hash: String, ratio: String) = runDetailAction(hash) {
        val value = ratio.trim().toDoubleOrNull() ?: throw IllegalArgumentException("分享比率格式无效")
        repository.setTorrentShareRatio(hash, value).getOrThrow()
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun loadGlobalSelectionOptions() {
        if (!_uiState.value.connected) return
        viewModelScope.launch {
            val categoryOptions = repository.fetchCategoryOptions().getOrElse { emptyList() }
            val tagOptions = repository.fetchTagOptions().getOrElse { emptyList() }
            _uiState.update {
                it.copy(
                    categoryOptions = categoryOptions,
                    tagOptions = tagOptions,
                )
            }
        }
    }

    fun addTorrent(
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
    ) {
        if (!_uiState.value.connected) {
            _uiState.update { it.copy(errorMessage = "请先连接 qBittorrent 服务器。") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            runCatching {
                val request = AddTorrentRequest(
                    urls = urls.trim(),
                    files = files,
                    autoTmm = autoTmm,
                    category = category.trim(),
                    tags = tags.trim(),
                    savePath = savePath.trim(),
                    paused = paused,
                    skipChecking = skipChecking,
                    sequentialDownload = sequentialDownload,
                    firstLastPiecePrio = firstLastPiecePrio,
                    uploadLimitBytes = parseLimitKbToBytes(uploadLimitKb),
                    downloadLimitBytes = parseLimitKbToBytes(downloadLimitKb),
                )
                repository.addTorrent(request).getOrThrow()
            }.onSuccess {
                loadGlobalSelectionOptions()
                refresh()
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "添加种子失败。") }
            }
        }
    }

    private fun runTorrentAction(hash: String, action: suspend () -> Unit) {
        if (hash.isBlank()) return
        if (_uiState.value.pendingHashes.contains(hash)) return

        viewModelScope.launch {
            _uiState.update { it.copy(pendingHashes = it.pendingHashes + hash, errorMessage = null) }
            runCatching { action() }
                .onSuccess { refresh() }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Action failed.")
                    }
                }
            _uiState.update { it.copy(pendingHashes = it.pendingHashes - hash) }
        }
    }

    private fun runDetailAction(hash: String, action: suspend () -> Unit) {
        runTorrentAction(hash) {
            action()
            val detail = repository.fetchTorrentDetail(hash).getOrThrow()
            val trackers = repository.fetchTorrentTrackers(hash).getOrElse { emptyList() }
            val categoryOptions = repository.fetchCategoryOptions().getOrElse { emptyList() }
            val tagOptions = repository.fetchTagOptions().getOrElse { emptyList() }
            _uiState.update {
                it.copy(
                    detailHash = hash,
                    detailProperties = detail.properties,
                    detailFiles = detail.files,
                    detailTrackers = trackers,
                    categoryOptions = categoryOptions,
                    tagOptions = tagOptions,
                )
            }
        }
    }

    private suspend fun refreshDetailSnapshot(hash: String) {
        val detail = repository.fetchTorrentDetail(hash).getOrNull() ?: return
        val trackers = repository.fetchTorrentTrackers(hash).getOrElse { emptyList() }
        _uiState.update {
            if (it.detailHash != hash) {
                it
            } else {
                it.copy(
                    detailProperties = detail.properties,
                    detailFiles = detail.files,
                    detailTrackers = trackers,
                )
            }
        }
    }

    private fun refreshServerVersion() {
        viewModelScope.launch {
            repository.fetchServerVersion()
                .onSuccess { version ->
                    _uiState.update { it.copy(serverVersion = version.ifBlank { "-" }) }
                }
        }
    }

    private fun parseLimitKbToBytes(value: String): Long {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return -1L
        val kb = trimmed.toLongOrNull() ?: throw IllegalArgumentException("限速值必须是数字")
        if (kb < 0L) return -1L
        return kb * 1024L
    }

    private fun shouldSuppressRefreshError(message: String?): Boolean {
        val normalized = message?.trim()?.lowercase().orEmpty()
        if (normalized.isBlank()) return false
        return normalized.contains("unable to resolve host") ||
            normalized.contains("no address associated with hostname")
    }

    private fun updateSettings(update: (ConnectionSettings) -> ConnectionSettings) {
        _uiState.update { current ->
            val nextSettings = update(current.settings)
            if (nextSettings == current.settings) {
                current
            } else {
                current.copy(settings = nextSettings)
            }
        }
    }

    private fun updateAndPersistSettings(update: (ConnectionSettings) -> ConnectionSettings) {
        var changed = false
        _uiState.update { current ->
            val nextSettings = update(current.settings)
            if (nextSettings == current.settings) {
                current
            } else {
                changed = true
                current.copy(settings = nextSettings)
            }
        }
        if (!changed) return
        val settingsToPersist = _uiState.value.settings
        viewModelScope.launch {
            connectionStore.save(settingsToPersist)
        }
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                val intervalMs = resolveAutoRefreshIntervalMs(_uiState.value)
                delay(intervalMs)
                if (_uiState.value.connected) refresh()
            }
        }
    }

    private fun startHourlyBoundaryRefresh() {
        hourlyBoundaryRefreshJob?.cancel()
        hourlyBoundaryRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(millisUntilNextHourBoundary())
                if (_uiState.value.connected) {
                    refresh()
                }
            }
        }
    }

    private fun resolveAutoRefreshIntervalMs(state: MainUiState): Long {
        val base = state.settings.refreshSeconds.coerceIn(5, 120)
        val adaptiveSeconds = when (state.refreshScene) {
            RefreshScene.TORRENT_DETAIL -> base
            RefreshScene.SETTINGS -> (base * 2).coerceIn(10, 120)
            RefreshScene.DASHBOARD -> base
        }
        return adaptiveSeconds * 1000L
    }

    private fun millisUntilNextHourBoundary(): Long {
        val now = ZonedDateTime.now()
        val nextHour = now.plusHours(1).withMinute(0).withSecond(0).withNano(0)
        return Duration.between(now, nextHour)
            .toMillis()
            .coerceAtLeast(1_000L)
    }

    private fun resetDailyUploadTrackingState() {
        dailyUploadTrackingScopeKey = null
        dailyUploadBaselineDate = null
        dailyUploadBaselineByTorrent.clear()
        dailyUploadLastSeenByTorrent.clear()
        _uiState.update {
            it.copy(
                dailyTagUploadDate = "",
                dailyTagUploadStats = emptyList(),
            )
        }
    }

    private suspend fun buildDailyTagUploadStats(torrents: List<TorrentInfo>): Pair<String, List<DailyTagUploadStat>> {
        val scopeKey = currentDailyUploadTrackingScopeKey()
        ensureDailyUploadTrackingLoaded(scopeKey)
        val today = LocalDate.now()
        if (dailyUploadBaselineDate != today) {
            val carryOver = dailyUploadLastSeenByTorrent.toMap()
            dailyUploadBaselineDate = today
            dailyUploadBaselineByTorrent.clear()
            if (carryOver.isNotEmpty()) {
                dailyUploadBaselineByTorrent.putAll(carryOver)
            }
        }

        val activeKeys = torrents.map(::torrentTrackingKey).toSet()
        dailyUploadBaselineByTorrent.keys.retainAll(activeKeys)
        dailyUploadLastSeenByTorrent.keys.retainAll(activeKeys)

        val uploadByTag = mutableMapOf<String, Long>()
        val torrentCountByTag = mutableMapOf<String, Int>()

        torrents.forEach { torrent ->
            val trackingKey = torrentTrackingKey(torrent)
            val currentUploaded = torrent.uploaded.coerceAtLeast(0L)
            val baseline = dailyUploadBaselineByTorrent[trackingKey]
                ?: dailyUploadLastSeenByTorrent[trackingKey]

            if (baseline == null) {
                dailyUploadBaselineByTorrent[trackingKey] = currentUploaded
                dailyUploadLastSeenByTorrent[trackingKey] = currentUploaded
                return@forEach
            }
            if (currentUploaded < baseline) {
                dailyUploadBaselineByTorrent[trackingKey] = currentUploaded
                dailyUploadLastSeenByTorrent[trackingKey] = currentUploaded
                return@forEach
            }

            val delta = currentUploaded - baseline
            dailyUploadLastSeenByTorrent[trackingKey] = currentUploaded
            if (delta <= 0L) return@forEach

            val tags = parseTorrentTags(torrent.tags).ifEmpty { listOf(NO_TAG_KEY) }
            val baseShare = delta / tags.size
            var remainder = delta % tags.size

            for (tag in tags) {
                val share = baseShare + if (remainder > 0L) {
                    remainder -= 1L
                    1L
                } else {
                    0L
                }
                if (share <= 0L) continue
                uploadByTag[tag] = (uploadByTag[tag] ?: 0L) + share
                torrentCountByTag[tag] = (torrentCountByTag[tag] ?: 0) + 1
            }
        }

        val stats = uploadByTag.entries
            .filter { it.value > 0L }
            .sortedByDescending { it.value }
            .map { (tag, uploaded) ->
                DailyTagUploadStat(
                    tag = tag,
                    uploadedBytes = uploaded,
                    torrentCount = torrentCountByTag[tag] ?: 0,
                    isNoTag = tag == NO_TAG_KEY,
                )
            }

        connectionStore.saveDailyUploadTrackingSnapshot(
            scopeKey = scopeKey,
            snapshot = DailyUploadTrackingSnapshot(
                date = today.toString(),
                baselineByTorrent = dailyUploadBaselineByTorrent.toMap(),
                lastSeenByTorrent = dailyUploadLastSeenByTorrent.toMap(),
            ),
        )

        return today.toString() to stats
    }

    private suspend fun ensureDailyUploadTrackingLoaded(scopeKey: String) {
        if (dailyUploadTrackingScopeKey == scopeKey) return

        dailyUploadTrackingScopeKey = scopeKey
        dailyUploadBaselineDate = null
        dailyUploadBaselineByTorrent.clear()
        dailyUploadLastSeenByTorrent.clear()

        val snapshot = connectionStore.loadDailyUploadTrackingSnapshot(scopeKey) ?: return
        dailyUploadBaselineDate = runCatching {
            snapshot.date
                .takeIf { it.isNotBlank() }
                ?.let(LocalDate::parse)
        }.getOrNull()
        dailyUploadBaselineByTorrent.putAll(snapshot.baselineByTorrent)
        dailyUploadLastSeenByTorrent.putAll(snapshot.lastSeenByTorrent)
    }

    private fun currentDailyUploadTrackingScopeKey(): String {
        val activeProfileId = _uiState.value.activeServerProfileId.orEmpty().trim()
        if (activeProfileId.isNotBlank()) {
            return "profile:$activeProfileId"
        }

        val settings = _uiState.value.settings
        val host = settings.host.trim().lowercase()
        return if (host.isNotBlank()) {
            "server:${settings.useHttps}|$host|${settings.port}"
        } else {
            "default"
        }
    }

    private fun parseTorrentTags(rawTags: String): List<String> {
        val normalizedByKey = linkedMapOf<String, String>()
        rawTags
            .split(',', ';', '|')
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "-" && !it.equals("null", ignoreCase = true) }
            .forEach { tag ->
                val key = tag.lowercase()
                if (!normalizedByKey.containsKey(key)) {
                    normalizedByKey[key] = tag
                }
            }
        return normalizedByKey.values.toList()
    }

    private fun torrentTrackingKey(torrent: TorrentInfo): String {
        return torrent.hash.ifBlank {
            "${torrent.name}|${torrent.addedOn}|${torrent.savePath}|${torrent.size}"
        }
    }

    override fun onCleared() {
        autoRefreshJob?.cancel()
        hourlyBoundaryRefreshJob?.cancel()
        super.onCleared()
    }

    companion object {
        private const val NO_TAG_KEY = "__NO_TAG__"

        fun factory(
            connectionStore: ConnectionStore,
            repository: QbRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(connectionStore, repository) as T
            }
        }
    }
}




