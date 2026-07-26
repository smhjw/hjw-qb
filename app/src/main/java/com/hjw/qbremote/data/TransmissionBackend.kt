package com.hjw.qbremote.data

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.JsonReader
import com.hjw.qbremote.data.model.AddTorrentRequest
import com.hjw.qbremote.data.model.CountryPeerSnapshot
import com.hjw.qbremote.data.model.DashboardData
import com.hjw.qbremote.data.model.TorrentDetailData
import com.hjw.qbremote.data.model.TorrentFileInfo
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.data.model.TorrentProperties
import com.hjw.qbremote.data.model.TorrentTracker
import com.hjw.qbremote.data.model.TransferInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URI
import java.util.Base64
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class TransmissionBackend : TorrentBackend {
    override val backendType: ServerBackendType = ServerBackendType.TRANSMISSION
    override val capabilities: ServerCapabilities = defaultCapabilitiesFor(backendType)

    private val gson = Gson()
    private var activeClient: TransmissionRpcClient? = null

    override suspend fun connect(settings: ConnectionSettings): Result<Unit> = runCatching {
        val client = TransmissionRpcClient(settings, gson, sharedOkHttpClient)
        client.sessionGet()
        activeClient = client
    }

    override fun clearSession() {
        activeClient = null
    }

    override suspend fun fetchTransferInfo(): Result<TransferInfo> = runCatching {
        fetchTransferInfoData(requireClient())
    }

    override suspend fun fetchDashboard(): Result<DashboardData> = runCatching {
        val client = requireClient()
        fetchDashboardData(client)
    }

    override suspend fun fetchDashboardSnapshot(settings: ConnectionSettings): Result<DashboardSnapshotFetchResult> =
        runCatching {
            val client = TransmissionRpcClient(settings, gson, sharedOkHttpClient)
            val session = client.sessionGet()
            DashboardSnapshotFetchResult(
                serverVersion = sanitizeTransmissionVersion(session.version),
                dashboardData = fetchDashboardData(client),
            )
        }

    override suspend fun fetchServerVersion(): Result<String> = runCatching {
        sanitizeTransmissionVersion(requireClient().sessionGet().version)
    }

    override suspend fun fetchTorrentDetail(hash: String): Result<TorrentDetailData> = runCatching {
        require(hash.isNotBlank()) { "Invalid torrent hash." }
        val client = requireClient()
        val torrents = client.torrentGet(
            ids = listOf(hash),
            fields = DETAIL_FIELDS,
        )
        val torrent = torrents.firstOrNull() ?: throw IllegalStateException("Torrent not found.")
        TorrentDetailData(
            properties = torrent.toTorrentProperties(),
            files = torrent.toTorrentFiles(),
        )
    }

    override suspend fun fetchTorrentTrackers(hash: String): Result<List<TorrentTracker>> = runCatching {
        require(hash.isNotBlank()) { "Invalid torrent hash." }
        val client = requireClient()
        val torrents = client.torrentGet(
            ids = listOf(hash),
            fields = listOf("trackers", "trackerStats"),
        )
        torrents.firstOrNull()?.toTorrentTrackers().orEmpty()
    }

    override suspend fun exportTorrentFile(hash: String): Result<ByteArray> = Result.failure(
        UnsupportedOperationException("Transmission does not support exporting torrent files via RPC.")
    )

    override suspend fun fetchCategoryOptions(): Result<List<String>> = Result.success(emptyList())

    override suspend fun fetchTagOptions(): Result<List<String>> = runCatching {
        val torrents = requireClient().torrentGet(
            ids = emptyList(),
            fields = listOf("labels"),
        )
        torrents
            .flatMap { it.labels }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.US) }
            .sorted()
    }

    override suspend fun fetchCountryPeerSnapshots(hashes: List<String>): Result<List<CountryPeerSnapshot>> =
        Result.success(emptyList())

    override suspend fun renameTorrent(hash: String, name: String): Result<Unit> = runCatching {
        require(hash.isNotBlank()) { "Invalid torrent hash." }
        require(name.isNotBlank()) { "Name cannot be empty." }
        val currentName = requireClient().torrentGet(
            ids = listOf(hash),
            fields = listOf("name"),
        ).firstOrNull()?.name.orEmpty()
        requireClient().rpc(
            method = "torrent-rename-path",
            arguments = buildJsonObject(
                "ids" to listOf(hash),
                "path" to resolveTransmissionRenamePath(currentTorrentName = currentName),
                "name" to name.trim(),
            ),
        )
    }

    override suspend fun reannounceTorrent(hash: String): Result<Unit> = runCatching {
        require(hash.isNotBlank()) { "Invalid torrent hash." }
        requireClient().rpc(
            method = "torrent-reannounce",
            arguments = buildJsonObject("ids" to listOf(hash)),
        )
    }

    override suspend fun recheckTorrent(hash: String): Result<Unit> = runCatching {
        require(hash.isNotBlank()) { "Invalid torrent hash." }
        requireClient().rpc(
            method = "torrent-verify",
            arguments = buildJsonObject("ids" to listOf(hash)),
        )
    }

    override suspend fun setTorrentLocation(hash: String, location: String): Result<Unit> = runCatching {
        require(hash.isNotBlank()) { "Invalid torrent hash." }
        require(location.isNotBlank()) { "Location cannot be empty." }
        requireClient().rpc(
            method = "torrent-set-location",
            arguments = buildJsonObject(
                "ids" to listOf(hash),
                "location" to location.trim(),
                "move" to true,
            ),
        )
    }

    override suspend fun setTorrentCategory(hash: String, category: String): Result<Unit> = Result.failure(
        UnsupportedOperationException("Transmission does not support categories.")
    )

    override suspend fun setTorrentTags(hash: String, oldTags: String, newTags: String): Result<Unit> = runCatching {
        require(hash.isNotBlank()) { "Invalid torrent hash." }
        val labels = newTags
            .split(',', ';', '|')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        requireClient().rpc(
            method = "torrent-set",
            arguments = buildJsonObject(
                "ids" to listOf(hash),
                "labels" to labels,
            ),
        )
    }

    override suspend fun addTracker(hash: String, trackerUrl: String): Result<Unit> = runCatching {
        require(hash.isNotBlank()) { "Invalid torrent hash." }
        require(trackerUrl.isNotBlank()) { "Tracker URL cannot be empty." }
        requireClient().rpc(
            method = "torrent-set",
            arguments = buildJsonObject(
                "ids" to listOf(hash),
                "trackerAdd" to listOf(trackerUrl.trim()),
            ),
        )
    }

    override suspend fun editTracker(
        hash: String,
        tracker: TorrentTracker,
        newUrl: String,
    ): Result<Unit> = runCatching {
        require(hash.isNotBlank()) { "Invalid torrent hash." }
        val trackerId = tracker.backendTrackerId
        require(trackerId >= 0) { "Tracker id is missing." }
        require(newUrl.isNotBlank()) { "New tracker URL cannot be empty." }
        requireClient().rpc(
            method = "torrent-set",
            arguments = buildJsonObject(
                "ids" to listOf(hash),
                "trackerReplace" to listOf(trackerId, newUrl.trim()),
            ),
        )
    }

    override suspend fun removeTracker(hash: String, tracker: TorrentTracker): Result<Unit> = runCatching {
        require(hash.isNotBlank()) { "Invalid torrent hash." }
        val trackerId = tracker.backendTrackerId
        require(trackerId >= 0) { "Tracker id is missing." }
        requireClient().rpc(
            method = "torrent-set",
            arguments = buildJsonObject(
                "ids" to listOf(hash),
                "trackerRemove" to listOf(trackerId),
            ),
        )
    }

    override suspend fun setTorrentSpeedLimit(
        hash: String,
        downloadLimitBytes: Long,
        uploadLimitBytes: Long,
    ): Result<Unit> = runCatching {
        require(hash.isNotBlank()) { "Invalid torrent hash." }
        val downloadLimitKb = (downloadLimitBytes / 1024L).toInt().coerceAtLeast(0)
        val uploadLimitKb = (uploadLimitBytes / 1024L).toInt().coerceAtLeast(0)
        requireClient().rpc(
            method = "torrent-set",
            arguments = buildJsonObject(
                "ids" to listOf(hash),
                "downloadLimited" to (downloadLimitKb > 0),
                "downloadLimit" to downloadLimitKb,
                "uploadLimited" to (uploadLimitKb > 0),
                "uploadLimit" to uploadLimitKb,
            ),
        )
    }

    override suspend fun setTorrentShareRatio(hash: String, ratioLimit: Double): Result<Unit> = runCatching {
        require(hash.isNotBlank()) { "Invalid torrent hash." }
        requireClient().rpc(
            method = "torrent-set",
            arguments = buildJsonObject(
                "ids" to listOf(hash),
                "seedRatioMode" to if (ratioLimit < 0) 1 else 2,
                "seedRatioLimit" to ratioLimit.coerceAtLeast(0.0),
            ),
        )
    }

    override suspend fun fetchGlobalSpeedLimits(): Result<GlobalSpeedLimits> = runCatching {
        val session = requireClient().sessionGet()
        GlobalSpeedLimits(
            downloadLimitKb = if (session.speedLimitDownEnabled) {
                session.speedLimitDown.toLong().coerceAtLeast(0L)
            } else {
                0L
            },
            uploadLimitKb = if (session.speedLimitUpEnabled) {
                session.speedLimitUp.toLong().coerceAtLeast(0L)
            } else {
                0L
            },
            alternativeDownloadLimitKb = session.altSpeedDown.toLong().coerceAtLeast(0L),
            alternativeUploadLimitKb = session.altSpeedUp.toLong().coerceAtLeast(0L),
            schedulerEnabled = session.altSpeedTimeEnabled,
            scheduleStartMinutes = session.altSpeedTimeBegin.coerceIn(0, 1439),
            scheduleEndMinutes = session.altSpeedTimeEnd.coerceIn(0, 1439),
            scheduleDayPreset = ScheduleDayPreset.fromTransmissionDayMask(session.altSpeedTimeDay),
            alternativeModeEnabled = session.altSpeedEnabled,
        )
    }

    override suspend fun setGlobalSpeedLimits(limits: GlobalSpeedLimits): Result<Unit> = runCatching {
        requireClient().sessionSet(
            buildJsonObject(
                "speed-limit-down-enabled" to (limits.downloadLimitKb > 0L),
                "speed-limit-down" to limits.downloadLimitKb.toInt(),
                "speed-limit-up-enabled" to (limits.uploadLimitKb > 0L),
                "speed-limit-up" to limits.uploadLimitKb.toInt(),
                "alt-speed-down" to limits.alternativeDownloadLimitKb.toInt(),
                "alt-speed-up" to limits.alternativeUploadLimitKb.toInt(),
                "alt-speed-time-enabled" to limits.schedulerEnabled,
                "alt-speed-time-begin" to limits.scheduleStartMinutes,
                "alt-speed-time-end" to limits.scheduleEndMinutes,
                "alt-speed-time-day" to limits.scheduleDayPreset.transmissionDayMask,
            ),
        )
    }

    override suspend fun addTorrent(request: AddTorrentRequest): Result<Unit> = runCatching {
        require(request.urls.isNotBlank() || request.files.isNotEmpty()) {
            "Please provide a torrent URL or file."
        }
        val client = requireClient()
        request.urls
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { url ->
                client.rpc(
                    method = "torrent-add",
                    arguments = buildTransmissionAddTorrentArguments(
                        request = request,
                        common = mutableMapOf<String, Any?>(
                            "filename" to url,
                        ),
                    ),
                )
            }

        request.files.forEach { file ->
            client.rpc(
                method = "torrent-add",
                arguments = buildTransmissionAddTorrentArguments(
                    request = request,
                    common = mutableMapOf<String, Any?>(
                        "metainfo" to Base64.getEncoder().encodeToString(file.bytes),
                    ),
                ),
            )
        }
    }

    override suspend fun pauseTorrent(hash: String): Result<Unit> = runCatching {
        require(hash.isNotBlank()) { "Invalid torrent hash." }
        requireClient().rpc(
            method = "torrent-stop",
            arguments = buildJsonObject("ids" to listOf(hash)),
        )
    }

    override suspend fun resumeTorrent(hash: String): Result<Unit> = runCatching {
        require(hash.isNotBlank()) { "Invalid torrent hash." }
        requireClient().rpc(
            method = "torrent-start",
            arguments = buildJsonObject("ids" to listOf(hash)),
        )
    }

    override suspend fun deleteTorrent(hash: String, deleteFiles: Boolean): Result<Unit> = runCatching {
        require(hash.isNotBlank()) { "Invalid torrent hash." }
        requireClient().rpc(
            method = "torrent-remove",
            arguments = buildJsonObject(
                "ids" to listOf(hash),
                "delete-local-data" to deleteFiles,
            ),
        )
    }

    private suspend fun fetchDashboardData(client: TransmissionRpcClient): DashboardData {
        val transferInfo = fetchTransferInfoData(client)
        val (transmissionTorrents, _) = client.torrentGetStreamed(
            fields = transmissionDashboardFields(),
        )
        val torrents = transmissionTorrents.map { torrent ->
            torrent.toTorrentInfo()
        }

        return DashboardData(
            transferInfo = transferInfo,
            torrents = torrents,
        )
    }

    private suspend fun fetchTransferInfoData(client: TransmissionRpcClient): TransferInfo {
        val session = client.sessionGet()
        val sessionStats = client.sessionStats()
        val downloadLimit = if (session.speedLimitDownEnabled) {
            session.speedLimitDown.toLong().coerceAtLeast(0L) * 1024L
        } else {
            0L
        }
        val uploadLimit = if (session.speedLimitUpEnabled) {
            session.speedLimitUp.toLong().coerceAtLeast(0L) * 1024L
        } else {
            0L
        }
        val freeSpace = session.downloadDir.takeIf { it.isNotBlank() }?.let { path ->
            client.freeSpace(path).getOrElse { 0L }
        } ?: 0L

        return TransferInfo(
            downloadSpeed = sessionStats.downloadSpeed.coerceAtLeast(0L),
            uploadSpeed = sessionStats.uploadSpeed.coerceAtLeast(0L),
            downloadedTotal = sessionStats.cumulativeDownloadedBytes.coerceAtLeast(0L),
            uploadedTotal = sessionStats.cumulativeUploadedBytes.coerceAtLeast(0L),
            downloadRateLimit = downloadLimit,
            uploadRateLimit = uploadLimit,
            freeSpaceOnDisk = freeSpace.coerceAtLeast(0L),
            dhtNodes = 0,
            totalTorrentCount = sessionStats.torrentCount.coerceAtLeast(0),
        )
    }

    private fun requireClient(): TransmissionRpcClient {
        return activeClient ?: throw IllegalStateException("Not connected to Transmission yet.")
    }

    private class TransmissionRpcClient(
        settings: ConnectionSettings,
        private val gson: Gson,
        okHttpClient: OkHttpClient,
    ) {
        private val rpcUrls = settings.transmissionRpcUrlCandidates()
        private val authHeader = if (settings.username.isNotBlank()) {
            Credentials.basic(settings.username, settings.password)
        } else {
            null
        }

        // RPC-specific transport hardening: redirects must not be followed
        // (OkHttp turns a redirected POST into a GET and lands on the web UI,
        // which then parses as an HTML "response"), and some reverse proxies
        // in front of transmission-daemon mangle HTTP/2 POST bodies, so pin
        // the RPC channel to HTTP/1.1.
        private val client = okHttpClient.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .protocols(listOf(Protocol.HTTP_1_1))
            .build()

        // Read/written from different Dispatchers.IO threads across requests.
        @Volatile
        private var selectedUrl: String? = null

        private fun buildJsonObject(vararg pairs: Pair<String, Any?>): JsonObject {
            val result = JsonObject()
            for ((key, value) in pairs) {
                if (value == null) continue
                result.add(key, gson.toJsonTree(value))
            }
            return result
        }
        @Volatile
        private var sessionId: String? = null
        private var probeResult: TransmissionRpcProbeResult? = null

        private fun JsonElement.asTransmissionSessionInfo(): TransmissionSessionInfo {
            return gson.fromJson(this, TransmissionSessionInfo::class.java)
        }

        private fun JsonElement.asTransmissionSessionStats(): TransmissionSessionStats {
            return gson.fromJson(this, TransmissionSessionStats::class.java)
        }

        suspend fun sessionGet(): TransmissionSessionInfo {
            return rpc(
                method = "session-get",
                arguments = buildJsonObject(
                    "fields" to listOf(
                        "version",
                        "download-dir",
                        "speed-limit-down-enabled",
                        "speed-limit-down",
                        "speed-limit-up-enabled",
                        "speed-limit-up",
                        "alt-speed-down",
                        "alt-speed-up",
                        "alt-speed-enabled",
                        "alt-speed-time-enabled",
                        "alt-speed-time-begin",
                        "alt-speed-time-end",
                        "alt-speed-time-day",
                    ),
                ),
            ).asTransmissionSessionInfo()
        }

        suspend fun sessionSet(arguments: JsonObject) {
            rpc(method = "session-set", arguments = arguments)
        }

        suspend fun sessionStats(): TransmissionSessionStats {
            return rpc(
                method = "session-stats",
                arguments = JsonObject(),
            ).asTransmissionSessionStats()
        }

        suspend fun freeSpace(path: String): Result<Long> = runCatching {
            val arguments = rpc(
                method = "free-space",
                arguments = buildJsonObject("path" to path),
            )
            arguments.asJsonObject.get("size-bytes")?.asLong ?: 0L
        }

        suspend fun torrentGet(ids: List<String>, fields: List<String>): List<TransmissionTorrent> {
            val arguments = buildJsonObject("fields" to fields)
            if (ids.isNotEmpty()) {
                arguments.add("ids", gson.toJsonTree(ids))
            }
            return withContext(Dispatchers.IO) {
                val bodyJson = gson.toJson(
                    mapOf(
                        "method" to "torrent-get",
                        "arguments" to arguments,
                    ),
                )
                var lastError: Throwable? = null
                val attemptFailures = mutableListOf<RpcAttemptFailure>()
                for (candidate in effectiveRpcUrls()) {
                    try {
                        val response = execute(candidate, bodyJson) { rawResponse, preview ->
                            parseTorrentGetResponse(
                                url = candidate,
                                response = rawResponse,
                                preview = preview,
                            )
                        }
                        selectedUrl = candidate
                        probeResult = TransmissionRpcProbeResult(
                            resolvedUrl = candidate,
                            attempts = rpcUrls,
                            failureSummary = attemptFailures.joinToString(" | ") { attempt ->
                                "${attempt.url} => ${attempt.summary}"
                            },
                        )
                        return@withContext response
                    } catch (error: TransmissionRpcAttemptException) {
                        attemptFailures += RpcAttemptFailure(
                            url = error.url,
                            summary = error.summary,
                            authFailure = error.authFailure,
                        )
                        lastError = error
                    } catch (error: Throwable) {
                        attemptFailures += RpcAttemptFailure(
                            url = candidate,
                            summary = error.message?.takeIf { it.isNotBlank() }
                                ?: error::class.simpleName
                                ?: "Request failed.",
                        )
                        lastError = error
                    }
                }

                val authFailure = attemptFailures.firstOrNull { it.authFailure }
                if (authFailure != null) {
                    throw BackendConnectionError.AuthFailed(
                        backendType = ServerBackendType.TRANSMISSION,
                        detail = authFailure.summary,
                    )
                }

                if (attemptFailures.isNotEmpty()) {
                    throw BackendConnectionError.RpcPathNotFound(
                        attempts = attemptFailures.map { it.url },
                        failureSummary = attemptFailures.joinToString(" | ") { attempt ->
                            "${attempt.url} => ${attempt.summary}"
                        },
                    )
                }

                throw lastError ?: IllegalStateException("Transmission RPC failed.")
            }
        }

        suspend fun torrentGetStreamed(
            fields: List<String>,
        ): Pair<List<TransmissionTorrent>, Int> {
            val arguments = buildJsonObject("fields" to fields)
            return withContext(Dispatchers.IO) {
                val bodyJson = gson.toJson(
                    mapOf(
                        "method" to "torrent-get",
                        "arguments" to arguments,
                    ),
                )
                var lastError: Throwable? = null
                val attemptFailures = mutableListOf<RpcAttemptFailure>()
                for (candidate in effectiveRpcUrls()) {
                    try {
                        val result = executeStreamed(candidate, bodyJson)
                        selectedUrl = candidate
                        probeResult = TransmissionRpcProbeResult(
                            resolvedUrl = candidate,
                            attempts = rpcUrls,
                            failureSummary = attemptFailures.joinToString(" | ") { attempt ->
                                "${attempt.url} => ${attempt.summary}"
                            },
                        )
                        return@withContext result
                    } catch (error: TransmissionRpcAttemptException) {
                        attemptFailures += RpcAttemptFailure(
                            url = error.url,
                            summary = error.summary,
                            authFailure = error.authFailure,
                        )
                        lastError = error
                    } catch (error: Throwable) {
                        attemptFailures += RpcAttemptFailure(
                            url = candidate,
                            summary = error.message?.takeIf { it.isNotBlank() }
                                ?: error::class.simpleName
                                ?: "Request failed.",
                        )
                        lastError = error
                    }
                }

                val authFailure = attemptFailures.firstOrNull { it.authFailure }
                if (authFailure != null) {
                    throw BackendConnectionError.AuthFailed(
                        backendType = ServerBackendType.TRANSMISSION,
                        detail = authFailure.summary,
                    )
                }

                if (attemptFailures.isNotEmpty()) {
                    throw BackendConnectionError.RpcPathNotFound(
                        attempts = attemptFailures.map { it.url },
                        failureSummary = attemptFailures.joinToString(" | ") { attempt ->
                            "${attempt.url} => ${attempt.summary}"
                        },
                    )
                }

                throw lastError ?: IllegalStateException("Transmission RPC failed.")
            }
        }

        private fun executeStreamed(
            url: String,
            bodyJson: String,
            handshakeAttempt: Int = 0,
        ): Pair<List<TransmissionTorrent>, Int> {
            val mediaType = "application/json".toMediaType()
            var requestBuilder = Request.Builder()
                .url(url)
                .post(bodyJson.toRequestBody(mediaType))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
            if (!authHeader.isNullOrBlank()) {
                requestBuilder = requestBuilder.header("Authorization", authHeader)
            }
            sessionId?.takeIf { it.isNotBlank() }?.let { value ->
                requestBuilder = requestBuilder.header("X-Transmission-Session-Id", value)
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (response.code == 409) {
                    val newSessionId = response.header("X-Transmission-Session-Id").orEmpty()
                    if (newSessionId.isBlank()) {
                        throw TransmissionRpcAttemptException(
                            url = url,
                            summary = "Session handshake failed (409 without session id).",
                        )
                    }
                    // Per the RPC spec one retry with the refreshed session id must
                    // succeed; bound it so a proxy that always answers 409 with a new
                    // id cannot recurse forever.
                    if (handshakeAttempt >= MAX_SESSION_HANDSHAKE_RETRIES) {
                        throw TransmissionRpcAttemptException(
                            url = url,
                            summary = "Session handshake did not converge (repeated 409).",
                        )
                    }
                    sessionId = newSessionId
                    return executeStreamed(url, bodyJson, handshakeAttempt + 1)
                }
                if (response.code == 401 || response.code == 403) {
                    throw TransmissionRpcAttemptException(
                        url = url,
                        summary = "Authentication failed (HTTP ${response.code}).",
                        authFailure = true,
                    )
                }
                val preview = response.peekBody(RESPONSE_PREVIEW_BYTES).string()
                if (!response.isSuccessful) {
                    throw TransmissionRpcAttemptException(
                        url = url,
                        summary = when {
                            response.isRedirect ->
                                "Redirected to ${response.header("Location").orEmpty().ifBlank { "unknown location" }} (HTTP ${response.code})."
                            looksLikeHtml(preview) -> "Invalid HTML response (HTTP ${response.code})."
                            preview.isBlank() -> "HTTP ${response.code}."
                            else -> "HTTP ${response.code}: ${summarizeResponseText(preview, maxChars = 120)}"
                        },
                    )
                }
                val body = response.body ?: throw TransmissionRpcAttemptException(
                    url = url,
                    summary = "Empty response body.",
                )
                return parseTorrentGetResponseStreamed(url, body)
            }
        }

        private fun parseTorrentGetResponseStreamed(
            url: String,
            body: okhttp3.ResponseBody,
        ): Pair<List<TransmissionTorrent>, Int> {
            body.charStream().use { reader ->
                val jsonReader = JsonReader(reader)
                jsonReader.isLenient = true

                var result = ""
                val torrents = mutableListOf<TransmissionTorrent>()
                var totalCount = 0

                jsonReader.beginObject()
                while (jsonReader.hasNext()) {
                    when (jsonReader.nextName()) {
                        "arguments" -> {
                            jsonReader.beginObject()
                            while (jsonReader.hasNext()) {
                                when (jsonReader.nextName()) {
                                    "torrents" -> {
                                        jsonReader.beginArray()
                                        while (jsonReader.hasNext()) {
                                            totalCount++
                                            if (torrents.size < MAX_FETCH_DASHBOARD_TORRENTS) {
                                                val t: TransmissionTorrent = gson.fromJson(
                                                    jsonReader,
                                                    TransmissionTorrent::class.java,
                                                )
                                                torrents.add(t)
                                            } else {
                                                jsonReader.skipValue()
                                            }
                                        }
                                        jsonReader.endArray()
                                    }
                                    else -> jsonReader.skipValue()
                                }
                            }
                            jsonReader.endObject()
                        }
                        "result" -> {
                            result = jsonReader.nextString()
                        }
                        else -> jsonReader.skipValue()
                    }
                }
                jsonReader.endObject()

                if (!result.equals("success", ignoreCase = true)) {
                    val summary = result.ifBlank { "Transmission RPC returned an error." }
                    throw TransmissionRpcAttemptException(
                        url = url,
                        summary = summarizeResponseText(summary, maxChars = 120),
                        authFailure = looksLikeAuthFailure(summary),
                    )
                }

                return torrents to totalCount
            }
        }

        suspend fun rpc(method: String, arguments: JsonObject): JsonElement {
            return withContext(Dispatchers.IO) {
                val bodyJson = gson.toJson(
                    mapOf(
                        "method" to method,
                        "arguments" to arguments,
                    ),
                )
                var lastError: Throwable? = null
                val attemptFailures = mutableListOf<RpcAttemptFailure>()
                for (candidate in effectiveRpcUrls()) {
                    try {
                        val response = execute(candidate, bodyJson) { rawResponse, preview ->
                            parseRpcArguments(
                                url = candidate,
                                response = rawResponse,
                                preview = preview,
                            )
                        }
                        selectedUrl = candidate
                        probeResult = TransmissionRpcProbeResult(
                            resolvedUrl = candidate,
                            attempts = rpcUrls,
                            failureSummary = attemptFailures.joinToString(" | ") { attempt ->
                                "${attempt.url} => ${attempt.summary}"
                            },
                        )
                        return@withContext response
                    } catch (error: TransmissionRpcAttemptException) {
                        attemptFailures += RpcAttemptFailure(
                            url = error.url,
                            summary = error.summary,
                            authFailure = error.authFailure,
                        )
                        lastError = error
                    } catch (error: Throwable) {
                        attemptFailures += RpcAttemptFailure(
                            url = candidate,
                            summary = error.message?.takeIf { it.isNotBlank() }
                                ?: error::class.simpleName
                                ?: "Request failed.",
                        )
                        lastError = error
                    }
                }

                val authFailure = attemptFailures.firstOrNull { it.authFailure }
                if (authFailure != null) {
                    throw BackendConnectionError.AuthFailed(
                        backendType = ServerBackendType.TRANSMISSION,
                        detail = authFailure.summary,
                    )
                }

                if (attemptFailures.isNotEmpty()) {
                    throw BackendConnectionError.RpcPathNotFound(
                        attempts = attemptFailures.map { it.url },
                        failureSummary = attemptFailures.joinToString(" | ") { attempt ->
                            "${attempt.url} => ${attempt.summary}"
                        },
                    )
                }

                throw lastError ?: IllegalStateException("Transmission RPC failed.")
            }
        }

        private fun effectiveRpcUrls(): List<String> {
            val selected = selectedUrl
            return if (selected == null) rpcUrls else listOf(selected) + rpcUrls.filterNot { candidate ->
                candidate == selected
            }
        }

        private fun <T> execute(
            url: String,
            bodyJson: String,
            handshakeAttempt: Int = 0,
            parser: (Response, String) -> T,
        ): T {
            val mediaType = "application/json".toMediaType()
            var requestBuilder = Request.Builder()
                .url(url)
                .post(bodyJson.toRequestBody(mediaType))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
            if (!authHeader.isNullOrBlank()) {
                requestBuilder = requestBuilder.header("Authorization", authHeader)
            }
            sessionId?.takeIf { it.isNotBlank() }?.let { value ->
                requestBuilder = requestBuilder.header("X-Transmission-Session-Id", value)
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (response.code == 409) {
                    val newSessionId = response.header("X-Transmission-Session-Id").orEmpty()
                    if (newSessionId.isBlank()) {
                        throw TransmissionRpcAttemptException(
                            url = url,
                            summary = "Session handshake failed (409 without session id).",
                        )
                    }
                    // Per the RPC spec one retry with the refreshed session id must
                    // succeed; bound it so a proxy that always answers 409 with a new
                    // id cannot recurse forever.
                    if (handshakeAttempt >= MAX_SESSION_HANDSHAKE_RETRIES) {
                        throw TransmissionRpcAttemptException(
                            url = url,
                            summary = "Session handshake did not converge (repeated 409).",
                        )
                    }
                    sessionId = newSessionId
                    return execute(url, bodyJson, handshakeAttempt + 1, parser)
                }
                if (response.code == 401 || response.code == 403) {
                    throw TransmissionRpcAttemptException(
                        url = url,
                        summary = "Authentication failed (HTTP ${response.code}).",
                        authFailure = true,
                    )
                }
                val preview = response.peekBody(RESPONSE_PREVIEW_BYTES).string()
                if (!response.isSuccessful) {
                    throw TransmissionRpcAttemptException(
                        url = url,
                        summary = when {
                            response.isRedirect ->
                                "Redirected to ${response.header("Location").orEmpty().ifBlank { "unknown location" }} (HTTP ${response.code})."
                            looksLikeHtml(preview) -> "Invalid HTML response (HTTP ${response.code})."
                            preview.isBlank() -> "HTTP ${response.code}."
                            else -> "HTTP ${response.code}: ${summarizeResponseText(preview, maxChars = 120)}"
                        },
                    )
                }
                val payload = response.body
                if (payload == null) {
                    throw TransmissionRpcAttemptException(
                        url = url,
                        summary = "Empty response body.",
                    )
                }
                return parser(response, preview)
            }
        }

        private fun parseRpcArguments(
            url: String,
            response: Response,
            preview: String,
        ): JsonElement {
            val body = response.body ?: throw TransmissionRpcAttemptException(
                url = url,
                summary = "Empty response body.",
            )
            val payload = body.string()
            if (payload.isBlank()) {
                throw TransmissionRpcAttemptException(
                    url = url,
                    summary = "Empty response body.",
                )
            }
            val root = runCatching {
                JsonParser.parseString(payload.removePrefix("\uFEFF").trim()).asJsonObject
            }.getOrElse {
                throw TransmissionRpcAttemptException(
                    url = url,
                    summary = when {
                        looksLikeHtml(preview) -> "Invalid HTML response."
                        preview.isBlank() -> "Empty response body."
                        else -> "Non-JSON response: ${summarizeResponseText(preview, maxChars = 80)}"
                    },
                )
            }
            val result = root.get("result")?.asString.orEmpty()
            if (!result.equals("success", ignoreCase = true)) {
                val summary = result.ifBlank { "Transmission RPC returned an error." }
                throw TransmissionRpcAttemptException(
                    url = url,
                    summary = summarizeResponseText(summary, maxChars = 120),
                    authFailure = looksLikeAuthFailure(summary),
                )
            }
            return root.get("arguments") ?: JsonObject()
        }

        private fun parseTorrentGetResponse(
            url: String,
            response: Response,
            preview: String,
        ): List<TransmissionTorrent> {
            val body = response.body ?: throw TransmissionRpcAttemptException(
                url = url,
                summary = "Empty response body.",
            )
            val envelope = runCatching {
                body.charStream().use { reader ->
                    gson.fromJson(reader, TransmissionTorrentGetEnvelope::class.java)
                }
            }.getOrElse {
                throw TransmissionRpcAttemptException(
                    url = url,
                    summary = when {
                        looksLikeHtml(preview) -> "Invalid HTML response."
                        preview.isBlank() -> "Empty response body."
                        else -> "Non-JSON response: ${summarizeResponseText(preview, maxChars = 80)}"
                    },
                )
            } ?: throw TransmissionRpcAttemptException(
                url = url,
                summary = "Empty response body.",
            )
            val result = envelope.result.orEmpty()
            if (!result.equals("success", ignoreCase = true)) {
                val summary = result.ifBlank { "Transmission RPC returned an error." }
                throw TransmissionRpcAttemptException(
                    url = url,
                    summary = summarizeResponseText(summary, maxChars = 120),
                    authFailure = looksLikeAuthFailure(summary),
                )
            }
            return envelope.arguments.torrents
        }

    }

    private fun TransmissionTorrent.toTorrentInfo(): TorrentInfo {
        val labelsText = labels
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")
        val primaryTracker = resolveTransmissionPrimaryTracker(trackerList).ifBlank {
            trackers.firstOrNull()?.announce
            ?: trackerStats.firstOrNull()?.announce
            ?: ""
        }
        return TorrentInfo(
            hash = hashString.trim(),
            name = name.trim(),
            category = "",
            ratio = uploadRatio,
            size = totalSize.coerceAtLeast(0L),
            progress = percentDone.coerceIn(0.0, 1.0).toFloat(),
            state = mapTransmissionState(
                status = status,
                percentDone = percentDone,
                isFinished = isFinished,
            ),
            downloadSpeed = rateDownload.coerceAtLeast(0L),
            uploadSpeed = rateUpload.coerceAtLeast(0L),
            downloaded = downloadedEver.coerceAtLeast(0L),
            uploaded = uploadedEver.coerceAtLeast(0L),
            addedOn = addedDate.coerceAtLeast(0L),
            lastActivity = activityDate.coerceAtLeast(0L),
            eta = eta.coerceAtLeast(-1L),
            seeders = peersSendingToUs.coerceAtLeast(0),
            leechers = peersGettingFromUs.coerceAtLeast(0),
            numComplete = peersSendingToUs.coerceAtLeast(0),
            numIncomplete = peersGettingFromUs.coerceAtLeast(0),
            tracker = primaryTracker.trim(),
            savePath = downloadDir.trim(),
            tags = labelsText,
        )
    }

    private fun TransmissionTorrent.toTorrentProperties(): TorrentProperties {
        val resolvedDownloadLimit = if (downloadLimited) {
            downloadLimit.toLong().coerceAtLeast(0L) * 1024L
        } else {
            0L
        }
        val resolvedUploadLimit = if (uploadLimited) {
            uploadLimit.toLong().coerceAtLeast(0L) * 1024L
        } else {
            0L
        }
        return TorrentProperties(
            savePath = downloadDir.trim(),
            creationDate = dateCreated.coerceAtLeast(0L),
            pieceSize = pieceSize.coerceAtLeast(0L),
            comment = comment.orEmpty(),
            totalSize = totalSize.coerceAtLeast(0L),
            downloadLimit = resolvedDownloadLimit,
            uploadLimit = resolvedUploadLimit,
            timeElapsed = secondsDownloading.coerceAtLeast(0L),
            seedingTime = secondsSeeding.coerceAtLeast(0L),
            shareRatio = uploadRatio,
        )
    }

    private fun TransmissionTorrent.toTorrentFiles(): List<TorrentFileInfo> {
        val fileByIndex = files.associateBy { it.name to it.length }
        return files.mapIndexed { index, file ->
            val bytesCompleted = fileByIndex[file.name to file.length]?.bytesCompleted ?: 0L
            TorrentFileInfo(
                index = index,
                name = file.name,
                size = file.length.coerceAtLeast(0L),
                progress = if (file.length > 0L) {
                    (bytesCompleted.toDouble() / file.length.toDouble()).coerceIn(0.0, 1.0).toFloat()
                } else {
                    0f
                },
                priority = fileStats.getOrNull(index)?.priority ?: 0,
            )
        }
    }

    private fun TransmissionTorrent.toTorrentTrackers(): List<TorrentTracker> {
        val statsById = trackerStats.associateBy { it.id }
        return trackers.map { tracker ->
            val stat = statsById[tracker.id]
            TorrentTracker(
                backendTrackerId = tracker.id,
                url = tracker.announce,
                status = if (stat?.hasAnnounced == true || stat?.lastAnnounceSucceeded == true) 2 else 0,
                message = stat?.lastAnnounceResult.orEmpty().ifBlank {
                    stat?.lastScrapeResult.orEmpty()
                },
                tier = tracker.tier,
                numPeers = (stat?.seederCount ?: 0) + (stat?.leecherCount ?: 0),
                numSeeds = stat?.seederCount ?: 0,
                numLeeches = stat?.leecherCount ?: 0,
                numDownloaded = stat?.downloadCount ?: 0,
            )
        }
    }

    private fun buildJsonObject(vararg pairs: Pair<String, Any?>): JsonObject {
        val result = JsonObject()
        for ((key, value) in pairs) {
            if (value == null) continue
            result.add(key, gson.toJsonTree(value))
        }
        return result
    }

    companion object {
        private val DETAIL_FIELDS = listOf(
            "hashString",
            "name",
            "labels",
            "uploadRatio",
            "totalSize",
            "percentDone",
            "status",
            "rateDownload",
            "rateUpload",
            "downloadedEver",
            "uploadedEver",
            "addedDate",
            "activityDate",
            "eta",
            "peersSendingToUs",
            "peersGettingFromUs",
            "trackers",
            "trackerStats",
            "downloadDir",
            "isFinished",
            "comment",
            "dateCreated",
            "pieceSize",
            "downloadLimited",
            "downloadLimit",
            "uploadLimited",
            "uploadLimit",
            "secondsDownloading",
            "secondsSeeding",
            "files",
            "fileStats",
            "magnetLink",
        )

        private const val RESPONSE_PREVIEW_BYTES = 4_096L
        private const val MAX_SESSION_HANDSHAKE_RETRIES = 1

        private val sharedOkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(18, TimeUnit.SECONDS)
                .writeTimeout(18, TimeUnit.SECONDS)
                .build()
        }
    }
}

