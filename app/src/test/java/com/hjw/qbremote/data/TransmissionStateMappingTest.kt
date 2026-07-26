package com.hjw.qbremote.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TransmissionStateMappingTest {
    @Test
    fun `maps every transmission status branch`() {
        val cases = listOf(
            Triple(0, 0.4, false) to "pausedDL",
            Triple(0, 0.4, true) to "pausedUP",
            Triple(0, 1.0, false) to "pausedUP",
            Triple(1, 0.4, false) to "checkUP",
            Triple(2, 1.0, true) to "checkUP",
            Triple(3, 0.4, false) to "queuedDL",
            Triple(4, 0.4, false) to "downloading",
            Triple(5, 1.0, true) to "queuedUP",
            Triple(6, 1.0, true) to "uploading",
            Triple(7, 0.4, false) to "pausedDL",
            Triple(7, 1.0, false) to "pausedUP",
        )
        cases.forEach { (input, expected) ->
            val (status, percentDone, isFinished) = input
            assertEquals(
                "status=$status percentDone=$percentDone isFinished=$isFinished",
                expected,
                mapTransmissionState(status, percentDone, isFinished),
            )
        }
    }
}
