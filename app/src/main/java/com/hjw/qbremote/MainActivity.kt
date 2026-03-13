package com.hjw.qbremote

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hjw.qbremote.data.AppLanguage
import com.hjw.qbremote.data.AppTheme
import com.hjw.qbremote.data.ConnectionStore
import com.hjw.qbremote.data.QbRepository
import com.hjw.qbremote.ui.MainScreen
import com.hjw.qbremote.ui.MainViewModel
import com.hjw.qbremote.ui.theme.QBRemoteTheme
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val connectionStore = ConnectionStore(applicationContext)
        val repository = QbRepository()

        lifecycleScope.launch {
            connectionStore.settingsFlow
                .map { it.appLanguage }
                .distinctUntilChanged()
                .collect { applyLanguage(it) }
        }

        setContent {
            val vm: MainViewModel = viewModel(
                factory = MainViewModel.factory(connectionStore, repository)
            )
            val uiState by vm.uiState.collectAsStateWithLifecycle()
            val darkTheme = when (uiState.settings.appTheme) {
                AppTheme.DARK -> true
                AppTheme.LIGHT -> false
                AppTheme.CUSTOM -> !uiState.settings.customBackgroundToneIsLight
            }
            QBRemoteTheme(
                appTheme = uiState.settings.appTheme,
                customBackgroundToneIsLight = uiState.settings.customBackgroundToneIsLight,
            ) {
                ConfigureSystemBars(darkTheme = darkTheme)
                MainScreen(viewModel = vm)
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
