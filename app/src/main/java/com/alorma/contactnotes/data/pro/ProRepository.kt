package com.alorma.contactnotes.data.pro

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/** L'únic lloc on l'app pregunta si l'usuari és Pro (l'entitlement, mai el producte), i on compra i restaura. */
class ProRepository(
  private val dataSource: ProDataSource,
) {
  val isPro: StateFlow<Boolean> get() = dataSource.isPro

  suspend fun price(): String? = dataSource.price()

  suspend fun purchase(activity: Activity): PurchaseResult = dataSource.purchase(activity)

  suspend fun restore(): RestoreResult = dataSource.restore()
}
