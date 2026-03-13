package com.hjw.qbremote.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.hjw.qbremote.data.model.CountryUploadRecord
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.ui.theme.qbGlassCardColors
import com.hjw.qbremote.ui.theme.qbGlassEmptyStateColor
import com.hjw.qbremote.ui.theme.qbGlassHoleColor
import com.hjw.qbremote.ui.theme.qbGlassOutlineColor
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun CategorySharePieCard(
    torrents: List<TorrentInfo>,
    onHide: () -> Unit,
) {
    var showHideButton by rememberSaveable { mutableStateOf(false) }
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

    LaunchedEffect(showHideButton) {
        if (!showHideButton) return@LaunchedEffect
        delay(5_000)
        showHideButton = false
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
                onRevealHide = { showHideButton = true },
                onHide = {
                    showHideButton = false
                    onHide()
                },
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
                    modifier = Modifier.fillMaxWidth(),
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
        }
    }
}

@Composable
fun CountryFlowMapCard(
    stats: List<CountryUploadRecord>,
    onHide: () -> Unit,
) {
    var showHideButton by rememberSaveable { mutableStateOf(false) }
    val locale = LocalContext.current.resources.configuration.locales[0] ?: Locale.getDefault()
    val displayStats = remember(stats) { mergeCountryUploadRecordsForDisplay(stats) }
    val topCountries = remember(displayStats) { displayStats.take(3) }
    val emptyText = stringResource(R.string.dashboard_country_flow_empty)

    LaunchedEffect(showHideButton) {
        if (!showHideButton) return@LaunchedEffect
        delay(5_000)
        showHideButton = false
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
                onRevealHide = { showHideButton = true },
                onHide = {
                    showHideButton = false
                    onHide()
                },
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
        }
    }
}

@Composable
fun DailyTagUploadPieCard(
    date: String,
    stats: List<DailyTagUploadStat>,
    onHide: () -> Unit,
) {
    var showHideButton by rememberSaveable { mutableStateOf(false) }
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

    LaunchedEffect(showHideButton) {
        if (!showHideButton) return@LaunchedEffect
        delay(5_000)
        showHideButton = false
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
                title = if (date.isNotBlank()) {
                    stringResource(R.string.dashboard_upload_title_with_date, date)
                } else {
                    stringResource(R.string.dashboard_upload_title)
                },
                showHideButton = showHideButton,
                onRevealHide = { showHideButton = true },
                onHide = {
                    showHideButton = false
                    onHide()
                },
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
                    modifier = Modifier.fillMaxWidth(),
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
fun DashboardCardHeader(
    title: String,
    showHideButton: Boolean,
    onRevealHide: () -> Unit,
    onHide: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onRevealHide() })
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
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
        targetValue = if (isDragging) 1.012f else 1f,
        animationSpec = spring(
            dampingRatio = 0.86f,
            stiffness = 560f,
        ),
        label = "dashboardDraggedScale",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { onMeasured(it.height) }
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                translationY = if (isDragging) dragOffsetY else siblingOffsetY
                shadowElevation = if (isDragging) 12f else 0f
                scaleX = draggedScale
                scaleY = draggedScale
                alpha = 1f
            }
            .pointerInput(card) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragDelta(dragAmount.y)
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
        modifier = Modifier.widthIn(min = 204.dp, max = 216.dp),
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
                .width(116.dp),
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
            modifier = Modifier.width(52.dp),
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
