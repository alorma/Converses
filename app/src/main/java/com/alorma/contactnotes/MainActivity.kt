package com.alorma.contactnotes

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.fragment.app.FragmentActivity
import com.alorma.contactnotes.data.prefs.ThemePreferences
import com.alorma.contactnotes.designsystem.theme.ConversesTheme
import com.alorma.contactnotes.designsystem.theme.ThemeMode
import com.alorma.contactnotes.ui.AppNavigation
import org.koin.compose.koinInject

class MainActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      val themePreferences: ThemePreferences = koinInject()
      val themeMode = themePreferences.themeMode.value
      val systemDark = isSystemInDarkTheme()
      val isDark = when (themeMode) {
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

      ConversesTheme(themeMode = themeMode, colorStyle = themePreferences.colorStyle.value) {
        AppNavigation()
      }
    }
  }
}
