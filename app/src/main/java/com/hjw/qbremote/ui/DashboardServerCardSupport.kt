package com.hjw.qbremote.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hjw.qbremote.data.model.TorrentInfo

internal data class DashboardStateSummary(
    val uploadingCount: Int = 0,
    val downloadingCount: Int = 0,
    val pausedUploadCount: Int = 0,
    val pausedDownloadCount: Int = 0,
    val errorCount: Int = 0,
    val checkingCount: Int = 0,
    val waitingCount: Int = 0,
)

internal data class DashboardStatusPillItem(
    val label: String,
    val count: Int,
    val accentColor: Color,
)

internal val HomeServerStackExpandedCardHeight = 210.dp
internal val HomeServerStackCollapsedCardHeight = 90.dp
internal val HomeServerStackExposedHeight = 60.dp
internal val DashboardCardSpacing = 10.dp
internal const val ServerCardClickSuppressionWindowMs = 140L
internal val ServerCardClickSuppressionDragThreshold = 6.dp

internal fun resolveServerCardClickSuppressionTimestamp(
    dragDistanceSinceStart: Float,
    clickSuppressionThresholdPx: Float,
    currentTimeMillis: Long,
): Long {
    return if (dragDistanceSinceStart >= clickSuppressionThresholdPx) {
        currentTimeMillis
    } else {
        0L
    }
}

internal fun shouldSuppressServerCardClick(
    lastDragFinishedAt: Long,
    currentTimeMillis: Long,
    suppressionWindowMs: Long = ServerCardClickSuppressionWindowMs,
): Boolean {
    if (lastDragFinishedAt <= 0L) return false
    return currentTimeMillis - lastDragFinishedAt <= suppressionWindowMs
}

internal data class WalletServerStackCardPresentation(
    val showExpandedLayout: Boolean,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val borderAlpha: Float,
)

internal fun resolveWalletServerStackCardPresentation(
    selected: Boolean,
    isDragging: Boolean,
    isSettling: Boolean,
): WalletServerStackCardPresentation {
    val showExpandedLayout = selected || isDragging || isSettling
    return WalletServerStackCardPresentation(
        showExpandedLayout = showExpandedLayout,
        horizontalPadding = if (showExpandedLayout) 16.dp else 15.dp,
        verticalPadding = if (showExpandedLayout) 14.dp else 10.dp,
        borderAlpha = if (showExpandedLayout) 0.28f else 0.14f,
    )
}

internal fun buildDashboardStateSummary(torrents: List<TorrentInfo>): DashboardStateSummary {
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
