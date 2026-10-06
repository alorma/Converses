package com.alorma.contactnotes.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

internal fun ColorScheme.withFixedError(isDark: Boolean): ColorScheme =
  if (isDark) {
    copy(
      error = Color(0xFFFFB4AB),
      onError = Color(0xFF690005),
      errorContainer = Color(0xFF93000A),
      onErrorContainer = Color(0xFFFFB4AB),
    )
  } else {
    copy(
      error = Color(0xFFBA1A1A),
      onError = Color(0xFFFFFFFF),
      errorContainer = Color(0xFFFFDAD6),
      onErrorContainer = Color(0xFFBA1A1A),
    )
  }
