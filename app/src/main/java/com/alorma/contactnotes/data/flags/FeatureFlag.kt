package com.alorma.contactnotes.data.flags

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Una funció que es pot amagar o mostrar, al estil de FireAndForget: cada flag defineix la seva clau i el valor per
 * defecte, i el [FeatureFlagRunner] diu en quin [FeatureState] és. Una funció apagada simplement no hi és; una de Pro
 * encesa, sense Pro, és [FeatureState.Locked].
 *
 * Una funció acabada té el flag encès per defecte i, si és Pro, queda només darrere del Pro; una de nova o a mitges
 * neix apagada i s'encén des del menú de debug fins que està llesta.
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

class CustomAvatarFeature(
  runner: FeatureFlagRunner,
) : FeatureFlag(runner, key = "custom_avatar_enabled", defaultValue = true, pro = true)

class PersonColorFeature(
  runner: FeatureFlagRunner,
) : FeatureFlag(runner, key = "person_color_enabled", defaultValue = true, pro = true)

/** Canals propis (fase 6): camp «On», full «Nou canal» i Ajustos › Canals propis. */
class CustomChannelsFeature(
  runner: FeatureFlagRunner,
) : FeatureFlag(runner, key = "custom_channels_enabled", defaultValue = true, pro = true)

/** Pendents (fase 7): tasques, secció al detall, marcar com a fet i comptador al llistat. */
class FollowUpsFeature(
  runner: FeatureFlagRunner,
) : FeatureFlag(runner, key = "follow_ups_enabled", defaultValue = true, pro = true)
