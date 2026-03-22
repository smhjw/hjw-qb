package com.hjw.qbremote.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.hjw.qbremote.R
import com.hjw.qbremote.data.HomeAggregateSpeedHistorySnapshot
import com.hjw.qbremote.data.HomeSpeedHistoryPoint
import com.hjw.qbremote.data.model.CountryUploadRecord
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.ui.theme.qbGlassCardColors
import com.hjw.qbremote.ui.theme.qbGlassEmptyStateColor
import com.hjw.qbremote.ui.theme.qbGlassHoleColor
import com.hjw.qbremote.ui.theme.qbGlassOutlineColor
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@Composable
fun CategorySharePieCard(
    torrents: List<TorrentInfo>,
    coverageNote: String = "",
    showHideButton: Boolean,
    onRevealHide: () -> Unit,
    onHide: () -> Unit,
) {
    val noCategoryLabel = stringResource(R.string.no_category)
    val otherLabel = stringResource(R.string.chart_other_label)
    val entries = remember(torrents, noCategoryLabel, otherLabel) {
        collapsePieEntries(
            entries = buildCategoryShareEntries(
                torrents = torrents,
                noCategoryLabel = noCategoryLabel,
            ),
            maxEntries = 7,
            otherLabel = otherLabel,
        )
    }.map { (label, count) ->
        val torrentCount = count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        PieLegendEntry(
            label = label,
            value = count,
            valueText = pluralStringResource(
                R.plurals.chart_category_count,
                torrentCount,
                torrentCount,
            ),
        )
    }

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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DashboardCardHeader(
                title = stringResource(R.string.dashboard_category_share_title),
                showHideButton = showHideButton,
                onRevealHide = onRevealHide,
                onHide = onHide,
            )

            if (entries.isEmpty()) {
                DashboardChartEmptyState(
                    text = stringResource(R.string.chart_no_data),
                )
                return@Column
            }

            val total = entries.sumOf { it.value }.coerceAtLeast(1L)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DashboardPieChart(
                    entries = entries,
                    total = total,
                    holeColor = qbGlassHoleColor(),
                    modifier = Modifier.size(132.dp),
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    entries.forEachIndexed { index, entry ->
                        val color = DashboardPiePalette[index % DashboardPiePalette.size]
                        val share = (entry.value.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        CategoryLegendRow(
                            color = color,
                            label = entry.label,
                            shareText = formatPercent(share),
                            valueText = entry.valueText,
                        )
                    }
                }
            }
            if (coverageNote.isNotBlank()) {
                Text(
                    text = coverageNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun CountryFlowMapCard(
    stats: List<CountryUploadRecord>,
    coverageNote: String = "",
    showHideButton: Boolean,
    onRevealHide: () -> Unit,
    onHide: () -> Unit,
) {
    val locale = LocalContext.current.resources.configuration.locales[0] ?: Locale.getDefault()
    val displayStats = remember(stats) { mergeCountryUploadRecordsForDisplay(stats) }
    val topCountries = remember(displayStats) { displayStats.take(3) }
    val emptyText = stringResource(R.string.dashboard_country_flow_empty)

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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DashboardCardHeader(
                title = stringResource(R.string.dashboard_country_flow_title),
                showHideButton = showHideButton,
                onRevealHide = onRevealHide,
                onHide = onHide,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(188.dp),
                contentAlignment = Alignment.Center,
            ) {
                WorldMapChart(
                    countries = displayStats,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(top = 14.dp, bottom = 8.dp)
                        .height(172.dp),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (topCountries.isEmpty()) {
                    DashboardInlineEmptyState(text = emptyText)
                } else {
                    topCountries.forEachIndexed { index, entry ->
                        if (index > 0) {
                            Spacer(modifier = Modifier.width(14.dp))
                        }
                        Text(
                            text = buildString {
                                append(
                                    compactCountryLabelForDisplay(
                                        countryCode = entry.countryCode,
                                        fallbackName = entry.countryName,
                                        locale = locale,
                                    )
                                )
                                append(formatUploadAmountInMbOrGb(entry.uploadedBytes))
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.dashboard_country_flow_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (coverageNote.isNotBlank()) {
                Text(
                    text = coverageNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun DailyTagUploadPieCard(
    date: String,
    stats: List<DailyTagUploadStat>,
    titleOverride: String? = null,
    showHideButton: Boolean,
    onRevealHide: () -> Unit,
    onHide: () -> Unit,
) {
    val noTagLabel = stringResource(R.string.no_tags)
    val otherLabel = stringResource(R.string.chart_other_label)
    val rawEntries = remember(stats, noTagLabel) {
        stats
            .filter { it.uploadedBytes > 0L }
            .map { stat ->
                val tagLabel = if (stat.isNoTag) noTagLabel else stat.tag
                tagLabel to stat.uploadedBytes
            }
    }
    val collapsed = remember(rawEntries, otherLabel) {
        collapsePieEntries(
            entries = rawEntries,
            maxEntries = 7,
            otherLabel = otherLabel,
        )
    }
    val entries = collapsed.map { (label, uploadedBytes) ->
        PieLegendEntry(
            label = label,
            value = uploadedBytes,
            valueText = formatBytes(uploadedBytes),
        )
    }
    val totalUploaded = entries.sumOf { it.value }

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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DashboardCardHeader(
                title = titleOverride ?: if (date.isNotBlank()) {
                    stringResource(R.string.dashboard_upload_title_with_date, date)
                } else {
                    stringResource(R.string.dashboard_upload_title)
                },
                showHideButton = showHideButton,
                onRevealHide = onRevealHide,
                onHide = onHide,
            )

            if (entries.isEmpty()) {
                DashboardInlineEmptyState(
                    text = stringResource(R.string.dashboard_daily_tag_upload_empty),
                )
                return@Column
            }

            val total = totalUploaded.coerceAtLeast(1L)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DashboardPieChart(
                    entries = entries,
                    total = total,
                    holeColor = qbGlassHoleColor(),
                    modifier = Modifier.size(132.dp),
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    entries.forEachIndexed { index, entry ->
                        val color = DashboardPiePalette[index % DashboardPiePalette.size]
                        val share = (entry.value.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        DailyUploadLegendRow(
                            color = color,
                            label = entry.label,
                            shareText = formatPercent(share),
                            valueText = entry.valueText,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InlineRealtimeSpeedChart(
    aggregate: DashboardAggregateState,
    modifier: Modifier = Modifier,
) {
    val axisValues = remember(aggregate.realtimeSpeedSeries, aggregate.transferInfo) {
        buildRealtimeAxisValues(
            values = buildList {
                add(aggregate.transferInfo.uploadSpeed.coerceAtLeast(0L))
                add(aggregate.transferInfo.downloadSpeed.coerceAtLeast(0L))
                aggregate.realtimeSpeedSeries.forEach { point ->
                    add(point.uploadSpeed.coerceAtLeast(0L))
                    add(point.downloadSpeed.coerceAtLeast(0L))
                }
            },
        )
    }
    if (aggregate.realtimeSpeedSeries.size < 2) return
    RealtimeSpeedChart(
        modifier = modifier,
        points = aggregate.realtimeSpeedSeries,
        axisValues = axisValues,
        uploadColor = Color(0xFFFF4B8B),
        downloadColor = Color(0xFF5E7CFF),
    )
}

@Composable
private fun RealtimeSpeedChart(
    modifier: Modifier = Modifier,
    points: List<RealtimeSpeedPoint>,
    axisValues: List<Long>,
    uploadColor: Color,
    downloadColor: Color,
) {
    val chartHeight = 168.dp
    val downloadValues = remember(points) { points.map { it.downloadSpeed.coerceAtLeast(0L) } }
    val uploadValues = remember(points) { points.map { it.uploadSpeed.coerceAtLeast(0L) } }
    val gridColor = qbGlassOutlineColor(defaultAlpha = 0.14f)
    val baselineColor = qbGlassOutlineColor(defaultAlpha = 0.18f)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 74.dp)
                .height(chartHeight)
                .padding(top = 2.dp, end = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            axisValues.forEach { axisValue ->
                Text(
                    text = formatSpeed(axisValue),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(chartHeight)
                .background(
                    color = qbGlassHoleColor(),
                    shape = RoundedCornerShape(18.dp),
                )
                .border(
                    width = 1.dp,
                    color = qbGlassOutlineColor(defaultAlpha = 0.18f),
                    shape = RoundedCornerShape(18.dp),
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val safeAxisMax = axisValues.firstOrNull()?.coerceAtLeast(1L)?.toFloat() ?: 1f
                val dashEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    intervals = floatArrayOf(12f, 10f),
                    phase = 0f,
                )

                for (index in 0..3) {
                    val y = size.height * (index / 3f)
                    drawLine(
                        color = if (index == 3) baselineColor else gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = if (index == 3) null else dashEffect,
                    )
                }

                drawRealtimeSeriesLine(
                    values = downloadValues,
                    color = downloadColor,
                    axisMax = safeAxisMax,
                )
                drawRealtimeSeriesLine(
                    values = uploadValues,
                    color = uploadColor,
                    axisMax = safeAxisMax,
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRealtimeSeriesLine(
    values: List<Long>,
    color: Color,
    axisMax: Float,
) {
    if (values.size < 2) return
    val stepX = if (values.lastIndex <= 0) 0f else size.width / values.lastIndex.toFloat()
    val points = values.mapIndexed { index, rawValue ->
        val x = stepX * index
        val normalized = (rawValue.coerceAtLeast(0L).toFloat() / axisMax).coerceIn(0f, 1f)
        val y = size.height - (size.height * normalized)
        Offset(x, y.coerceIn(0f, size.height))
    }
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (index in 0 until points.lastIndex) {
            val previous = points.getOrElse(index - 1) { points[index] }
            val current = points[index]
            val next = points[index + 1]
            val afterNext = points.getOrElse(index + 2) { next }
            val control1 = Offset(
                x = current.x + (next.x - previous.x) / 6f,
                y = (current.y + (next.y - previous.y) / 6f).coerceIn(0f, size.height),
            )
            val control2 = Offset(
                x = next.x - (afterNext.x - current.x) / 6f,
                y = (next.y - (afterNext.y - current.y) / 6f).coerceIn(0f, size.height),
            )
            cubicTo(
                control1.x,
                control1.y,
                control2.x,
                control2.y,
                next.x,
                next.y,
            )
        }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

private fun buildRealtimeAxisValues(values: List<Long>): List<Long> {
    val axisMax = roundRealtimeAxisMax(values.maxOrNull()?.coerceAtLeast(1L) ?: 1L)
    return listOf(
        axisMax,
        (axisMax * 2L) / 3L,
        axisMax / 3L,
        0L,
    )
}

private fun roundRealtimeAxisMax(value: Long): Long {
    if (value <= 1L) return 1L
    var magnitude = 1L
    while (magnitude <= Long.MAX_VALUE / 10L && magnitude * 10L < value) {
        magnitude *= 10L
    }
    val normalized = value.toDouble() / magnitude.toDouble()
    val rounded = when {
        normalized <= 1.0 -> 1L
        normalized <= 2.0 -> 2L
        normalized <= 5.0 -> 5L
        else -> 10L
    }
    return rounded * magnitude
}

@Composable
fun TransmissionLabelCategoryPieCard(
    torrents: List<TorrentInfo>,
    showHideButton: Boolean,
    onRevealHide: () -> Unit,
    onHide: () -> Unit,
) {
    val noTagLabel = stringResource(R.string.no_tags)
    val otherLabel = stringResource(R.string.chart_other_label)
    val entries = remember(torrents, noTagLabel, otherLabel) {
        collapsePieEntries(
            entries = buildTransmissionLabelShareEntries(
                torrents = torrents,
                noTagLabel = noTagLabel,
            ),
            maxEntries = 7,
            otherLabel = otherLabel,
        )
    }.map { (label, count) ->
        val torrentCount = count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        PieLegendEntry(
            label = label,
            value = count,
            valueText = pluralStringResource(
                R.plurals.chart_category_count,
                torrentCount,
                torrentCount,
            ),
        )
    }

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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DashboardCardHeader(
                title = stringResource(R.string.dashboard_category_share_title),
                showHideButton = showHideButton,
                onRevealHide = onRevealHide,
                onHide = onHide,
            )

            if (entries.isEmpty()) {
                DashboardChartEmptyState(
                    text = stringResource(R.string.chart_no_data),
                )
                return@Column
            }

            val total = entries.sumOf { it.value }.coerceAtLeast(1L)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DashboardPieChart(
                    entries = entries,
                    total = total,
                    holeColor = qbGlassHoleColor(),
                    modifier = Modifier.size(142.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    entries.forEachIndexed { index, entry ->
                        val color = DashboardPiePalette[index % DashboardPiePalette.size]
                        val share = entry.value.toFloat() / total.toFloat()
                        CategoryLegendRow(
                            color = color,
                            label = entry.label,
                            shareText = formatPercent(share),
                            valueText = entry.valueText,
                        )
                    }
                }
            }
        }
    }
}

private enum class TransmissionStateGroup {
    UPLOADING,
    DOWNLOADING,
    PAUSED,
    QUEUED,
    CHECKING,
    COMPLETED,
    ERROR,
    UNKNOWN,
}

@Composable
fun TransmissionStatePieCard(
    torrents: List<TorrentInfo>,
    showHideButton: Boolean,
    onRevealHide: () -> Unit,
    onHide: () -> Unit,
) {
    val uploadingLabel = stringResource(R.string.status_uploading)
    val downloadingLabel = stringResource(R.string.status_downloading)
    val pausedLabel = stringResource(R.string.status_paused)
    val queuedLabel = stringResource(R.string.state_queued)
    val checkingLabel = stringResource(R.string.status_checking)
    val completedLabel = stringResource(R.string.status_completed)
    val errorLabel = stringResource(R.string.status_error)
    val unknownLabel = stringResource(R.string.state_unknown)
    val otherLabel = stringResource(R.string.chart_other_label)
    val rawEntries = remember(
        torrents,
        uploadingLabel,
        downloadingLabel,
        pausedLabel,
        queuedLabel,
        checkingLabel,
        completedLabel,
        errorLabel,
        unknownLabel,
    ) {
        val grouped = torrents.groupingBy(::transmissionStateGroupOf).eachCount()
        listOf(
            uploadingLabel to grouped[TransmissionStateGroup.UPLOADING],
            downloadingLabel to grouped[TransmissionStateGroup.DOWNLOADING],
            pausedLabel to grouped[TransmissionStateGroup.PAUSED],
            queuedLabel to grouped[TransmissionStateGroup.QUEUED],
            checkingLabel to grouped[TransmissionStateGroup.CHECKING],
            completedLabel to grouped[TransmissionStateGroup.COMPLETED],
            errorLabel to grouped[TransmissionStateGroup.ERROR],
            unknownLabel to grouped[TransmissionStateGroup.UNKNOWN],
        ).mapNotNull { (label, count) ->
            count?.takeIf { it > 0 }?.let { label to it.toLong() }
        }
    }
    val collapsed = remember(rawEntries, otherLabel) {
        collapsePieEntries(
            entries = rawEntries,
            maxEntries = 7,
            otherLabel = otherLabel,
        )
    }
    val entries = collapsed.map { (label, count) ->
        val torrentCount = count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        PieLegendEntry(
            label = label,
            value = count,
            valueText = pluralStringResource(
                R.plurals.chart_category_count,
                torrentCount,
                torrentCount,
            ),
        )
    }

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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DashboardCardHeader(
                title = stringResource(R.string.dashboard_torrent_state_share_title),
                showHideButton = showHideButton,
                onRevealHide = onRevealHide,
                onHide = onHide,
            )

            if (entries.isEmpty()) {
                DashboardInlineEmptyState(text = stringResource(R.string.chart_no_data))
                return@Column
            }

            val total = entries.sumOf { it.value }.coerceAtLeast(1L)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DashboardPieChart(
                    entries = entries,
                    total = total,
                    holeColor = qbGlassHoleColor(),
                    modifier = Modifier.size(132.dp),
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    entries.forEachIndexed { index, entry ->
                        val color = DashboardPiePalette[index % DashboardPiePalette.size]
                        val share = (entry.value.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        DailyUploadLegendRow(
                            color = color,
                            label = entry.label,
                            shareText = formatPercent(share),
                            valueText = entry.valueText,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransmissionTrackerSitePieCard(
    torrents: List<TorrentInfo>,
    showHideButton: Boolean,
    onRevealHide: () -> Unit,
    onHide: () -> Unit,
) {
    val unknownSiteLabel = stringResource(R.string.dashboard_tracker_site_unknown)
    val otherLabel = stringResource(R.string.chart_other_label)
    val rawEntries = remember(torrents, unknownSiteLabel) {
        torrents
            .groupingBy { torrent -> transmissionTrackerSiteLabel(torrent.tracker, unknownSiteLabel) }
            .eachCount()
            .mapNotNull { (site, count) ->
                count.takeIf { it > 0 }?.let { site to it.toLong() }
            }
    }
    val collapsed = remember(rawEntries, otherLabel) {
        collapsePieEntries(
            entries = rawEntries,
            maxEntries = 7,
            otherLabel = otherLabel,
        )
    }
    val entries = collapsed.map { (label, count) ->
        val torrentCount = count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        PieLegendEntry(
            label = label,
            value = count,
            valueText = pluralStringResource(
                R.plurals.chart_category_count,
                torrentCount,
                torrentCount,
            ),
        )
    }

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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DashboardCardHeader(
                title = stringResource(R.string.dashboard_tracker_site_share_title),
                showHideButton = showHideButton,
                onRevealHide = onRevealHide,
                onHide = onHide,
            )

            if (entries.isEmpty()) {
                DashboardInlineEmptyState(text = stringResource(R.string.chart_no_data))
                return@Column
            }

            val total = entries.sumOf { it.value }.coerceAtLeast(1L)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DashboardPieChart(
                    entries = entries,
                    total = total,
                    holeColor = qbGlassHoleColor(),
                    modifier = Modifier.size(132.dp),
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    entries.forEachIndexed { index, entry ->
                        val color = DashboardPiePalette[index % DashboardPiePalette.size]
                        val share = (entry.value.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        DailyUploadLegendRow(
                            color = color,
                            label = entry.label,
                            shareText = formatPercent(share),
                            valueText = entry.valueText,
                        )
                    }
                }
            }
        }
    }
}

private fun transmissionStateGroupOf(torrent: TorrentInfo): TransmissionStateGroup {
    val state = normalizeTorrentState(effectiveTorrentState(torrent))
    return when {
        state in setOf("uploading", "forcedup") -> TransmissionStateGroup.UPLOADING
        state == "stalledup" || (torrent.progress >= 1f && state in setOf("pausedup", "stoppedup")) ->
            TransmissionStateGroup.COMPLETED
        state in setOf("downloading", "forceddl", "stalleddl", "metadl", "forcedmetadl", "allocating", "moving") ->
            TransmissionStateGroup.DOWNLOADING
        state in setOf("checkingdl", "checkingup", "checkingresumedata") ->
            TransmissionStateGroup.CHECKING
        state in setOf("queueddl", "queuedup") -> TransmissionStateGroup.QUEUED
        state in setOf("pauseddl", "pausedup", "stoppeddl", "stoppedup") ->
            if (torrent.progress >= 1f) TransmissionStateGroup.COMPLETED else TransmissionStateGroup.PAUSED
        state in setOf("error", "missingfiles") -> TransmissionStateGroup.ERROR
        torrent.progress >= 1f -> TransmissionStateGroup.COMPLETED
        else -> TransmissionStateGroup.UNKNOWN
    }
}

private fun transmissionTrackerSiteLabel(
    trackerUrl: String,
    unknownLabel: String,
): String {
    return formatTrackerSiteName(trackerUrl, unknownLabel)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardCardHeader(
    title: String,
    showHideButton: Boolean,
    onRevealHide: () -> Unit,
    onHide: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {},
                    onDoubleClick = onRevealHide,
                ),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showHideButton) {
            TextButton(
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                onClick = onHide,
            ) {
                Text(text = stringResource(R.string.hide), maxLines = 1)
            }
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }
    }
}

@Composable
fun ReorderableDashboardCard(
    card: DashboardChartCard,
    gestureKey: Any,
    isDragging: Boolean,
    dragOffsetY: Float,
    siblingOffsetY: Float,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onMeasured: (Int) -> Unit,
    content: @Composable () -> Unit,
) {
    val draggedScale by animateFloatAsState(
        targetValue = if (isDragging) ReorderDraggedScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.84f,
            stiffness = 520f,
        ),
        label = "dashboardDraggedScale",
    )
    val animatedSiblingOffset by animateFloatAsState(
        targetValue = siblingOffsetY,
        animationSpec = spring(
            dampingRatio = 0.84f,
            stiffness = 520f,
        ),
        label = "dashboardSiblingOffset",
    )
    val latestOnDragStart by rememberUpdatedState(onDragStart)
    val latestOnDragDelta by rememberUpdatedState(onDragDelta)
    val latestOnDragEnd by rememberUpdatedState(onDragEnd)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { onMeasured(it.height) }
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                translationY = if (isDragging) dragOffsetY else animatedSiblingOffset
                shadowElevation = if (isDragging) ReorderDraggedShadow else 0f
                scaleX = draggedScale
                scaleY = draggedScale
                alpha = 1f
                shape = PanelShape
                clip = true
            }
            .pointerInput(card, gestureKey) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { latestOnDragStart() },
                    onDragEnd = { latestOnDragEnd() },
                    onDragCancel = { latestOnDragEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        latestOnDragDelta(dragAmount.y)
                    },
                )
            },
    ) {
        content()
    }
}

@Composable
fun DashboardHomeSkeleton(
    showCharts: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DashboardSkeletonCard(
            headerWidthFraction = 0.34f,
            bodyHeight = 118.dp,
        )
        if (showCharts) {
            DashboardSkeletonCard(
                headerWidthFraction = 0.42f,
                bodyHeight = 188.dp,
            )
            DashboardSkeletonCard(
                headerWidthFraction = 0.3f,
                bodyHeight = 172.dp,
            )
        }
    }
}

@Composable
private fun DashboardSkeletonCard(
    headerWidthFraction: Float,
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
                    .fillMaxWidth(headerWidthFraction)
                    .height(16.dp)
                    .background(
                        color = qbGlassEmptyStateColor(),
                        shape = RoundedCornerShape(999.dp),
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bodyHeight)
                    .background(
                        color = qbGlassHoleColor(),
                        shape = RoundedCornerShape(18.dp),
                    )
            )
        }
    }
}

@Composable
fun DashboardInlineEmptyState(
    text: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun PieLegendCard(
    title: String?,
    entries: List<PieLegendEntry>,
    emptyText: String,
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
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (entries.isEmpty()) {
                Text(
                    text = emptyText,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                return@Column
            }

            val total = entries.sumOf { it.value }.coerceAtLeast(1L)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DashboardPieChart(
                    entries = entries,
                    total = total,
                    holeColor = qbGlassHoleColor(),
                    modifier = Modifier.size(150.dp),
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    entries.forEachIndexed { index, entry ->
                        val color = DashboardPiePalette[index % DashboardPiePalette.size]
                        val share = (entry.value.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        PieLegendRow(
                            color = color,
                            label = entry.label,
                            shareText = formatPercent(share),
                            valueText = entry.valueText,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardChartEmptyState(
    text: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 116.dp)
            .background(
                color = qbGlassEmptyStateColor(),
                shape = RoundedCornerShape(18.dp),
            )
            .border(
                width = 1.dp,
                color = qbGlassOutlineColor(defaultAlpha = 0.18f),
                shape = RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun DashboardPieChart(
    entries: List<PieLegendEntry>,
    total: Long,
    holeColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val diameter = size.minDimension
        val topLeft = Offset(
            x = (size.width - diameter) / 2f,
            y = (size.height - diameter) / 2f,
        )
        val arcSize = Size(width = diameter, height = diameter)

        var startAngle = -90f
        entries.forEachIndexed { index, entry ->
            val sweepAngle = (entry.value.toFloat() / total.toFloat()) * 360f
            if (sweepAngle <= 0f) return@forEachIndexed
            drawArc(
                color = DashboardPiePalette[index % DashboardPiePalette.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = topLeft,
                size = arcSize,
            )
            startAngle += sweepAngle
        }

        drawCircle(
            color = holeColor,
            radius = diameter * 0.30f,
            center = Offset(size.width / 2f, size.height / 2f),
        )
    }
}

@Composable
fun PieLegendRow(
    color: Color,
    label: String,
    shareText: String,
    valueText: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = color, shape = RoundedCornerShape(50)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = shareText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun DailyUploadLegendRow(
    color: Color,
    label: String,
    shareText: String,
    valueText: String,
) {
    DashboardLegendRow(
        color = color,
        label = label,
        valueText = valueText,
        shareText = shareText,
        shareColor = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun CategoryLegendRow(
    color: Color,
    label: String,
    shareText: String,
    valueText: String,
) {
    DashboardLegendRow(
        color = color,
        label = label,
        valueText = valueText,
        shareText = shareText,
        shareColor = MaterialTheme.colorScheme.secondary,
    )
}

@Composable
fun DashboardLegendRow(
    color: Color,
    label: String,
    valueText: String,
    shareText: String,
    shareColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .size(9.dp)
                .background(color = color, shape = RoundedCornerShape(50)),
        )
        Column(
            modifier = Modifier
                .padding(start = 6.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = shareText,
            modifier = Modifier.widthIn(min = 64.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = shareColor,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.End,
        )
    }
}

fun buildCategoryShareEntries(
    torrents: List<TorrentInfo>,
    noCategoryLabel: String,
): List<Pair<String, Long>> {
    val grouped = mutableMapOf<String, Long>()
    torrents.forEach { torrent ->
        val label = normalizeCategoryLabel(
            category = torrent.category,
            noCategoryText = noCategoryLabel,
        )
        grouped[label] = (grouped[label] ?: 0L) + 1L
    }
    return grouped.entries
        .sortedByDescending { it.value }
        .map { it.key to it.value }
}

fun buildTransmissionLabelShareEntries(
    torrents: List<TorrentInfo>,
    noTagLabel: String,
): List<Pair<String, Long>> {
    val grouped = linkedMapOf<String, Long>()
    torrents.forEach { torrent ->
        val tags = parseTags(torrent.tags)
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "-" && !it.equals("null", ignoreCase = true) }
            .distinctBy { it.lowercase() }

        if (tags.isEmpty()) {
            grouped[noTagLabel] = (grouped[noTagLabel] ?: 0L) + 1L
        } else {
            tags.forEach { tag ->
                grouped[tag] = (grouped[tag] ?: 0L) + 1L
            }
        }
    }
    return grouped.entries
        .sortedByDescending { it.value }
        .map { it.key to it.value }
}

fun collapsePieEntries(
    entries: List<Pair<String, Long>>,
    maxEntries: Int,
    otherLabel: String,
): List<Pair<String, Long>> {
    if (entries.isEmpty()) return emptyList()
    if (entries.size <= maxEntries) return entries

    val safeMax = maxEntries.coerceAtLeast(2)
    val head = entries.take(safeMax - 1)
    val otherValue = entries.drop(safeMax - 1).sumOf { it.second }
    return if (otherValue > 0L) {
        head + listOf(otherLabel to otherValue)
    } else {
        head
    }
}

fun normalizeCategoryLabel(category: String, noCategoryText: String): String {
    val normalized = category.trim()
    if (normalized.isBlank()) return noCategoryText
    if (normalized == "-" || normalized.equals("null", ignoreCase = true)) return noCategoryText
    return normalized
}
