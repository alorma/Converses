package com.alorma.contactnotes.data.sync.crypto

import java.security.SecureRandom

/**
 * Xifra i desxifra els registres d'un compte amb la seva clau de dades. El compte, l'identificador del registre i la
 * versió del format van a les dades associades: un registre copiat a un altre identificador o a un altre compte no
 * s'obre, encara que la clau sigui la mateixa.
 */
class RecordCipher(
  private val key: DataKey,
  private val uid: String,
  private val random: SecureRandom = SecureRandom(),
) {
  fun encrypt(
    recordId: String,
    plaintext: ByteArray,
  ): ByteArray = AesGcm.seal(key.bytes, plaintext, aad(recordId), random)

  /** `null` si no és un registre d'aquest compte amb aquest identificador, o si l'han manipulat o truncat. */
  fun decrypt(
    recordId: String,
    sealed: ByteArray,
  ): ByteArray? = AesGcm.open(key.bytes, sealed, aad(recordId))

  private fun aad(recordId: String): ByteArray = aadOf("record", SYNC_FORMAT.toString(), uid, recordId)
}
