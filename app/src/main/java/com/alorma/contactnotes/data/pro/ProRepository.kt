package com.alorma.contactnotes.data.pro

import kotlinx.coroutines.flow.StateFlow

/**
 * L'únic lloc on l'app pregunta si l'usuari és Pro (l'entitlement, mai el producte). Més endavant hi aniran la compra
 * i la restauració.
 */
class ProRepository(
  private val dataSource: ProDataSource,
) {
  val isPro: StateFlow<Boolean> get() = dataSource.isPro
}
