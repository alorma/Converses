package com.alorma.contactnotes.data.sync.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class KeyWrapTest {
  private val secret = RecoverySecret.generate()
  private val key = DataKey.generate()

  @Test
  fun unwrapsWithTheSameWords() {
    val wrapped = KeyWrap.wrap(key, secret, uid = "u1")

    val typed = RecoverySecret.fromWords(secret.words)!!

    assertEquals(key, KeyWrap.unwrap(wrapped, typed, uid = "u1"))
  }

  @Test
  fun otherWordsDoNotUnwrap() {
    val wrapped = KeyWrap.wrap(key, secret, uid = "u1")

    assertNull(KeyWrap.unwrap(wrapped, RecoverySecret.generate(), uid = "u1"))
  }

  @Test
  fun anotherAccountDoesNotUnwrap() {
    val wrapped = KeyWrap.wrap(key, secret, uid = "u1")

    assertNull(KeyWrap.unwrap(wrapped, secret, uid = "u2"))
  }

  @Test
  fun aTamperedWrappedKeyDoesNotUnwrap() {
    val wrapped = KeyWrap.wrap(key, secret, uid = "u1")
    wrapped[20] = (wrapped[20].toInt() xor 1).toByte()

    assertNull(KeyWrap.unwrap(wrapped, secret, uid = "u1"))
    assertNull(KeyWrap.unwrap(ByteArray(0), secret, uid = "u1"))
  }

  @Test
  fun theWrappedKeyHasTheExpectedSize() {
    assertEquals(12 + 32 + 16, KeyWrap.wrap(key, secret, uid = "u1").size)
  }

  @Test
  fun generatedSecretsAndKeysDiffer() {
    assertNotEquals(RecoverySecret.generate(), RecoverySecret.generate())
    assertNotEquals(DataKey.generate(), DataKey.generate())
    assertEquals(16, secret.bytes.size)
    assertEquals(32, key.bytes.size)
    assertEquals(12, secret.words.size)
  }

  @Test
  fun keysAreEqualByContentAndCopyTheirBytes() {
    val bytes = ByteArray(32) { 7 }
    val first = DataKey(bytes)
    bytes.fill(0)
    first.bytes.fill(0)

    assertEquals(DataKey(ByteArray(32) { 7 }), first)
    assertEquals(DataKey(ByteArray(32) { 7 }).hashCode(), first.hashCode())
    assertEquals(RecoverySecret(ByteArray(16) { 7 }), RecoverySecret(ByteArray(16) { 7 }))
  }

  @Test
  fun keysDoNotLeakInToString() {
    val known = RecoverySecret(ByteArray(16) { 0x7f })

    assertEquals("RecoverySecret", known.toString())
    assertEquals("DataKey", key.toString())
    assertFalse(known.words.any { it in known.toString() })
  }

  @Test
  fun fromWordsRejectsInvalidWords() {
    val swapped = "winner legal thank year wave sausage worth useful legal winner thank yellow".split(" ")

    assertNull(RecoverySecret.fromWords(swapped))
    assertNull(RecoverySecret.fromWords(listOf("legal")))
  }

  @Test
  fun rejectsKeysOfTheWrongSize() {
    assertThrows(IllegalArgumentException::class.java) { RecoverySecret(ByteArray(15)) }
    assertThrows(IllegalArgumentException::class.java) { DataKey(ByteArray(16)) }
  }
}
