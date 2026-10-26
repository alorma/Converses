package com.alorma.contactnotes.data.flags

import com.alorma.contactnotes.data.pro.ProRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Infraestructura dels flags: diu en quin [FeatureState] és cada flag. */
abstract class FeatureFlagRunner {
  /** Canvia cada cop que canvia algun valor, perquè la UI es pugui tornar a calcular. */
  abstract val changes: StateFlow<Any>

  abstract fun state(flag: FeatureFlag): FeatureState

  fun isEnabled(flag: FeatureFlag): Boolean = state(flag) == FeatureState.Unlocked
}

/**
 * Cada flag val el seu valor per defecte (a la v1, apagat) tret que s'hi hagi posat un override. Els overrides els
 * posa només el menú de debug. Les lectures són de memòria; cada canvi es passa a [persist] perquè es conservi.
 * No sap res de Pro: un flag encès és [FeatureState.Unlocked].
 */
class LocalFeatureFlagRunner(
  initial: Map<String, Boolean> = emptyMap(),
  private val persist: (Map<String, Boolean>) -> Unit = {},
) : FeatureFlagRunner() {
  private val overrides = MutableStateFlow(initial)

  override val changes: StateFlow<Any> = overrides.asStateFlow()

  override fun state(flag: FeatureFlag): FeatureState =
    if (overrides.value[flag.key] ?: flag.defaultValue) FeatureState.Unlocked else FeatureState.Hidden

  fun setOverride(
    key: String,
    value: Boolean,
  ) = change { it + (key to value) }

  fun clearOverride(key: String) = change { it - key }

  fun hasOverride(key: String): Boolean = key in overrides.value

  fun clearAllOverrides() = change { emptyMap() }

  private fun change(transform: (Map<String, Boolean>) -> Map<String, Boolean>) {
    overrides.update(transform)
    persist(overrides.value)
  }
}

/**
 * El runner de l'app: [flags] diu si la funció és oberta (ara local, més endavant remot) i [pro] si l'usuari té
 * l'entitlement. Un flag Pro obert sense Pro és [FeatureState.Locked]; els que no són Pro no miren el Pro.
 */
class ProFeatureFlagRunner(
  private val flags: FeatureFlagRunner,
  private val pro: ProRepository,
  scope: CoroutineScope,
) : FeatureFlagRunner() {
  override val changes: StateFlow<Any> =
    combine(flags.changes, pro.isPro, ::Pair)
      .stateIn(scope, SharingStarted.Eagerly, flags.changes.value to pro.isPro.value)

  override fun state(flag: FeatureFlag): FeatureState =
    when {
      flags.state(flag) == FeatureState.Hidden -> FeatureState.Hidden
      !flag.pro || pro.isPro.value -> FeatureState.Unlocked
      else -> FeatureState.Locked
    }
}
