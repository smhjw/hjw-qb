package com.hjw.qbremote.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardReorderRefreshHoldSupportTest {

    @Test
    fun shouldSkipRefreshForDashboardReorderHold_skipsOnlyMatchingProfile() {
        assertTrue(
            shouldSkipRefreshForDashboardReorderHold(
                heldProfileId = "tr-1",
                holdAllProfiles = false,
                profileId = "tr-1",
            ),
        )
        assertFalse(
            shouldSkipRefreshForDashboardReorderHold(
                heldProfileId = "tr-1",
                holdAllProfiles = false,
                profileId = "qb-1",
            ),
        )
        assertFalse(
            shouldSkipRefreshForDashboardReorderHold(
                heldProfileId = null,
                holdAllProfiles = false,
                profileId = "tr-1",
            ),
        )
    }

    @Test
    fun shouldSkipRefreshForDashboardReorderHold_holdAllProfilesSkipsEveryProfile() {
        assertTrue(
            shouldSkipRefreshForDashboardReorderHold(
                heldProfileId = null,
                holdAllProfiles = true,
                profileId = "tr-1",
            ),
        )
        assertTrue(
            shouldSkipRefreshForDashboardReorderHold(
                heldProfileId = "tr-1",
                holdAllProfiles = true,
                profileId = "qb-1",
            ),
        )
    }

    @Test
    fun releaseDashboardReorderHold_returnsImmediateRefreshRequestForHeldProfile() {
        val released = releaseDashboardReorderHold(
            state = MainUiState(
                selectedDashboardProfileId = "tr-1",
                dashboardRefreshHoldProfileId = "tr-1",
            ),
        )

        assertEquals("tr-1", released.profileIdToRefreshImmediately)
        assertNull(released.nextHeldProfileId)
    }

    @Test
    fun releaseDashboardReorderHold_ignoresAlreadyClearedHold() {
        val released = releaseDashboardReorderHold(
            state = MainUiState(
                selectedDashboardProfileId = "tr-1",
                dashboardRefreshHoldProfileId = null,
            ),
        )

        assertNull(released.profileIdToRefreshImmediately)
        assertNull(released.nextHeldProfileId)
    }
}
