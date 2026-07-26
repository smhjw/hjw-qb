package com.hjw.qbremote.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.hjw.qbremote.R
import com.hjw.qbremote.data.model.CountryUploadRecord
import com.hjw.qbremote.data.model.TransferInfo
import com.hjw.qbremote.ui.theme.qbGlassCardColors
import com.hjw.qbremote.ui.theme.qbGlassEmptyStateColor
import com.hjw.qbremote.ui.theme.qbGlassHoleColor
import com.hjw.qbremote.ui.theme.qbGlassOutlineColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

@Composable
internal fun CountryFlowMapCard(
    stats: List<CountryUploadRecord>,
    showHideButton: Boolean,
    onRevealHide: () -> Unit,
    onHide: () -> Unit,
) {
    val displayStats = remember(stats) { mergeCountryUploadRecordsForDisplay(stats) }
    val emptyText = stringResource(R.string.dashboard_country_flow_empty)
    val topSummaryEntries = remember(displayStats) {
        trimDashboardBarEntries(
            entries = displayStats.map { record ->
                DashboardBarSeedEntry(
                    label = LegendLabelSpec.Raw(record.countryName.ifBlank { record.countryCode }),
                    value = record.uploadedBytes,
                    valueKind = LegendValueKind.BYTES,
                )
            },
            maxEntries = 3,
        )
    }
    val resolvedTopEntries = resolveDashboardBarEntries(topSummaryEntries)
    val compactSummaryItems = remember(resolvedTopEntries) {
        buildCompactCountrySummaryItems(resolvedTopEntries)
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

            if (resolvedTopEntries.isEmpty()) {
                DashboardInlineEmptyState(text = emptyText)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    compactSummaryItems.forEach { item ->
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = item.labelText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (item.valueText.isNotBlank()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = item.valueText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
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
    val chartTransferInfo = aggregate.chartTransferInfo ?: aggregate.transferInfo
    val resolvedPoints = remember(aggregate.realtimeSpeedSeries, chartTransferInfo) {
        resolveRealtimeChartPoints(
            series = aggregate.realtimeSpeedSeries,
            transferInfo = chartTransferInfo,
        )
    }
    val axisValues = remember(resolvedPoints, chartTransferInfo) {
        buildRealtimeAxisValues(
            values = buildRealtimeAxisInputValues(
                points = resolvedPoints,
                transferInfo = chartTransferInfo,
            ),
        )
    }
    RealtimeSpeedChart(
        modifier = modifier,
        points = resolvedPoints,
        axisValues = axisValues,
        uploadColor = Color(0xFFFF4B8B),
        downloadColor = Color(0xFF5E7CFF),
    )
}

internal fun resolveRealtimeChartPoints(
    series: List<RealtimeSpeedPoint>,
    transferInfo: TransferInfo,
): List<RealtimeSpeedPoint> {
    val sanitizedSeries = series.map { point ->
        point.copy(
            uploadSpeed = point.uploadSpeed.coerceAtLeast(0L),
            downloadSpeed = point.downloadSpeed.coerceAtLeast(0L),
        )
    }
    val transferPoint = RealtimeSpeedPoint(
        timestamp = sanitizedSeries.lastOrNull()?.timestamp?.let { timestamp ->
            if (timestamp == Long.MAX_VALUE) timestamp else timestamp + 1L
        } ?: 0L,
        uploadSpeed = transferInfo.uploadSpeed.coerceAtLeast(0L),
        downloadSpeed = transferInfo.downloadSpeed.coerceAtLeast(0L),
        onlineServerCount = sanitizedSeries.lastOrNull()?.onlineServerCount ?: 0,
    )

    val merged = when {
        sanitizedSeries.isEmpty() -> listOf(transferPoint)
        sanitizedSeries.last().uploadSpeed == transferPoint.uploadSpeed &&
            sanitizedSeries.last().downloadSpeed == transferPoint.downloadSpeed -> sanitizedSeries
        else -> sanitizedSeries + transferPoint
    }
    if (merged.size >= 2) return merged

    val stablePoint = merged.firstOrNull() ?: transferPoint
    val nextTimestamp = if (stablePoint.timestamp == Long.MAX_VALUE) {
        Long.MAX_VALUE
    } else {
        stablePoint.timestamp + 1L
    }
    return listOf(stablePoint, stablePoint.copy(timestamp = nextTimestamp))
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

internal fun buildRealtimeAxisInputValues(
    points: List<RealtimeSpeedPoint>,
    transferInfo: TransferInfo,
): List<Long> {
    return buildList(capacity = points.size * 2 + 2) {
        add(transferInfo.uploadSpeed.coerceAtLeast(0L))
        add(transferInfo.downloadSpeed.coerceAtLeast(0L))
        points.forEach { point ->
            add(point.uploadSpeed.coerceAtLeast(0L))
            add(point.downloadSpeed.coerceAtLeast(0L))
        }
    }
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
    if (value <= 3L) return 3L
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DashboardCardHeader(
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
    isSettling: Boolean,
    dragOffsetY: () -> Float,
    settlingOffsetY: () -> Float,
    siblingOffsetY: Float,
    animateSiblingOffset: Boolean,
    lockedHeightPx: Int?,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onMeasured: (Int) -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val animatedSiblingOffset by animateFloatAsState(
        targetValue = siblingOffsetY,
        animationSpec = if (animateSiblingOffset) {
            ReorderSiblingOffsetAnimationSpec
        } else {
            snap()
        },
        label = "dashboardSiblingOffset",
    )
    val latestOnDragStart by rememberUpdatedState(onDragStart)
    val latestOnDragDelta by rememberUpdatedState(onDragDelta)
    val latestOnDragEnd by rememberUpdatedState(onDragEnd)
    val latestOnDragCancel by rememberUpdatedState(onDragCancel)
    val latestIsDragging by rememberUpdatedState(isDragging)
    // pointerInput restarts on key change without invoking onDragCancel, which would
    // leave a drag permanently active (stuck card, page scroll disabled). Cancel it
    // explicitly whenever the gesture host is torn down mid-drag.
    DisposableEffect(card, gestureKey) {
        onDispose {
            if (latestIsDragging) {
                latestOnDragCancel()
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (lockedHeightPx != null) {
                    Modifier.height(with(density) { lockedHeightPx.toDp() })
                } else {
                    Modifier
                }
            )
            .onSizeChanged { onMeasured(it.height) }
            .zIndex(
                when {
                    isDragging -> 2f
                    isSettling -> 1f
                    else -> 0f
                },
            )
            .graphicsLayer {
                translationY = when {
                    isDragging -> dragOffsetY()
                    isSettling -> settlingOffsetY()
                    // Hard zero once the reorder session is gone: animateFloatAsState
                    // snaps one frame late, and that stale frame after the order commit
                    // is exactly the "ghost card" flicker on drop.
                    !animateSiblingOffset -> 0f
                    else -> animatedSiblingOffset
                }
            }
            .pointerInput(card, gestureKey) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { latestOnDragStart() },
                    onDragEnd = { latestOnDragEnd() },
                    onDragCancel = { latestOnDragCancel() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        latestOnDragDelta(dragAmount.y)
                    },
                )
            },
    ) {
        if (isDragging || isSettling) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surface, PanelShape),
            )
        }
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
private fun DashboardInlineEmptyState(
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
internal fun DashboardSeedPieCard(
    title: String,
    entries: List<PieLegendSeedEntry>,
    emptyText: String,
    showHideButton: Boolean,
    onRevealHide: () -> Unit,
    onHide: () -> Unit,
    shareColor: Color,
    compactLegendRows: Boolean = false,
) {
    val resolvedEntries = entries.map { entry ->
        PieLegendEntry(
            label = resolveDashboardLegendLabel(entry.label),
            value = entry.value,
            valueText = resolveDashboardLegendValueText(
                value = entry.value,
                valueKind = entry.valueKind,
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
                title = title,
                showHideButton = showHideButton,
                onRevealHide = onRevealHide,
                onHide = onHide,
            )

            if (resolvedEntries.isEmpty()) {
                DashboardInlineEmptyState(text = emptyText)
                return@Column
            }

            val total = resolvedEntries.sumOf { it.value }.coerceAtLeast(1L)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DashboardPieChart(
                    entries = resolvedEntries,
                    total = total,
                    holeColor = qbGlassHoleColor(),
                    modifier = Modifier.size(132.dp),
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    resolvedEntries.forEachIndexed { index, entry ->
                        val color = DashboardPiePalette[index % DashboardPiePalette.size]
                        val share = (entry.value.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        DashboardLegendRow(
                            color = color,
                            label = entry.label,
                            valueText = entry.valueText,
                            shareText = formatPercent(share),
                            shareColor = shareColor,
                            compact = compactLegendRows,
                        )
                    }
                }
            }
        }
    }
}

internal data class ResolvedDashboardBarEntry(
    val label: String,
    val value: Long,
    val valueText: String,
)

internal data class CompactCountrySummaryItem(
    val labelText: String,
    val valueText: String,
)

internal fun buildCompactCountrySummaryItems(
    entries: List<ResolvedDashboardBarEntry>,
): List<CompactCountrySummaryItem> {
    return entries
        .take(3)
        .map { entry ->
            CompactCountrySummaryItem(
                labelText = entry.label.trim(),
                valueText = entry.valueText.trim(),
            )
        }
        .filter { item -> item.labelText.isNotBlank() || item.valueText.isNotBlank() }
}

internal fun trimDashboardBarEntries(
    entries: List<DashboardBarSeedEntry>,
    maxEntries: Int,
): List<DashboardBarSeedEntry> {
    if (entries.isEmpty() || maxEntries <= 0) return emptyList()
    return entries
        .sortedByDescending { it.value }
        .take(maxEntries)
}

@Composable
private fun resolveDashboardLegendLabel(
    label: LegendLabelSpec,
): String {
    return when (label) {
        is LegendLabelSpec.Raw -> label.text
        is LegendLabelSpec.Res -> stringResource(label.resId)
    }
}

@Composable
private fun resolveDashboardLegendValueText(
    value: Long,
    valueKind: LegendValueKind,
): String {
    return when (valueKind) {
        LegendValueKind.TORRENT_COUNT -> {
            val torrentCount = value.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            pluralStringResource(
                id = R.plurals.chart_category_count,
                count = torrentCount,
                torrentCount,
            )
        }

        LegendValueKind.BYTES -> formatBytes(value)
    }
}

@Composable
private fun resolveDashboardBarEntries(
    entries: List<DashboardBarSeedEntry>,
): List<ResolvedDashboardBarEntry> {
    return entries.map { entry ->
        ResolvedDashboardBarEntry(
            label = resolveDashboardLegendLabel(entry.label),
            value = entry.value,
            valueText = resolveDashboardLegendValueText(
                value = entry.value,
                valueKind = entry.valueKind,
            ),
        )
    }
}

@Composable
internal fun DashboardHorizontalBarChartCard(
    title: String,
    entries: List<DashboardBarSeedEntry>,
    emptyText: String,
    showHideButton: Boolean,
    onRevealHide: () -> Unit,
    onHide: () -> Unit,
) {
    val resolvedEntries = resolveDashboardBarEntries(entries)

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
                title = title,
                showHideButton = showHideButton,
                onRevealHide = onRevealHide,
                onHide = onHide,
            )

            if (resolvedEntries.isEmpty()) {
                DashboardInlineEmptyState(text = emptyText)
                return@Column
            }

            DashboardHorizontalBarList(
                entries = resolvedEntries,
                barColorProvider = { index -> DashboardPiePalette[index % DashboardPiePalette.size] },
            )
        }
    }
}

@Composable
private fun DashboardHorizontalBarList(
    entries: List<ResolvedDashboardBarEntry>,
    barColorProvider: (Int) -> Color,
) {
    val maxValue = entries.maxOf { it.value }.coerceAtLeast(1L).toFloat()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        entries.forEachIndexed { index, entry ->
            val fillRatio = (entry.value.toFloat() / maxValue).coerceIn(0f, 1f)
            val visibleFillRatio = if (entry.value > 0L) {
                fillRatio.coerceAtLeast(0.05f)
            } else {
                0f
            }
            val primaryText = buildDashboardDensityPrimaryText(
                label = entry.label,
                valueText = entry.valueText,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                DashboardCompactMetricRow(
                    color = barColorProvider(index),
                    primaryText = primaryText,
                    shareText = null,
                    shareColor = MaterialTheme.colorScheme.primary,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(visibleFillRatio)
                        .height(10.dp)
                        .background(
                            color = barColorProvider(index).copy(alpha = 0.92f),
                            shape = RoundedCornerShape(999.dp),
                        ),
                )
            }
        }
    }
}

@Composable
private fun DashboardCompactSummaryList(
    entries: List<ResolvedDashboardBarEntry>,
    barColorProvider: (Int) -> Color,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        entries.forEachIndexed { index, entry ->
            DashboardCompactMetricRow(
                color = barColorProvider(index),
                primaryText = buildDashboardDensityPrimaryText(
                    label = entry.label,
                    valueText = entry.valueText,
                ),
                shareText = null,
                shareColor = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun DashboardCompactMetricRow(
    color: Color,
    primaryText: String,
    shareText: String?,
    shareColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(color = color, shape = RoundedCornerShape(50)),
        )
        Text(
            text = primaryText,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!shareText.isNullOrBlank()) {
            Text(
                text = shareText,
                modifier = Modifier.widthIn(min = 52.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = shareColor,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.End,
            )
        }
    }
}

private fun buildDashboardDensityPrimaryText(
    label: String,
    valueText: String,
): String {
    val trimmedLabel = label.trim()
    val trimmedValueText = valueText.trim()
    if (trimmedLabel.isEmpty()) return trimmedValueText
    if (trimmedValueText.isEmpty()) return trimmedLabel
    return "$trimmedLabel $trimmedValueText"
}

@Composable
private fun DashboardPieChart(
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
private fun DashboardLegendRow(
    color: Color,
    label: String,
    valueText: String,
    shareText: String,
    shareColor: Color,
    compact: Boolean,
) {
    if (compact) {
        val primaryText = buildDashboardDensityPrimaryText(
            label = label,
            valueText = valueText,
        )
        DashboardCompactMetricRow(
            color = color,
            primaryText = primaryText,
            shareText = null,
            shareColor = shareColor,
        )
        return
    }

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

fun normalizeCategoryLabel(category: String, noCategoryText: String): String {
    val normalized = category.trim()
    if (normalized.isBlank()) return noCategoryText
    if (normalized == "-" || normalized.equals("null", ignoreCase = true)) return noCategoryText
    return normalized
}



