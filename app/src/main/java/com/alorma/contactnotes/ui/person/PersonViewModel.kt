package com.alorma.contactnotes.ui.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.data.db.PersonEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PersonViewModel(
  personId: Long,
  repository: ConversesRepository,
) : ViewModel() {
  val person: StateFlow<PersonEntity?> = repository.observePerson(personId)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
