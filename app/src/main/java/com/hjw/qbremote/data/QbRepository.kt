package com.hjw.qbremote.data

import com.hjw.qbremote.data.model.DashboardData
import com.hjw.qbremote.data.model.AddTorrentRequest
import com.hjw.qbremote.data.model.CountryPeerSnapshot
import com.hjw.qbremote.data.model.TorrentDetailData
import com.hjw.qbremote.data.model.TorrentInfo
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.HttpUrl.Companion.toHttpUrl
import kotlinx.coroutines.delay
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

class QbRepository {
    private var api: QbApi? = null
    private var activeSettings: ConnectionSettings? = null

    private val torrentCache = linkedMapOf<String, TorrentInfo>()
    private val credentialTrimChars = charArrayOf(' ', '\t', '\r', '\n', '\u0000', '\uFEFF')

    suspend fun connect(settings: ConnectionSettings): Result<Unit> = runCatching {
        require(settings.host.isNotBlank()) { "Host cannot be empty." }

        var lastError: Throwable? = null
        val candidates = settings.baseUrlCandidates()
        val failureDetails = mutableListOf<String>()
        for (baseUrl in candidates) {
            val apiCandidates = listOf(
                "webui_headers" to buildApi(baseUrl, HeaderMode.WEBUI_HEADERS),
                "no_webui_headers" to buildApi(baseUrl, HeaderMode.NO_WEBUI_HEADERS),
            )
            for ((apiLabel, newApi) in apiCandidates) {
                try {
                    val effectiveCredentials = loginWithRetry(newApi, settings)

                    api = newApi
                    activeSettings = settings.copy(
                        username = effectiveCredentials.username,
                        password = effectiveCredentials.password,
                    )
                    torrentCache.clear()
                    return@runCatching
                } catch (error: Throwable) {
                    lastError = error
                    val detail = error.message?.takeIf { it.isNotBlank() }
                        ?: error::class.simpleName
                        ?: "Unknown error"
                    failureDetails += "$baseUrl [$apiLabel] => $detail"
                    if (error is AuthLockedException) {
                        throw error
                    }
                }
            }
        }

        val candidateText = candidates.joinToString()
        val rootMessage = if (failureDetails.isNotEmpty()) {
            failureDetails.joinToString(" | ")
        } else {
            lastError?.message?.takeIf { it.isNotBlank() } ?: "Unknown error."
        }
        throw IllegalStateException(
            "Unable to connect to qBittorrent. Tried: $candidateText. $rootMessage",
            lastError,
        )
    }

    suspend fun fetchDashboard(): Result<DashboardData> = runCatching {
        // Always use full refresh to avoid incremental-field overwrite issues.
        fallbackFullRefresh()
    }

    suspend fun pauseTorrent(hash: String): Result<Unit> = runCatching {
        require(hash.isNotBlank()) { "Invalid torrent hash." }
        executeWithRetry { liveApi ->
            liveApi.pauseTorrents(hash).ensureSuccess("Pause failed.")
        }
    }

    suspend fun resumeTorrent(hash: String): Result<Unit> = runCatching {
        require(hash.isNotBlank()) { "Invalid torrent hash." }
        executeWithRetry { liveApi ->
            liveApi.resumeTorrents(hash).ensureSuccess("Resume failed.")
        }
    }

    suspend fun deleteTorrent(hash: String, deleteFiles: Boolean): Result<Unit> = runCatching {
        require(hash.isNotBlank()) { "Invalid torrent hash." }
        executeWithRetry { liveApi ->
            liveApi.deleteTorrents(hash, deleteFiles).ensureSuccess("Delete failed.")
        }
    }

    suspend fun fetchServerVersion(): Result<String> = runCatching {
        executeWithRetry { liveApi -> liveApi.appVersion().trim() }.ifBlank { "-" }
    }

