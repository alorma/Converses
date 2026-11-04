package com.alorma.contactnotes.ui.settings

import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.testing.FakeDb
import com.alorma.contactnotes.testing.FakeSettingsRepository
import com.alorma.contactnotes.testing.MainDispatcherRule
import com.alorma.contactnotes.testing.RecordingTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CustomChannelsViewModelTest {
  @get:Rule val main = MainDispatcherRule()
  private val db = FakeDb()

  private fun kotlinx.coroutines.test.TestScope.viewModel(): CustomChannelsViewModel {
    val vm = CustomChannelsViewModel(db.repository, RecordingTracker().eventTracker)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.groups.collect {} }
    return vm
  }

  @Test
  fun channelsAreGroupedInGroupOrderWithoutEmptyGroups() =
    runTest {
      db.addCustomChannel("Instagram", ChannelType.SOCIAL)
      db.addCustomChannel("Telegram", ChannelType.CHAT)
      db.addCustomChannel("WhatsApp", ChannelType.CHAT)
      val person = db.addPerson("Marta")
      val whatsapp = db.channels.value.first { it.name == "WhatsApp" }
      db.addEntry(person.id, channel = ChannelType.CHAT, customChannelId = whatsapp.id)

      val groups = viewModel().groups.value!!

      assertEquals(listOf(ChannelType.CHAT, ChannelType.SOCIAL), groups.map { it.group })
      assertEquals(listOf("Telegram", "WhatsApp"), groups[0].channels.map { it.channel.name })
      assertEquals(listOf(0, 1), groups[0].channels.map { it.entryCount })
    }

  @Test
  fun deletingAChannelKeepsItsEntriesInTheGroup() =
    runTest {
      val whatsapp = db.addCustomChannel("WhatsApp", ChannelType.CHAT)
      val person = db.addPerson("Marta")
      db.addEntry(person.id, channel = ChannelType.CHAT, customChannelId = whatsapp.id)
      val vm = viewModel()

      vm.delete(whatsapp)

      assertEquals(emptyList<CustomChannelGroup>(), vm.groups.value)
      val entry = db.entries.value.single()
      assertEquals(ChannelType.CHAT, entry.channelType)
      assertNull(entry.customChannelId)
    }

  @Test
  fun settingsCountsTheCustomChannels() =
    runTest {
      db.addCustomChannel("WhatsApp", ChannelType.CHAT)
      db.addCustomChannel("Instagram", ChannelType.SOCIAL)
      val vm = SettingsViewModel(FakeSettingsRepository(), db.repository, RecordingTracker().eventTracker)
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.customChannelCount.collect {} }

      assertEquals(2, vm.customChannelCount.value)
    }
}
