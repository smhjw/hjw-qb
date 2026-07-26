package com.hjw.qbremote.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.Constraints
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hjw.qbremote.data.ConnectionStore
import com.hjw.qbremote.data.TorrentRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class CompletionNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val store = ConnectionStore(applicationContext)
        val settings = store.settingsFlow.first()
        if (!settings.completionNotificationsEnabled) return Result.success()

        val profiles = store.serverProfilesFlow.first().profiles
        var states = store.loadCompletionNotificationStates()
        var hadTransientFailure = false
        profiles.forEach { profile ->
            val profileSettings = store.loadSettingsForProfile(profile.id) ?: return@forEach
            TorrentRepository().fetchDashboardSnapshot(profileSettings).fold(
                onSuccess = { snapshot ->
                    val torrents = snapshot.dashboardData.torrents
                    findCompletedTorrentTransitions(profile.id, states, torrents).forEach { transition ->
                        TorrentCompletionNotifier.notifyCompleted(
                            context = applicationContext,
                            profileId = profile.id,
                            profileName = profile.name,
                            torrentHash = transition.torrent.hash,
                            torrentName = transition.torrent.name,
                        )
                    }
                    states = mergeCompletionStates(profile.id, states, torrents)
                    store.saveCompletionNotificationStatesForProfile(profile.id, states)
                },
                onFailure = { hadTransientFailure = true },
            )
        }
        return if (hadTransientFailure) Result.retry() else Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "torrent_completion_notifications"

        fun synchronize(context: Context, enabled: Boolean) {
            val workManager = WorkManager.getInstance(context)
            if (!enabled) {
                workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
                return
            }
            val request = PeriodicWorkRequestBuilder<CompletionNotificationWorker>(
                15,
                TimeUnit.MINUTES,
            ).setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            ).build()
            workManager.enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
