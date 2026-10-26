package com.alorma.contactnotes.data.pro

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Pro fals per als tests: es commuta amb [setPro]. */
class LocalProDataSource(
  initial: Boolean = false,
) : ProDataSource {
  private val state = MutableStateFlow(initial)

  override val isPro: StateFlow<Boolean> = state.asStateFlow()

  fun setPro(value: Boolean) {
    state.value = value
  }
}
