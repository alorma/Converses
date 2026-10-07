package com.alorma.contactnotes.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
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

/** Verd d'èxit fix, fora de l'esquema dinàmic. Només s'usa a l'onboarding. */
object SuccessColors {
  val success: Color @Composable @ReadOnlyComposable get() = if (isDarkTheme) Color(0xFF5AC576) else Color(0xFF1B9247)
  val onSuccess: Color @Composable @ReadOnlyComposable get() = if (isDarkTheme) Color(0xFF00220A) else Color(0xFFFFFFFF)
  val successContainer: Color @Composable @ReadOnlyComposable get() = if (isDarkTheme) Color(0xFF133C1F) else Color(0xFFD1F2D7)
}
