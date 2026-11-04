package com.alorma.contactnotes.data.tracking

import com.alorma.contactnotes.testing.FakeFirebaseGateway
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseControllerTest {
  private val gateway = FakeFirebaseGateway()
  private val scope = TestScope(UnconfinedTestDispatcher())

  // No executa res fins que algú l'avança: comprova que l'arrencada no depèn del collect
  private val idleScope = TestScope()

  private fun start(
    consent: MutableStateFlow<Boolean?>,
    scope: TestScope = this.scope,
  ) = FirebaseController(gateway, consent, scope).start()

  @Test
  fun withoutChoiceFirebaseIsNeverInitialized() {
    start(MutableStateFlow(null))

    assertFalse(gateway.initialized)
  }

  @Test
  fun withNoFirebaseIsNeverInitialized() {
    start(MutableStateFlow(false))

    assertFalse(gateway.initialized)
  }

  @Test
  fun withYesFirebaseIsEnabledBeforeStartReturns() {
    start(MutableStateFlow(true), idleScope)

    assertTrue(gateway.initialized)
    assertTrue(gateway.enabled)
  }

  @Test
  fun sayingYesLiveEnablesFirebase() {
    val consent = MutableStateFlow<Boolean?>(null)
    start(consent)

    consent.value = true

    assertTrue(gateway.enabled)
  }

  @Test
  fun sayingNoAfterYesDisablesAndDiscardsPending() {
    val consent = MutableStateFlow<Boolean?>(true)
    start(consent)

    consent.value = false

    assertFalse(gateway.enabled)
    assertTrue(gateway.discarded)
  }

  @Test
  fun clearingTheChoiceAfterYesDisablesFirebase() {
    val consent = MutableStateFlow<Boolean?>(true)
    start(consent)

    consent.value = null

    assertFalse(gateway.enabled)
  }
}
