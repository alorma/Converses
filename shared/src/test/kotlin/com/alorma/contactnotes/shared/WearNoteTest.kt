package com.alorma.contactnotes.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WearNoteTest {
  private fun note(
    id: String = "6f1c0b3e-0000-4000-8000-000000000001",
    text: String = "Li va agradar el llibre",
  ) = WearNote(id = id, personId = 7, who = EntryKind.HEARD, text = text, timestamp = 1_000)

  @Test
  fun noteRoundTrip() {
    val note = note()
    assertEquals(note, WearNoteCodec.decodeNote(WearNoteCodec.encodeNote(note)))
  }

  @Test
  fun longTextIsKeptUpToTheLimitAndRejectedAbove() {
    val ok = note(text = "x".repeat(WEAR_MAX_NOTE_TEXT_LENGTH))
    assertEquals(ok, WearNoteCodec.decodeNote(WearNoteCodec.encodeNote(ok)))
    assertNull(
      WearNoteCodec.decodeNote(WearNoteCodec.encodeNote(note(text = "x".repeat(WEAR_MAX_NOTE_TEXT_LENGTH + 1)))),
    )
  }

  @Test
  fun blankTextOrIdIsRejected() {
    assertNull(WearNoteCodec.decodeNote(WearNoteCodec.encodeNote(note(text = "  "))))
    assertNull(WearNoteCodec.decodeNote(WearNoteCodec.encodeNote(note(id = ""))))
  }

  @Test
  fun anotherVersionOrGarbageIsRejected() {
    assertNull(
      WearNoteCodec.decodeNote("""{"version":99,"id":"a","personId":1,"who":"SAID","text":"x","timestamp":1}"""),
    )
    assertNull(WearNoteCodec.decodeNote("no és json"))
    assertNull(WearNoteCodec.decodeResponse("no és json"))
  }

  @Test
  fun responseRoundTrip() {
    val response = WearNoteResponse(result = WearNoteResult.REJECTED)
    assertEquals(response, WearNoteCodec.decodeResponse(WearNoteCodec.encodeResponse(response)))
  }
}
