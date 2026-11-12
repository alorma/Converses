package com.alorma.contactnotes.data.sync.record

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordCodecTest {
  private val person =
    PersonRecord(
      name = "Ana",
      alias = "A",
      avatarShape = "Cookie9Sided",
      avatarColor = "Lavender",
      isProtected = true,
      archived = false,
      notes = null,
      createdAt = 1,
      facts = listOf(FactRecord("Birthday", "3 May")),
      modifiedAt = 2,
    )
  private val entry =
    EntryRecord("person-1", "SAID", "CHAT", "hola", occurredAt = 10, customChannelId = "chan-1", modifiedAt = 11)
  private val task =
    TaskRecord("person-1", "call", dueDate = 20_000, remind = false, createdAt = 5, doneAt = null, modifiedAt = 6)
  private val channel = ChannelRecord("CHAT", "Signal", createdAt = 3, modifiedAt = 4)

  private fun json(record: SyncRecord) = RecordCodec.encode(record).decodeToString()

  /**
   * Fixa el format: si aquest test canvia, els registres que ja són al núvol deixen d'obrir-se. No es toca sense
   * pujar `v` del tipus i un camí per llegir l'antic.
   */
  @Test
  fun theRecordFormatIsFrozen() {
    assertEquals(
      """{"type":"person","name":"Ana","alias":"A","avatarShape":"Cookie9Sided","avatarColor":"Lavender",""" +
        """"isProtected":true,"archived":false,"notes":null,"createdAt":1,""" +
        """"facts":[{"label":"Birthday","value":"3 May"}],"modifiedAt":2,"v":1}""",
      json(person),
    )
    assertEquals(
      """{"type":"entry","personId":"person-1","kind":"SAID","channelType":"CHAT","text":"hola","occurredAt":10,""" +
        """"customChannelId":"chan-1","modifiedAt":11,"v":1}""",
      json(entry),
    )
    assertEquals(
      """{"type":"task","personId":"person-1","text":"call","dueDate":20000,"remind":false,"createdAt":5,""" +
        """"doneAt":null,"modifiedAt":6,"v":1}""",
      json(task),
    )
    assertEquals(
      """{"type":"channel","channelType":"CHAT","name":"Signal","createdAt":3,"modifiedAt":4,"v":1}""",
      json(channel),
    )
    assertEquals("""{"deletedAt":99}""", RecordCodec.encodeDeleted(99).decodeToString())
  }

  @Test
  fun everyTypeRoundTrips() {
    listOf(person, entry, task, channel).forEach {
      assertEquals(it, RecordCodec.decode(RecordCodec.encode(it)))
    }
    assertEquals(DeletedRecord(99), RecordCodec.decodeDeleted(RecordCodec.encodeDeleted(99)))
  }

  @Test
  fun unknownFieldsAreIgnoredSoANewerAppCanAddThem() {
    val newer = """{"type":"channel","channelType":"CHAT","name":"Signal","createdAt":3,"modifiedAt":4,"v":2,"x":1}"""

    assertEquals(
      ChannelRecord("CHAT", "Signal", 3, 4, v = 2),
      RecordCodec.decode(newer.encodeToByteArray()),
    )
  }

  @Test
  fun anUnknownTypeOrBrokenJsonIsNotReadable() {
    assertNull(RecordCodec.decode("""{"type":"gadget","modifiedAt":1,"v":1}""".encodeToByteArray()))
    assertNull(RecordCodec.decode("not json".encodeToByteArray()))
    assertNull(RecordCodec.decodeDeleted("""{"nope":1}""".encodeToByteArray()))
  }

  @Test
  fun recordsNeverShowTheirContentInToString() {
    listOf(person, entry, task, channel).forEach {
      assertTrue(!it.toString().contains("Ana") && !it.toString().contains("hola") && !it.toString().contains("Signal"))
    }
  }
}
