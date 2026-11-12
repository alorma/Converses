package com.alorma.contactnotes.data.sync

import com.alorma.contactnotes.data.db.CustomChannelDao
import com.alorma.contactnotes.data.db.CustomChannelEntity
import com.alorma.contactnotes.data.db.EntryDao
import com.alorma.contactnotes.data.db.EntryEntity
import com.alorma.contactnotes.data.db.PersonDao
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.db.PinnedFactDao
import com.alorma.contactnotes.data.db.PinnedFactEntity
import com.alorma.contactnotes.data.db.SyncTombstoneDao
import com.alorma.contactnotes.data.db.SyncTombstoneEntity
import com.alorma.contactnotes.data.db.TaskDao
import com.alorma.contactnotes.data.db.TaskEntity
import com.alorma.contactnotes.data.sync.record.ChannelRecord
import com.alorma.contactnotes.data.sync.record.EntryRecord
import com.alorma.contactnotes.data.sync.record.FactRecord
import com.alorma.contactnotes.data.sync.record.PersonRecord
import com.alorma.contactnotes.data.sync.record.SyncRecord
import com.alorma.contactnotes.data.sync.record.TaskRecord
import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.shared.AvatarShape
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * [LocalSyncSource] sobre els DAOs de Room. Tradueix entre files (amb ids locals) i registres (amb `syncId`), i escriu
 * el que ve de fora amb els mètodes dels DAOs que no segellen.
 */
