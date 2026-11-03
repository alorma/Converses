package com.alorma.contactnotes.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.data.db.CustomChannelEntity
import com.alorma.contactnotes.data.db.CustomChannelWithCount
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewTheme
import com.alorma.contactnotes.shared.ChannelType

private fun channel(
  id: Long,
  group: ChannelType,
  name: String,
  entries: Int,
) = CustomChannelWithCount(CustomChannelEntity(id = id, channelType = group, name = name, createdAt = 0), entries)

/** Ajustos › Canals propis (7d), amb els canals del disseny. */
@Preview
@Composable
internal fun CustomChannelsPreview() {
  ConversesPreviewTheme {
    CustomChannelsContent(
      groups =
        listOf(
          CustomChannelGroup(
            ChannelType.CHAT,
            listOf(
              channel(3, ChannelType.CHAT, "Discord", 21),
              channel(2, ChannelType.CHAT, "Telegram", 8),
              channel(1, ChannelType.CHAT, "WhatsApp", 34),
            ),
          ),
          CustomChannelGroup(ChannelType.SOCIAL, listOf(channel(4, ChannelType.SOCIAL, "Instagram", 5))),
          CustomChannelGroup(ChannelType.OTHER, listOf(channel(5, ChannelType.OTHER, "Correu", 1))),
        ),
      onBack = {},
      onNew = {},
      onEdit = {},
      onDelete = {},
    )
  }
}

/** Sense cap canal: l'explicació, la nota i el botó «Nou canal». */
@Preview
@Composable
internal fun CustomChannelsEmptyPreview() {
  ConversesPreviewTheme {
    CustomChannelsContent(groups = emptyList(), onBack = {}, onNew = {}, onEdit = {}, onDelete = {})
  }
}
