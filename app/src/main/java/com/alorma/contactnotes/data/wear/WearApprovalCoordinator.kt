package com.alorma.contactnotes.data.wear

import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.shared.WEAR_APPROVAL_TTL_MS
import com.alorma.contactnotes.shared.WEAR_ENTRIES_PAGE_SIZE
import com.alorma.contactnotes.shared.WearApprovalAck
import com.alorma.contactnotes.shared.WearApprovalCodec
import com.alorma.contactnotes.shared.WearApprovalDecision
import com.alorma.contactnotes.shared.WearApprovalRequest
import com.alorma.contactnotes.shared.WearApprovalResult
import com.alorma.contactnotes.shared.WearApprovalStatus
import com.alorma.contactnotes.shared.WearEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Nom de la persona i del rellotge d'una petició pendent; [watchName] és `null` si el sistema no el dona. */
data class ApprovalDetails(
  val personName: String,
  val watchName: String?,
)

/** Avisa l'usuari al mòbil. La notificació és genèrica: no pot dur mai el nom de la persona ni cap contingut. */
interface ApprovalNotifier {
  fun show(requestId: String)

  fun cancel(requestId: String)
}

/** Envia el resultat al rellotge que ha fet la petició. */
fun interface ApprovalResultSender {
  suspend fun send(
    nodeId: String,
    payload: String,
  )
}

/**
 * Gestiona les peticions d'aprovació d'una persona protegida (P0). El filtre de privacitat d'aquest canal és aquí:
 *  - una persona inexistent, arxivada o no protegida no té res a aprovar;
 *  - cada petició és d'un sol ús i caduca als [WEAR_APPROVAL_TTL_MS]; una aprovació no es reutilitza mai;
 *  - el temps el valida el mòbil (el rellotge només dibuixa el compte enrere);
 *  - hi ha un límit de peticions pendents i per minut, perquè no es pugui inundar el mòbil de notificacions;
 *  - només les entrades d'aquella persona viatgen cap al rellotge, i només després d'[approve].
 *
 * Tot viu en memòria i no es registra mai cap contingut: si l'app mor, les peticions pendents es perden.
 */
