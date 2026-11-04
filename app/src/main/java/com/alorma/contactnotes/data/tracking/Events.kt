package com.alorma.contactnotes.data.tracking

import com.alorma.contactnotes.data.pro.PurchaseResult
import com.alorma.contactnotes.data.pro.RestoreResult

/**
 * Les coses que passen sense que l'usuari les demani directament. Igual que [Action], els paràmetres són valors
 * tancats. El desbloqueig d'una persona protegida no hi és mai: diria que n'hi ha.
 */
object Events {
  // Pro

  /** [late]: el resultat ha arribat després que la pantalla ja digués «pendent». */
  class PurchaseFinished(
    result: PurchaseResult,
    late: Boolean,
  ) : Event("purchase_finished", 1, mapOf("result" to result.tracked(), "late" to late.toString()))

  /** RevenueCat no ha respost en `PURCHASE_TIMEOUT` des que l'app ha tornat del full de Play. */
  class PurchaseTimedOut : Event("purchase_timed_out", 1)

  class RestoreFinished(
    result: RestoreResult,
  ) : Event("restore_finished", 1, mapOf("result" to result.tracked()))

  /** Pro canvia pel listener de RevenueCat (un pagament pendent confirmat, un reemborsament, un altre mòbil). */
  class ProChanged(
    active: Boolean,
  ) : Event("pro_changed", 1, mapOf("active" to active.toString()))

  // Bloqueig de l'app

  class AppLocked(
    relock: Boolean,
  ) : Event("app_locked", 1, mapOf("reason" to if (relock) "relock" else "cold_start"))

  /** El mòbil no té bloqueig de pantalla i l'app desactiva el seu. */
  class AppLockDisabled : Event("app_lock_disabled", 1, mapOf("reason" to "device_unprotected"))

  // Importar text

  /** Algú comparteix o selecciona text buit: surt un toast i l'app es tanca. */
  class TextImportEmpty(
    processText: Boolean,
  ) : Event("text_import_empty", 1, mapOf("source" to if (processText) "process_text" else "share"))
}

private fun PurchaseResult.tracked(): String =
  when (this) {
    PurchaseResult.Purchased -> "purchased"
    PurchaseResult.Pending -> "pending"
    PurchaseResult.Cancelled -> "cancelled"
    PurchaseResult.Offline -> "offline"
    PurchaseResult.Failed -> "failed"
  }

private fun RestoreResult.tracked(): String =
  when (this) {
    RestoreResult.Restored -> "restored"
    RestoreResult.NothingToRestore -> "nothing"
    RestoreResult.Pending -> "pending"
    RestoreResult.Offline -> "offline"
    RestoreResult.Failed -> "failed"
  }
