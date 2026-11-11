package com.alorma.contactnotes.data.sync

import com.alorma.contactnotes.data.sync.crypto.Bip39
import com.alorma.contactnotes.data.sync.crypto.DataKey
import com.alorma.contactnotes.data.sync.crypto.RecoverySecret
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

/**
 * El contingut del fitxer de la clau abans de xifrar-lo ([AndroidSyncKeyStore]), en JVM pur perquè es pugui provar
 * sense Android:
 *
 * `versió (1 byte) ‖ llargada de l'UID (2 bytes, big-endian) ‖ UID en UTF-8 ‖ secret (16 bytes) ‖ clau de dades (32 bytes)`
 *
 * No té res a veure amb [com.alorma.contactnotes.data.sync.crypto.SYNC_FORMAT]: això no surt mai del dispositiu.
 */
object StoredKeysCodec {
  private const val VERSION: Byte = 1
  private const val UID_LENGTH_BYTES = 2
  private const val MAX_UID_BYTES = 0xFFFF

  fun encode(keys: StoredKeys): ByteArray {
    val uid = keys.uid.toByteArray(Charsets.UTF_8)
    require(uid.isNotEmpty() && uid.size <= MAX_UID_BYTES) { "The uid does not fit in the stored keys" }
    val secret = keys.secret.bytes
    val dataKey = keys.dataKey.bytes
    return ByteBuffer
      .allocate(1 + UID_LENGTH_BYTES + uid.size + secret.size + dataKey.size)
      .put(VERSION)
      .putShort(uid.size.toShort())
      .put(uid)
      .put(secret)
      .put(dataKey)
      .array()
      .also {
        secret.fill(0)
        dataKey.fill(0)
      }
  }

  /** `null` si no és un fitxer sencer d'una versió coneguda: massa curt, massa llarg, una altra versió o brossa. */
  fun decode(bytes: ByteArray): StoredKeys? {
    val buffer = ByteBuffer.wrap(bytes)
    if (buffer.remaining() < 1 + UID_LENGTH_BYTES || buffer.get() != VERSION) return null
    val uidSize = buffer.getShort().toInt() and MAX_UID_BYTES
    if (uidSize == 0 || buffer.remaining() != uidSize + Bip39.ENTROPY_BYTES + DataKey.SIZE_BYTES) return null
    val uid = decodeUtf8(ByteArray(uidSize).also(buffer::get)) ?: return null
    val secret = ByteArray(Bip39.ENTROPY_BYTES).also(buffer::get)
    val dataKey = ByteArray(DataKey.SIZE_BYTES).also(buffer::get)
    return StoredKeys(uid, RecoverySecret(secret), DataKey(dataKey)).also {
      secret.fill(0)
      dataKey.fill(0)
    }
  }

  /** Estricte: uns bytes que no són UTF-8 no es converteixen en un UID amb caràcters de substitució. */
  private fun decodeUtf8(bytes: ByteArray): String? =
    try {
      Charsets.UTF_8
        .newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes))
        .toString()
    } catch (_: CharacterCodingException) {
      null
    }
}
