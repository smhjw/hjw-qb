package com.hjw.qbremote.ui

import com.hjw.qbremote.data.ConnectionStore
import com.hjw.qbremote.data.ServerProfile
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.notifications.CompletionNotificationWorker
import com.hjw.qbremote.notifications.TorrentCompletionNotifier
import com.hjw.qbremote.notifications.findCompletedTorrentTransitions
import com.hjw.qbremote.notifications.mergeCompletionStates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class CompletionNotificationCoordinator(
    private val connectionStore: ConnectionStore,
    private val scope: CoroutineScope,
) {
    private val previousTorrentStates = mutableMapOf<String, String>()
    private var navigationTargetId = 0L
    private var statesLoaded = false

    suspend fun initialize() {
        previousTorrentStates.clear()
        previousTorrentStates.putAll(connectionStore.loadCompletionNotificationStates())
        statesLoaded = true
    }

    fun setEnabled(enabled: Boolean) {
        if (enabled) {
            previousTorrentStates.clear()
            scope.launch { connectionStore.saveCompletionNotificationStates(emptyMap()) }
        }
        CompletionNotificationWorker.synchronize(
            connectionStore.context.applicationContext,
            enabled,
        )
    }

    fun createNavigationTarget(
        profileId: String,
        torrentHash: String,
    ): NotificationNavigationTarget? {
        val normalizedProfileId = profileId.trim()
        val normalizedHash = torrentHash.trim()
        if (normalizedProfileId.isBlank() || normalizedHash.isBlank()) return null
        navigationTargetId += 1L
        return NotificationNavigationTarget(
            id = navigationTargetId,
            profileId = normalizedProfileId,
            torrentHash = normalizedHash,
        )
    }

    fun detectCompletedTorrents(
        enabled: Boolean,
        activeProfileId: String?,
        profiles: List<ServerProfile>,
        torrents: List<TorrentInfo>,
    ) {
        if (!enabled || !statesLoaded) return
        val profileId = activeProfileId?.trim().orEmpty()
        if (profileId.isBlank()) return
        val profileName = profiles.firstOrNull { it.id == profileId }?.name.orEmpty()
        findCompletedTorrentTransitions(profileId, previousTorrentStates, torrents).forEach { transition ->
            TorrentCompletionNotifier.notifyCompleted(
                context = connectionStore.context.applicationContext,
                profileId = profileId,
                profileName = profileName,
                torrentHash = transition.torrent.hash,
                torrentName = transition.torrent.name,
            )
        }
        val merged = mergeCompletionStates(profileId, previousTorrentStates, torrents)
        if (merged == previousTorrentStates) return
        previousTorrentStates.clear()
        previousTorrentStates.putAll(merged)
        scope.launch {
            connectionStore.saveCompletionNotificationStatesForProfile(profileId, merged)
        }
    }
}
