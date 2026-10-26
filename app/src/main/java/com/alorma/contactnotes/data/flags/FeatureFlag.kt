package com.alorma.contactnotes.data.flags

/**
 * Una funció que es pot amagar o mostrar, al estil de FireAndForget: cada flag defineix la seva clau i el valor per
 * defecte, i el [FeatureFlagRunner] diu en quin [FeatureState] és. Una funció apagada simplement no hi és; una de Pro
 * encesa, sense Pro, és [FeatureState.Locked].
 */
abstract class FeatureFlag(
  val runner: FeatureFlagRunner,
  val key: String,
  val defaultValue: Boolean = false,
  /** Si és de Pro: només funciona amb l'entitlement `pro` ([ProFeatureFlagRunner]). */
  val pro: Boolean = false,
) {
  fun state(): FeatureState = runner.state(this)

  /** Vol dir [FeatureState.Unlocked]: la funció funciona. */
  open fun isEnabled(): Boolean = runner.isEnabled(this)
}

class CustomAvatarFeature(
  runner: FeatureFlagRunner,
) : FeatureFlag(runner, key = "custom_avatar_enabled", pro = true)

class PersonColorFeature(
  runner: FeatureFlagRunner,
) : FeatureFlag(runner, key = "person_color_enabled", pro = true)

/** Vincular una persona a un contacte del dispositiu (`docs/contacts`). */
class ContactLinkFeature(
  runner: FeatureFlagRunner,
) : FeatureFlag(runner, key = "contact_link_enabled", pro = true)
