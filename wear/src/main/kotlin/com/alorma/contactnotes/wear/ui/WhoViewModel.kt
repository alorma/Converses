package com.alorma.contactnotes.wear.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.shared.EntryKind
import com.alorma.contactnotes.shared.WearNote
import com.alorma.contactnotes.wear.data.NoteDelivery
import com.alorma.contactnotes.wear.data.NoteSender
import com.alorma.contactnotes.wear.data.NoteSubmitter
import com.alorma.contactnotes.wear.data.PhoneConnection
import com.alorma.contactnotes.wear.data.ProtectedSessions
import com.alorma.contactnotes.wear.data.StoredSnapshot
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface WhoUiState {
  data object Loading : WhoUiState

  /** La persona ja no existeix o és protegida: no s'hi pot apuntar res i la pantalla es tanca. */
  data object Unavailable : WhoUiState

  data class Choosing(
    val name: String,
    val initials: String,
    val color: AvatarColor,
    /** 0 = Jo, 1 = la persona, 2 = Apunt (el mateix ordre que `whoLabel`). */
    val selectedIndex: Int,
  ) : WhoUiState

  /**
   * La nota era per a una persona protegida (no va a cap cua ni al disc) i no ha arribat al mòbil: [phoneMissing] si el
   * mòbil no hi era; si no, l'ha rebutjada (el testimoni de l'aprovació ha caducat).
   */
  data class NotSent(
    val phoneMissing: Boolean,
  ) : WhoUiState

  /** Nota a la cua. [pendingPhone]: el mòbil no era accessible, així que s'enviarà quan torni. */
  data class Saved(
    val pendingPhone: Boolean,
  ) : WhoUiState
}

/**
 * Pantalles «Qui» i «Desat»: tria qui ho diu, deixa la nota a la cua i confirma. El text viu només en memòria i a la cua;
 * si la persona és una protegida oberta, la nota no passa per la cua (és al disc): s'envia directament i, si no arriba,
 * no es desa enlloc.
 */
class WhoViewModel(
  private val personId: Long,
  private val text: String,
  snapshots: Flow<StoredSnapshot?>,
  private val outbox: NoteSubmitter,
  private val phone: PhoneConnection,
  private val clock: () -> Long = System::currentTimeMillis,
  private val newId: () -> String = { UUID.randomUUID().toString() },
  /** Sessions de protegides obertes: si n'hi ha una per a la persona, la nota va directa i amb el testimoni. */
  private val sessions: ProtectedSessions = ProtectedSessions(),
  /** Enviament directe, sense cua: només per a notes a protegides, que no poden quedar al disc. */
  private val directSender: NoteSender = NoteSender { NoteDelivery.Unavailable },
) : ViewModel() {
  private val selected = MutableStateFlow(DefaultIndex)
  private val saved = MutableStateFlow<Boolean?>(null)
  private val notSent = MutableStateFlow<Boolean?>(null)
  private var submitting = false

  val state: StateFlow<WhoUiState> =
    combine(snapshots, selected, saved, notSent) { stored, index, pendingPhone, phoneMissing ->
      val person = stored?.snapshot?.people?.firstOrNull { it.id == personId }
      when {
        pendingPhone != null -> WhoUiState.Saved(pendingPhone)
        phoneMissing != null -> WhoUiState.NotSent(phoneMissing)
        person == null || person.isProtected -> WhoUiState.Unavailable
        else -> WhoUiState.Choosing(person.name, initialsOf(person.name), person.avatarColor, index)
      }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WhoUiState.Loading)

  fun select(index: Int) {
    if (index in 0..2) selected.value = index
  }

  /** Deixa la nota a la cua (amb UUID propi) i passa a «Desat». Un segon toc no en crea una altra. */
  fun save() {
    // Es marca abans de l'espera perquè un doble toc no dupliqui la nota
    if (submitting || state.value !is WhoUiState.Choosing) return
    submitting = true
    viewModelScope.launch {
      val note =
        WearNote(
          id = newId(),
          personId = personId,
          who = selected.value.toKind(),
          text = text.trim(),
          timestamp = clock(),
        )
      // Una protegida oberta: la nota no passa per la cua (que és al disc) sinó que s'envia ara, amb el testimoni
      val session = sessions.get(personId)
      if (session != null) {
        when (directSender.send(note.copy(approvalToken = session.noteToken))) {
          NoteDelivery.Delivered -> saved.value = false
          NoteDelivery.Unavailable -> notSent.value = true
          NoteDelivery.Rejected -> notSent.value = false
        }
        return@launch
      }
      val connected = phone.connected.first()
      outbox.submit(note)
      saved.value = !connected
    }
  }

  private fun Int.toKind(): EntryKind =
    when (this) {
      0 -> EntryKind.SAID
      1 -> EntryKind.HEARD
      else -> EntryKind.HAPPENED
    }

  private companion object {
    /** Per defecte, la persona. */
    const val DefaultIndex = 1
  }
}
