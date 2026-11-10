package com.alorma.contactnotes.data.sync.crypto

import java.security.SecureRandom

/**
 * Xifra i desxifra els registres d'un compte. El compte, l'identificador del registre i la versió del format van a
 * les dades associades: un registre copiat a un altre identificador o a un altre compte no s'obre, encara que la clau
 * sigui la mateixa.
 *
 * La clau que xifra no és la clau de dades, sinó una de derivada només per als registres. Així la clau de dades pot
 * servir demà per a una altra cosa sense que una mateixa clau faci dues feines ni calgui tornar-ho a xifrar tot.
 *
 * El format està fixat per `RecordCipherTest.theRecordFormatIsFrozen`. `format` és el del document del compte.
 */
class RecordCipher(
  key: DataKey,
  private val uid: String,
  private val random: SecureRandom = SecureRandom(),
  private val format: Int = SYNC_FORMAT,
) {
  private val recordKey: ByteArray =
    Hkdf.sha256(ikm = key.bytes, salt = ByteArray(0), info = INFO, length = DataKey.SIZE_BYTES)

  fun encrypt(
    recordId: String,
    plaintext: ByteArray,
  ): ByteArray = AesGcm.seal(recordKey, plaintext, aad(recordId), random)

  /** `null` si no és un registre d'aquest compte amb aquest identificador, o si l'han manipulat o truncat. */
  fun decrypt(
    recordId: String,
    sealed: ByteArray,
  ): ByteArray? = AesGcm.open(recordKey, sealed, aad(recordId))

  private fun aad(recordId: String): ByteArray = aadOf("record", format.toString(), uid, recordId)

  private companion object {
    val INFO = "converses/sync/v1/record".toByteArray(Charsets.UTF_8)
  }
}
