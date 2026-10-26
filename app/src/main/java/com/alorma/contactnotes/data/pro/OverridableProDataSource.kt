package com.alorma.contactnotes.data.pro

import android.app.Activity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * El Pro del debug: amb l'override encès mana [local] (l'interruptor «Pro user» del menú de debug); apagat, [remote]
 * (RevenueCat amb el Test Store). [remote] arrenca sempre, perquè el menú en pugui ensenyar l'estat real.
 */
class OverridableProDataSource(
  val local: LocalProDataSource,
  val remote: ProDataSource,
  initialOverride: Boolean = false,
  private val persistOverride: (Boolean) -> Unit = {},
  scope: CoroutineScope,
) : ProDataSource {
  private val overrideState = MutableStateFlow(initialOverride)

  val isOverridden: StateFlow<Boolean> = overrideState.asStateFlow()

  override val isPro: StateFlow<Boolean> =
    combine(overrideState, local.isPro, remote.isPro) { overridden, localPro, remotePro ->
      if (overridden) localPro else remotePro
    }.stateIn(scope, SharingStarted.Eagerly, if (initialOverride) local.isPro.value else remote.isPro.value)

  private val active: ProDataSource get() = if (overrideState.value) local else remote

  fun setOverride(value: Boolean) {
    overrideState.value = value
    persistOverride(value)
  }

  override fun start() = remote.start()

  override suspend fun price(): String? = active.price()

  override suspend fun purchase(activity: Activity): PurchaseResult = active.purchase(activity)

  override suspend fun restore(): RestoreResult = active.restore()
}
