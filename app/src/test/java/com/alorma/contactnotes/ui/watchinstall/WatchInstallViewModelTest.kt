package com.alorma.contactnotes.ui.watchinstall

import com.alorma.contactnotes.data.wear.WatchInstall
import com.alorma.contactnotes.shared.CompanionNode
import com.alorma.contactnotes.shared.CompanionState
import com.alorma.contactnotes.testing.FakeSettingsRepository
import com.alorma.contactnotes.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WatchInstallViewModelTest {
  @get:Rule val main = MainDispatcherRule()
  private val settings = FakeSettingsRepository()
  private val watch = CompanionNode(id = "w", displayName = "Pixel Watch 3", isNearby = true)

  private class FakeInstall(
    private val state: CompanionState,
    private val opens: Boolean = true,
  ) : WatchInstall {
    val opened = mutableListOf<CompanionNode>()

    override suspend fun state() = state

    override suspend fun openPlayOnWatch(node: CompanionNode): Boolean {
      opened += node
      return opens
    }
  }

  private fun viewModel(install: FakeInstall) = WatchInstallViewModel(install, settings)

  @Test
  fun proposesWhenAWatchWithoutTheAppIsConnected() =
    runTest {
      val vm = viewModel(FakeInstall(CompanionState.Missing(watch)))
      assertEquals(WatchInstallUiState.Propose("Pixel Watch 3"), vm.state.value)
    }

  @Test
  fun staysHiddenWithTheAppOnTheWatchOrWithoutWatch() =
    runTest {
      for (state in listOf(CompanionState.Present, CompanionState.NoDevice)) {
        assertEquals(WatchInstallUiState.Hidden, viewModel(FakeInstall(state)).state.value)
      }
    }

  @Test
  fun blankNameFallsBackToNull() =
    runTest {
      val vm = viewModel(FakeInstall(CompanionState.Missing(watch.copy(displayName = " "))))
      assertEquals(WatchInstallUiState.Propose(null), vm.state.value)
    }

  @Test
  fun installOpensPlayOnTheWatchAndMovesToFinish() =
    runTest {
      val install = FakeInstall(CompanionState.Missing(watch))
      val vm = viewModel(install)
      vm.onInstall()
      assertEquals(WatchInstallUiState.Finish("Pixel Watch 3"), vm.state.value)
      assertEquals(listOf(watch), install.opened)
    }

  @Test
  fun failedOpenStaysOnThePropose() =
    runTest {
      val vm = viewModel(FakeInstall(CompanionState.Missing(watch), opens = false))
      vm.onInstall()
      assertEquals(WatchInstallUiState.Propose("Pixel Watch 3"), vm.state.value)
    }

  @Test
  fun notNowHidesButDoesNotRememberIt() =
    runTest {
      val vm = viewModel(FakeInstall(CompanionState.Missing(watch)))
      vm.onNotNow()
      assertEquals(WatchInstallUiState.Hidden, vm.state.value)
      assertEquals(false, settings.state.value.watchInstallDismissed)
      // Una arrencada nova torna a proposar-ho
      assertEquals(
        WatchInstallUiState.Propose("Pixel Watch 3"),
        viewModel(FakeInstall(CompanionState.Missing(watch))).state.value,
      )
    }

  @Test
  fun neverAgainHidesAndIsRememberedForNextLaunches() =
    runTest {
      val vm = viewModel(FakeInstall(CompanionState.Missing(watch)))
      vm.onNeverAgain()
      assertEquals(WatchInstallUiState.Hidden, vm.state.value)
      assertTrue(settings.state.value.watchInstallDismissed)
      assertEquals(WatchInstallUiState.Hidden, viewModel(FakeInstall(CompanionState.Missing(watch))).state.value)
    }

  @Test
  fun doneClosesTheFinishSheet() =
    runTest {
      val vm = viewModel(FakeInstall(CompanionState.Missing(watch)))
      vm.onInstall()
      vm.onDone()
      assertEquals(WatchInstallUiState.Hidden, vm.state.value)
    }
}
