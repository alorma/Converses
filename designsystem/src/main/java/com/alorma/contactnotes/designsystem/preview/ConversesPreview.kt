package com.alorma.contactnotes.designsystem.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.designsystem.theme.ConversesTheme
import com.alorma.contactnotes.designsystem.theme.ThemeColor
import com.alorma.contactnotes.designsystem.theme.ThemeMode

/**
 * Tema de les previews i dels tests de captures: sense color dinàmic, perquè el resultat no depengui del fons
 * de pantalla. El mode clar o fosc el decideix el `uiMode` de la preview.
 */
@Composable
fun ConversesPreviewTheme(content: @Composable () -> Unit) {
  ConversesTheme(
    themeMode = ThemeMode.System,
    dynamicColor = false,
    themeColor = ThemeColor.Lavender,
    content = content,
  )
}

/** [ConversesPreviewTheme] amb fons `surface` i marge, per a previews de components. */
@Composable
fun ConversesPreviewSurface(content: @Composable () -> Unit) {
  ConversesPreviewTheme {
    Surface(color = MaterialTheme.colorScheme.surface) {
      Box(modifier = Modifier.padding(16.dp)) {
        content()
      }
    }
  }
}
