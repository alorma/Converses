package com.alorma.contactnotes.data.flags

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject

/** Injecta el flag i es torna a calcular si canvia el flag (menú de debug) o el Pro. */
@Composable
inline fun <reified T : FeatureFlag> rememberFeatureEnabled(): Boolean =
  rememberFeatureState<T>() == FeatureState.Unlocked

/** Com [rememberFeatureEnabled], per a la UI que ha d'ensenyar [FeatureState.Locked] (l'etiqueta PRO). */
@Composable
inline fun <reified T : FeatureFlag> rememberFeatureState(): FeatureState {
  val flag: T = koinInject()
  flag.runner.changes
    .collectAsState()
    .value
  return flag.state()
}
