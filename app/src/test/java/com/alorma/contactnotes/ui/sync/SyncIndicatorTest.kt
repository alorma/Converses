package com.alorma.contactnotes.ui.sync

import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.sync.SyncAccount
import com.alorma.contactnotes.data.sync.SyncState
import com.alorma.contactnotes.ui.settings.subtitleRes
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncIndicatorTest {
  private val account = SyncAccount(uid = "u", name = "  bernat", email = "b@example.com")

  @Test
  fun `nothing is active until the account is ready, so it looks signed out`() {
    assertEquals(SyncIndicatorState.SignedOut, SyncState.SignedOut.toIndicatorState())
    assertEquals(SyncIndicatorState.SignedOut, SyncState.Checking.toIndicatorState())
    assertEquals(SyncIndicatorState.SignedOut, SyncState.Activating(account).toIndicatorState())
  }

  @Test
  fun `ready and needs key carry the capitalised initial of the name`() {
    assertEquals(SyncIndicatorState.On("B"), SyncState.Ready(account).toIndicatorState())
    assertEquals(SyncIndicatorState.NeedsKey("B"), SyncState.NeedsKey(account).toIndicatorState())
  }

  @Test
  fun `the initial falls back to the email and then to a question mark`() {
    val byEmail = SyncAccount("u", name = null, email = "z@example.com")
    val blank = SyncAccount("u", name = " ", email = null)
    assertEquals(SyncIndicatorState.On("Z"), SyncState.Ready(byEmail).toIndicatorState())
    assertEquals(SyncIndicatorState.On("?"), SyncState.Ready(blank).toIndicatorState())
  }

  @Test
  fun `settings subtitle and accessibility text follow the state`() {
    assertEquals(R.string.sync_state_off, SyncIndicatorState.SignedOut.subtitleRes())
    assertEquals(R.string.sync_state_on, SyncIndicatorState.On("B").subtitleRes())
    assertEquals(R.string.sync_state_key, SyncIndicatorState.NeedsKey("B").subtitleRes())
    assertEquals(R.string.sync_indicator_off, SyncIndicatorState.SignedOut.descriptionRes())
    assertEquals(R.string.sync_indicator_on, SyncIndicatorState.On("B").descriptionRes())
    assertEquals(R.string.sync_indicator_key, SyncIndicatorState.NeedsKey("B").descriptionRes())
  }
}
