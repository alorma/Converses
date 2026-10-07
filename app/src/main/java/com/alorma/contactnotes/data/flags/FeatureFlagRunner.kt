package com.alorma.contactnotes.data.flags

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Infraestructura dels flags: de moment només local; més endavant hi pot haver una font remota o la de la compra. */
abstract class FeatureFlagRunner {
  /** Canvia cada cop que canvia algun valor, perquè la UI es pugui tornar a calcular. */
  abstract val changes: StateFlow<Any>

  fun isEnabled(flag: FeatureFlag): Boolean = getBoolean(flag.key, flag.defaultValue)

  protected abstract fun getBoolean(key: String, defaultValue: Boolean): Boolean
}

/**
 * Cada flag val el seu valor per defecte (a la v1, apagat) tret que s'hi hagi posat un override. Els overrides els
 * posa només el menú de debug i viuen en memòria; el menú és qui els desa i els torna a aplicar en arrencar.
 */
class LocalFeatureFlagRunner : FeatureFlagRunner() {
  private val overrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())

  override val changes: StateFlow<Any> = overrides.asStateFlow()

  override fun getBoolean(key: String, defaultValue: Boolean): Boolean = overrides.value[key] ?: defaultValue

  fun setOverride(key: String, value: Boolean) = overrides.update { it + (key to value) }

  fun clearOverride(key: String) = overrides.update { it - key }

  fun hasOverride(key: String): Boolean = key in overrides.value

  fun clearAllOverrides() = overrides.update { emptyMap() }
}
