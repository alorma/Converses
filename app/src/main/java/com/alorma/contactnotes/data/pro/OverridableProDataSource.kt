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
 * El Pro del debug: el de [remote] (RevenueCat amb el Test Store), o Pro sense comprar si l'override del menú de debug
 * està encès. La compra i la restauració sempre van a [remote].
 */
class OverridableProDataSource(
  val remote: ProDataSource,
  initialOverride: Boolean = false,
  private val persistOverride: (Boolean) -> Unit = {},
  scope: CoroutineScope,
) : ProDataSource {
  private val overrideState = MutableStateFlow(initialOverride)

  val isOverridden: StateFlow<Boolean> = overrideState.asStateFlow()

  override val isPro: StateFlow<Boolean> =
    combine(overrideState, remote.isPro) { overridden, bought -> overridden || bought }
      .stateIn(scope, SharingStarted.Eagerly, initialOverride || remote.isPro.value)

  fun setOverride(value: Boolean) {
    overrideState.value = value
    persistOverride(value)
  }

  override fun start() = remote.start()

  override suspend fun price(): String? = remote.price()

  override suspend fun purchase(activity: Activity): PurchaseResult = remote.purchase(activity)

  override suspend fun restore(): RestoreResult = remote.restore()
}
