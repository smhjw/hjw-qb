package com.hjw.qbremote.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hjw.qbremote.R
import com.hjw.qbremote.data.model.TorrentInfo

@Immutable
internal data class DashboardDisplayCardItem(
    val owner: DashboardChartCard,
    val representedCards: List<DashboardChartCard>,
) {
    init {
        require(representedCards.isNotEmpty())
        require(owner in representedCards)
        require(representedCards.distinct().size == representedCards.size)
    }

    val ownerKey: String
        get() = owner.storageKey

    val representedCardKeys: Set<String>
        get() = representedCards.mapTo(linkedSetOf()) { it.storageKey }
}

@Composable
internal fun dashboardChartCardLabel(card: DashboardChartCard): String {
    return when (card) {
        DashboardChartCard.COUNTRY_FLOW -> stringResource(R.string.dashboard_country_flow_title)
        DashboardChartCard.CATEGORY_SHARE -> stringResource(R.string.dashboard_category_share_title)
        DashboardChartCard.DAILY_UPLOAD -> stringResource(R.string.dashboard_upload_title)
        DashboardChartCard.TAG_UPLOAD -> stringResource(R.string.dashboard_tag_upload_share_title)
        DashboardChartCard.TORRENT_STATE -> stringResource(R.string.dashboard_torrent_state_share_title)
        DashboardChartCard.TRACKER_SITE -> stringResource(R.string.dashboard_tracker_site_share_title)
        DashboardChartCard.SIZE_DISTRIBUTION ->
            stringResource(R.string.dashboard_size_distribution_title)
        DashboardChartCard.SHARE_RATIO_DISTRIBUTION ->
            stringResource(R.string.dashboard_share_ratio_distribution_title)
    }
}

@Immutable
internal data class ShareRatioDistributionDisplay(
    val bucketCounts: List<Int> = emptyList(),
    val belowOneCount: Int = 0,
    val totalCount: Int = 0,
    val medianRatio: Double = 0.0,
)

internal fun buildShareRatioDistributionDisplay(
    torrents: List<TorrentInfo>,
): ShareRatioDistributionDisplay {
    if (torrents.isEmpty()) return ShareRatioDistributionDisplay()
    val ratios = torrents.map { it.ratio.coerceAtLeast(0.0) }.sorted()
    val bucketCounts = listOf(
        ratios.count { it < 1.0 },
        ratios.count { it >= 1.0 && it < 2.0 },
        ratios.count { it >= 2.0 && it < 5.0 },
        ratios.count { it >= 5.0 },
    )
    val medianRatio = if (ratios.size % 2 == 1) {
        ratios[ratios.size / 2]
    } else {
        (ratios[ratios.size / 2 - 1] + ratios[ratios.size / 2]) / 2.0
    }
    return ShareRatioDistributionDisplay(
        bucketCounts = bucketCounts,
        belowOneCount = bucketCounts.first(),
        totalCount = ratios.size,
        medianRatio = medianRatio,
    )
}

internal fun parseDashboardCardOrder(
    raw: String,
    availableCards: List<DashboardChartCard>,
): List<DashboardChartCard> {
    val availableByKey = availableCards.associateBy { it.storageKey }
    val parsed = raw
        .split(',')
        .mapNotNull { token ->
            availableByKey[token.trim()]
        }
        .distinct()
        .toMutableList()
    availableCards.forEach { card ->
        if (!parsed.contains(card)) {
            parsed += card
        }
    }
    return parsed
}

internal fun serializeDashboardCardOrder(
    order: List<DashboardChartCard>,
    availableCards: List<DashboardChartCard>,
): String {
    return parseDashboardCardOrder(
        order.joinToString(",") { it.storageKey },
        availableCards,
    ).joinToString(",") { it.storageKey }
}

internal fun buildDashboardDisplayCards(
    visibleCards: List<DashboardChartCard>,
): List<DashboardDisplayCardItem> {
    return visibleCards.map { card ->
        DashboardDisplayCardItem(
            owner = card,
            representedCards = listOf(card),
        )
    }
}

internal fun applyDashboardDisplayCardVisibility(
    visibleKeys: Set<String>,
    displayCard: DashboardDisplayCardItem,
    visible: Boolean,
): Set<String> {
    return if (visible) {
        visibleKeys + displayCard.representedCardKeys
    } else {
        visibleKeys - displayCard.representedCardKeys
    }
}

internal fun reorderDashboardCardOrderForDisplay(
    order: List<DashboardChartCard>,
    displayCards: List<DashboardDisplayCardItem>,
    owner: DashboardChartCard,
    targetIndex: Int,
): List<DashboardChartCard> {
    val currentDisplayIndex = displayCards.indexOfFirst { it.owner == owner }
    if (currentDisplayIndex < 0 || targetIndex !in displayCards.indices || currentDisplayIndex == targetIndex) {
        return order
    }

    val movingDisplayCard = displayCards[currentDisplayIndex]
    val movingCards = movingDisplayCard.representedCards.toSet()
    val remainingOrder = order.filterNot { it in movingCards }
    val remainingDisplayCards = displayCards.filterIndexed { index, _ -> index != currentDisplayIndex }

    if (remainingDisplayCards.isEmpty()) return order

    return if (targetIndex >= remainingDisplayCards.size) {
        val anchor = remainingDisplayCards.last().representedCards.last()
        val anchorIndex = remainingOrder.indexOf(anchor)
        if (anchorIndex < 0) {
            order
        } else {
            remainingOrder.toMutableList().apply {
                addAll(anchorIndex + 1, movingDisplayCard.representedCards)
            }
        }
    } else {
        val anchor = remainingDisplayCards[targetIndex].representedCards.first()
        val anchorIndex = remainingOrder.indexOf(anchor)
        if (anchorIndex < 0) {
            order
        } else {
            remainingOrder.toMutableList().apply {
                addAll(anchorIndex, movingDisplayCard.representedCards)
            }
        }
    }
}
