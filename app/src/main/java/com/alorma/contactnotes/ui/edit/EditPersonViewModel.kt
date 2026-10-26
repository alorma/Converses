package com.alorma.contactnotes.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.data.contacts.ContactsGateway
import com.alorma.contactnotes.data.contacts.ContactsPermission
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.db.PinnedFactEntity
import com.alorma.contactnotes.data.flags.ContactLinkFeature
import com.alorma.contactnotes.data.flags.FeatureState
import com.alorma.contactnotes.data.settings.SettingsRepository
import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.shared.AvatarShape
import com.alorma.contactnotes.ui.person.PersonExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FactDraft(
  val key: Long,
  val label: String,
  val value: String,
)

data class EditSnapshot(
  val name: String = "",
  val alias: String = "",
  val shape: AvatarShape? = null,
  val color: AvatarColor? = null,
  val isProtected: Boolean = false,
  val facts: List<FactDraft> = emptyList(),
)

/** Contacte del dispositiu vinculat: el que es desa a Room (C11). */
data class LinkedContact(
  val id: Long,
  val lookupKey: String,
  val name: String,
)

/** Secció «Contacte» de l'editor (`docs/contacts/disseny.md`, 5a, 5c i 5d). */
sealed interface ContactSection {
  /** Flag apagat: no es veu res. */
  data object Hidden : ContactSection

  /** Flag obert sense Pro: fila amb l'etiqueta PRO i sense acció fins al paywall. */
  data object Locked : ContactSection

  data object NotLinked : ContactSection

  /**
   * El contacte triat ja és d'una altra persona i no s'ha vinculat (C13). El missatge no diu de qui, perquè pot ser una
   * persona protegida.
   */
  data object Taken : ContactSection

  /** Sense permís també es mostra, perquè treure el vincle no en necessita. */
  data class Linked(
    val name: String,
  ) : ContactSection

  /** S'ha demanat el permís i no s'ha donat: no es pot vincular. */
  data object NoPermission : ContactSection
}

/** Què fa «Vincula un contacte»: l'explicació 5b la primera vegada, després el diàleg del sistema o el selector. */
enum class ContactLinkStep { Explain, RequestPermission, Pick }

data class ContactLinkState(
  val enabled: Boolean = false,
  /** Flag obert sense Pro: la fila surt amb l'etiqueta PRO i el vincle que hi hagi es conserva però no es mostra. */
  val locked: Boolean = false,
  val permissionGranted: Boolean = false,
  val explained: Boolean = false,
  val linked: LinkedContact? = null,
  val pickedTaken: Boolean = false,
) {
  val section: ContactSection
    get() =
      when {
        locked -> ContactSection.Locked
        !enabled -> ContactSection.Hidden
        linked != null -> ContactSection.Linked(linked.name)
        explained && !permissionGranted -> ContactSection.NoPermission
        pickedTaken -> ContactSection.Taken
        else -> ContactSection.NotLinked
      }

  val linkStep: ContactLinkStep
    get() =
      when {
        !explained -> ContactLinkStep.Explain
        !permissionGranted -> ContactLinkStep.RequestPermission
        else -> ContactLinkStep.Pick
      }
}

data class EditPersonUiState(
  val isNew: Boolean,
  val loaded: Boolean,
  val name: String = "",
  val alias: String = "",
  val shape: AvatarShape? = null,
  val color: AvatarColor? = null,
  val isProtected: Boolean = false,
  val facts: List<FactDraft> = emptyList(),
  val aliasTaken: Boolean = false,
  val entryCount: Int = 0,
  val baseline: EditSnapshot = EditSnapshot(),
  val contact: ContactLinkState = ContactLinkState(),
) {
  private val snapshot: EditSnapshot
    get() = EditSnapshot(name, alias, shape, color, isProtected, facts)

  val canSave: Boolean get() = loaded && name.isNotBlank() && !aliasTaken && (isNew || snapshot != baseline)
}

/** Accions de l'editor; les implementa el `ViewModel` i les previews les deixen buides. */
interface EditPersonActions {
  fun onName(value: String)

  fun onAlias(value: String)

  fun onShape(shape: AvatarShape)

  fun onColor(color: AvatarColor?)

  fun onProtected(value: Boolean)

  fun onAddFact()

