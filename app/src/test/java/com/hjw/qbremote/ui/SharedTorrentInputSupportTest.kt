package com.hjw.qbremote.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SharedTorrentInputSupportTest {
    @Test
    fun extractsSupportedUrlsAndRemovesDuplicates() {
        val magnet = "magnet:?xt=urn:btih:abcdef"

        assertEquals(
            "$magnet\nhttps://example.com/file.torrent",
            normalizeSharedTorrentInput(
                "Download $magnet or https://example.com/file.torrent.\n$magnet",
            ),
        )
    }

    @Test
    fun rejectsPlainTextWithoutTorrentInput() {
        assertEquals("", normalizeSharedTorrentInput("not a torrent link"))
    }

    @Test
    fun mergesInputsWithoutDuplicatingExistingUrls() {
        assertEquals(
            "magnet:?xt=urn:btih:first\nhttps://example.com/second.torrent",
            mergeSharedTorrentInputs(
                existing = "magnet:?xt=urn:btih:first",
                incoming = "magnet:?xt=urn:btih:first\nhttps://example.com/second.torrent",
            ),
        )
    }

    @Test
    fun truncatesOversizedInputAndDropsUrlsBeyondTheBound() {
        val insideMagnet = "magnet:?xt=urn:btih:inside"
        val raw = insideMagnet + "\n" + "x".repeat(64_000) + "\nmagnet:?xt=urn:btih:beyond"

        assertEquals(insideMagnet, normalizeSharedTorrentInput(raw))
    }

    @Test
    fun dropsSingleUrlLongerThan8KAndKeepsValidOnes() {
        val oversized = "https://example.com/" + "a".repeat(8_200)
        val magnet = "magnet:?xt=urn:btih:ok"

        assertEquals(magnet, normalizeSharedTorrentInput("$oversized\n$magnet"))
    }

    @Test
    fun capsNormalizedUrlCountAt50() {
        val urls = (1..51).map { "https://example.com/file-$it.torrent" }

        val result = normalizeSharedTorrentInput(urls.joinToString("\n"))

        assertEquals(urls.take(50), result.lines())
    }

    @Test
    fun mergeCapsAccumulatedUrlsAt50() {
        val existing = (1..50).joinToString("\n") { "https://example.com/existing-$it.torrent" }

        val merged = mergeSharedTorrentInputs(
            existing = existing,
            incoming = "magnet:?xt=urn:btih:overflow",
        )

        assertEquals(existing, merged)
    }
}
