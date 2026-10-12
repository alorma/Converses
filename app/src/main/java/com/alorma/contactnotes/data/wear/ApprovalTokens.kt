package com.alorma.contactnotes.data.wear

import com.alorma.contactnotes.shared.WEAR_NOTE_TOKEN_TTL_MS
import java.util.UUID

/** Testimoni de vigència curta que una aprovació dona per desar notes a una persona protegida. */
data class NoteToken(
  val value: String,
  val expiresAt: Long,
)

/**
 * Testimonis emesos en aprovar una persona protegida. Només viuen en memòria: si l'app mor, es perden i el rellotge
 * ha de demanar una aprovació nova. Un testimoni només val per a la persona per a la qual es va emetre.
 */
class ApprovalTokens(
  private val clock: () -> Long = System::currentTimeMillis,
  private val ttlMillis: Long = WEAR_NOTE_TOKEN_TTL_MS,
) {
  private val tokens = mutableMapOf<String, Pair<Long, Long>>()

  @Synchronized
  fun issue(personId: Long): NoteToken {
    val now = clock()
    tokens.values.removeAll { (_, expiresAt) -> expiresAt <= now }
    val token = NoteToken(UUID.randomUUID().toString(), now + ttlMillis)
    tokens[token.value] = personId to token.expiresAt
    return token
  }

  @Synchronized
  fun isValid(
    value: String?,
    personId: Long,
  ): Boolean {
    val (owner, expiresAt) = tokens[value ?: return false] ?: return false
    return owner == personId && expiresAt > clock()
  }
}
