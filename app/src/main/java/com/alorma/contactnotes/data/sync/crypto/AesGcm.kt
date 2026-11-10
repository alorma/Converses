package com.alorma.contactnotes.data.sync.crypto

import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM amb un nonce aleatori per xifratge. El resultat és `nonce (12 bytes) || text xifrat i etiqueta (16 bytes)`.
 * Les dades associades no es desen: qui obre les ha de tornar a donar iguals, i així un blob no serveix fora de lloc.
 */
internal object AesGcm {
  const val NONCE_BYTES = 12
  private const val TAG_BITS = 128
  private const val TAG_BYTES = TAG_BITS / 8
  private const val TRANSFORMATION = "AES/GCM/NoPadding"

  fun seal(
    key: ByteArray,
    plaintext: ByteArray,
    aad: ByteArray,
    random: SecureRandom,
  ): ByteArray {
    val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
    val cipher = cipher(Cipher.ENCRYPT_MODE, key, nonce, aad)
    return nonce + cipher.doFinal(plaintext)
  }

  /** `null` si la clau, les dades associades o el contingut no quadren, o si el blob és massa curt per ser-ho. */
  fun open(
    key: ByteArray,
    sealed: ByteArray,
    aad: ByteArray,
  ): ByteArray? {
    if (sealed.size < NONCE_BYTES + TAG_BYTES) return null
    return try {
      val cipher = cipher(Cipher.DECRYPT_MODE, key, sealed.copyOf(NONCE_BYTES), aad)
      cipher.doFinal(sealed, NONCE_BYTES, sealed.size - NONCE_BYTES)
    } catch (_: GeneralSecurityException) {
      // Etiqueta incorrecta: la clau no és aquesta o algú ha tocat el blob. No se'n diu res més
      null
    }
  }

  private fun cipher(
    mode: Int,
    key: ByteArray,
    nonce: ByteArray,
    aad: ByteArray,
  ): Cipher =
    Cipher.getInstance(TRANSFORMATION).apply {
      init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
      updateAAD(aad)
    }
}

/**
 * Dades associades a partir de camps de text. Cada camp va precedit de la seva llargada (4 bytes), perquè dos
 * conjunts de camps diferents no puguin donar mai els mateixos bytes.
 */
internal fun aadOf(vararg fields: String): ByteArray {
  val encoded = fields.map { it.toByteArray(Charsets.UTF_8) }
  val buffer = ByteBuffer.allocate(encoded.sumOf { Int.SIZE_BYTES + it.size })
  encoded.forEach { buffer.putInt(it.size).put(it) }
  return buffer.array()
}
