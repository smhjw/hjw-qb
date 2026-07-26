package com.hjw.qbremote

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hjw.qbremote.data.AppLanguage
import com.hjw.qbremote.data.AppTheme
import com.hjw.qbremote.data.ConnectionStore
import com.hjw.qbremote.data.TorrentRepository
import com.hjw.qbremote.ui.MainScreen
import com.hjw.qbremote.ui.MainViewModel
import com.hjw.qbremote.ui.SystemEventNotifier
import com.hjw.qbremote.ui.resolveEffectiveAppTheme
import com.hjw.qbremote.ui.theme.QBRemoteTheme
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    // Snapshot state so onNewIntent (activity already in foreground) triggers
    // recomposition and the LaunchedEffect below actually consumes the link.
    private var pendingSharedUrl: String? by mutableStateOf(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not, proceed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel()
        requestNotificationPermissionIfNeeded()

        val connectionStore = ConnectionStore(applicationContext)
        val repository = TorrentRepository()
        val appContext = applicationContext
        val systemEventNotifier = object : SystemEventNotifier {
            override fun notifyTorrentCompleted(torrentName: String) {
                notifyTorrentCompleted(appContext, torrentName, vibrate = true)
            }
        }

        lifecycleScope.launch {
            connectionStore.settingsFlow
                .map { it.appLanguage }
                .distinctUntilChanged()
                .collect { applyLanguage(it) }
        }

        handleShareIntent(intent)

        setContent {
            val vm: MainViewModel = viewModel(
                factory = MainViewModel.factory(connectionStore, repository, systemEventNotifier)
            )
            val uiState by vm.uiState.collectAsStateWithLifecycle()
            val effectiveAppTheme = resolveEffectiveAppTheme(
                appTheme = uiState.settings.appTheme,
                customBackgroundAvailable = uiState.customBackgroundAvailable,
            )
            val darkTheme = when (effectiveAppTheme) {
                AppTheme.DARK -> true
                AppTheme.LIGHT -> false
                AppTheme.CUSTOM -> !uiState.settings.customBackgroundToneIsLight
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, vm) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> vm.setAppForeground(true)
                        Lifecycle.Event.ON_STOP -> vm.setAppForeground(false)
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            LaunchedEffect(pendingSharedUrl) {
                val url = pendingSharedUrl ?: return@LaunchedEffect
                vm.handleSharedMagnet(url)
                pendingSharedUrl = null
            }

            QBRemoteTheme(
                appTheme = effectiveAppTheme,
                customBackgroundToneIsLight = uiState.settings.customBackgroundToneIsLight,
            ) {
                ConfigureSystemBars(darkTheme = darkTheme)
                MainScreen(viewModel = vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent) {
        if (intent.action != Intent.ACTION_SEND) return
        if (intent.type?.startsWith("text/plain") != true) return
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        if (sharedText.isBlank()) return
        pendingSharedUrl = sharedText
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_torrent),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            setSound(soundUri, audioAttributes)
            enableVibration(true)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun applyLanguage(language: AppLanguage) {
        val locales = when (language) {
            AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
            AppLanguage.ZH_CN -> LocaleListCompat.forLanguageTags("zh-CN")
            AppLanguage.EN -> LocaleListCompat.forLanguageTags("en")
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "torrent_status"

        fun notifyTorrentCompleted(context: Context, name: String, vibrate: Boolean) {
            val publicNotification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_qbremote_foreground)
                .setContentTitle(context.getString(R.string.notification_torrent_completed))
                .build()
            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_qbremote_foreground)
                .setContentTitle(context.getString(R.string.notification_torrent_completed))
                .setContentText(name)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publicNotification)
                .build()
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.notify(name.hashCode(), notification)

            if (vibrate) {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(VibratorManager::class.java)
                    vm?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        }
    }
}

@Composable
private fun ConfigureSystemBars(darkTheme: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return

    SideEffect {
        val activity = view.context.findActivity() as? AppCompatActivity ?: return@SideEffect
        val window = activity.window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
