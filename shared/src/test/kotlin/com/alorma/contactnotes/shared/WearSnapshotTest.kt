package com.alorma.contactnotes.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WearSnapshotTest {
  private fun entry(
    id: Long,
    at: Long = id,
    text: String = "text $id",
  ) = WearEntry(id, EntryKind.SAID, ChannelType.CHAT, text, at)

  private fun person(
    id: Long,
    isProtected: Boolean = false,
    entries: List<WearEntry> = emptyList(),
    lastEntryAt: Long? = null,
  ) = WearPerson(id, "Persona $id", AvatarColor.Sky, AvatarShape.Sunny, isProtected, lastEntryAt, entries)

  private fun snapshot(vararg people: WearPerson) =
    WearSnapshot(generatedAt = 1_000, appLockActive = true, people = people.toList())

  @Test
  fun roundTripKeepsEverything() {
    val original =
      snapshot(person(1, entries = listOf(entry(2), entry(1)), lastEntryAt = 2), person(2, isProtected = true))
    assertEquals(original, WearSnapshotCodec.decode(WearSnapshotCodec.encode(original)))
  }

  @Test
  fun protectedPersonNeverCarriesContent() {
    val leaky = snapshot(person(1, isProtected = true, entries = listOf(entry(1)), lastEntryAt = 99))
    val result = leaky.sanitized().people.single()
    assertTrue(result.entries.isEmpty())
    assertNull(result.lastEntryAt)
    assertEquals("Persona 1", result.name)
  }

  @Test
  fun encodingASnapshotWithLeakedContentDropsIt() {
    val leaky = snapshot(person(1, isProtected = true, entries = listOf(entry(1, text = "secret")), lastEntryAt = 5))
    assertTrue("secret" !in WearSnapshotCodec.encode(leaky))
  }

  @Test
  fun decodingAProtectedPersonWithContentDropsIt() {
    val raw =
      """{"version":1,"generatedAt":1,"appLockActive":false,"people":[""" +
        """{"id":1,"name":"A","avatarColor":"Sky","avatarShape":"Sunny",""" +
        """"isProtected":true,"lastEntryAt":7,"entries":[{"id":1,"kind":"SAID","channel":"CHAT","text":"x","occurredAt":7}]}]}"""
    val person = WearSnapshotCodec.decode(raw)!!.people.single()
    assertTrue(person.entries.isEmpty())
    assertNull(person.lastEntryAt)
  }

  @Test
  fun keepsOnlyTheLatestFiveEntriesNewestFirst() {
    val entries = (1L..8L).map { entry(it) }.shuffled()
    val result =
      snapshot(person(1, entries = entries))
        .sanitized()
        .people
        .single()
        .entries
    assertEquals(listOf(8L, 7L, 6L, 5L, 4L), result.map { it.id })
  }

  @Test
  fun longTextIsTruncatedWithAnEllipsis() {
    val long = "a".repeat(1_000)
    val text =
      snapshot(
        person(1, entries = listOf(entry(1, text = long))),
      ).sanitized().people.single().entries.single().text
    assertEquals(WEAR_MAX_ENTRY_TEXT_LENGTH, text.length)
    assertTrue(text.endsWith("…"))
  }

  @Test
  fun shortTextIsLeftAlone() {
    assertEquals("hola", "hola".truncatedForWear())
    val exact = "b".repeat(WEAR_MAX_ENTRY_TEXT_LENGTH)
    assertEquals(exact, exact.truncatedForWear())
  }

  @Test
  fun unknownProtocolVersionIsRejected() {
    val raw = """{"version":99,"generatedAt":1,"appLockActive":true,"people":[]}"""
    assertNull(WearSnapshotCodec.decode(raw))
  }

  @Test
  fun malformedInputIsRejected() {
    assertNull(WearSnapshotCodec.decode("not json"))
    assertNull(WearSnapshotCodec.decode("""{"version":1}"""))
  }

  @Test
  fun unknownFieldsFromNewerPhonesAreIgnored() {
    val raw = """{"version":1,"generatedAt":1,"appLockActive":true,"people":[],"future":"x"}"""
    assertEquals(true, WearSnapshotCodec.decode(raw)?.appLockActive)
  }

  @Test
  fun appLockStateIsIncluded() {
    val on = WearSnapshotCodec.decode(WearSnapshotCodec.encode(snapshot().copy(appLockActive = true)))!!
    val off = WearSnapshotCodec.decode(WearSnapshotCodec.encode(snapshot().copy(appLockActive = false)))!!
    assertTrue(on.appLockActive)
    assertTrue(!off.appLockActive)
  }
}
