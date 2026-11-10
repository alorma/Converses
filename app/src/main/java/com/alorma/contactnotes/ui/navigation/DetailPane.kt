package com.alorma.contactnotes.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.designsystem.components.WelcomeEmptyState

private val PanelMargin = 16.dp
private val PanelShape = RoundedCornerShape(28.dp)

/**
 * Fons d'una pantalla de persona: `surface` a pantalla completa i un to més fosc quan és el panell de detall d'una
 * pantalla gran, perquè el panell es distingeixi del llistat. Es crida dins de `PersonTheme`: pren el color de la
 * persona.
 */
@Composable
fun personContainerColor(inPane: Boolean): Color =
  if (inPane) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface

/**
 * Marc del panell de detall d'una pantalla gran: cantonades de 28 dp i 16 dp de marge a dalt, a baix i a la dreta. Amb
 * [inPane] fals (el mòbil) no fa res. [content] es crida sempre des del mateix lloc, perquè la pantalla no perdi
 * l'estat quan la finestra passa d'un panell a dos.
 */
@Composable
fun DetailPanel(
  inPane: Boolean,
  content: @Composable () -> Unit,
) {
  val modifier =
    if (inPane) {
      Modifier
        .fillMaxSize()
        // El panell queda dins de les barres del sistema; la pantalla de dins ja no les ha de tornar a descomptar
        .windowInsetsPadding(
          WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End),
        ).padding(top = PanelMargin, bottom = PanelMargin, end = PanelMargin)
        .clip(PanelShape)
    } else {
      Modifier.fillMaxSize()
    }
  Box(modifier = modifier) { content() }
}

/** Panell de detall quan no hi ha cap persona oberta. En arrencar no se n'obre cap automàticament. */
@Composable
fun EmptyDetailPane() {
  DetailPanel(inPane = true) {
    Box(
      modifier = Modifier.fillMaxSize().background(personContainerColor(inPane = true)),
      contentAlignment = Alignment.Center,
    ) {
      WelcomeEmptyState(
        icon = Icons.Rounded.Forum,
        title = stringResource(R.string.detail_empty_title),
        description = stringResource(R.string.detail_empty_description),
      )
    }
  }
}