internal fun mapTransmissionState(
    status: Int,
    percentDone: Double,
    isFinished: Boolean,
): String {
    return when (status) {
        0 -> if (isFinished || percentDone >= 1.0) "pausedUP" else "pausedDL"
        1, 2 -> "checkUP"
        3 -> "queuedDL"
        4 -> "downloading"
        5 -> "queuedUP"
        6 -> "uploading"
        else -> if (percentDone >= 1.0) "pausedUP" else "pausedDL"
    }
}

internal fun transmissionDashboardFields(): List<String> = listOf(
    "hashString",
    "name",
    "labels",
    "uploadRatio",
    "totalSize",
    "percentDone",
    "status",
    "rateDownload",
    "rateUpload",
    "downloadedEver",
    "uploadedEver",
    "addedDate",
    "activityDate",
    "eta",
    "peersSendingToUs",
    "peersGettingFromUs",
    "trackerList",
    "downloadDir",
    "isFinished",
)

internal fun resolveTransmissionPrimaryTracker(rawTrackerList: String): String {
    return rawTrackerList
        .lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
}

internal fun sanitizeTransmissionVersion(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return "-"
    return trimmed
        .replace(Regex("\\s*\\([^)]*\\)\\s*$"), "")
        .trim()
        .ifBlank { "-" }
}

