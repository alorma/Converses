package com.alorma.contactnotes.data.contacts

import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.flags.ContactLinkFeature
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Manté les files de Contactes al dia amb Room (`docs/contacts`, «Reconciliació»): cada cop que canvien les persones
 * vinculades (desar, protegir, esborrar, vincular), el flag o es crida [refresh] (l'app torna a primer pla: permisos,
 * compte esborrada o idioma nou), calcula un [ContactsPlan] i l'aplica. Si falla (per exemple, el permís s'ha retirat
 * entremig), el següent canvi ho torna a provar.
 */
@OptIn(FlowPreview::class)
class ContactsReconciler(
  private val repository: ConversesRepository,
  private val contactLink: ContactLinkFeature,
  private val permission: ContactsPermission,
  private val gateway: ContactsGateway,
  private val scope: CoroutineScope,
  private val debounceMillis: Long = DEBOUNCE_MILLIS,
) {
  private val ticks = MutableStateFlow(0)

  fun start() {
    scope.launch {
      combine(
        repository.observeLinkedPeople(),
        contactLink.runner.changes
          .map { contactLink.isEnabled() }
          .distinctUntilChanged(),
        ticks,
      ) { people, enabled, _ -> people to enabled }
        .debounce(debounceMillis)
        .collectLatest { (people, enabled) -> reconcile(people, enabled) }
    }
  }

  /** Torna a comprovar-ho tot encara que Room no hagi canviat. */
  fun refresh() = ticks.update { it + 1 }

  private suspend fun reconcile(
    people: List<PersonEntity>,
    enabled: Boolean,
  ) {
    try {
      val linked = people.mapNotNull { it.toLinked() }
      val granted = permission.isGranted()
      val plan =
        ContactsPlan.of(
          people = linked,
          enabled = enabled,
          granted = granted,
          hasAccount = gateway.hasAccount(),
          rows = if (granted) gateway.rows() else emptyList(),
          labels = gateway.rowLabels(),
        )
      apply(plan)
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      // No es registra res: el següent canvi o la propera vegada que s'obri l'app ho tornarà a provar
    }
  }

  private suspend fun apply(plan: ContactsPlan) {
    if (plan.ensureAccount) gateway.ensureAccount()
    plan.remove.forEach { gateway.removeRows(it) }
    plan.create.forEach { person ->
      val contact = gateway.addRows(person.id, person.contactId, person.lookupKey)
      when {
        // El contacte s'ha esborrat: fora les files velles, que ara serien un contacte sense nom
        contact == null -> gateway.removeRows(person.id)
        contact.id != person.contactId || contact.lookupKey != person.lookupKey ->
          repository.refreshContact(person.id, person.lookupKey, contact)
      }
    }
    if (plan.removeAccount) gateway.removeAccount()
  }

  private fun PersonEntity.toLinked(): LinkedPerson? {
    val contactId = contactId ?: return null
    val lookupKey = contactLookupKey ?: return null
    return LinkedPerson(id = id, contactId = contactId, lookupKey = lookupKey, isProtected = isProtected)
  }

  private companion object {
    const val DEBOUNCE_MILLIS = 500L
  }
}
