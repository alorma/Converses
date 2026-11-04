package com.alorma.contactnotes.data.flags

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Una funció que es pot amagar o mostrar, al estil de FireAndForget: cada flag defineix la seva clau i el valor per
 * defecte, i el [FeatureFlagRunner] diu en quin [FeatureState] és. Una funció apagada simplement no hi és; una de Pro
 * encesa, sense Pro, és [FeatureState.Locked].
 *
 * Una funció nova o a mitges neix amb el flag apagat i s'encén des del menú de debug fins que està llesta. Quan està
 * acabada, es treu el flag: les funcions Pro queden només darrere de `ProRepository.isPro` (`rememberIsPro()`).
 * Ara mateix no n'hi ha cap; cada flag nou es registra a Koin amb `bind FeatureFlag::class` perquè surti al menú de debug.
 */
abstract class FeatureFlag(
  val runner: FeatureFlagRunner,
  val key: String,
  val defaultValue: Boolean = false,
  /** Si és de Pro: només funciona amb l'entitlement `pro` ([ProFeatureFlagRunner]). */
  val pro: Boolean = false,
) {
  fun state(): FeatureState = runner.state(this)

  /** [state] cada cop que canvia (el flag o el Pro), per als ViewModels que han de seguir-lo. */
  fun observeState(): Flow<FeatureState> = runner.changes.map { state() }.distinctUntilChanged()

  /** Vol dir [FeatureState.Unlocked]: la funció funciona. */
  open fun isEnabled(): Boolean = runner.isEnabled(this)
}
