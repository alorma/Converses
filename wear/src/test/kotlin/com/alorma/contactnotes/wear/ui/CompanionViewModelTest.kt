package com.alorma.contactnotes.wear.ui

import com.alorma.contactnotes.shared.CompanionNode
import com.alorma.contactnotes.shared.CompanionState
import com.alorma.contactnotes.wear.data.PhoneInstall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CompanionViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private val phone = CompanionNode(id = "phone", displayName = "Pixel 8a", isNearby = true)

  private class FakeInstall(
    initial: CompanionState,
    var opens: Boolean = true,
  ) : PhoneInstall {
    private val current = MutableStateFlow(initial)
    val opened = mutableListOf<CompanionNode>()

    override val state: Flow<CompanionState> = current

    override suspend fun openPlayOnPhone(node: CompanionNode): Boolean {
      opened += node
      return opens
    }

    fun set(state: CompanionState) {
      current.value = state
    }
  }

  @Before
  fun setUp() = Dispatchers.setMain(dispatcher)

  @After
  fun tearDown() = Dispatchers.resetMain()

  @Test
  fun appOnPhoneOrStoredDataLetsTheAppRun() =
    runTest(dispatcher) {
      for (state in listOf(CompanionState.Present, CompanionState.NoDevice)) {
        val vm = CompanionViewModel(FakeInstall(state), flowOf(true))
        backgroundScope.launch { vm.state.collect {} }
        runCurrent()
        assertEquals(CompanionUiState.Ready, vm.state.value)
      }
    }

  @Test
  fun noPhoneAndNoDataShowsNoPhone() =
    runTest(dispatcher) {
      val vm = CompanionViewModel(FakeInstall(CompanionState.NoDevice), flowOf(false))
      backgroundScope.launch { vm.state.collect {} }
      runCurrent()
      assertEquals(CompanionUiState.NoPhone, vm.state.value)
    }

  @Test
  fun pairedPhoneWithoutTheAppShowsMissing() =
    runTest(dispatcher) {
      val vm = CompanionViewModel(FakeInstall(CompanionState.Missing(phone)), flowOf(false))
      backgroundScope.launch { vm.state.collect {} }
      runCurrent()
      assertEquals(CompanionUiState.Missing(phone), vm.state.value)
    }

  @Test
  fun openingPlayMovesToOpenedAndTargetsThePhone() =
    runTest(dispatcher) {
      val install = FakeInstall(CompanionState.Missing(phone))
      val vm = CompanionViewModel(install, flowOf(false))
      backgroundScope.launch { vm.state.collect {} }
      runCurrent()
      vm.openOnPhone(phone)
      runCurrent()
      assertEquals(CompanionUiState.Opened, vm.state.value)
      assertEquals(listOf(phone), install.opened)
    }

  @Test
  fun failedOpenStaysOnMissing() =
    runTest(dispatcher) {
      val vm = CompanionViewModel(FakeInstall(CompanionState.Missing(phone), opens = false), flowOf(false))
      backgroundScope.launch { vm.state.collect {} }
      runCurrent()
      vm.openOnPhone(phone)
      runCurrent()
      assertEquals(CompanionUiState.Missing(phone), vm.state.value)
    }

  @Test
  fun installingTheAppOnThePhoneLetsTheAppRun() =
    runTest(dispatcher) {
      val install = FakeInstall(CompanionState.Missing(phone))
      val vm = CompanionViewModel(install, flowOf(false))
      backgroundScope.launch { vm.state.collect {} }
      runCurrent()
      vm.openOnPhone(phone)
      runCurrent()
      install.set(CompanionState.Present)
      runCurrent()
      assertEquals(CompanionUiState.Ready, vm.state.value)
    }
}
