package com.alorma.contactnotes.data.sync.crypto

import java.security.SecureRandom
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AesGcmTest {
  private val key = ByteArray(32) { it.toByte() }
  private val aad = aadOf("record", "1", "uid", "id")
  private val random = SecureRandom()

  /** NIST, cas de prova 14: clau i nonce a zero, un bloc de zeros. Comprova el text xifrat, no només l'etiqueta. */
  @Test
  fun nistGcmVectorWithOneBlock() {
    val sealed = AesGcm.seal(ByteArray(32), ByteArray(16), ByteArray(0), Zeros())

    assertEquals(
      "000000000000000000000000" + "cea7403d4d606b6e074ec5d3baf39d18" + "d0d1c8a799996bf0265b98b5d48ab919",
      sealed.hex(),
    )
  }

  /** NIST, «The Galois/Counter Mode of Operation», cas de prova 13: clau i nonce a zero, sense text. */
  @Test
  fun nistGcmVectorWithAnEmptyMessage() {
    val sealed = AesGcm.seal(ByteArray(32), ByteArray(0), ByteArray(0), Zeros())

    assertEquals("000000000000000000000000" + "530f8afbc74536b9a963b4f1c4cb738b", sealed.hex())
  }

  @Test
  fun opensWhatItSeals() {
    val plaintext = "Què em va dir la Marta? 🙂".toByteArray()

    val sealed = AesGcm.seal(key, plaintext, aad, random)

    assertArrayEquals(plaintext, AesGcm.open(key, sealed, aad))
    assertEquals(plaintext.size + AesGcm.NONCE_BYTES + 16, sealed.size)
  }

  @Test
  fun everySealUsesAFreshNonce() {
    val plaintext = "hola".toByteArray()

    assertFalse(AesGcm.seal(key, plaintext, aad, random).contentEquals(AesGcm.seal(key, plaintext, aad, random)))
  }

  @Test
  fun aWrongKeyAadOrAFlippedBitDoesNotOpen() {
    val sealed = AesGcm.seal(key, "hola".toByteArray(), aad, random)

    assertNull(AesGcm.open(ByteArray(32) { 9 }, sealed, aad))
    assertNull(AesGcm.open(key, sealed, aadOf("record", "1", "uid", "other")))
    sealed.indices.forEach { index ->
      val tampered = sealed.copyOf().also { it[index] = (it[index].toInt() xor 1).toByte() }
      assertNull("byte $index", AesGcm.open(key, tampered, aad))
    }
  }

  @Test
  fun aTruncatedOrEmptyBlobDoesNotOpen() {
    val sealed = AesGcm.seal(key, "hola".toByteArray(), aad, random)

    assertNull(AesGcm.open(key, ByteArray(0), aad))
    assertNull(AesGcm.open(key, sealed.copyOf(11), aad))
    assertNull(AesGcm.open(key, sealed.copyOf(27), aad))
    assertNull(AesGcm.open(key, sealed.copyOf(sealed.size - 1), aad))
  }

  @Test
  fun aadFieldsCannotBeConfusedByMovingABoundary() {
    assertFalse(aadOf("ab", "c").contentEquals(aadOf("a", "bc")))
    assertFalse(aadOf("ab").contentEquals(aadOf("a", "b")))
    assertArrayEquals(aadOf("a", "b"), aadOf("a", "b"))
  }
}