  fun onFactChange(
    key: Long,
    label: String,
    value: String,
  )

  fun onRemoveFact(key: Long)

  /** L'URI que torna `PickContact`. */
  fun onContactPicked(contactUri: String)

  fun onUnlinkContact()

  /** «Continua» a l'explicació 5b: no es torna a mostrar. */
  fun onContactsExplained()

  /** Després del diàleg del sistema o en tornar a l'editor (el permís es pot haver canviat des dels ajustos). */
  fun onContactsPermissionChanged()

  fun onMoveFact(
    from: Int,
    to: Int,
  )

  fun delete(
    exportFirst: Boolean,
    onDeleted: () -> Unit,
  )

  fun save(onSaved: (Long) -> Unit)
}

class EditPersonViewModel(
  private val personId: Long,
  private val repository: ConversesRepository,
  private val exporter: PersonExporter,
  private val contactLink: ContactLinkFeature,
  private val contactsPermission: ContactsPermission,
  private val contacts: ContactsGateway,
  private val settings: SettingsRepository,
) : ViewModel(),
  EditPersonActions {
  private var original: PersonEntity? = null
  private var nextKey = 1L

  private val _state =
    MutableStateFlow(
      EditPersonUiState(
        isNew = personId == 0L,
        loaded = personId == 0L,
        contact =
          ContactLinkState(
            enabled = contactLink.isEnabled(),
            locked = contactLink.state() == FeatureState.Locked,
            permissionGranted = contactsPermission.isGranted(),
          ),
      ),
    )
  val state: StateFlow<EditPersonUiState> = _state

  init {
    if (personId != 0L) load()
    // El flag o el Pro poden canviar amb l'editor obert (debug, o en tornar del paywall)
    viewModelScope.launch {
      contactLink.observeState().collect { feature ->
        val enabled = feature == FeatureState.Unlocked
        val explained = enabled && settings.settings.first().contactsExplained
        val locked = feature == FeatureState.Locked
        _state.update {
          it.copy(contact = it.contact.copy(enabled = enabled, locked = locked, explained = explained))
        }
      }
    }
  }

  private fun load() {
    viewModelScope.launch {
      val person = repository.observePerson(personId).first() ?: return@launch
      val facts = repository.observePinnedFacts(personId).first()
      original = person
      val loadedFacts = facts.map { FactDraft(nextKey++, it.label, it.value) }
      _state.update { current ->
        EditPersonUiState(
          isNew = false,
          loaded = true,
          contact = current.contact.copy(linked = person.linkedContact()),
          name = person.name,
          alias = person.alias.orEmpty(),
          shape = person.avatarShape,
          color = person.avatarColor,
          isProtected = person.isProtected,
          facts = loadedFacts,
          entryCount = repository.getEntries(personId).size,
          baseline =
            EditSnapshot(
              name = person.name,
              alias = person.alias.orEmpty(),
              shape = person.avatarShape,
              color = person.avatarColor,
              isProtected = person.isProtected,
              facts = loadedFacts,
            ),
        )
      }
    }
  }

  override fun onName(value: String) = _state.update { it.copy(name = value) }

  override fun onAlias(value: String) {
    val alias = value.removePrefix("@")
    _state.update { it.copy(alias = alias) }
    viewModelScope.launch {
      val taken = alias.isNotBlank() && repository.isAliasTaken(alias, personId)
      _state.update { current -> if (current.alias == alias) current.copy(aliasTaken = taken) else current }
    }
  }

  override fun onShape(shape: AvatarShape) = _state.update { it.copy(shape = shape) }

  override fun onColor(color: AvatarColor?) = _state.update { it.copy(color = color) }

  /** En una persona que ja existeix, el canvi es desa a l'acte; en una de nova, es desa amb el perfil. */
  override fun onProtected(value: Boolean) {
    val person = original
    if (person == null) {
      _state.update { it.copy(isProtected = value) }
      return
    }
    _state.update { it.copy(isProtected = value, baseline = it.baseline.copy(isProtected = value)) }
    val updated = person.copy(isProtected = value)
    original = updated
    viewModelScope.launch { repository.setProtected(updated, value) }
  }

  override fun onAddFact() = _state.update { it.copy(facts = it.facts + FactDraft(nextKey++, "", "")) }

  override fun onFactChange(
    key: Long,
    label: String,
    value: String,
  ) = _state.update { state ->
    state.copy(facts = state.facts.map { if (it.key == key) it.copy(label = label, value = value) else it })
  }

  override fun onRemoveFact(key: Long) =
    _state.update { state ->
      state.copy(
        facts =
          state.facts.filterNot {
            it.key ==
              key
          },
      )
    }

  override fun onMoveFact(
    from: Int,
    to: Int,
  ) = _state.update { state ->
    if (from !in state.facts.indices || to !in state.facts.indices) {
      state
    } else {
      state.copy(facts = state.facts.toMutableList().apply { add(to, removeAt(from)) })
    }
  }

  /** Com [onProtected]: en una persona que ja existeix es desa a l'acte; en una de nova, amb el perfil. */
  override fun onContactPicked(contactUri: String) {
    viewModelScope.launch {
      val contact = runCatching { contacts.readPicked(contactUri) }.getOrNull() ?: return@launch
      // Un contacte, una persona (C13): les files a Contactes no diuen de qui són
      val taken =
        repository.observeLinkedPeople().first().any {
          it.id != personId && (it.contactId == contact.id || it.contactLookupKey == contact.lookupKey)
        }
      if (taken) {
        _state.update { it.copy(contact = it.contact.copy(pickedTaken = true)) }
        return@launch
      }
      val linked = LinkedContact(contact.id, contact.lookupKey, contact.name)
      _state.update { it.copy(contact = it.contact.copy(linked = linked, pickedTaken = false)) }
      val person = original ?: return@launch
      repository.linkContact(person, linked.id, linked.lookupKey, linked.name)
      original = person.copy(contactId = linked.id, contactLookupKey = linked.lookupKey, contactName = linked.name)
    }
  }

  /** Sense confirmació: no toca ni les notes ni la persona. */
  override fun onUnlinkContact() {
    _state.update { it.copy(contact = it.contact.copy(linked = null)) }
    val person = original ?: return
    original = person.copy(contactId = null, contactLookupKey = null, contactName = null)
    viewModelScope.launch { repository.unlinkContact(person) }
  }

  override fun onContactsExplained() {
    _state.update { it.copy(contact = it.contact.copy(explained = true)) }
    viewModelScope.launch { settings.setContactsExplained(true) }
  }

  override fun onContactsPermissionChanged() =
    _state.update { it.copy(contact = it.contact.copy(permissionGranted = contactsPermission.isGranted())) }

  override fun delete(
    exportFirst: Boolean,
    onDeleted: () -> Unit,
  ) {
    val person = original ?: return
    viewModelScope.launch {
      if (exportFirst) exporter.buildText(personId)?.let { exporter.share(person.name, it) }
      repository.deletePerson(person)
      onDeleted()
    }
  }

  override fun save(onSaved: (Long) -> Unit) {
    val current = _state.value
    if (!current.canSave) return
    viewModelScope.launch {
      val alias = current.alias.trim().takeIf { it.isNotBlank() }
      if (alias != null && repository.isAliasTaken(alias, personId)) {
        _state.update { it.copy(aliasTaken = true) }
        return@launch
      }
      val now = System.currentTimeMillis()
      val base = original
      val person =
        PersonEntity(
          id = personId,
          name = current.name.trim(),
          alias = alias,
          avatarShape = current.shape,
          avatarColor = current.color,
          isProtected = current.isProtected,
          archived = base?.archived ?: false,
          notes = base?.notes,
          createdAt = base?.createdAt ?: now,
          updatedAt = now,
          contactId = current.contact.linked?.id,
          contactLookupKey = current.contact.linked?.lookupKey,
          contactName = current.contact.linked?.name,
        )
      val facts =
        current.facts
          .filter { it.label.isNotBlank() || it.value.isNotBlank() }
          .map { PinnedFactEntity(personId = 0, label = it.label.trim(), value = it.value.trim(), position = 0) }
      onSaved(repository.savePerson(person, facts))
    }
  }
}

private fun PersonEntity.linkedContact(): LinkedContact? {
  val id = contactId ?: return null
  val lookupKey = contactLookupKey ?: return null
  return LinkedContact(id, lookupKey, contactName.orEmpty())
}
