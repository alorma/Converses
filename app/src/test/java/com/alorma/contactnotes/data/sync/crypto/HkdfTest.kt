package com.alorma.contactnotes.data.sync.crypto

import org.junit.Assert.assertEquals
import org.junit.Test

/** Els vectors de l'RFC 5869, apèndix A (SHA-256). */
class HkdfTest {
  @Test
  fun rfc5869TestCase1() {
    val okm =
      Hkdf.sha256(
        ikm = ByteArray(22) { 0x0b },
        salt = unhex("000102030405060708090a0b0c"),
        info = unhex("f0f1f2f3f4f5f6f7f8f9"),
        length = 42,
      )

    assertEquals("3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865", okm.hex())
  }

  @Test
  fun rfc5869TestCase3() {
    val okm = Hkdf.sha256(ikm = ByteArray(22) { 0x0b }, salt = ByteArray(0), info = ByteArray(0), length = 42)

    assertEquals("8da4e775a563c18f715f802a063c5a31b8a11f5c5ee1879ec3454e5f3c738d2d9d201395faa4b61a96c8", okm.hex())
  }

  @Test
  fun derivesTheRequestedLength() {
    val ikm = ByteArray(16) { 1 }

    assertEquals(32, Hkdf.sha256(ikm, ByteArray(0), ByteArray(0), length = 32).size)
    assertEquals(80, Hkdf.sha256(ikm, ByteArray(0), ByteArray(0), length = 80).size)
  }
}

internal fun ByteArray.hex(): String = joinToString("") { "%02x".format(it) }

internal fun unhex(hex: String): ByteArray = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
