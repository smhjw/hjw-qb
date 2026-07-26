package com.hjw.qbremote.ui

/**
 * Framework side effects (system notifications) injected into [MainViewModel] so it
 * never reaches back into Activity/BroadcastReceiver classes and needs no Context
 * of its own.
 */
interface SystemEventNotifier {
    fun notifyTorrentCompleted(torrentName: String)
}

/** No-op default so the ViewModel can be constructed without Android framework classes. */
object NoOpSystemEventNotifier : SystemEventNotifier {
    override fun notifyTorrentCompleted(torrentName: String) = Unit
}
