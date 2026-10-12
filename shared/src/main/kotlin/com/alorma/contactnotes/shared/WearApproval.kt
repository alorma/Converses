package com.alorma.contactnotes.shared

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Camí del missatge (`MessageClient`) amb el qual el rellotge demana obrir una persona protegida. */
const val WEAR_APPROVAL_PATH = "/converses/approval"

/** Camí del missatge amb el qual el rellotge cancel·la una petició (retira la notificació del mòbil). */
const val WEAR_APPROVAL_CANCEL_PATH = "/converses/approval/cancel"

/** Camí del missatge amb el qual el mòbil envia al rellotge el resultat d'una petició. */
const val WEAR_APPROVAL_RESULT_PATH = "/converses/approval/result"

/** Vigència d'una petició: passat aquest temps el mòbil la tracta com a caducada. El temps el valida el mòbil. */
const val WEAR_APPROVAL_TTL_MS = 60_000L

/**
 * Petició d'aprovació d'una persona protegida. [requestId] és un UUID d'un sol ús: cada intent (també «Reintenta»)
 * en porta un de nou, i una aprovació no es reutilitza mai.
 */
@Serializable
data class WearApprovalRequest(
  val version: Int = WEAR_PROTOCOL_VERSION,
  val requestId: String,
  val personId: Long,
)

/** Cancel·lació d'una petició pendent, per exemple quan l'usuari toca «Cancel·la» al rellotge. */
@Serializable
data class WearApprovalCancel(
  val version: Int = WEAR_PROTOCOL_VERSION,
  val requestId: String,
)

enum class WearApprovalStatus {
  /** El mòbil ha rebut la petició i espera l'aprovació de l'usuari. El resultat arriba per [WEAR_APPROVAL_RESULT_PATH]. */
  PENDING,

  /** No hi ha res a aprovar: la persona no existeix, és arxivada o no és protegida, la petició és repetida o massa freqüent. */
  REJECTED,
}

/** Resposta immediata del mòbil a la petició; no porta contingut. */
@Serializable
data class WearApprovalAck(
  val version: Int = WEAR_PROTOCOL_VERSION,
  val requestId: String,
  val status: WearApprovalStatus,
)

enum class WearApprovalDecision {
  APPROVED,

  /** L'usuari ha denegat, ha cancel·lat el `BiometricPrompt` o la biometria ha fallat. */
  DENIED,

  /** No s'ha aprovat dins [WEAR_APPROVAL_TTL_MS]. */
  EXPIRED,
}

/**
 * Resultat final d'una petició. Només [WearApprovalDecision.APPROVED] porta contingut: la primera pàgina d'entrades
 * de la persona i el [noteToken] de vigència curta (fins a [noteTokenExpiresAt], en mil·lisegons) que el rellotge
 * adjunta a les notes per a aquesta persona.
 */
@Serializable
data class WearApprovalResult(
  val version: Int = WEAR_PROTOCOL_VERSION,
  val requestId: String,
  val decision: WearApprovalDecision,
  val entries: List<WearEntry> = emptyList(),
  val hasMore: Boolean = false,
  val noteToken: String? = null,
  val noteTokenExpiresAt: Long? = null,
)

/** Una petició o cancel·lació és vàlida si té un `requestId` no buit i curt. */
private fun String.isValidRequestId() = isNotBlank() && length <= 64

fun WearApprovalRequest.isValid(): Boolean = requestId.isValidRequestId()

fun WearApprovalCancel.isValid(): Boolean = requestId.isValidRequestId()

/**
 * Aplica els límits de privacitat a qualsevol resultat, vingui d'on vingui: sense aprovació no hi ha entrades ni
 * testimoni, i amb aprovació les entrades es limiten a una pàgina de text tallat.
 */
fun WearApprovalResult.sanitized(): WearApprovalResult =
  if (decision != WearApprovalDecision.APPROVED) {
    WearApprovalResult(version = version, requestId = requestId, decision = decision)
  } else {
    copy(
      entries =
        entries
          .sortedWith(compareByDescending<WearEntry> { it.occurredAt }.thenByDescending { it.id })
          .take(WEAR_ENTRIES_PAGE_SIZE)
          .map { it.copy(text = it.text.truncatedForWear()) },
      noteToken = noteToken?.takeIf { noteTokenExpiresAt != null },
      noteTokenExpiresAt = noteTokenExpiresAt?.takeIf { noteToken != null },
    )
  }

/** Serialització de l'aprovació de protegides; tolerant amb camps nous, com [WearEntriesCodec]. */
object WearApprovalCodec {
  private val json =
    Json {
      ignoreUnknownKeys = true
      encodeDefaults = true
    }

  fun encodeRequest(request: WearApprovalRequest): String =
    json.encodeToString(WearApprovalRequest.serializer(), request)

  /** `null` si el text està malmès, la versió no és la d'aquesta app o la petició no és vàlida. */
  fun decodeRequest(text: String): WearApprovalRequest? =
    decode(text, WearApprovalRequest.serializer(), WearApprovalRequest::version)?.takeIf { it.isValid() }

  fun encodeCancel(cancel: WearApprovalCancel): String = json.encodeToString(WearApprovalCancel.serializer(), cancel)

  fun decodeCancel(text: String): WearApprovalCancel? =
    decode(text, WearApprovalCancel.serializer(), WearApprovalCancel::version)?.takeIf { it.isValid() }

  fun encodeAck(ack: WearApprovalAck): String = json.encodeToString(WearApprovalAck.serializer(), ack)

  fun decodeAck(text: String): WearApprovalAck? = decode(text, WearApprovalAck.serializer(), WearApprovalAck::version)

  fun encodeResult(result: WearApprovalResult): String =
    json.encodeToString(WearApprovalResult.serializer(), result.sanitized())

  fun decodeResult(text: String): WearApprovalResult? =
    decode(text, WearApprovalResult.serializer(), WearApprovalResult::version)?.sanitized()

  private fun <T> decode(
    text: String,
    serializer: KSerializer<T>,
    version: (T) -> Int,
  ): T? =
    try {
      json.decodeFromString(serializer, text).takeIf { version(it) == WEAR_PROTOCOL_VERSION }
    } catch (_: SerializationException) {
      null
    } catch (_: IllegalArgumentException) {
      null
    }
}
