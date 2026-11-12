package com.alorma.contactnotes.ui.tracking

import com.alorma.contactnotes.data.sync.SyncOutcome
import com.alorma.contactnotes.designsystem.theme.ThemeMode
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind
import com.alorma.contactnotes.ui.entry.EntryChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ActionsTest {
  @Test
  fun entrySavedSaysOnlyThatTheChannelIsCustom() {
    val action = Actions.EntrySaved(EntryMode.QUICK, EntryKind.HEARD, EntryChannel(ChannelType.CHAT, 42L))
    assertEquals("entry_saved", action.eventName)
    assertEquals(
      mapOf("mode" to "quick", "kind" to "heard", "channel" to "chat", "custom_channel" to "true"),
      action.eventProperties,
    )
    assertFalse(action.eventProperties.values.any { it.contains("42") })
  }

  @Test
  fun entrySavedWithABuiltInChannel() {
    val action = Actions.EntrySaved(EntryMode.EDIT, EntryKind.SAID, EntryChannel(ChannelType.IN_PERSON))
    assertEquals("in_person", action.eventProperties["channel"])
    assertEquals("false", action.eventProperties["custom_channel"])
  }

  @Test
  fun settings() {
    assertEquals(
      mapOf("setting" to "theme", "value" to "dark"),
      Actions.SettingChanged.theme(ThemeMode.Dark).eventProperties,
    )
    assertEquals(
      mapOf("setting" to "app_lock", "value" to "true"),
      Actions.SettingChanged.toggle(ToggleSetting.APP_LOCK, true).eventProperties,
    )
    assertEquals(
      mapOf("setting" to "relock_after", "value" to "60"),
      Actions.SettingChanged.relockAfter(60).eventProperties,
    )
  }

  @Test
  fun closedValues() {
    assertEquals(
      mapOf("channel" to "social", "selected" to "false"),
      Actions.FilterToggled(ChannelType.SOCIAL, false).eventProperties,
    )
    assertEquals(mapOf("source" to "undo"), Actions.TaskReopened(fromUndo = true).eventProperties)
    assertEquals(
      mapOf("mode" to "create", "has_due" to "true"),
      Actions.TaskSaved(creating = true, hasDue = true).eventProperties,
    )
    assertEquals(mapOf("enabled" to "false"), Actions.PersonProtectionChanged(false).eventProperties)
    assertEquals(mapOf("target" to "privacy"), Actions.LinkOpened(LinkTarget.PRIVACY).eventProperties)
  }

  @Test
  fun syncInitialActionsCarryOnlyTheResult() {
    assertEquals("sync_upload_finished", Actions.SyncUploadFinished(SyncOutcome.Done).eventName)
    assertEquals(mapOf("result" to "done"), Actions.SyncUploadFinished(SyncOutcome.Done).eventProperties)
    assertEquals("sync_download_finished", Actions.SyncDownloadFinished(SyncOutcome.Offline).eventName)
    assertEquals(mapOf("result" to "offline"), Actions.SyncDownloadFinished(SyncOutcome.Offline).eventProperties)
    assertEquals(mapOf("result" to "failed"), Actions.SyncDownloadFinished(SyncOutcome.Failed).eventProperties)
    assertEquals("sync_merge_shown", Actions.SyncMergeShown().eventName)
    assertEquals(emptyMap<String, String>(), Actions.SyncMergeShown().eventProperties)
  }

  @Test
  fun avatarCarriesNoShapeNorColor() {
    assertEquals(emptyMap<String, String>(), Actions.AvatarApplied().eventProperties)
  }
}