internal fun parseTransmissionLabels(raw: JsonElement?): List<String> {
    if (raw == null || raw.isJsonNull) return emptyList()

    fun normalize(values: Sequence<String>): List<String> {
        val normalizedByKey = linkedMapOf<String, String>()
        values
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "-" && !it.equals("null", ignoreCase = true) }
            .forEach { label ->
                val key = label.lowercase(Locale.US)
                if (!normalizedByKey.containsKey(key)) {
                    normalizedByKey[key] = label
                }
            }
        return normalizedByKey.values.toList()
    }

    return when {
        raw.isJsonArray -> normalize(raw.asJsonArray.asSequence().flatMap { element ->
            when {
                element.isJsonNull -> emptySequence()
                element.isJsonPrimitive -> sequenceOf(element.asString)
                element.isJsonObject -> {
                    val objectValue = element.asJsonObject
                    sequenceOf(
                        objectValue.get("name"),
                        objectValue.get("label"),
                        objectValue.get("value"),
                    )
                        .filterNotNull()
                        .filterNot { it.isJsonNull }
                        .map { it.asString }
                }

                else -> emptySequence()
            }
        })

        raw.isJsonPrimitive -> {
            val text = raw.asString.trim()
            val parsedJson = if (text.startsWith("[") && text.endsWith("]")) {
                runCatching { JsonParser.parseString(text) }.getOrNull()
            } else {
                null
            }
            if (parsedJson != null && parsedJson != raw) {
                parseTransmissionLabels(parsedJson)
            } else {
                normalize(text.split(',', ';', '|').asSequence())
            }
        }

        else -> emptyList()
    }
}

