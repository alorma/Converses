package com.alorma.contactnotes.data.sync.crypto

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** HKDF amb SHA-256 (RFC 5869): deriva claus a partir d'un secret que ja té prou entropia. */
internal object Hkdf {
  private const val ALGORITHM = "HmacSHA256"
  private const val HASH_BYTES = 32

  fun sha256(
    ikm: ByteArray,
    salt: ByteArray,
    info: ByteArray,
    length: Int,
  ): ByteArray {
    require(length in 1..255 * HASH_BYTES) { "Invalid length" }
    // Extract: una sal buida és, segons l'RFC, un bloc de zeros de la mida del hash
    val pseudoRandomKey = hmac(key = if (salt.isEmpty()) ByteArray(HASH_BYTES) else salt, data = ikm)
    // Expand: T(n) = HMAC(PRK, T(n-1) || info || n)
    var block = ByteArray(0)
    var output = ByteArray(0)
    var counter = 1
    while (output.size < length) {
      block = hmac(pseudoRandomKey, block + info + counter.toByte())
      output += block
      counter++
    }
    return output.copyOf(length)
  }

  private fun hmac(
    key: ByteArray,
    data: ByteArray,
  ): ByteArray = Mac.getInstance(ALGORITHM).apply { init(SecretKeySpec(key, ALGORITHM)) }.doFinal(data)
}
