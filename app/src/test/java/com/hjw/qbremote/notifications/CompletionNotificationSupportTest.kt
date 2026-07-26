package com.hjw.qbremote.notifications

import com.hjw.qbremote.data.model.TorrentInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompletionNotificationSupportTest {
    @Test
    fun `same torrent hash on different profiles has different keys`() {
        assertTrue(
            torrentCompletionKey("server-a", "ABC") !=
                torrentCompletionKey("server-b", "ABC")
        )
    }

    @Test
    fun `only download to completion creates transition`() {
        val downloading = TorrentInfo(hash = "abc", state = "downloading", progress = 0.8f)
        val completed = downloading.copy(state = "uploading", progress = 1f)
        val previous = mergeCompletionStates("server", emptyMap(), listOf(downloading))

        val transitions = findCompletedTorrentTransitions("server", previous, listOf(completed))

        assertEquals(listOf("abc"), transitions.map { it.torrent.hash })
    }

    @Test
    fun `first observation establishes baseline without notification`() {
        val completed = TorrentInfo(hash = "abc", state = "seeding", progress = 1f)

        assertTrue(
            findCompletedTorrentTransitions("server", emptyMap(), listOf(completed)).isEmpty()
        )
    }

    @Test
    fun `merge drops vanished torrents for this profile and keeps other profiles`() {
        val previous = mapOf(
            torrentCompletionKey("server", "aaa") to "downloading",
            torrentCompletionKey("other", "bbb") to "uploading",
        )

        val merged = mergeCompletionStates(
            "server",
            previous,
            listOf(TorrentInfo(hash = "ccc", state = "downloading", progress = 0.1f)),
        )

        assertEquals(
            mapOf(
                torrentCompletionKey("other", "bbb") to "uploading",
                torrentCompletionKey("server", "ccc") to "downloading",
            ),
            merged,
        )
    }

    @Test
    fun `full progress in transient states does not count as completed`() {
        listOf("checking", "checkingUP", "checkingDL", "moving", "error", "missingFiles").forEach { state ->
            assertEquals(
                state.lowercase(),
                completionStateToken(TorrentInfo(hash = "abc", state = state, progress = 1f)),
            )
        }
    }

    @Test
    fun `static seeding states count as completed regardless of progress`() {
        listOf("pausedUP", "seedwait", "stalledUP", "queuedUP").forEach { state ->
            assertEquals(
                "completed",
                completionStateToken(TorrentInfo(hash = "abc", state = state, progress = 0.99f)),
            )
        }
    }
}