internal fun ConnectionSettings.transmissionRpcUrlCandidates(): List<String> {
    return buildTransmissionRpcUrlCandidates(baseUrlCandidates())
}

internal fun buildTransmissionRpcUrlCandidates(baseUrls: List<String>): List<String> {
    return baseUrls
        .flatMap { candidate ->
            runCatching {
                val uri = URI(candidate)
                val host = uri.host?.takeIf { it.isNotBlank() }
                    ?: uri.rawAuthority?.substringAfterLast('@')?.substringBefore(':')
                    ?: return@runCatching emptyList()
                val normalizedHost = if (host.contains(':') && !host.startsWith("[")) {
                    "[$host]"
                } else {
                    host
                }
                val port = if (uri.port > 0) uri.port else if (uri.scheme.equals("https", ignoreCase = true)) 443 else 80
                val basePath = uri.rawPath.orEmpty().trim().trimEnd('/')
                val candidatePaths = buildList {
                    if (basePath.endsWith("/rpc")) {
                        add(basePath)
                    }
                    if (basePath.isNotBlank() && !basePath.endsWith("/rpc")) {
                        add("$basePath/rpc")
                    }
                    add("/transmission/rpc")
                    add("/rpc")
                    add("/tr-control/rpc")
                    add("/tr/rpc")
                }
                candidatePaths.distinct().map { rpcPath ->
                    val normalizedPath = if (rpcPath.startsWith("/")) rpcPath else "/$rpcPath"
                    "${uri.scheme}://$normalizedHost:$port$normalizedPath"
                }
            }.getOrElse { emptyList() }
        }
        .distinct()
}

