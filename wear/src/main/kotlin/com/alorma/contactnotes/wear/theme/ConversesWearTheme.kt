package com.alorma.contactnotes.wear.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme

/** El rellotge és sempre fosc: sense color de tema, sense Pro. El color només és identitat (avatars). */
@Composable
fun ConversesWearTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = ConversesWearColorScheme,
    typography = ConversesWearTypography,
    content = content,
  )
}
