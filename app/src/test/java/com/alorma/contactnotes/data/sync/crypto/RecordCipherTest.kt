package com.alorma.contactnotes.data.sync.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class RecordCipherTest {
  private val key = DataKey.generate()
  private val cipher = RecordCipher(key, uid = "u1")

  private fun ByteArray.contains(part: ByteArray): Boolean =
    (0..size - part.size).any { start -> part.indices.all { this[start + it] == part[it] } }

  @Test
  fun decryptsWhatItEncrypts() {
    listOf("Què em va dir la Marta? 🙂".toByteArray(), ByteArray(0), ByteArray(64 * 1024) { it.toByte() }).forEach {
      assertArrayEquals(it, cipher.decrypt("a", cipher.encrypt("a", it)))
    }
  }

  @Test
  fun aRecordMovedToAnotherIdDoesNotDecrypt() {
    assertNull(cipher.decrypt("b", cipher.encrypt("a", "hola".toByteArray())))
  }

  @Test
  fun aRecordMovedToAnotherAccountDoesNotDecrypt() {
    val sealed = cipher.encrypt("a", "hola".toByteArray())

    assertNull(RecordCipher(key, uid = "u2").decrypt("a", sealed))
  }

  @Test
  fun anotherKeyDoesNotDecrypt() {
    val sealed = cipher.encrypt("a", "hola".toByteArray())

    assertNull(RecordCipher(DataKey.generate(), uid = "u1").decrypt("a", sealed))
  }

  @Test
  fun aRecordInAnotherFormatOnlyOpensWhenAskedForThatFormat() {
    val sealed = RecordCipher(key, uid = "u1", format = 2).encrypt("a", "hola".toByteArray())

    assertNull(cipher.decrypt("a", sealed))
    assertArrayEquals("hola".toByteArray(), RecordCipher(key, uid = "u1", format = 2).decrypt("a", sealed))
  }

  /**
   * Fixa el format: si aquest test canvia, els registres que ja són al núvol deixen d'obrir-se. No es toca sense una
   * versió nova de [SYNC_FORMAT] i un camí per llegir l'antiga. La clau derivada i el text xifrat (`0f4547bb`) s'han
   * comprovat amb `openssl` (HKDF i AES-256-CTR); l'etiqueta surt d'aquesta implementació.
   */
  @Test
  fun theRecordFormatIsFrozen() {
    val frozen = RecordCipher(DataKey(ByteArray(32) { it.toByte() }), uid = "u1", random = Zeros())

    assertEquals(
      "000000000000000000000000" + "0f4547bb" + "ef1867bd877c335ba4b467669ec8fded",
      frozen.encrypt("a", "hola".toByteArray()).hex(),
    )
  }

  /** Els registres no es xifren amb la clau de dades directament, sinó amb una clau derivada només per a això. */
  @Test
  fun recordsAreNotSealedWithTheDataKeyItself() {
    val sealed = cipher.encrypt("a", "hola".toByteArray())

    assertNull(AesGcm.open(key.bytes, sealed, aadOf("record", "1", "u1", "a")))
  }

  @Test
  fun aWrappedKeyIsNotARecord() {
    val wrapped = KeyWrap.wrap(key, RecoverySecret.generate(), uid = "u1")

    assertNull(cipher.decrypt("a", wrapped))
  }

  @Test
  fun theSameRecordEncryptsDifferentlyEachTime() {
    val plaintext = "hola".toByteArray()

    assertFalse(cipher.encrypt("a", plaintext).contentEquals(cipher.encrypt("a", plaintext)))
  }

  @Test
  fun theCiphertextDoesNotContainThePlaintext() {
    val name = "Marta Soler".toByteArray()

    assertFalse(cipher.encrypt("a", name).contains(name))
  }

  @Test
  fun aTruncatedOrEmptyBlobDoesNotDecrypt() {
    assertNull(cipher.decrypt("a", ByteArray(0)))
    assertNull(cipher.decrypt("a", cipher.encrypt("a", "hola".toByteArray()).copyOf(20)))
  }

  @Test
  fun theOverheadIs28Bytes() {
    assertEquals(100 + 12 + 16, cipher.encrypt("a", ByteArray(100)).size)
  }
}