    suspend fun fetchTorrentDetail(hash: String): Result<TorrentDetailData> = runCatching {
        require(hash.isNotBlank()) { "Invalid torrent hash." }
        val properties = executeWithRetry { liveApi -> liveApi.torrentProperties(hash) }
        val files = executeWithRetry { liveApi -> liveApi.torrentFiles(hash) }
        TorrentDetailData(properties = properties, files = files)
    }

    suspend fun fetchTorrentTrackers(hash: String): Result<List<com.hjw.qbremote.data.model.TorrentTracker>> =
        runCatching {
            require(hash.isNotBlank()) { "Invalid torrent hash." }
            executeWithRetry { liveApi -> liveApi.torrentTrackers(hash) }
        }

    suspend fun fetchCategoryOptions(): Result<List<String>> = runCatching {
        val categories = executeWithRetry { liveApi -> liveApi.torrentCategories() }
        categories.keys
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .sorted()
    }

    suspend fun fetchTagOptions(): Result<List<String>> = runCatching {
        val raw = executeWithRetry { liveApi -> liveApi.torrentTagsRaw() }
        parseTagOptions(raw)
    }

    suspend fun fetchCountryPeerSnapshots(hashes: List<String>): Result<List<CountryPeerSnapshot>> = runCatching {
        val snapshots = mutableListOf<CountryPeerSnapshot>()
        hashes
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { hash ->
                val raw = executeWithRetry { liveApi -> liveApi.torrentPeers(hash = hash) }
                snapshots += parseCountryPeerSnapshots(hash = hash, raw = raw)
            }
        snapshots
    }

    suspend fun renameTorrent(hash: String, name: String): Result<Unit> = runCatching {
        require(hash.isNotBlank()) { "Invalid torrent hash." }
        require(name.isNotBlank()) { "Name cannot be empty." }
        executeWithRetry { liveApi ->
            liveApi.renameTorrent(hash, name).ensureSuccess("Rename failed.")
        }
    }

    suspend fun setTorrentLocation(hash: String, location: String): Result<Unit> = runCatching {
        require(hash.isNotBlank()) { "Invalid torrent hash." }
        require(location.isNotBlank()) { "Location cannot be empty." }
        executeWithRetry { liveApi ->
            liveApi.setLocation(hash, location).ensureSuccess("Set location failed.")
        }
    }

    suspend fun setTorrentCategory(hash: String, category: String): Result<Unit> = runCatching {
        require(hash.isNotBlank()) { "Invalid torrent hash." }
        executeWithRetry { liveApi ->
            liveApi.setCategory(hash, category).ensureSuccess("Set category failed.")
        }
    }

    suspend fun setTorrentTags(hash: String, oldTags: String, newTags: String): Result<Unit> = runCatching {
        require(hash.isNotBlank()) { "Invalid torrent hash." }
        val oldNormalized = oldTags.trim()
        val newNormalized = newTags.trim()
        executeWithRetry { liveApi ->
            if (oldNormalized.isNotBlank()) {
                liveApi.removeTags(hash, oldNormalized).ensureSuccess("Remove old tags failed.")
            }
            if (newNormalized.isNotBlank()) {
                liveApi.addTags(hash, newNormalized).ensureSuccess("Add tags failed.")
            }
        }
    }

    suspend fun setTorrentSpeedLimit(hash: String, downloadLimitBytes: Long, uploadLimitBytes: Long): Result<Unit> =
        runCatching {
            require(hash.isNotBlank()) { "Invalid torrent hash." }
            executeWithRetry { liveApi ->
                liveApi.setDownloadLimit(hash, downloadLimitBytes.coerceAtLeast(-1L))
                    .ensureSuccess("Set download limit failed.")
                liveApi.setUploadLimit(hash, uploadLimitBytes.coerceAtLeast(-1L))
                    .ensureSuccess("Set upload limit failed.")
            }
        }