class WearApprovalCoordinator(
  private val repository: ConversesRepository,
  private val notifier: ApprovalNotifier,
  private val sender: ApprovalResultSender,
  private val tokens: ApprovalTokens,
  private val clock: () -> Long = System::currentTimeMillis,
  private val watchNames: WatchNames = WatchNames { null },
) {
  private class Pending(
    val personId: Long,
    val nodeId: String,
    val createdAt: Long,
  )

  private val pending = mutableMapOf<String, Pending>()

  private val _cancelledByWatch = MutableSharedFlow<String>(extraBufferCapacity = 8)

  /** Peticions que el rellotge ha cancel·lat: la pantalla del mòbil que les tingui oberta es tanca. */
  val cancelledByWatch: Flow<String> = _cancelledByWatch.asSharedFlow()
  private val used = ArrayDeque<String>()
  private val recentRequests = ArrayDeque<Long>()

  /** Resposta immediata a una petició del rellotge; `null` si és il·legible (no es respon res). */
  suspend fun onRequest(
    nodeId: String,
    payload: String,
  ): String? {
    val request = WearApprovalCodec.decodeRequest(payload) ?: return null
    return WearApprovalCodec.encodeAck(
      WearApprovalAck(requestId = request.requestId, status = register(nodeId, request)),
    )
  }

  private suspend fun register(
    nodeId: String,
    request: WearApprovalRequest,
  ): WearApprovalStatus {
    val person = repository.getPerson(request.personId)
    if (person == null || person.archived || !person.isProtected) return WearApprovalStatus.REJECTED
    val accepted =
      synchronized(this) {
        val now = clock()
        purgeExpired(now)
        recentRequests.removeAll { now - it >= RATE_WINDOW_MS }
        when {
          request.requestId in pending || request.requestId in used -> false
          pending.size >= MAX_PENDING || recentRequests.size >= MAX_REQUESTS_PER_WINDOW -> false
          else -> {
            pending[request.requestId] = Pending(request.personId, nodeId, now)
            recentRequests.addLast(now)
            true
          }
        }
      }
    if (!accepted) return WearApprovalStatus.REJECTED
    notifier.show(request.requestId)
    return WearApprovalStatus.PENDING
  }

  /**
   * Qui demana què, per al text del `BiometricPrompt` («{rellotge} vol desbloquejar {persona}»). **Només per a aquest
   * prompt, que surt després que l'usuari actuï**: la notificació no duu mai el nom. `null` si la petició ja no és viva.
   */
  suspend fun details(requestId: String): ApprovalDetails? {
    val request = synchronized(this) { pending[requestId] } ?: return null
    val person = repository.getPerson(request.personId) ?: return null
    return ApprovalDetails(personName = person.name, watchName = watchNames.nameOf(request.nodeId))
  }

  /** Si la petició encara espera l'aprovació de l'usuari: la pantalla del mòbil ho comprova en obrir-se. */
  @Synchronized
  fun isPending(requestId: String): Boolean {
    purgeExpired(clock())
    return requestId in pending
  }

  /** El rellotge ha cancel·lat la petició (toc a «Cancel·la»): s'invalida i es retira la notificació. */
  fun onCancel(payload: String) {
    val cancel = WearApprovalCodec.decodeCancel(payload) ?: return
    if (take(cancel.requestId) != null) {
      notifier.cancel(cancel.requestId)
      _cancelledByWatch.tryEmit(cancel.requestId)
    }
  }

  /**
   * L'usuari ha aprovat al mòbil (biometria vàlida). Envia al rellotge les entrades de la persona i un testimoni per a
   * notes. Retorna `false` si ja no hi havia res a aprovar (desconeguda, repetida, cancel·lada o caducada) i llavors no
   * s'envia res: el rellotge ja mostra «Ha caducat» pel seu compte enrere. Si la petició caduca just en aprovar-la,
   * el rellotge rep un resultat «caducat» sense contingut.
   */
  suspend fun approve(requestId: String): Boolean {
    val request = take(requestId) ?: return false
    notifier.cancel(requestId)
    val expired = clock() - request.createdAt > WEAR_APPROVAL_TTL_MS
    val person = repository.getPerson(request.personId)
    val result =
      when {
        expired -> WearApprovalResult(requestId = requestId, decision = WearApprovalDecision.EXPIRED)
        person == null || person.archived ->
          WearApprovalResult(
            requestId = requestId,
            decision = WearApprovalDecision.DENIED,
          )
        else -> approvedResult(requestId, request.personId)
      }
    sender.send(request.nodeId, WearApprovalCodec.encodeResult(result))
    return result.decision == WearApprovalDecision.APPROVED
  }

  /** L'usuari ha denegat, ha cancel·lat el `BiometricPrompt` o la biometria ha fallat. */
  suspend fun deny(requestId: String) {
    val request = take(requestId) ?: return
    notifier.cancel(requestId)
    sender.send(
      request.nodeId,
      WearApprovalCodec.encodeResult(WearApprovalResult(requestId = requestId, decision = WearApprovalDecision.DENIED)),
    )
  }

  private suspend fun approvedResult(
    requestId: String,
    personId: Long,
  ): WearApprovalResult {
    // Una entrada de més per saber si en queden
    val page = repository.getEntriesBefore(personId, Long.MAX_VALUE, Long.MAX_VALUE, WEAR_ENTRIES_PAGE_SIZE + 1)
    val token = tokens.issue(personId)
    return WearApprovalResult(
      requestId = requestId,
      decision = WearApprovalDecision.APPROVED,
      entries =
        page
          .take(
            WEAR_ENTRIES_PAGE_SIZE,
          ).map { WearEntry(it.id, it.kind, it.channelType, it.text, it.occurredAt) },
      hasMore = page.size > WEAR_ENTRIES_PAGE_SIZE,
      noteToken = token.value,
      noteTokenExpiresAt = token.expiresAt,
    )
  }

  /** Treu la petició (d'un sol ús) i la dona per usada; `null` si no hi era. */
  @Synchronized
  private fun take(requestId: String): Pending? {
    val request = pending.remove(requestId) ?: return null
    used.addLast(requestId)
    while (used.size > MAX_USED_IDS) used.removeFirst()
    return request
  }

  // Les caducades se'n van de la memòria; el seu id es manté a `used` perquè no es pugui reutilitzar
  private fun purgeExpired(now: Long) {
    val expired = pending.filterValues { now - it.createdAt > WEAR_APPROVAL_TTL_MS }.keys
    for (id in expired) {
      pending.remove(id)
      used.addLast(id)
      notifier.cancel(id)
    }
    while (used.size > MAX_USED_IDS) used.removeFirst()
  }

  private companion object {
    const val MAX_PENDING = 3
    const val MAX_REQUESTS_PER_WINDOW = 5
    const val RATE_WINDOW_MS = 60_000L
    const val MAX_USED_IDS = 100
  }
}
