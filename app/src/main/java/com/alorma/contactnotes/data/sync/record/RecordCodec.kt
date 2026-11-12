package com.alorma.contactnotes.data.sync.record

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Passa els registres a bytes (el que després xifra `RecordCipher`) i al revés. El format està fixat per
 * `RecordCodecTest.theRecordFormatIsFrozen`: si canvia, els registres que ja són al núvol deixen d'obrir-se.
 *
 * Els camps desconeguts s'ignoren, perquè una versió més nova de l'app en pugui afegir sense trencar les velles.
 */
object RecordCodec {
  private val json =
    Json {
      classDiscriminator = "type"
      ignoreUnknownKeys = true
      encodeDefaults = true
    }

  fun encode(record: SyncRecord): ByteArray = json.encodeToString(SyncRecord.serializer(), record).encodeToByteArray()

  /** `null` si no es pot llegir: un tipus que aquesta versió no coneix, o un JSON malmès. */
  fun decode(bytes: ByteArray): SyncRecord? =
    try {
      json.decodeFromString(SyncRecord.serializer(), bytes.decodeToString())
    } catch (_: SerializationException) {
      null
    } catch (_: IllegalArgumentException) {
      null
    }

  fun encodeDeleted(deletedAt: Long): ByteArray =
    json.encodeToString(DeletedRecord.serializer(), DeletedRecord(deletedAt)).encodeToByteArray()

  fun decodeDeleted(bytes: ByteArray): DeletedRecord? =
    try {
      json.decodeFromString(DeletedRecord.serializer(), bytes.decodeToString())
    } catch (_: SerializationException) {
      null
    } catch (_: IllegalArgumentException) {
      null
    }
}