    suspend fun setTorrentShareRatio(hash: String, ratioLimit: Double): Result<Unit> = runCatching {
        require(hash.isNotBlank()) { "Invalid torrent hash." }
        executeWithRetry { liveApi ->
            liveApi.setShareLimits(
                hashes = hash,
                ratioLimit = ratioLimit.coerceAtLeast(-1.0),
                seedingTimeLimit = -1,
                inactiveSeedingTimeLimit = -1,
            ).ensureSuccess("Set share ratio failed.")
        }
    }

    suspend fun addTorrent(request: AddTorrentRequest): Result<Unit> = runCatching {
        val urlsText = request.urls.trim()
        require(urlsText.isNotBlank() || request.files.isNotEmpty()) {
            "请填写种子链接或选择种子文件。"
        }

        val textMediaType = "text/plain".toMediaType()
        val fields = linkedMapOf<String, RequestBody>()
        fun putField(key: String, value: String) {
            fields[key] = value.toRequestBody(textMediaType)
        }
        fun putBoolField(key: String, value: Boolean) {
            putField(key, if (value) "true" else "false")
        }

        if (urlsText.isNotBlank()) putField("urls", urlsText)
        if (request.category.trim().isNotBlank()) putField("category", request.category.trim())
        if (request.tags.trim().isNotBlank()) putField("tags", request.tags.trim())
        if (request.savePath.trim().isNotBlank()) putField("savepath", request.savePath.trim())

        putBoolField("autoTMM", request.autoTmm)
        putBoolField("paused", request.paused)
        putBoolField("skip_checking", request.skipChecking)
        putBoolField("sequentialDownload", request.sequentialDownload)
        putBoolField("firstLastPiecePrio", request.firstLastPiecePrio)

        if (request.uploadLimitBytes >= 0L) putField("upLimit", request.uploadLimitBytes.toString())
        if (request.downloadLimitBytes >= 0L) putField("dlLimit", request.downloadLimitBytes.toString())

        val torrentMediaType = "application/x-bittorrent".toMediaType()
        val fileParts = request.files.map { file ->
            MultipartBody.Part.createFormData(
                name = "torrents",
                filename = file.name.ifBlank { "upload.torrent" },
                body = file.bytes.toRequestBody(torrentMediaType),
            )
        }

        val response = executeWithRetry { liveApi ->
            liveApi.addTorrents(fields, fileParts)
        }
        response.ensureSuccess("Add torrent failed.")
        val resultText = response.body().orEmpty().trim()
        if (resultText.contains("fail", ignoreCase = true)) {
            throw IllegalStateException("添加种子失败：$resultText")
        }
    }

    private suspend fun fallbackFullRefresh(): DashboardData {
        val transfer = executeWithRetry { liveApi -> liveApi.transferInfo() }
        val rawTorrents = executeWithRetry { liveApi ->
            liveApi.torrentsInfo().sortedByDescending { it.addedOn }
        }
        val previousByHash = torrentCache.toMap()
        val baseTorrents = rawTorrents
            .map { torrent ->
                val previous = previousByHash[torrent.hash]
                sanitizeTorrentInfo(current = torrent, previous = previous)
            }
        val suspiciousHashes = baseTorrents
            .mapNotNull { torrent ->
                val previous = previousByHash[torrent.hash]
                torrent.hash.takeIf { it.isNotBlank() && shouldRefetchTorrentSnapshot(torrent, previous) }
            }
            .distinct()
        val refreshedByHash = if (suspiciousHashes.isNotEmpty()) {
            fetchTorrentSnapshotsByHashes(suspiciousHashes)
        } else {
            emptyMap()
        }
        val torrents = baseTorrents
            .map { torrent ->
                val refreshed = refreshedByHash[torrent.hash] ?: return@map torrent
                sanitizeTorrentInfo(current = refreshed, previous = torrent)
            }
            .sortedByDescending { it.addedOn }

        torrentCache.clear()
        torrents.forEach { torrent ->
            if (torrent.hash.isNotBlank()) {
                torrentCache[torrent.hash] = torrent
            }
        }

        return DashboardData(
            transferInfo = normalizeTransferInfo(transfer, torrents),
            torrents = torrents,
        )
    }

