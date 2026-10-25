package com.alorma.contactnotes.ui.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.data.contacts.ContactsGateway
import com.alorma.contactnotes.data.contacts.ContactsPermission
import com.alorma.contactnotes.data.db.EntryEntity
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.db.PinnedFactEntity
import com.alorma.contactnotes.data.flags.ContactLinkFeature
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PersonDetailUiState(
  val person: PersonEntity? = null,
  val channels: List<ChannelType> = emptyList(),
  val facts: List<PinnedFactEntity> = emptyList(),
  val entries: List<EntryEntity> = emptyList(),
  val selectedKinds: Set<EntryKind> = EntryKind.entries.toSet(),
  val saidCount: Int = 0,
  val heardCount: Int = 0,
  val happenedCount: Int = 0,
  val isLoading: Boolean = true,
) {
  fun countOf(kind: EntryKind): Int =
    when (kind) {
      EntryKind.SAID -> saidCount
      EntryKind.HEARD -> heardCount
      EntryKind.HAPPENED -> happenedCount
    }
}

/** Vincle amb Contactes al detall (`docs/contacts/disseny.md`, 5e i 5f). */
sealed interface DetailContact {
  /** Sense vincle, flag apagat, sense permís o el proveïdor no s'ha pogut llegir: cap fila. */
  data object None : DetailContact

  /** [id] i [lookupKey] són els resolts ara, per obrir el contacte a Contactes. */
  data class Linked(
    val id: Long,
    val lookupKey: String,
    val name: String,
  ) : DetailContact

  /** El contacte s'ha esborrat de Contactes: es pot desvincular. */
  data object Deleted : DetailContact
}

@OptIn(ExperimentalCoroutinesApi::class)
class PersonDetailViewModel(
  private val personId: Long,
  private val repository: ConversesRepository,
  private val exporter: PersonExporter,
  private val contactLink: ContactLinkFeature,
  private val contactsPermission: ContactsPermission,
  private val contacts: ContactsGateway,
) : ViewModel() {
  private val selectedKinds = MutableStateFlow(EntryKind.entries.toSet())

  private val _contact = MutableStateFlow<DetailContact>(DetailContact.None)
  val contact: StateFlow<DetailContact> = _contact

  init {
    // Es resol en obrir el detall i cada vegada que el vincle canvia (per exemple, en tornar de l'editor)
    viewModelScope.launch {
      repository
        .observePerson(personId)
        .map { person -> person?.takeIf { it.contactId != null && it.contactLookupKey != null } }
        .distinctUntilChanged { old, new -> old?.linkKey() == new?.linkKey() }
        .collectLatest { person -> _contact.value = resolveContact(person) }
    }
  }

  private suspend fun resolveContact(person: PersonEntity?): DetailContact {
    if (person == null || !contactLink.isEnabled() || !contactsPermission.isGranted()) return DetailContact.None
    val contactId = person.contactId ?: return DetailContact.None
    val lookupKey = person.contactLookupKey ?: return DetailContact.None
    val resolved =
      runCatching { contacts.resolve(contactId, lookupKey) }
        .getOrElse { return DetailContact.None }
        ?: return DetailContact.Deleted
    // L'id, el lookup key (canvia en agregar) i el nom poden haver canviat: es desa el que hi ha ara
    if (resolved.id != contactId || resolved.lookupKey != lookupKey || resolved.name != person.contactName) {
      repository.linkContact(person, resolved.id, resolved.lookupKey, resolved.name)
    }
    return DetailContact.Linked(resolved.id, resolved.lookupKey, resolved.name)
  }

  fun unlinkContact() {
    viewModelScope.launch {
      val person = repository.observePerson(personId).first() ?: return@launch
      repository.unlinkContact(person)
    }
  }

  private val timeline =
    selectedKinds.flatMapLatest { kinds ->
      repository.observeTimeline(personId, kinds).map { kinds to it }
    }

  val uiState: StateFlow<PersonDetailUiState> =
    combine(
      repository.observePerson(personId),
      repository.observeChannels(personId),
      repository.observePinnedFacts(personId),
      repository.observeCounts(personId),
      timeline,
    ) { person, channels, facts, counts, (kinds, entries) ->
      val byKind = counts.associate { it.kind to it.count }
      PersonDetailUiState(
        person = person,
        channels = channels,
        facts = facts,
        entries = entries,
        selectedKinds = kinds,
        saidCount = byKind[EntryKind.SAID] ?: 0,
        heardCount = byKind[EntryKind.HEARD] ?: 0,
        happenedCount = byKind[EntryKind.HAPPENED] ?: 0,
        isLoading = false,
      )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonDetailUiState())

  /** Persones a les quals es pot moure una entrada. */
  val otherPeople: StateFlow<List<PersonEntity>> =
    repository
      .observePeople()
      .map { items -> items.map { it.person }.filter { it.id != personId } }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  fun onKindToggled(kind: EntryKind) {
    selectedKinds.value = selectedKinds.value.let { if (kind in it) it - kind else it + kind }
  }

  fun addEntry(
    kind: EntryKind,
    channel: ChannelType,
    text: String,
    occurredAt: Long?,
  ) {
    viewModelScope.launch {
      repository.addEntry(
        EntryEntity(
          personId = personId,
          kind = kind,
          channelType = channel,
          text = text,
          occurredAt = occurredAt ?: System.currentTimeMillis(),
        ),
      )
    }
  }

  fun updateEntry(
    entry: EntryEntity,
    kind: EntryKind,
    channel: ChannelType,
    text: String,
    occurredAt: Long?,
  ) {
    viewModelScope.launch {
      repository.updateEntry(
        entry.copy(kind = kind, channelType = channel, text = text, occurredAt = occurredAt ?: entry.occurredAt),
      )
    }
  }

  fun moveEntry(
    entry: EntryEntity,
    toPersonId: Long,
  ) {
    viewModelScope.launch { repository.moveEntry(entry, toPersonId) }
  }

  fun deleteEntry(entry: EntryEntity) {
    viewModelScope.launch { repository.deleteEntry(entry) }
  }

  fun restoreEntry(entry: EntryEntity) {
    viewModelScope.launch { repository.addEntry(entry) }
  }

  suspend fun countDay(entry: EntryEntity): Int {
    val (from, to) = dayRange(entry)
    return repository.countEntriesInRange(personId, entry.channelType, from, to)
  }

  fun deleteDay(entry: EntryEntity) {
    viewModelScope.launch {
      val (from, to) = dayRange(entry)
      repository.deleteEntriesInRange(personId, entry.channelType, from, to)
    }
  }

  fun setArchived(
    archived: Boolean,
    onDone: () -> Unit,
  ) {
    val person = uiState.value.person ?: return
    viewModelScope.launch {
      repository.setArchived(person, archived)
      onDone()
    }
  }

  fun export() {
    viewModelScope.launch {
      val name = uiState.value.person?.name ?: return@launch
      exporter.buildText(personId)?.let { exporter.share(name, it) }
    }
  }

  private fun PersonEntity.linkKey() = contactId to contactLookupKey

  private fun dayRange(entry: EntryEntity): Pair<Long, Long> {
    val zone = ZoneId.systemDefault()
    val day = Instant.ofEpochMilli(entry.occurredAt).atZone(zone).toLocalDate()
    return day.atStartOfDay(zone).toInstant().toEpochMilli() to
      day
        .plusDays(1)
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()
  }
}
