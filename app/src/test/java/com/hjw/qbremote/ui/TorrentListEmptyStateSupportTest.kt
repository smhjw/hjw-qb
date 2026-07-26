package com.hjw.qbremote.ui

import com.hjw.qbremote.data.model.TorrentInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class TorrentListEmptyStateSupportTest {

    @Test
    fun buildTorrentListDisplayState_marksNoTorrentsWhenBaseListIsEmpty() {
        val displayState = buildTorrentListDisplayState(
            torrents = emptyList(),
            filterState = TorrentListFilterState(),
        )

        assertEquals(TorrentListEmptyState.NO_TORRENTS, displayState.emptyState)
    }

    @Test
    fun buildTorrentListDisplayState_marksSearchNoResultsWhenOnlyQueryFiltersEverything() {
        val displayState = buildTorrentListDisplayState(
            torrents = listOf(TorrentInfo(hash = "a", name = "Ubuntu")),
            filterState = TorrentListFilterState(query = "debian"),
        )

        assertEquals(TorrentListEmptyState.SEARCH_NO_RESULTS, displayState.emptyState)
    }

    @Test
    fun buildTorrentListDisplayState_marksFilterNoResultsWhenOnlyFiltersHideEverything() {
        val displayState = buildTorrentListDisplayState(
            torrents = listOf(TorrentInfo(hash = "a", name = "Ubuntu", state = "pauseddl", progress = 0.1f)),
            filterState = TorrentListFilterState(stateFilter = TorrentStateFilter.SEEDING),
        )

        assertEquals(TorrentListEmptyState.FILTER_NO_RESULTS, displayState.emptyState)
    }

    @Test
    fun buildTorrentListDisplayState_marksSearchFilterNoResultsWhenBothAreActive() {
        val displayState = buildTorrentListDisplayState(
            torrents = listOf(
                TorrentInfo(hash = "a", name = "Ubuntu", category = "linux", state = "downloading"),
            ),
            filterState = TorrentListFilterState(
                query = "debian",
                categoryFilter = "movies",
            ),
        )

        assertEquals(TorrentListEmptyState.SEARCH_FILTER_NO_RESULTS, displayState.emptyState)
    }

    @Test
    fun clearTorrentListFilters_preservesQueryAndSortState() {
        val current = TorrentListFilterState(
            query = "ubuntu",
            sortOption = TorrentListSortOption.ADDED_TIME,
            descending = false,
            stateFilter = TorrentStateFilter.ERROR,
            categoryFilter = "linux",
            tagFilter = "iso",
        )

        assertEquals(
            TorrentListFilterState(
                query = "ubuntu",
                sortOption = TorrentListSortOption.ADDED_TIME,
                descending = false,
            ),
            clearTorrentListFilters(current),
        )
    }
}
