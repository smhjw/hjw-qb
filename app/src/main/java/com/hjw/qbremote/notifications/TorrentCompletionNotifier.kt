package com.hjw.qbremote.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.hjw.qbremote.MainActivity
import com.hjw.qbremote.R

object TorrentCompletionNotifier {
    const val EXTRA_PROFILE_ID = "completion_profile_id"
    const val EXTRA_TORRENT_HASH = "completion_torrent_hash"
    private const val CHANNEL_ID = "torrent_status"

    fun ensureChannel(context: Context) {
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_torrent),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            setSound(
                soundUri,
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build(),
            )
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun notifyCompleted(
        context: Context,
        profileId: String,
        profileName: String,
        torrentHash: String,
        torrentName: String,
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        ensureChannel(context)
        val notificationKey = torrentCompletionKey(profileId, torrentHash)
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationKey.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_PROFILE_ID, profileId)
                putExtra(EXTRA_TORRENT_HASH, torrentHash)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val displayName = torrentName.ifBlank { torrentHash.take(12) }
        val contentText = context.getString(
            R.string.notification_torrent_completed_detail,
            profileName.ifBlank { profileId },
            displayName,
        )
        val publicNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_qbremote_foreground)
            .setContentTitle(context.getString(R.string.notification_torrent_completed))
            .build()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_qbremote_foreground)
            .setContentTitle(context.getString(R.string.notification_torrent_completed))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(notificationKey.hashCode(), notification)
    }
}
