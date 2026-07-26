package com.hjw.qbremote.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.hjw.qbremote.data.CachedDashboardServerSnapshot
import com.hjw.qbremote.data.ServerBackendType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WalletServerStackCardGestureTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun walletServerStackCard_supportsRepeatedLongPressDragGestures() {
        var dragStartedCount = 0
        var dragEndedCount = 0
        var dragCancelled = false
        var totalDragDelta = 0f
        var clickCount = 0

        composeRule.setContent {
            MaterialTheme {
                WalletServerStackCard(
                    snapshot = CachedDashboardServerSnapshot(
                        profileId = "alpha",
                        profileName = "Alpha",
                        backendType = ServerBackendType.QBITTORRENT,
                    ),
                    gestureKey = "gesture-test",
                    stackedIndex = 0,
                    paletteIndex = 0,
                    cardHeight = 188.dp,
                    collapsedCardHeight = 90.dp,
                    exposedHeight = 60.dp,
                    selected = true,
                    stackCount = 2,
                    isDragging = false,
                    isSettling = false,
                    dragOffsetY = { 0f },
                    settlingOffsetY = { 0f },
                    siblingOffsetY = 0f,
                    animateSiblingOffset = false,
                    onDragStart = { dragStartedCount += 1 },
                    onDragDelta = { deltaY -> totalDragDelta += deltaY },
                    onDragEnd = { dragEndedCount += 1 },
                    onDragCancel = { dragCancelled = true },
                    onClick = { clickCount += 1 },
                )
            }
        }

        repeat(2) {
            composeRule.onNodeWithText("Alpha").performTouchInput {
                down(center)
                advanceEventTime(700L)
                moveBy(Offset(0f, 120f))
                up()
            }
            composeRule.waitForIdle()
        }

        assertEquals(2, dragStartedCount)
        assertEquals(2, dragEndedCount)
        assertTrue(totalDragDelta > 0f)
        assertFalse(dragCancelled)
        assertEquals(0, clickCount)
    }
}
