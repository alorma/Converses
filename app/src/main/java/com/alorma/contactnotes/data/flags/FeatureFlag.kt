package com.alorma.contactnotes.data.flags

/**
 * Una funció que es pot amagar o mostrar, al estil de FireAndForget: cada flag defineix la seva clau i el valor per
 * defecte, i el [FeatureFlagRunner] diu d'on surt el valor. No comprova cap pagament (el Pro encara no existeix);
 * una funció apagada simplement no hi és i la UI mai no ensenya res de gratuït/Pro.
 */
abstract class FeatureFlag(
  val runner: FeatureFlagRunner,
  val key: String,
  val defaultValue: Boolean = false,
) {
  open fun isEnabled(): Boolean = runner.isEnabled(this)
}

class CustomAvatarFeature(runner: FeatureFlagRunner) : FeatureFlag(runner, key = "custom_avatar_enabled")

class CustomChannelsFeature(runner: FeatureFlagRunner) : FeatureFlag(runner, key = "custom_channels_enabled")

class FollowUpsFeature(runner: FeatureFlagRunner) : FeatureFlag(runner, key = "follow_ups_enabled")

class ContactIntegrationFeature(runner: FeatureFlagRunner) : FeatureFlag(runner, key = "contact_integration_enabled")
