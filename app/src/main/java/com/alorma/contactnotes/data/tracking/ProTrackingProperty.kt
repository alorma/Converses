package com.alorma.contactnotes.data.tracking

import com.alorma.contactnotes.data.pro.ProRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Manté la propietat comuna `is_pro` de l'[EventTracker] al dia amb [ProRepository.isPro]. Es fa des de fora perquè
 * l'[EventTracker] no depengui de [ProRepository] (que, a través de RevenueCat, ja depèn d'ell).
 */
class ProTrackingProperty(
  private val pro: ProRepository,
  private val tracker: EventTracker,
  private val scope: CoroutineScope,
) {
  /** Es crida un cop en arrencar l'app (`ConversesApp`). El valor inicial es posa abans de tornar. */
  fun start() {
    set(pro.isPro.value)
    scope.launch { pro.isPro.collect(::set) }
  }

  private fun set(isPro: Boolean) = tracker.setCommonProperty(KEY, isPro.toString())

  companion object {
    const val KEY = "is_pro"
  }
}