    private suspend fun fetchTorrentSnapshotsByHashes(
        hashes: List<String>,
    ): Map<String, TorrentInfo> {
        if (hashes.isEmpty()) return emptyMap()
        val snapshots = linkedMapOf<String, TorrentInfo>()
        hashes
            .distinct()
            .chunked(40)
            .forEach { chunk ->
                val hashQuery = chunk.joinToString("|")
                val list = executeWithRetry { liveApi ->
                    liveApi.torrentsInfo(hashes = hashQuery)
                }
                list.forEach { torrent ->
                    if (torrent.hash.isNotBlank()) {
                        snapshots[torrent.hash] = torrent
                    }
                }
            }
        return snapshots
    }

    private suspend fun <T> executeWithRetry(
        maxAttempts: Int = 3,
        initialDelayMs: Long = 400L,
        action: suspend (QbApi) -> T,
    ): T {
        var attempt = 0
        var nextDelayMs = initialDelayMs

        while (attempt < maxAttempts) {
            attempt++
            try {
                return action(requireApi())
            } catch (error: Throwable) {
                val shouldRetry = when (error) {
                    is HttpException -> {
                        if (error.code() == 401 || error.code() == 403) {
                            relogin()
                            true
                        } else {
                            error.code() in 500..599
                        }
                    }
                    is IOException -> true
                    else -> false
                }

                if (!shouldRetry || attempt >= maxAttempts) {
                    throw error
                }

                delay(nextDelayMs)
                nextDelayMs = (nextDelayMs * 2).coerceAtMost(4000L)
            }
        }

        throw IllegalStateException("Request failed after retries.")
    }

    private suspend fun loginWithRetry(
        liveApi: QbApi,
        settings: ConnectionSettings,
    ): EffectiveCredentials {
        val credentialCandidates = buildCredentialCandidates(settings)
        var lastError: Throwable? = null

        for (credentials in credentialCandidates) {
            var attempt = 0
            var nextDelayMs = 400L

            while (attempt < 3) {
                attempt++
                try {
                    val loginResult = loginWithCompatibleEndpoints(liveApi, credentials)
                    if (loginResult.ipBanned) {
                        throw AuthLockedException(
                            "服务器返回：认证失败次数过多，当前出口 IP 已被封禁。请在 qB WebUI 解除封禁或等待封禁时间结束后再试。详情：${loginResult.detail}"
                        )
                    }
                    if (loginResult.retryableServerError) {
                        throw IOException(loginResult.detail)
                    }
                    if (loginResult.success) {
                        return credentials
                    }

                    val hint = if (credentials.wasNormalized) {
                        " (retried with trimmed credentials)"
                    } else {
                        ""
                    }
                    throw IllegalStateException(
                        "Login failed. Check host, port, credentials, and WebUI settings$hint. Detail: ${loginResult.detail}"
                    )
                } catch (error: IOException) {
                    lastError = error
                    if (attempt >= 3) break
                    delay(nextDelayMs)
                    nextDelayMs = (nextDelayMs * 2).coerceAtMost(4000L)
                } catch (error: AuthLockedException) {
                    throw error
                } catch (error: Throwable) {
                    lastError = error
                    break
                }
            }
        }

        throw lastError ?: IllegalStateException("Login failed. Unknown error.")
    }

    private suspend fun loginWithCompatibleEndpoints(
        liveApi: QbApi,
        credentials: EffectiveCredentials,
    ): LoginResult {
        val v2Response = liveApi.login(credentials.username, credentials.password)
        val v2Result = parseLoginResponse(
            response = v2Response,
            endpointLabel = "api/v2/auth/login",
            passwordBlank = credentials.password.isBlank(),
        )
        if (v2Result.success) return v2Result
        if (v2Result.ipBanned) return v2Result

        if (!shouldTryLegacyLogin(v2Response, v2Result.detail)) {
            return v2Result
        }

        val legacyResponse = liveApi.loginLegacy(credentials.username, credentials.password)
        val legacyResult = parseLoginResponse(
            response = legacyResponse,
            endpointLabel = "login",
            passwordBlank = credentials.password.isBlank(),
        )
        if (legacyResult.success) return legacyResult

        return LoginResult(
            success = false,
            detail = "${v2Result.detail}; ${legacyResult.detail}",
            retryableServerError = v2Result.retryableServerError && legacyResult.retryableServerError,
        )
    }

