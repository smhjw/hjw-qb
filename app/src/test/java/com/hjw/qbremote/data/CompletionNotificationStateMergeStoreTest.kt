package com.hjw.qbremote.data

import com.hjw.qbremote.notifications.mergeProfileScopedCompletionStates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CompletionNotificationStateMergeStoreTest {
    @Test
    fun `replaces only keys with this profile prefix`() {
        val persisted = mapOf(
            "a|h1" to "downloading",
            "a|h2" to "completed",
            "b|h1" to "uploading",
        )

        val updated = mergeProfileScopedCompletionStates(
            persisted = persisted,
            profileId = "a",
            states = mapOf("a|h1" to "completed", "a|h3" to "downloading"),
        )

        assertEquals(
            mapOf(
                "b|h1" to "uploading",
                "a|h1" to "completed",
                "a|h3" to "downloading",
            ),
            updated,
        )
    }

    @Test
    fun `input keys for other profiles are ignored`() {
        val persisted = mapOf("b|h1" to "uploading")

        val updated = mergeProfileScopedCompletionStates(
            persisted = persisted,
            profileId = "a",
            states = mapOf("b|h1" to "downloading", "a|h1" to "completed"),
        )

        assertEquals(
            mapOf("b|h1" to "uploading", "a|h1" to "completed"),
            updated,
        )
    }

    @Test
    fun `unchanged merge returns null`() {
        val persisted = mapOf("a|h1" to "completed", "b|h1" to "uploading")

        assertNull(
            mergeProfileScopedCompletionStates(
                persisted = persisted,
                profileId = "a",
                states = mapOf("a|h1" to "completed"),
            )
        )
    }
}