private data class RpcAttemptFailure(
    val url: String,
    val summary: String,
    val authFailure: Boolean = false,
)

private class TransmissionRpcAttemptException(
    val url: String,
    val summary: String,
    val authFailure: Boolean = false,
) : IOException(summary)

internal data class TransmissionSessionInfo(
    val version: String = "-",
    @SerializedName("download-dir") val downloadDir: String = "",
    @SerializedName("speed-limit-down-enabled") val speedLimitDownEnabled: Boolean = false,
    @SerializedName("speed-limit-down") val speedLimitDown: Int = 0,
    @SerializedName("speed-limit-up-enabled") val speedLimitUpEnabled: Boolean = false,
    @SerializedName("speed-limit-up") val speedLimitUp: Int = 0,
    @SerializedName("alt-speed-down") val altSpeedDown: Int = 0,
    @SerializedName("alt-speed-up") val altSpeedUp: Int = 0,
    @SerializedName("alt-speed-enabled") val altSpeedEnabled: Boolean = false,
    @SerializedName("alt-speed-time-enabled") val altSpeedTimeEnabled: Boolean = false,
    @SerializedName("alt-speed-time-begin") val altSpeedTimeBegin: Int = 0,
    @SerializedName("alt-speed-time-end") val altSpeedTimeEnd: Int = 0,
    @SerializedName("alt-speed-time-day") val altSpeedTimeDay: Int = 0x7F,
)

