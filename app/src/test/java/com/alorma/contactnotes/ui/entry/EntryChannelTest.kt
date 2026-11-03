package com.alorma.contactnotes.ui.entry

import com.alorma.contactnotes.data.db.CustomChannelEntity
import com.alorma.contactnotes.shared.ChannelType
import org.junit.Assert.assertEquals
import org.junit.Test

class EntryChannelTest {
  private val whatsapp = CustomChannelEntity(id = 7, channelType = ChannelType.CHAT, name = "WhatsApp", createdAt = 0)
  private val telegram = CustomChannelEntity(id = 8, channelType = ChannelType.CHAT, name = "Telegram", createdAt = 0)

  @Test
  fun changingTheGroupClearsTheCustomChannel() {
    val channel = EntryChannel(ChannelType.CHAT, whatsapp.id)

    assertEquals(EntryChannel(ChannelType.PHONE), channel.withGroup(ChannelType.PHONE))
  }

  @Test
  fun tappingTheSameGroupKeepsTheCustomChannel() {
    val channel = EntryChannel(ChannelType.CHAT, whatsapp.id)

    assertEquals(channel, channel.withGroup(ChannelType.CHAT))
  }

  @Test
  fun togglingTheSelectedChannelClearsItAndAnotherOneReplacesIt() {
    val selected = EntryChannel(ChannelType.CHAT).toggle(whatsapp)
    assertEquals(EntryChannel(ChannelType.CHAT, whatsapp.id), selected)

    assertEquals(EntryChannel(ChannelType.CHAT, telegram.id), selected.toggle(telegram))
    assertEquals(EntryChannel(ChannelType.CHAT), selected.toggle(whatsapp))
  }

  @Test
  fun aChannelFromAnotherGroupBringsItsGroup() {
    val social = CustomChannelEntity(id = 9, channelType = ChannelType.SOCIAL, name = "Bluesky", createdAt = 0)

    assertEquals(EntryChannel(ChannelType.SOCIAL, social.id), EntryChannel(ChannelType.CHAT).toggle(social))
  }
}
