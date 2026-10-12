package com.alorma.contactnotes.wear.data

import com.alorma.contactnotes.shared.WearEntry
import com.alorma.contactnotes.shared.WearPerson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

/**
 * Entrades d'una persona protegida després d'una aprovació. **Només viuen en memòria**: no s'escriuen mai al disc ni a
 * la còpia, i s'esborren en sortir de la persona o quan l'app passa a segon pla. [noteToken] és el testimoni de
 * vigència curta que s'adjunta a les notes per a aquesta persona.
 */
data class ProtectedSession(
  val personId: Long,
  val entries: List<WearEntry>,
  val hasMore: Boolean,
  val noteToken: String?,
  val noteTokenExpiresAt: Long?,
)

class ProtectedSessions {
  private val sessions = MutableStateFlow<Map<Long, ProtectedSession>>(emptyMap())
  val all: StateFlow<Map<Long, ProtectedSession>> = sessions.asStateFlow()

  fun open(session: ProtectedSession) = sessions.update { it + (session.personId to session) }

  fun close(personId: Long) = sessions.update { it - personId }

  fun clear() = sessions.update { emptyMap() }

  fun get(personId: Long): ProtectedSession? = sessions.value[personId]

  companion object {
    /** Una de sola per procés: l'estat només viu mentre l'app és viva. */
    val shared = ProtectedSessions()
  }
}

/**
 * La còpia local amb les persones protegides que tenen una aprovació oberta passades a persones normals amb les
 * entrades de la sessió. Sense sessió, una protegida continua sense contingut (i la cronologia torna enrere).
 */
fun Flow<StoredSnapshot?>.withProtectedSessions(sessions: Flow<Map<Long, ProtectedSession>>): Flow<StoredSnapshot?> =
  combine(this, sessions) { stored, open ->
    stored?.copy(
      snapshot =
        stored.snapshot.copy(
          people =
            stored.snapshot.people.map { person ->
              val session = open[person.id]
              if (person.isProtected && session != null) person.opened(session) else person
            },
        ),
    )
  }

private fun WearPerson.opened(session: ProtectedSession): WearPerson =
  copy(isProtected = false, lastEntryAt = session.entries.maxOfOrNull { it.occurredAt }, entries = session.entries)