internal data class TransmissionSessionStats(
    val downloadSpeed: Long = 0,
    val uploadSpeed: Long = 0,
    val torrentCount: Int = 0,
    @SerializedName("cumulative-stats") val cumulativeStats: TransmissionCumulativeStats = TransmissionCumulativeStats(),
) {
    val cumulativeDownloadedBytes: Long
        get() = cumulativeStats.downloadedBytes

    val cumulativeUploadedBytes: Long
        get() = cumulativeStats.uploadedBytes
}

internal data class TransmissionCumulativeStats(
    val downloadedBytes: Long = 0,
    val uploadedBytes: Long = 0,
)

internal data class TransmissionTorrent(
    val hashString: String = "",
    val name: String = "",
    @SerializedName("labels") val labelsPayload: JsonElement? = null,
    val uploadRatio: Double = 0.0,
    val totalSize: Long = 0,
    val percentDone: Double = 0.0,
    val status: Int = 0,
    val rateDownload: Long = 0,
    val rateUpload: Long = 0,
    val downloadedEver: Long = 0,
    val uploadedEver: Long = 0,
    val addedDate: Long = 0,
    val activityDate: Long = 0,
    val eta: Long = 0,
    val peersSendingToUs: Int = 0,
    val peersGettingFromUs: Int = 0,
    val trackerList: String = "",
    val trackers: List<TransmissionTracker> = emptyList(),
    val trackerStats: List<TransmissionTrackerStat> = emptyList(),
    val downloadDir: String = "",
    val isFinished: Boolean = false,
    val comment: String? = null,
    val dateCreated: Long = 0,
    val pieceSize: Long = 0,
    val downloadLimited: Boolean = false,
    val downloadLimit: Int = 0,
    val uploadLimited: Boolean = false,
    val uploadLimit: Int = 0,
    val secondsDownloading: Long = 0,
    val secondsSeeding: Long = 0,
    val files: List<TransmissionFile> = emptyList(),
    val fileStats: List<TransmissionFileStat> = emptyList(),
    val magnetLink: String = "",
) {
    val labels: List<String>
        get() = parseTransmissionLabels(labelsPayload)
}

internal data class TransmissionTracker(
    val id: Int = 0,
    val announce: String = "",
    val tier: Int = 0,
)

internal data class TransmissionTrackerStat(
    val id: Int = 0,
    val announce: String = "",
    val seederCount: Int = 0,
    val leecherCount: Int = 0,
    val downloadCount: Int = 0,
    val hasAnnounced: Boolean = false,
    val lastAnnounceSucceeded: Boolean = false,
    val lastAnnounceResult: String = "",
    val lastScrapeResult: String = "",
)

internal data class TransmissionTorrentGetEnvelope(
    val result: String = "",
    val arguments: TransmissionTorrentGetArguments = TransmissionTorrentGetArguments(),
)

internal data class TransmissionTorrentGetArguments(
    val torrents: List<TransmissionTorrent> = emptyList(),
)

internal data class TransmissionFile(
    val name: String = "",
    val length: Long = 0,
    val bytesCompleted: Long = 0,
)

internal data class TransmissionFileStat(
    val priority: Int = 0,
)
