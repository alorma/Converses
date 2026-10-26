package com.alorma.contactnotes.data.pro

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** D'on surt si l'usuari té l'entitlement `pro`. Cada variant en registra un a Koin (`proDataSourceModule`). */
interface ProDataSource {
  val isPro: StateFlow<Boolean>
}

/** Mai Pro: el que porta la release fins que hi hagi RevenueCat (`docs/pro/fases.md`, fase 2). */
object NoProDataSource : ProDataSource {
  override val isPro: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
}

/** Pro de mentida, el commuta el menú de debug; cada canvi es passa a [persist] perquè es conservi. */
class LocalProDataSource(
  initial: Boolean = false,
  private val persist: (Boolean) -> Unit = {},
) : ProDataSource {
  private val state = MutableStateFlow(initial)

  override val isPro: StateFlow<Boolean> = state.asStateFlow()

  fun setPro(value: Boolean) {
    state.value = value
    persist(value)
  }
}
