package com.hjw.qbremote.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

enum class DashboardChartCard(
    val storageKey: String,
) {
    COUNTRY_FLOW("country_flow"),
    CATEGORY_SHARE("category_share"),
    DAILY_UPLOAD("daily_upload"),
    TAG_UPLOAD("tag_upload"),
    TORRENT_STATE("torrent_state"),
    TRACKER_SITE("tracker_site"),
    SIZE_DISTRIBUTION("size_distribution"),
    SHARE_RATIO_DISTRIBUTION("share_ratio_distribution"),
}

val PanelShape = RoundedCornerShape(20.dp)
