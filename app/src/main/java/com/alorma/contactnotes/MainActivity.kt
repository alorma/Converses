package com.alorma.contactnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.alorma.contactnotes.designsystem.theme.ColorStyle
import com.alorma.contactnotes.designsystem.theme.ConversesTheme
import com.alorma.contactnotes.designsystem.theme.ThemeMode
import com.alorma.contactnotes.ui.gallery.ThemeGalleryScreen

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      var themeMode by rememberSaveable { mutableStateOf(ThemeMode.System) }
      var colorStyle by rememberSaveable { mutableStateOf(ColorStyle.Tinted) }
      ConversesTheme(themeMode = themeMode, colorStyle = colorStyle) {
        ThemeGalleryScreen(
          themeMode = themeMode,
          onThemeModeChange = { themeMode = it },
          colorStyle = colorStyle,
          onColorStyleChange = { colorStyle = it },
        )
      }
    }
  }
}