    private fun parseLoginResponse(
        response: Response<String>,
        endpointLabel: String,
        passwordBlank: Boolean,
    ): LoginResult {
        val successBody = response.body()?.trim().orEmpty()
        val errorBodyText = response.errorBody()?.string()?.trim().orEmpty()
        val loginText = successBody.ifBlank { errorBodyText }
        val loginOk = response.isSuccessful && loginText.equals("Ok.", ignoreCase = true)
        if (loginOk) {
            return LoginResult(success = true, detail = "$endpointLabel -> Ok.")
        }

        val detail = when {
            loginText.equals("Fails.", ignoreCase = true) && passwordBlank -> "Password is empty."
            loginText.equals("Fails.", ignoreCase = true) -> "Credential rejected (Fails.)."
            loginText.isNotBlank() -> loginText
            response.code() == 403 -> "Forbidden (check WebUI auth/CSRF settings)."
            response.code() == 401 -> "Unauthorized."
            !response.isSuccessful -> "HTTP ${response.code()}."
            else -> "Unexpected response."
        }

        return LoginResult(
            success = false,
            detail = "$endpointLabel -> $detail",
            retryableServerError = !response.isSuccessful && response.code() in 500..599,
            ipBanned = looksLikeIpBanned(detail),
        )
    }

    private fun shouldTryLegacyLogin(
        response: Response<String>,
        detail: String,
    ): Boolean {
        if (response.code() in setOf(401, 403, 404, 405, 501)) return true
        val normalizedDetail = detail.lowercase()
        if (looksLikeIpBanned(normalizedDetail)) return false
        return normalizedDetail.contains("not found") ||
            normalizedDetail.contains("404") ||
            normalizedDetail.contains("forbidden")
    }

    private fun looksLikeIpBanned(text: String): Boolean {
        val normalized = text.lowercase()
        return normalized.contains("ip") && normalized.contains("封禁") ||
            normalized.contains("ip address has been banned") ||
            normalized.contains("too many failed") ||
            normalized.contains("banned")
    }

    private fun buildCredentialCandidates(settings: ConnectionSettings): List<EffectiveCredentials> {
        val rawUser = settings.username
        val rawPassword = settings.password
        val normalizedUser = rawUser.trim(*credentialTrimChars)
        val normalizedPassword = rawPassword.trim(*credentialTrimChars)
        val candidates = mutableListOf(
            EffectiveCredentials(
                username = rawUser,
                password = rawPassword,
                wasNormalized = false,
            )
        )

        if (normalizedUser != rawUser || normalizedPassword != rawPassword) {
            candidates += EffectiveCredentials(
                username = normalizedUser,
                password = normalizedPassword,
                wasNormalized = true,
            )
        }

        if (normalizedUser.isBlank()) {
            candidates += EffectiveCredentials(
                username = "admin",
                password = normalizedPassword,
                wasNormalized = true,
            )
        }

        return candidates.distinctBy { "${it.username}\u0000${it.password}" }
    }

    private data class EffectiveCredentials(
        val username: String,
        val password: String,
        val wasNormalized: Boolean,
    )

    private data class LoginResult(
        val success: Boolean,
        val detail: String,
        val retryableServerError: Boolean = false,
        val ipBanned: Boolean = false,
    )

    private class AuthLockedException(message: String) : IllegalStateException(message)

    private suspend fun relogin() {
        val liveApi = requireApi()
        val settings = activeSettings ?: throw IllegalStateException(
            "Connection settings are missing for session recovery."
        )
        loginWithRetry(liveApi, settings)
    }

