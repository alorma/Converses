package com.alorma.contactnotes.ui.sync

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.alorma.contactnotes.R

/**
 * Escriure les 12 paraules en un dispositiu nou (13j). [onClose] tanca la pantalla, tant si s'han acceptat (el compte
 * ja és a punt) com si l'usuari se'n va. Pendent (fase 3, tasca C): ara no hi ha cap camp.
 */
@Composable
fun EnterWordsScreen(onClose: () -> Unit) {
  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    TextButton(onClick = onClose) { Text(stringResource(R.string.close)) }
  }
}
