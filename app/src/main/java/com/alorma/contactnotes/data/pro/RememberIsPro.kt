package com.alorma.contactnotes.data.pro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject

/** Si l'usuari és Pro, per a la UI; es torna a calcular quan canvia (compra, restauració, baixada o menú de debug). */
@Composable
fun rememberIsPro(): Boolean {
  val pro: ProRepository = koinInject()
  val isPro by pro.isPro.collectAsStateWithLifecycle()
  return isPro
}
