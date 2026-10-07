package com.alorma.contactnotes.data.flags

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject

/** Injecta el flag i es torna a calcular si el menú de debug en canvia el valor. */
@Composable
inline fun <reified T : FeatureFlag> rememberFeatureEnabled(): Boolean {
  val flag: T = koinInject()
  flag.runner.changes.collectAsState().value
  return flag.isEnabled()
}
