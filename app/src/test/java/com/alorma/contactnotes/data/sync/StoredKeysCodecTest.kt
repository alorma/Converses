package com.alorma.contactnotes.data.sync

import com.alorma.contactnotes.data.sync.crypto.DataKey
import com.alorma.contactnotes.data.sync.crypto.RecoverySecret
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class StoredKeysCodecTest {
  private val secret = RecoverySecret(ByteArray(16) { (it + 1).toByte() })
  private val dataKey = DataKey(ByteArray(32) { (0xA0 + it).toByte() })
  private val keys = StoredKeys("uid-1", secret, dataKey)

  @Test
  fun whatIsEncodedComesBackTheSame() {
    val decoded = StoredKeysCodec.decode(StoredKeysCodec.encode(keys))

    assertNotNull(decoded)
    assertEquals("uid-1", decoded!!.uid)
    assertEquals(secret, decoded.secret)
    assertEquals(dataKey, decoded.dataKey)
  }

  @Test
  fun aUidWithNonAsciiCharactersComesBackTheSame() {
    val decoded = StoredKeysCodec.decode(StoredKeysCodec.encode(StoredKeys("usuària-ñ-€", secret, dataKey)))

    assertEquals("usuària-ñ-€", decoded?.uid)
  }

  @Test
  fun theFileFormatIsFrozen() {
    val expected =
      byteArrayOf(1, 0, 5) + "uid-1".toByteArray(Charsets.UTF_8) + secret.bytes + dataKey.bytes

    assertArrayEquals(expected, StoredKeysCodec.encode(keys))
  }

  @Test
  fun aTruncatedFileIsNotRead() {
    val encoded = StoredKeysCodec.encode(keys)

    for (size in 0 until encoded.size) {
      assertNull("size $size", StoredKeysCodec.decode(encoded.copyOf(size)))
    }
  }

  @Test
  fun aFileWithBytesLeftOverIsNotRead() {
    assertNull(StoredKeysCodec.decode(StoredKeysCodec.encode(keys) + 0))
  }

  @Test
  fun anUnknownVersionIsNotRead() {
    val encoded = StoredKeysCodec.encode(keys)
    encoded[0] = 2

    assertNull(StoredKeysCodec.decode(encoded))
  }

  @Test
  fun garbageIsNotRead() {
    assertNull(StoredKeysCodec.decode(ByteArray(0)))
    assertNull(StoredKeysCodec.decode(ByteArray(64) { 0x7F }))
    assertNull(StoredKeysCodec.decode(ByteArray(51) { 0xFF.toByte() }))
  }

  @Test
  fun aUidLengthThatDoesNotMatchTheFileIsNotRead() {
    val encoded = StoredKeysCodec.encode(keys)
    encoded[2] = 6

    assertNull(StoredKeysCodec.decode(encoded))
  }

  @Test
  fun aUidThatIsNotUtf8IsNotRead() {
    val encoded = StoredKeysCodec.encode(keys)
    encoded[3] = 0xFF.toByte()

    assertNull(StoredKeysCodec.decode(encoded))
  }

  @Test
  fun anEmptyUidIsNeitherWrittenNorRead() {
    assertThrows(IllegalArgumentException::class.java) { StoredKeysCodec.encode(StoredKeys("", secret, dataKey)) }
    assertNull(StoredKeysCodec.decode(byteArrayOf(1, 0, 0) + secret.bytes + dataKey.bytes))
  }
}
