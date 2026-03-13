package com.hjw.qbremote.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hjw.qbremote.R
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.data.model.TorrentTracker
import com.hjw.qbremote.ui.theme.qbGlassOutlineColor
import com.hjw.qbremote.ui.theme.qbGlassSubtleContainerColor

@Composable
internal fun TrackerInfoCard(tracker: TorrentTracker) {
    val status = trackerStatusLabel(tracker.status)
    val statusColor = trackerStatusColor(tracker.status)
    val message = tracker.message.trim().ifBlank {
        stringResource(R.string.tracker_message_ok)
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, qbGlassOutlineColor(defaultAlpha = 0.28f)),
        colors = CardDefaults.outlinedCardColors(
            containerColor = qbGlassSubtleContainerColor(),
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
internal fun trackerStatusLabel(status: Int): String {
    return when (status) {
        0 -> stringResource(R.string.tracker_status_disabled)
        1 -> stringResource(R.string.tracker_status_not_contacted)
        2 -> stringResource(R.string.tracker_status_working)
        3 -> stringResource(R.string.tracker_status_updating)
        4 -> stringResource(R.string.tracker_status_not_working)
        else -> stringResource(R.string.state_unknown)
    }
}

internal fun trackerStatusColor(status: Int): Color {
    return when (status) {
        0 -> Color(0xFF9E9E9E)
        1 -> Color(0xFF90A4AE)
        2 -> Color(0xFF4CAF50)
        3 -> Color(0xFFFFC107)
        4 -> Color(0xFFE53935)
        else -> Color(0xFF607D8B)
    }
}

internal fun parseTags(input: String): List<String> {
    return input
        .split(',', ';', '|')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

internal fun matchesTorrentSearch(torrent: TorrentInfo, query: String): Boolean {
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

internal fun toggleTag(current: String, option: String): String {
    val tags = parseTags(current).toMutableList()
    val idx = tags.indexOfFirst { it.equals(option, ignoreCase = false) }
    if (idx >= 0) {
        tags.removeAt(idx)
    } else {
        tags.add(option)
    }
    return tags.joinToString(",")
}

internal data class TorrentStateStyle(
    val borderColor: Color,
    val progressColor: Color,
    val tagContainer: Color,
    val tagContent: Color,
)

@Composable
internal fun torrentStateStyle(state: String): TorrentStateStyle {
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
internal fun TorrentStateTag(
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
internal fun TorrentMetaChip(
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
internal fun TorrentInfoCell(
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
                color = qbGlassSubtleContainerColor(),
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
internal fun TorrentQuickStatsRow(
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
internal fun localizedTorrentStateLabel(state: String): String {
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

internal fun isPausedState(state: String): Boolean {
    return normalizeTorrentState(state) in setOf("pauseddl", "pausedup", "stoppeddl", "stoppedup")
}

internal fun effectiveTorrentState(torrent: TorrentInfo): String {
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

internal fun normalizeTorrentState(state: String): String {
    return state.trim().lowercase()
}

internal fun compactTagsLabel(tags: String, noTagsText: String): String {
    val normalizedTags = tags
        .split(',', ';', '|')
        .map { it.trim() }
        .filter { it.isNotBlank() && it != "-" && !it.equals("null", ignoreCase = true) }

    if (normalizedTags.isEmpty()) return noTagsText
    if (normalizedTags.size <= 2) return normalizedTags.joinToString(",")

    val preview = normalizedTags.take(2).joinToString(",")
    return "$preview +${normalizedTags.size - 2}"
}

@Composable
internal fun TorrentMetaHeaderRow(
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
internal fun TorrentTimestampLabel(
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