class RoomLocalSyncSource(
  private val people: PersonDao,
  private val entries: EntryDao,
  private val tasks: TaskDao,
  private val channels: CustomChannelDao,
  private val facts: PinnedFactDao,
  private val tombstones: SyncTombstoneDao,
) : LocalSyncSource {
  override suspend fun readPending(): PendingChanges {
    val records = mutableListOf<PendingRecord>()
    channels.dirtyRows().forEach { records += PendingRecord(it.syncId, RecordType.Channel, channelRecord(it)) }
    people.dirtyRows().forEach { records += PendingRecord(it.syncId, RecordType.Person, personRecord(it)) }
    entries.dirtyRows().forEach { row ->
      // Una referència que ja no hi és (esborrada entre dues lectures) no es puja: l'esborrat ja la cobreix
      entryRecord(row)?.let { records += PendingRecord(row.syncId, RecordType.Entry, it) }
    }
    tasks.dirtyRows().forEach { row ->
      taskRecord(row)?.let { records += PendingRecord(row.syncId, RecordType.Task, it) }
    }
    val deletions = tombstones.dirty().map { PendingDeletion(it.syncId, it.deletedAt) }
    return PendingChanges(records, deletions)
  }

  private suspend fun personRecord(row: PersonEntity) =
    PersonRecord(
      name = row.name,
      alias = row.alias,
      avatarShape = row.avatarShape?.name,
      avatarColor = row.avatarColor?.name,
      isProtected = row.isProtected,
      archived = row.archived,
      notes = row.notes,
      createdAt = row.createdAt,
      facts = facts.getForPerson(row.id).map { FactRecord(it.label, it.value) },
      modifiedAt = row.updatedAt,
    )

  private suspend fun entryRecord(row: EntryEntity): EntryRecord? {
    val person = people.get(row.personId) ?: return null
    val channel = row.customChannelId?.let { channels.get(it) ?: return null }
    return EntryRecord(
      personId = person.syncId,
      kind = row.kind.name,
      channelType = row.channelType.name,
      text = row.text,
      occurredAt = row.occurredAt,
      customChannelId = channel?.syncId,
      modifiedAt = row.modifiedAt,
    )
  }

  private suspend fun taskRecord(row: TaskEntity): TaskRecord? {
    val person = people.get(row.personId) ?: return null
    return TaskRecord(
      personId = person.syncId,
      text = row.text,
      dueDate = row.dueDate,
      remind = row.remind,
      createdAt = row.createdAt,
      doneAt = row.doneAt,
      modifiedAt = row.modifiedAt,
    )
  }

  private fun channelRecord(row: CustomChannelEntity) =
    ChannelRecord(
      channelType = row.channelType.name,
      name = row.name,
      createdAt = row.createdAt,
      modifiedAt = row.modifiedAt,
    )

  override suspend fun markClean(
    syncId: String,
    type: RecordType,
    modifiedAt: Long,
  ) {
    when (type) {
      RecordType.Person -> people.markClean(syncId, modifiedAt)
      RecordType.Entry -> entries.markClean(syncId, modifiedAt)
      RecordType.Task -> tasks.markClean(syncId, modifiedAt)
      RecordType.Channel -> channels.markClean(syncId, modifiedAt)
    }
  }

  override suspend fun markDirty(
    syncId: String,
    type: RecordType,
  ) {
    when (type) {
      RecordType.Person -> people.markDirty(syncId)
      RecordType.Entry -> entries.markDirty(syncId)
      RecordType.Task -> tasks.markDirty(syncId)
      RecordType.Channel -> channels.markDirty(syncId)
    }
  }

  override suspend fun markDeletionUploaded(
    syncId: String,
    deletedAt: Long,
  ) = tombstones.deleteUploaded(syncId, deletedAt)

  override suspend fun localState(syncId: String): LocalState {
    people.getBySyncId(syncId)?.let { return LocalState.Live(RecordType.Person, it.updatedAt, it.dirty) }
    entries.getBySyncId(syncId)?.let { return LocalState.Live(RecordType.Entry, it.modifiedAt, it.dirty) }
    tasks.getBySyncId(syncId)?.let { return LocalState.Live(RecordType.Task, it.modifiedAt, it.dirty) }
    channels.getBySyncId(syncId)?.let { return LocalState.Live(RecordType.Channel, it.modifiedAt, it.dirty) }
    tombstones.get(syncId)?.let { return LocalState.Deleted(it.deletedAt) }
    return LocalState.Absent
  }

  override suspend fun applyRemote(
    syncId: String,
    record: SyncRecord,
  ): ApplyResult {
    val result =
      when (record) {
        is PersonRecord -> applyPerson(syncId, record)
        is EntryRecord -> applyEntry(syncId, record)
        is TaskRecord -> applyTask(syncId, record)
        is ChannelRecord -> applyChannel(syncId, record)
      }
    if (result == ApplyResult.Applied) tombstones.clear(syncId)
    return result
  }

  private suspend fun applyPerson(
    syncId: String,
    record: PersonRecord,
  ): ApplyResult {
    val shape = record.avatarShape?.let { enumOrNull<AvatarShape>(it) ?: return ApplyResult.Unreadable }
    val color = record.avatarColor?.let { enumOrNull<AvatarColor>(it) ?: return ApplyResult.Unreadable }
    people.applyRemote(
      PersonEntity(
        name = record.name,
        alias = record.alias,
        avatarShape = shape,
        avatarColor = color,
        isProtected = record.isProtected,
        archived = record.archived,
        notes = record.notes,
        createdAt = record.createdAt,
        updatedAt = record.modifiedAt,
        syncId = syncId,
        dirty = false,
      ),
      record.facts.map { PinnedFactEntity(personId = 0, label = it.label, value = it.value, position = 0) },
    )
    return ApplyResult.Applied
  }

  private suspend fun applyEntry(
    syncId: String,
    record: EntryRecord,
  ): ApplyResult {
    val kind = enumOrNull<EntryKind>(record.kind) ?: return ApplyResult.Unreadable
    val channelType = enumOrNull<ChannelType>(record.channelType) ?: return ApplyResult.Unreadable
    val person = people.getBySyncId(record.personId) ?: return ApplyResult.MissingReference
    val channelId =
      record.customChannelId?.let { (channels.getBySyncId(it) ?: return ApplyResult.MissingReference).id }
    entries.applyRemote(
      EntryEntity(
        personId = person.id,
        kind = kind,
        channelType = channelType,
        text = record.text,
        occurredAt = record.occurredAt,
        customChannelId = channelId,
        syncId = syncId,
        modifiedAt = record.modifiedAt,
        dirty = false,
      ),
    )
    return ApplyResult.Applied
  }

  private suspend fun applyTask(
    syncId: String,
    record: TaskRecord,
  ): ApplyResult {
    val person = people.getBySyncId(record.personId) ?: return ApplyResult.MissingReference
    tasks.applyRemote(
      TaskEntity(
        personId = person.id,
        text = record.text,
        dueDate = record.dueDate,
        remind = record.remind,
        createdAt = record.createdAt,
        doneAt = record.doneAt,
        syncId = syncId,
        modifiedAt = record.modifiedAt,
        dirty = false,
      ),
    )
    return ApplyResult.Applied
  }

  private suspend fun applyChannel(
    syncId: String,
    record: ChannelRecord,
  ): ApplyResult {
    val channelType = enumOrNull<ChannelType>(record.channelType) ?: return ApplyResult.Unreadable
    channels.findClash(channelType, record.name, syncId)?.let { return ApplyResult.DuplicateChannel(it.syncId) }
    channels.applyRemote(
      CustomChannelEntity(
        channelType = channelType,
        name = record.name,
        createdAt = record.createdAt,
        syncId = syncId,
        modifiedAt = record.modifiedAt,
        dirty = false,
      ),
    )
    return ApplyResult.Applied
  }

  override suspend fun applyRemoteDeletion(
    syncId: String,
    type: RecordType,
  ) {
    when (type) {
      RecordType.Person -> people.deleteFromRemote(syncId)
      RecordType.Entry -> entries.deleteFromRemote(syncId)
      RecordType.Task -> tasks.deleteFromRemote(syncId)
      RecordType.Channel -> channels.deleteFromRemote(syncId)
    }
    tombstones.clear(syncId)
  }

  override suspend fun addTombstone(
    syncId: String,
    deletedAt: Long,
  ) = tombstones.put(SyncTombstoneEntity(syncId, deletedAt))

  override suspend fun ownChannels(): List<ChannelInfo> =
    channels.all().map { ChannelInfo(it.syncId, it.channelType.name, it.name) }

  override suspend fun adoptChannelSyncId(
    localSyncId: String,
    remoteSyncId: String,
    now: Long,
  ) {
    val channel = channels.getBySyncId(localSyncId) ?: return
    channels.adoptSyncId(channel.id, remoteSyncId, now)
  }

  override suspend fun entryCount(): Int = entries.count()

  override fun observePendingCount(): Flow<Int> =
    combine(
      people.observeDirtyCount(),
      entries.observeDirtyCount(),
      tasks.observeDirtyCount(),
      channels.observeDirtyCount(),
      tombstones.observeDirtyCount(),
    ) { counts -> counts.sum() }

  private inline fun <reified E : Enum<E>> enumOrNull(name: String): E? =
    enumValues<E>().firstOrNull { it.name == name }
}
