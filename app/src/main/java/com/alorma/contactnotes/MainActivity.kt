package com.alorma.contactnotes

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.contactnotes.data.flags.PersonColorFeature
import com.alorma.contactnotes.data.flags.rememberFeatureEnabled
import com.alorma.contactnotes.data.settings.AppLockManager
import com.alorma.contactnotes.data.settings.OnboardingFlag
import com.alorma.contactnotes.data.settings.SecureWindow
import com.alorma.contactnotes.data.settings.SettingsRepository
import com.alorma.contactnotes.debug.DebugMenuHost
import com.alorma.contactnotes.designsystem.theme.ConversesTheme
import com.alorma.contactnotes.designsystem.theme.ThemeMode
import com.alorma.contactnotes.ui.AppNavigation
import com.alorma.contactnotes.ui.lock.AppLockScreen
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

class MainActivity : FragmentActivity() {
  private val settingsRepository: SettingsRepository by inject()
  private val appLock: AppLockManager by inject()
  private val secureWindow: SecureWindow by inject()
  private val onboardingFlag: OnboardingFlag by inject()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Lectura síncrona abans del primer frame per evitar un parpelleig de tema en obrir.
    val initialSettings = runBlocking { settingsRepository.settings.first() }
    val showOnboarding = onboardingFlag.isEnabled()
    val secureFlow = combine(settingsRepository.settings.map { it.appLock }, secureWindow.isRequested) { lock, requested ->
      lock || requested
    }

    setContent {
      val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialSettings)
      val showLock by appLock.showLock.collectAsStateWithLifecycle()
      val secure by secureFlow.collectAsStateWithLifecycle(initialSettings.appLock)

      val personColorFeature = rememberFeatureEnabled<PersonColorFeature>()
      val systemDark = isSystemInDarkTheme()
      val isDark = when (settings.themeMode) {
        ThemeMode.System -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
      }
      DisposableEffect(isDark) {
        val style = if (isDark) {
          SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
          SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        onDispose {}
      }
      LaunchedEffect(secure) {
        if (secure) {
          window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
          window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
      }

      ConversesTheme(
        themeMode = settings.themeMode,
        dynamicColor = settings.dynamicColor,
        personColorTheme = settings.personColorTheme && personColorFeature,
      ) {
        DebugMenuHost {
          Box {
            AppNavigation(startOnboarding = showOnboarding)
            if (showLock) {
              AppLockScreen(
                onUnlocked = appLock::unlock,
                onDeviceUnprotected = {
                  lifecycleScope.launch { settingsRepository.setAppLock(false) }
                  appLock.unlock()
                },
              )
            }
          }
        }
      }
    }
  }
}
