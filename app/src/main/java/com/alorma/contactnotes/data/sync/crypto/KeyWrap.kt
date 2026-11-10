package com.alorma.contactnotes.data.sync.crypto

import java.security.SecureRandom

/**
 * Embolcalla la clau de dades amb una clau derivada del secret de recuperació, per poder-la desar al document del
 * compte. Qui no té les paraules no la pot obrir, i unes paraules equivocades es detecten perquè no l'obren.
 * Canviar de paraules només obliga a tornar a embolcallar la clau, no a tornar a xifrar els registres.
 */
object KeyWrap {
  private val INFO = "converses/sync/v1/wrap".toByteArray(Charsets.UTF_8)

  fun wrap(
    dataKey: DataKey,
    secret: RecoverySecret,
    uid: String,
    random: SecureRandom = SecureRandom(),
  ): ByteArray = AesGcm.seal(wrappingKey(secret), dataKey.bytes, aad(uid), random)

  /** `null` si les paraules no són les d'aquest compte o si la clau embolcallada no és vàlida. */
  fun unwrap(
    wrapped: ByteArray,
    secret: RecoverySecret,
    uid: String,
  ): DataKey? =
    AesGcm
      .open(wrappingKey(secret), wrapped, aad(uid))
      ?.takeIf { it.size == DataKey.SIZE_BYTES }
      ?.let(::DataKey)

  /** El secret ja té 128 bits d'entropia: n'hi ha prou amb HKDF, sense cap derivació lenta com les de contrasenyes. */
  private fun wrappingKey(secret: RecoverySecret): ByteArray =
    Hkdf.sha256(ikm = secret.bytes, salt = ByteArray(0), info = INFO, length = DataKey.SIZE_BYTES)

  private fun aad(uid: String): ByteArray = aadOf("wrap", SYNC_FORMAT.toString(), uid)
}
