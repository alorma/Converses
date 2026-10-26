package com.alorma.contactnotes.ui.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.data.pro.NetworkMonitor
import com.alorma.contactnotes.data.pro.ProRepository
import com.alorma.contactnotes.data.pro.PurchaseResult
import com.alorma.contactnotes.data.pro.RestoreResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** Els beneficis de la pantalla de Pro (8a), en l'ordre del disseny. */
@Serializable
enum class ProBenefit {
  /** Origen `avatar`: forma i color de l'avatar, i el color de la persona. */
  People,

  /** Origen `canals`. */
  Channels,

  /** Origen `pendents`. */
  FollowUps,

  /** Origen `contactes`. */
  Contacts,
}

/** [highlight] (8b, d'on ve l'usuari) va primer; des d'Ajustos (`null`), l'ordre del disseny. */
fun proBenefits(highlight: ProBenefit?): List<ProBenefit> =
  ProBenefit.entries.sortedBy { if (it == highlight) 0 else 1 }

data class PaywallState(
  /** Pro ja comprat o restaurat: «Ja tens Pro» (8c). */
  val isPro: Boolean = false,
  /** Preu localitzat de Play; `null` fins que arriba o si no es pot comprar. */
  val price: String? = null,
  val isOnline: Boolean = true,
  /** Una compra o una restauració en curs. */
  val busy: Boolean = false,
) {
  val canBuy: Boolean get() = isOnline && price != null && !busy

  val canRestore: Boolean get() = isOnline && !busy
}

/** Snackbars de la pantalla de Pro. La cancel·lació no en porta cap. */
enum class PaywallMessage { Pending, Failed, NothingToRestore }

class PaywallViewModel(
  private val pro: ProRepository,
  network: NetworkMonitor,
) : ViewModel() {
  private val local = MutableStateFlow(PaywallState(isPro = pro.isPro.value))
  private val messages = Channel<PaywallMessage>(Channel.BUFFERED)

  val events: Flow<PaywallMessage> = messages.receiveAsFlow()

  val state: StateFlow<PaywallState> =
    combine(local, pro.isPro) { state, isPro -> state.copy(isPro = state.isPro || isPro) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), local.value)

  init {
    network.isOnline
      .onEach { online -> local.update { it.copy(isOnline = online) } }
      .filter { it && local.value.price == null }
      .onEach { loadPrice() }
      .launchIn(viewModelScope)
  }

  /** Sense esperar que algú observi [state] (amb `WhileSubscribed`, el seu `value` pot ser antic). */
  private fun current(): PaywallState = local.value.let { it.copy(isPro = it.isPro || pro.isPro.value) }

  private suspend fun loadPrice() {
    val price = pro.price() ?: return
    local.update { it.copy(price = price) }
  }

  fun onBuy(activity: Activity) {
    if (!current().canBuy) return
    viewModelScope.launch {
      local.update { it.copy(busy = true) }
      val result = pro.purchase(activity)
      local.update {
        when (result) {
          PurchaseResult.Purchased -> it.copy(busy = false, isPro = true)
          else -> it.copy(busy = false)
        }
      }
      when (result) {
        PurchaseResult.Pending -> messages.send(PaywallMessage.Pending)
        PurchaseResult.Failed, PurchaseResult.Offline -> messages.send(PaywallMessage.Failed)
        PurchaseResult.Purchased, PurchaseResult.Cancelled -> Unit
      }
    }
  }

  fun onRestore() {
    if (!current().canRestore) return
    viewModelScope.launch {
      local.update { it.copy(busy = true) }
      val result = pro.restore()
      local.update {
        when (result) {
          RestoreResult.Restored -> it.copy(busy = false, isPro = true)
          else -> it.copy(busy = false)
        }
      }
      when (result) {
        RestoreResult.NothingToRestore -> messages.send(PaywallMessage.NothingToRestore)
        RestoreResult.Failed, RestoreResult.Offline -> messages.send(PaywallMessage.Failed)
        RestoreResult.Restored -> Unit
      }
    }
  }
}
