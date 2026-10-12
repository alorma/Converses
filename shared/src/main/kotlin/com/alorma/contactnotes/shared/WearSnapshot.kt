package com.alorma.contactnotes.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Versió del contracte mòbil → rellotge. Es puja quan el format deixa de ser compatible. */
const val WEAR_PROTOCOL_VERSION = 1

/** Entrades per persona que viatgen al rellotge; la resta es demanen sota demanda i no es desen. */
const val WEAR_MAX_ENTRIES_PER_PERSON = 5

/** Text màxim d'una entrada al rellotge (la pantalla no en pot mostrar més). */
const val WEAR_MAX_ENTRY_TEXT_LENGTH = 280

/**
 * Còpia reduïda del mòbil per al rellotge. El mòbil és l'única font de veritat.
 * Una persona protegida mai porta contingut: vegeu [WearPerson].
 */
@Serializable
data class WearSnapshot(
  val version: Int = WEAR_PROTOCOL_VERSION,
  val generatedAt: Long,
  /** Si el Bloqueig de l'app és actiu al mòbil. Si no es pot saber, el mòbil envia `true`. */
  val appLockActive: Boolean,
  val people: List<WearPerson>,
)

/**
 * Una protegida només té `id`, nom, color, forma i `isProtected = true`: cap entrada, canal ni hora.
 * Les no protegides porten com a molt [WEAR_MAX_ENTRIES_PER_PERSON] entrades, de la més nova a la més antiga.
 */
@Serializable
data class WearPerson(
  val id: Long,
  val name: String,
  val avatarColor: AvatarColor,
  val avatarShape: AvatarShape,
  val isProtected: Boolean,
  val lastEntryAt: Long? = null,
  val entries: List<WearEntry> = emptyList(),
)

@Serializable
data class WearEntry(
  val id: Long,
  val kind: EntryKind,
  val channel: ChannelType,
  val text: String,
  val occurredAt: Long,
)

/** Talla el text perquè no passi de [WEAR_MAX_ENTRY_TEXT_LENGTH], afegint «…» si cal. */
fun String.truncatedForWear(): String =
  if (length <= WEAR_MAX_ENTRY_TEXT_LENGTH) this else take(WEAR_MAX_ENTRY_TEXT_LENGTH - 1).trimEnd() + "…"

/**
 * Aplica les regles de privacitat a qualsevol instantani, vingui d'on vingui: les protegides perden tot el contingut
 * i les altres es limiten a [WEAR_MAX_ENTRIES_PER_PERSON] entrades de text tallat. El rellotge ho torna a aplicar en rebre'l.
 */
fun WearSnapshot.sanitized(): WearSnapshot =
  copy(
    people =
      people.map { person ->
        if (person.isProtected) {
          person.copy(lastEntryAt = null, entries = emptyList())
        } else {
          person.copy(
            entries =
              person.entries
                .sortedByDescending { it.occurredAt }
                .take(WEAR_MAX_ENTRIES_PER_PERSON)
                .map { it.copy(text = it.text.truncatedForWear()) },
          )
        }
      },
  )

/** Serialització de l'instantani. Ignora camps nous per ser tolerant amb versions més recents del mòbil. */
object WearSnapshotCodec {
  private val json = Json { ignoreUnknownKeys = true }

  fun encode(snapshot: WearSnapshot): String = json.encodeToString(WearSnapshot.serializer(), snapshot.sanitized())

  /** `null` si el text està malmès o la versió del protocol no és la d'aquesta app. */
  fun decode(text: String): WearSnapshot? =
    try {
      json
        .decodeFromString(WearSnapshot.serializer(), text)
        .takeIf { it.version == WEAR_PROTOCOL_VERSION }
        ?.sanitized()
    } catch (_: SerializationException) {
      null
    } catch (_: IllegalArgumentException) {
      null
    }
}
