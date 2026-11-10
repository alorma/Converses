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
