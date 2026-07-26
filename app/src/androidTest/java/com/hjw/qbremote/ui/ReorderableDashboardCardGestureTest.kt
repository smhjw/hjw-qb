package com.hjw.qbremote.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReorderableDashboardCardGestureTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun reorderableDashboardCard_reportsLongPressDragCallbacks() {
        var dragStarted = false
        var dragEndedCount = 0
        var dragCancelled = false
        var totalDragDelta = 0f

        composeRule.setContent {
            MaterialTheme {
                ReorderableDashboardCard(
                    card = DashboardChartCard.CATEGORY_SHARE,
                    gestureKey = "gesture-test",
                    isDragging = false,
                    isSettling = false,
                    dragOffsetY = { 0f },
                    settlingOffsetY = { 0f },
                    siblingOffsetY = 0f,
                    animateSiblingOffset = false,
                    lockedHeightPx = null,
                    onDragStart = { dragStarted = true },
                    onDragDelta = { deltaY -> totalDragDelta += deltaY },
                    onDragEnd = { dragEndedCount += 1 },
                    onDragCancel = { dragCancelled = true },
                    onMeasured = {},
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .testTag("drag-surface"),
                    )
                }
            }
        }

        composeRule.onNodeWithTag("drag-surface").performTouchInput {
            down(center)
            advanceEventTime(700L)
            moveBy(Offset(0f, 120f))
            up()
        }
        composeRule.waitForIdle()

        assertTrue(dragStarted)
        assertTrue(totalDragDelta > 0f)
        assertEquals(1, dragEndedCount)
        assertFalse(dragCancelled)
    }
}
