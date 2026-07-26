package com.hjw.qbremote.ui

internal enum class TorrentListEmptyState {
    NONE,
    NO_TORRENTS,
    SEARCH_NO_RESULTS,
    FILTER_NO_RESULTS,
    SEARCH_FILTER_NO_RESULTS,
}

internal fun resolveTorrentListEmptyState(
    hasTorrents: Boolean,
    visibleCount: Int,
    filterState: TorrentListFilterState,
): TorrentListEmptyState {
    if (!hasTorrents) return TorrentListEmptyState.NO_TORRENTS
    if (visibleCount > 0) return TorrentListEmptyState.NONE
    val hasQuery = filterState.query.trim().isNotBlank()
    val hasFilters = filterState.stateFilter != TorrentStateFilter.ALL ||
        filterState.categoryFilter.isNotBlank() ||
        filterState.tagFilter.isNotBlank()
    return when {
        hasQuery && hasFilters -> TorrentListEmptyState.SEARCH_FILTER_NO_RESULTS
        hasQuery -> TorrentListEmptyState.SEARCH_NO_RESULTS
        hasFilters -> TorrentListEmptyState.FILTER_NO_RESULTS
        else -> TorrentListEmptyState.NONE
    }
}

internal fun clearTorrentListFilters(current: TorrentListFilterState): TorrentListFilterState {
    return current.copy(
        stateFilter = TorrentStateFilter.ALL,
        categoryFilter = "",
        tagFilter = "",
    )
}