    private fun buildApi(baseUrl: String, headerMode: HeaderMode): QbApi {
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val parsedBaseUrl = baseUrl.toHttpUrl()

        val clientBuilder = OkHttpClient.Builder()
            .cookieJar(SessionCookieJar())
            .addInterceptor(logger)
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
        if (headerMode == HeaderMode.WEBUI_HEADERS) {
            clientBuilder.addInterceptor(WebUiHeaderInterceptor(parsedBaseUrl))
        }
        val client = clientBuilder.build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(QbApi::class.java)
    }

    private enum class HeaderMode {
        WEBUI_HEADERS,
        NO_WEBUI_HEADERS,
    }

    private fun requireApi(): QbApi {
        return api ?: throw IllegalStateException("Not connected to qBittorrent yet.")
    }

    private fun Response<*>.ensureSuccess(defaultMessage: String) {
        if (!isSuccessful) {
            val extra = errorBody()?.string()?.takeIf { it.isNotBlank() } ?: code().toString()
            throw IllegalStateException("$defaultMessage ($extra)")
        }
    }

    private fun parseTagOptions(raw: String): List<String> {
        val text = raw.trim()
        if (text.isBlank()) return emptyList()

        if (text.startsWith("[")) {
            runCatching {
                JsonParser.parseString(text)
                    .asJsonArray
                    .mapNotNull { item ->
                        runCatching { item.asString.trim() }.getOrNull()
                    }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()
            }.getOrNull()?.let { parsed ->
                if (parsed.isNotEmpty()) return parsed
            }
        }

        return text
            .removePrefix("[")
            .removeSuffix("]")
            .split(',', ';', '\n')
            .map { it.trim().trim('"') }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    private fun parseCountryPeerSnapshots(
        hash: String,
        raw: String,
    ): List<CountryPeerSnapshot> {
        val peersObject = runCatching {
            JsonParser.parseString(raw)
                .asJsonObject
                .getAsJsonObject("peers")
        }.getOrNull() ?: return emptyList()

        return peersObject.entrySet().mapNotNull { (peerId, value) ->
            val peer = value.asJsonObjectOrNull() ?: return@mapNotNull null
            val peerAddress = buildPeerAddress(peer, peerId)
            val countryCode = peer.readString("country_code", "countryCode", "country")
                .uppercase(Locale.US)
                .takeIf { it.length == 2 }
                ?: return@mapNotNull null
            val uploadedBytes = peer.readLong("uploaded", "uploaded_bytes", "up_total")
                ?: return@mapNotNull null
            CountryPeerSnapshot(
                key = "$hash|$peerAddress|$countryCode",
                peerAddress = peerAddress,
                countryCode = countryCode,
                countryName = peer.readString("country_name", "countryName", "country"),
                uploadedBytes = uploadedBytes.coerceAtLeast(0L),
            )
        }
    }

    private fun buildPeerAddress(peer: JsonObject, peerId: String): String {
        val ip = peer.readString("ip", "addr", "address")
        val port = peer.readString("port")
        val endpoint = when {
            ip.isNotBlank() && port.isNotBlank() -> "$ip:$port"
            ip.isNotBlank() -> ip
            else -> peerId.trim()
        }
        return endpoint.ifBlank { peerId.trim() }.ifBlank { "unknown-peer" }
    }

    private fun JsonElement.asJsonObjectOrNull(): JsonObject? {
        return runCatching { asJsonObject }.getOrNull()
    }

    private fun JsonObject.readString(vararg keys: String): String {
        keys.forEach { key ->
            val value = get(key) ?: return@forEach
            if (!value.isJsonNull) {
                val text = runCatching { value.asString.trim() }.getOrNull().orEmpty()
                if (text.isNotBlank()) return text
            }
        }
        return ""
    }

    private fun JsonObject.readLong(vararg keys: String): Long? {
        keys.forEach { key ->
            val value = get(key) ?: return@forEach
            if (value.isJsonNull) return@forEach
            runCatching { value.asLong }.getOrNull()?.let { return it }
            runCatching { value.asString.trim().toLong() }.getOrNull()?.let { return it }
        }
        return null
    }

    private fun normalizeTransferInfo(
        transfer: com.hjw.qbremote.data.model.TransferInfo,
        torrents: List<TorrentInfo>,
    ): com.hjw.qbremote.data.model.TransferInfo {
        var downloaded = transfer.downloadedTotal
        var uploaded = transfer.uploadedTotal
        if (downloaded <= 0L) {
            val sum = torrents.sumOf { it.downloaded.coerceAtLeast(0L) }
            if (sum > 0L) downloaded = sum
        }
        if (uploaded <= 0L) {
            val sum = torrents.sumOf { it.uploaded.coerceAtLeast(0L) }
            if (sum > 0L) uploaded = sum
        }
        if (downloaded == transfer.downloadedTotal && uploaded == transfer.uploadedTotal) {
            return transfer
        }
        return transfer.copy(downloadedTotal = downloaded, uploadedTotal = uploaded)
    }

    private fun sanitizeTorrentInfo(
        current: TorrentInfo,
        previous: TorrentInfo?,
    ): TorrentInfo {
        val mergedHash = current.hash.ifBlank { previous?.hash.orEmpty() }
        var merged = current.copy(hash = mergedHash)

        if (previous != null) {
            val currentState = normalizeState(current.state)
            val previousState = normalizeState(previous.state)
            merged = merged.copy(
                name = merged.name.ifBlank { previous.name },
                category = merged.category.ifBlank { previous.category },
                tags = merged.tags.ifBlank { previous.tags },
                state = when {
                    currentState.isNotBlank() && currentState != "unknown" -> current.state
                    previousState.isNotBlank() && previousState != "unknown" -> previous.state
                    else -> merged.state.ifBlank { previous.state }
                },
                size = if (merged.size > 0L) merged.size else previous.size,
                progress = if (merged.progress > 0f || previous.progress <= 0f) {
                    merged.progress
                } else {
                    previous.progress
                },
                addedOn = if (merged.addedOn > 0L) merged.addedOn else previous.addedOn,
                lastActivity = if (merged.lastActivity > 0L) merged.lastActivity else previous.lastActivity,
                savePath = merged.savePath.ifBlank { previous.savePath },
                tracker = merged.tracker.ifBlank { previous.tracker },
                downloaded = if (merged.downloaded > 0L || previous.downloaded <= 0L) merged.downloaded else previous.downloaded,
                uploaded = if (merged.uploaded > 0L || previous.uploaded <= 0L) merged.uploaded else previous.uploaded,
                downloadSpeed = if (merged.downloadSpeed > 0L || previous.downloadSpeed <= 0L) {
                    merged.downloadSpeed
                } else {
                    previous.downloadSpeed
                },
                uploadSpeed = if (merged.uploadSpeed > 0L || previous.uploadSpeed <= 0L) {
                    merged.uploadSpeed
                } else {
                    previous.uploadSpeed
                },
                ratio = if (merged.ratio >= 0.0) merged.ratio else previous.ratio,
                seeders = if (merged.seeders > 0 || previous.seeders <= 0) merged.seeders else previous.seeders,
                leechers = if (merged.leechers > 0 || previous.leechers <= 0) merged.leechers else previous.leechers,
                numComplete = if (merged.numComplete > 0 || previous.numComplete <= 0) {
                    merged.numComplete
                } else {
                    previous.numComplete
                },
                numIncomplete = if (merged.numIncomplete > 0 || previous.numIncomplete <= 0) {
                    merged.numIncomplete
                } else {
                    previous.numIncomplete
                },
            )
        }

        if (merged.name.isBlank()) {
            merged = merged.copy(
                name = previous?.name?.ifBlank { "" }?.takeIf { it.isNotBlank() }
                    ?: merged.hash.take(12).ifBlank { "Unnamed torrent" }
            )
        }

        if (isUnknownState(merged.state)) {
            merged = merged.copy(
                state = inferStateFromStats(merged, previous?.state.orEmpty()),
            )
        }

        if (merged.tags.isBlank() && !previous?.tags.isNullOrBlank()) {
            merged = merged.copy(tags = previous?.tags.orEmpty())
        }

        return merged
    }

    private fun shouldRefetchTorrentSnapshot(
        current: TorrentInfo,
        previous: TorrentInfo?,
    ): Boolean {
        val currentState = normalizeState(current.state)
        val missingCore = current.name.isBlank() ||
            current.size <= 0L ||
            current.savePath.isBlank() ||
            currentState.isBlank() ||
            currentState == "unknown"
        val hasRuntimeSignal = current.progress > 0f ||
            current.downloadSpeed > 0L ||
            current.uploadSpeed > 0L ||
            current.downloaded > 0L ||
            current.uploaded > 0L
        val regressedFromPrevious = previous != null && (
            (current.name.isBlank() && previous.name.isNotBlank()) ||
                (current.size <= 0L && previous.size > 0L) ||
                (current.savePath.isBlank() && previous.savePath.isNotBlank()) ||
                (isUnknownState(current.state) && !isUnknownState(previous.state)) ||
                (current.tags.isBlank() && previous.tags.isNotBlank())
            )
        return missingCore && (hasRuntimeSignal || regressedFromPrevious)
    }

    private fun normalizeState(state: String): String {
        return state.trim().lowercase()
    }

    private fun isUnknownState(state: String): Boolean {
        val normalized = normalizeState(state)
        return normalized.isBlank() || normalized == "unknown"
    }

    private fun inferStateFromStats(torrent: TorrentInfo, fallback: String): String {
        if (torrent.uploadSpeed > 0L) return "uploading"
        if (torrent.downloadSpeed > 0L) return "downloading"
        if (torrent.progress >= 1f && (torrent.uploaded > 0L || torrent.downloaded > 0L)) return "stalledup"
        if (torrent.progress >= 1f && torrent.size > 0L) return "stalledup"
        if (torrent.progress > 0f) return "stalleddl"
        val normalizedFallback = normalizeState(fallback)
        if (normalizedFallback.isNotBlank() && normalizedFallback != "unknown") return normalizedFallback
        return "unknown"
    }

    private class SessionCookieJar : CookieJar {
        private val cookieStore = mutableMapOf<String, List<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host].orEmpty()
        }
    }

    private class WebUiHeaderInterceptor(
        private val baseUrl: HttpUrl,
    ) : Interceptor {
        private val referer = baseUrl.toString()
        private val origin = buildOrigin(baseUrl)

        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            val request = chain.request()
            val isSameOrigin = request.url.scheme == baseUrl.scheme &&
                request.url.host == baseUrl.host &&
                request.url.port == baseUrl.port

            if (!isSameOrigin) {
                return chain.proceed(request)
            }

            val builder = request.newBuilder()
            if (request.header("Referer").isNullOrBlank()) {
                builder.header("Referer", referer)
            }
            if (request.header("Origin").isNullOrBlank()) {
                builder.header("Origin", origin)
            }
            if (request.header("X-Requested-With").isNullOrBlank()) {
                builder.header("X-Requested-With", "XMLHttpRequest")
            }
            return chain.proceed(builder.build())
        }

        private companion object {
            fun buildOrigin(url: HttpUrl): String {
                val host = if (url.host.contains(':') && !url.host.startsWith("[")) {
                    "[${url.host}]"
                } else {
                    url.host
                }
                val isDefaultPort = (url.scheme == "http" && url.port == 80) ||
                    (url.scheme == "https" && url.port == 443)
                return if (isDefaultPort) {
                    "${url.scheme}://$host"
                } else {
                    "${url.scheme}://$host:${url.port}"
                }
            }
        }
    }
}

