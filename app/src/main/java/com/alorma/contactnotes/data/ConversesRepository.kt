package com.alorma.contactnotes.data

import com.alorma.contactnotes.data.db.CustomChannelDao
import com.alorma.contactnotes.data.db.CustomChannelEntity
import com.alorma.contactnotes.data.db.CustomChannelWithCount
import com.alorma.contactnotes.data.db.EntryDao
import com.alorma.contactnotes.data.db.EntryEntity
import com.alorma.contactnotes.data.db.KindCount
import com.alorma.contactnotes.data.db.PersonDao
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.db.PersonListItem
import com.alorma.contactnotes.data.db.PinnedFactDao
import com.alorma.contactnotes.data.db.PinnedFactEntity
import com.alorma.contactnotes.data.db.TaskDao
import com.alorma.contactnotes.data.db.TaskEntity
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Resultat de crear, reanomenar o canviar de grup un canal propi. */
sealed interface CustomChannelResult {
  data class Saved(
    val id: Long,
  ) : CustomChannelResult

  /** El nom és buit (o només espais). */
  data object NameBlank : CustomChannelResult

  /** Ja hi ha un canal amb aquest nom al grup, sense distingir majúscules. */
  data object NameTaken : CustomChannelResult
}

class ConversesRepository(
  private val personDao: PersonDao,
  private val entryDao: EntryDao,
  private val pinnedFactDao: PinnedFactDao,
  private val customChannelDao: CustomChannelDao,
  private val taskDao: TaskDao,
  /** L'hora de cada escriptura; els tests la fixen. */
  private val clock: () -> Long = System::currentTimeMillis,
) {
  // Sincronització (`docs/sync`): tota escriptura surt d'aquí segellada, és a dir, pendent de pujar i amb la data de
  // modificació, encara que l'entitat vingués neta d'una pantalla. `SyncIdentityTest` falla si un mètode se n'oblida.

  private fun PersonEntity.stamped(at: Long) = copy(updatedAt = at, dirty = true)

  private fun EntryEntity.stamped(at: Long) = copy(modifiedAt = at, dirty = true)

  private fun TaskEntity.stamped(at: Long) = copy(modifiedAt = at, dirty = true)

  private fun CustomChannelEntity.stamped(at: Long) = copy(modifiedAt = at, dirty = true)

  /** Les persones protegides no exposen última entrada, canals ni pendents. */
  fun observePeople(archived: Boolean = false): Flow<List<PersonListItem>> =
    personDao.observeList(archived).map { items ->
      items.map { item ->
        if (item.person.isProtected) {
          item.copy(lastEntry = null, channels = emptyList(), openTasks = 0)
        } else {
          item
        }
      }
    }

  fun observePerson(id: Long): Flow<PersonEntity?> = personDao.observe(id)

  /** Grups de canal usats a les entrades de la persona, sempre en l'ordre fix de [ChannelType]. */
  fun observeChannels(personId: Long): Flow<List<ChannelType>> =
    entryDao.observeChannelTypes(personId).map { it.distinct().sorted() }

  fun observePinnedFacts(personId: Long): Flow<List<PinnedFactEntity>> = pinnedFactDao.observeForPerson(personId)

  fun observeTimeline(
    personId: Long,
    kinds: Set<EntryKind> = EntryKind.entries.toSet(),
  ): Flow<List<EntryEntity>> = entryDao.observeTimeline(personId, kinds)

  fun observeCounts(personId: Long): Flow<List<KindCount>> = entryDao.observeCounts(personId)

  suspend fun savePerson(
    person: PersonEntity,
    facts: List<PinnedFactEntity>,
  ): Long = personDao.save(person.stamped(clock()), facts)

  suspend fun deletePerson(person: PersonEntity) = personDao.delete(person)

  suspend fun isAliasTaken(
    alias: String,
    excludePersonId: Long = 0,
  ): Boolean = personDao.countAliasConflicts(alias.trim(), excludePersonId) > 0

  suspend fun addEntry(entry: EntryEntity): Long = entryDao.insert(entry.stamped(clock()))

  suspend fun updateEntry(entry: EntryEntity) = entryDao.update(entry.stamped(clock()))

  suspend fun deleteEntry(entry: EntryEntity) = entryDao.delete(entry)

  suspend fun moveEntry(
    entry: EntryEntity,
    toPersonId: Long,
  ) = entryDao.update(entry.copy(personId = toPersonId).stamped(clock()))

  suspend fun setArchived(
    person: PersonEntity,
    archived: Boolean,
  ) = personDao.update(person.copy(archived = archived).stamped(clock()))

  suspend fun setProtected(
    person: PersonEntity,
    isProtected: Boolean,
  ) = personDao.update(person.copy(isProtected = isProtected).stamped(clock()))

  suspend fun countEntriesInRange(
    personId: Long,
    channel: ChannelType,
    from: Long,
    to: Long,
  ): Int = entryDao.countInRange(personId, channel, from, to)

  suspend fun deleteEntriesInRange(
    personId: Long,
    channel: ChannelType,
    from: Long,
    to: Long,
  ): Int = entryDao.deleteInRange(personId, channel, from, to)

  suspend fun getPerson(id: Long): PersonEntity? = personDao.get(id)

  suspend fun getEntries(personId: Long): List<EntryEntity> =
    entryDao.observeTimeline(personId, EntryKind.entries.toSet()).first()

  // Tasques (Pro): independents de les entrades. Marcar com a fet i desfer-ho funcionen també sense Pro.

  fun observeOpenTasks(personId: Long): Flow<List<TaskEntity>> = taskDao.observeOpen(personId)

  fun observeDoneTasks(personId: Long): Flow<List<TaskEntity>> = taskDao.observeDone(personId)

  suspend fun getTask(id: Long): TaskEntity? = taskDao.get(id)

  /** Crea (`id == 0`) o desa una tasca; el text es desa sense espais als extrems. */
  suspend fun saveTask(task: TaskEntity): Long {
    val trimmed = task.copy(text = task.text.trim()).stamped(clock())
    return if (trimmed.id == 0L) {
      taskDao.insert(trimmed)
    } else {
      taskDao.update(trimmed)
      trimmed.id
    }
  }

  suspend fun deleteTask(task: TaskEntity) = taskDao.delete(task)

  suspend fun markTaskDone(
    task: TaskEntity,
    at: Long = clock(),
  ) = taskDao.setDoneAt(task.id, doneAt = at, now = at)

  /** «Desfés» o tocar el check d'una feta: torna a pendents. */
  suspend fun reopenTask(task: TaskEntity) = taskDao.setDoneAt(task.id, doneAt = null, now = clock())

  // Canals propis (Pro): globals, únics dins del grup sense distingir majúscules.

  /** Tots els canals propis, en l'ordre dels grups ([ChannelType]) i, dins de cada grup, per nom. */
  fun observeCustomChannels(): Flow<List<CustomChannelWithCount>> =
    customChannelDao.observeAll().map { list -> list.sortedBy { it.channel.channelType.ordinal } }

  suspend fun getCustomChannel(id: Long): CustomChannelEntity? = customChannelDao.get(id)

  suspend fun createCustomChannel(
    channelType: ChannelType,
    name: String,
    now: Long = clock(),
  ): CustomChannelResult =
    validateName(channelType, name, excludeId = 0) { trimmed ->
      customChannelDao.insert(
        CustomChannelEntity(channelType = channelType, name = trimmed, createdAt = now).stamped(now),
      )
    }

  suspend fun renameCustomChannel(
    channel: CustomChannelEntity,
    name: String,
  ): CustomChannelResult =
    validateName(channel.channelType, name, excludeId = channel.id) { trimmed ->
      customChannelDao.update(channel.copy(name = trimmed).stamped(clock()))
      channel.id
    }

  /** Les entrades del canal s'hi mouen: el seu grup passa a ser [channelType]. */
  suspend fun changeCustomChannelGroup(
    channel: CustomChannelEntity,
    channelType: ChannelType,
  ): CustomChannelResult =
    validateName(channelType, channel.name, excludeId = channel.id) {
      val at = clock()
      customChannelDao.changeGroup(channel.stamped(at), channelType, now = at)
      channel.id
    }

  /**
   * Full «Nou canal» / «Edita el canal» (7b): crea el canal o en canvia el nom i el grup alhora.
   * Si el grup canvia, les seves entrades s'hi mouen; si el nom no és vàlid al grup nou, no es canvia res.
   */
  suspend fun saveCustomChannel(
    editing: CustomChannelEntity?,
    channelType: ChannelType,
    name: String,
  ): CustomChannelResult =
    if (editing == null) {
      createCustomChannel(channelType, name)
    } else {
      validateName(channelType, name, excludeId = editing.id) { trimmed ->
        val at = clock()
        customChannelDao.changeGroup(editing.copy(name = trimmed).stamped(at), channelType, now = at)
        editing.id
      }
    }

  /** Les seves entrades es queden al grup; no es perd cap nota. */
  suspend fun deleteCustomChannel(channel: CustomChannelEntity) = customChannelDao.delete(channel)

  private suspend fun validateName(
    channelType: ChannelType,
    name: String,
    excludeId: Long,
    save: suspend (String) -> Long,
  ): CustomChannelResult {
    val trimmed = name.trim()
    return when {
      trimmed.isEmpty() -> CustomChannelResult.NameBlank
      customChannelDao.countNameConflicts(channelType, trimmed, excludeId) > 0 -> CustomChannelResult.NameTaken
      else -> CustomChannelResult.Saved(save(trimmed))
    }
  }
}
