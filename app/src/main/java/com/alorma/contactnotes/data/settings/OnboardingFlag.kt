package com.alorma.contactnotes.data.settings

import com.alorma.fireandforget.CounterFireAndForget
import com.alorma.fireandforget.FireAndForget
import com.alorma.fireandforget.FireAndForgetRunner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Runner sobre el DataStore dels ajustos: la llibreria és síncrona i l'estat s'ha de saber abans del primer frame. */
class DataStoreFireAndForgetRunner(private val settings: SettingsRepository) : FireAndForgetRunner() {
  override fun checkEnabled(fireAndForget: FireAndForget): Boolean = when (fireAndForget.name) {
    OnboardingFlagName -> !runBlocking { settings.settings.first().onboardingDone }
    else -> fireAndForget.defaultValue
  }

  override fun disable(fireAndForget: FireAndForget) = setDone(fireAndForget, true)

  override fun reset(fireAndForget: FireAndForget) = setDone(fireAndForget, false)

  override fun getCounter(counterFireAndForget: CounterFireAndForget): Int? = error("Counters are not used")

  override fun setCounter(counterFireAndForget: CounterFireAndForget, value: Int) = error("Counters are not used")

  // L'escriptura ha de quedar desada abans de passar a la pantalla següent
  private fun setDone(fireAndForget: FireAndForget, done: Boolean) {
    if (fireAndForget.name == OnboardingFlagName) runBlocking { settings.setOnboardingDone(done) }
  }
}

private const val OnboardingFlagName = "onboarding_done"

/** `isEnabled()` vol dir «cal mostrar l'onboarding»; `disable()` el marca com a fet. */
class OnboardingFlag(runner: FireAndForgetRunner) : FireAndForget(
  fireAndForgetRunner = runner,
  name = OnboardingFlagName,
  defaultValue = true,
)
