package com.alorma.contactnotes.data.pro

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** D'on surt si l'usuari té l'entitlement `pro`. Cada variant en registra un a Koin (`proDataSourceModule`). */
interface ProDataSource {
  val isPro: StateFlow<Boolean>

  /** Es crida un cop en arrencar l'app (`ConversesApp`). */
  fun start() {}

  /** Preu localitzat de Pro; `null` sense connexió o si no es pot comprar (el botó queda desactivat). */
  suspend fun price(): String? = null

  suspend fun purchase(activity: Activity): PurchaseResult = PurchaseResult.Failed

  suspend fun restore(): RestoreResult = RestoreResult.NothingToRestore
}

/** Mai Pro: la release quan no té clau de RevenueCat. */
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

  /** Perquè el paywall es pugui provar sense xarxa: «comprar» encén el Pro de mentida. */
  override suspend fun price(): String = "Debug"

  override suspend fun purchase(activity: Activity): PurchaseResult {
    setPro(true)
    return PurchaseResult.Purchased
  }

  override suspend fun restore(): RestoreResult =
    if (state.value) RestoreResult.Restored else RestoreResult.NothingToRestore
}
