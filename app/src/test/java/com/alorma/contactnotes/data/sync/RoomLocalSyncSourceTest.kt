package com.alorma.contactnotes.data.sync

import com.alorma.contactnotes.data.sync.record.ChannelRecord
import com.alorma.contactnotes.data.sync.record.EntryRecord
import com.alorma.contactnotes.data.sync.record.FactRecord
import com.alorma.contactnotes.data.sync.record.PersonRecord
import com.alorma.contactnotes.data.sync.record.TaskRecord
import com.alorma.contactnotes.testing.FakeDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomLocalSyncSourceTest {
  private val db = FakeDb()
  private val source = db.syncSource

  @Test
  fun pendingRecordsReferToOtherRowsBySyncId() =
    runTest {
      val person = db.addPerson("Ana")
      val entry = db.addEntry(person.id)

      val pending = source.readPending()

      val record = pending.records.single { it.type == RecordType.Entry }.record as EntryRecord
      assertEquals(person.syncId, record.personId)
      assertEquals(entry.syncId, pending.records.single { it.type == RecordType.Entry }.syncId)
      assertEquals(2, pending.records.size)
    }

  @Test
  fun markCleanOnlyWorksIfTheRowIsUnchanged() =
    runTest {
      val person = db.addPerson("Ana")
      val entry = db.addEntry(person.id)

      source.markClean(entry.syncId, RecordType.Entry, modifiedAt = entry.modifiedAt + 1)
      assertTrue(
        db.entries.value
          .single()
          .dirty,
      )

      source.markClean(entry.syncId, RecordType.Entry, modifiedAt = entry.modifiedAt)
      assertTrue(
        !db.entries.value
          .single()
          .dirty,
      )

      source.markClean(person.syncId, RecordType.Person, modifiedAt = person.updatedAt + 1)
      assertTrue(
        db.people.value
          .single()
          .dirty,
      )
      source.markClean(person.syncId, RecordType.Person, modifiedAt = person.updatedAt)
      assertTrue(
        !db.people.value
          .single()
          .dirty,
      )
    }

  @Test
  fun applyingRemoteRowsDoesNotMakeThemPending() =
    runTest {
      val channelResult = source.applyRemote("c1", ChannelRecord("CHAT", "Signal", 1, 5))
      val personResult =
        source.applyRemote(
          "p1",
          PersonRecord("Ana", null, null, null, false, false, null, 1, listOf(FactRecord("a", "b")), 7),
        )
      val entryResult = source.applyRemote("e1", EntryRecord("p1", "SAID", "CHAT", "hi", 10, "c1", 8))
      val taskResult = source.applyRemote("t1", TaskRecord("p1", "call", null, false, 1, null, 9))

      assertEquals(listOf(ApplyResult.Applied), listOf(channelResult, personResult, entryResult, taskResult).distinct())
      assertTrue(source.readPending().isEmpty)
      assertEquals(0, source.observePendingCount().first())
      assertEquals(
        7,
        db.people.value
          .single()
          .updatedAt,
      )
      assertEquals(1, db.facts.value.size)
      assertEquals(
        db.channels.value
          .single()
          .id,
        db.entries.value
          .single()
          .customChannelId,
      )
    }

  @Test
  fun applyingTheSameRemoteRowAgainKeepsTheLocalIdAndReplacesTheFacts() =
    runTest {
      val record = PersonRecord("Ana", null, null, null, false, false, null, 1, listOf(FactRecord("a", "b")), 7)
      source.applyRemote("p1", record)
      val id =
        db.people.value
          .single()
          .id

      source.applyRemote("p1", record.copy(name = "Anna", modifiedAt = 9, facts = emptyList()))

      assertEquals(
        id,
        db.people.value
          .single()
          .id,
      )
      assertEquals(
        "Anna",
        db.people.value
          .single()
          .name,
      )
      assertTrue(db.facts.value.isEmpty())
    }

  @Test
  fun aRowWhoseReferenceIsMissingIsNotWritten() =
    runTest {
      assertEquals(
        ApplyResult.MissingReference,
        source.applyRemote("e1", EntryRecord("nobody", "SAID", "CHAT", "x", 1, null, 1)),
      )
      assertEquals(
        ApplyResult.MissingReference,
        source.applyRemote("t1", TaskRecord("nobody", "x", null, false, 1, null, 1)),
      )
      assertTrue(db.entries.value.isEmpty() && db.tasks.value.isEmpty())
    }

  @Test
  fun anUnknownEnumValueIsUnreadable() =
    runTest {
      source.applyRemote("p1", PersonRecord("Ana", null, null, null, false, false, null, 1, emptyList(), 1))

      assertEquals(
        ApplyResult.Unreadable,
        source.applyRemote("e1", EntryRecord("p1", "WHISPERED", "CHAT", "x", 1, null, 1)),
      )
    }

  @Test
  fun aRemoteDeletionLeavesNoTombstone() =
    runTest {
      val person = db.addPerson("Ana")
      db.addEntry(person.id)
      db.markAllClean()

      source.applyRemoteDeletion(person.syncId, RecordType.Person)

      assertTrue(db.people.value.isEmpty() && db.entries.value.isEmpty())
      assertTrue(db.tombstones.value.isEmpty())
      assertEquals(0, source.observePendingCount().first())
    }

  @Test
  fun aLocalDeletionIsPendingAndItsUploadClearsItOnlyIfUnchanged() =
    runTest {
      val person = db.addPerson("Ana")
      db.markAllClean()
      db.repository.deletePerson(person)
      val deletion = source.readPending().deletions.single()
      assertEquals(person.syncId, deletion.syncId)
      assertEquals(LocalState.Deleted(deletion.deletedAt), source.localState(person.syncId))

      source.markDeletionUploaded(person.syncId, deletion.deletedAt + 1)
      assertEquals(1, db.tombstones.value.size)
      source.markDeletionUploaded(person.syncId, deletion.deletedAt)
      assertTrue(db.tombstones.value.isEmpty())
      assertEquals(LocalState.Absent, source.localState(person.syncId))
    }

  @Test
  fun applyingALiveRecordClearsItsTombstone() =
    runTest {
      source.addTombstone("c1", 3)

      source.applyRemote("c1", ChannelRecord("CHAT", "Signal", 1, 5))

      assertNull(db.tombstones.value.firstOrNull())
    }

  @Test
  fun aChannelThatClashesWithAnotherIsReportedAndNotWritten() =
    runTest {
      val local = db.addCustomChannel("Signal")

      val result = source.applyRemote("other", ChannelRecord("CHAT", "Signal", 1, 5))

      assertEquals(ApplyResult.DuplicateChannel(local.syncId), result)
      assertEquals(1, db.channels.value.size)
    }

  @Test
  fun adoptingTheRemoteSyncIdKeepsTheRowAndRepointsItsEntries() =
    runTest {
      val local = db.addCustomChannel("Signal")
      val person = db.addPerson("Ana")
      db.addEntry(person.id, customChannelId = local.id)
      db.markAllClean()

      source.adoptChannelSyncId(local.syncId, "remote", now = 50)

      assertEquals(
        "remote",
        db.channels.value
          .single()
          .syncId,
      )
      assertEquals(
        local.id,
        db.channels.value
          .single()
          .id,
      )
      assertTrue(
        db.entries.value
          .single()
          .dirty,
      )
      assertEquals(
        local.syncId,
        db.tombstones.value
          .single()
          .syncId,
      )
      assertEquals(
        "remote",
        (
          source
            .readPending()
            .records
            .single()
            .record as EntryRecord
        ).customChannelId,
      )
    }
}
