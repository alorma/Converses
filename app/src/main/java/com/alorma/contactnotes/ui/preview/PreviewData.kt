package com.alorma.contactnotes.ui.preview

import com.alorma.contactnotes.data.db.ChannelEntity
import com.alorma.contactnotes.data.db.ChannelType
import com.alorma.contactnotes.data.db.EntryEntity
import com.alorma.contactnotes.data.db.EntryKind
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.db.PersonListItem
import com.alorma.contactnotes.data.db.PinnedFactEntity
import com.alorma.contactnotes.designsystem.components.AvatarColor
import com.alorma.contactnotes.designsystem.components.AvatarShape
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Dades fixes per a previews i tests de captures. Les dates són d'un any passat perquè els textos relatius
 * ("Avui", "Ahir", dia de la setmana) no canviïn amb el dia en què es renderitzen.
 */
internal object PreviewData {

  private fun at(month: Int, day: Int, hour: Int, minute: Int = 0): Long =
    LocalDateTime.of(2025, month, day, hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

  /** Instant fix per a les previews del formulari d'entrada, perquè «Ara» no depengui del rellotge. */
  val formTime: Long = at(3, 14, 21, 46)

  val marta = PersonEntity(
    id = 1,
    name = "Marta Soler",
    alias = "Marta",
    avatarShape = AvatarShape.Cookie9Sided,
    avatarColor = AvatarColor.Lavender,
    createdAt = at(1, 10, 9),
    updatedAt = at(3, 14, 21),
  )

  private val luna = PersonEntity(
    id = 2,
    name = "Luna Roja",
    alias = "Luna",
    avatarShape = AvatarShape.Cookie9Sided,
    avatarColor = AvatarColor.Rose,
    isProtected = true,
    createdAt = at(1, 12, 9),
    updatedAt = at(3, 12, 20),
  )

  private val jordi = PersonEntity(
    id = 3,
    name = "Jordi Puig",
    alias = "Jordi P.",
    avatarShape = AvatarShape.Clover4Leaf,
    avatarColor = AvatarColor.Mint,
    createdAt = at(1, 20, 9),
    updatedAt = at(3, 2, 18),
  )

  private val laia = PersonEntity(
    id = 4,
    name = "Laia Ferrer",
    avatarShape = AvatarShape.Flower,
    avatarColor = AvatarColor.Peach,
    createdAt = at(2, 1, 9),
    updatedAt = at(2, 1, 9),
  )

  val martaChannels = listOf(
    ChannelEntity(id = 1, personId = 1, type = ChannelType.CHAT),
    ChannelEntity(id = 2, personId = 1, type = ChannelType.IN_PERSON),
  )

  val martaFacts = listOf(
    PinnedFactEntity(id = 1, personId = 1, label = "Birthday", value = "14 March", position = 0),
    PinnedFactEntity(id = 2, personId = 1, label = "Work", value = "Designer in Barcelona", position = 1),
  )

  val martaEntries = listOf(
    EntryEntity(
      id = 3,
      personId = 1,
      kind = EntryKind.SAID,
      channelType = ChannelType.CHAT,
      text = "Told her I can't make it to dinner on Saturday.",
      occurredAt = at(3, 14, 21, 15),
    ),
    EntryEntity(
      id = 2,
      personId = 1,
      kind = EntryKind.HEARD,
      channelType = ChannelType.CHAT,
      text = "She told me she moved flats.",
      occurredAt = at(3, 14, 19, 40),
    ),
    EntryEntity(
      id = 1,
      personId = 1,
      kind = EntryKind.HAPPENED,
      channelType = ChannelType.IN_PERSON,
      text = "We went to the photography exhibition.",
      occurredAt = at(3, 2, 18),
    ),
  )

  val people = listOf(
    PersonListItem(person = marta, lastEntry = martaEntries.first(), channels = martaChannels),
    PersonListItem(person = luna, lastEntry = null, channels = emptyList()),
    PersonListItem(
      person = jordi,
      lastEntry = EntryEntity(
        id = 4,
        personId = 3,
        kind = EntryKind.HEARD,
        channelType = ChannelType.CHAT,
        text = "He wants to set up a game on Friday.",
        occurredAt = at(3, 2, 18),
      ),
      channels = listOf(
        ChannelEntity(id = 3, personId = 3, type = ChannelType.CHAT),
        ChannelEntity(id = 4, personId = 3, type = ChannelType.SOCIAL),
      ),
    ),
    PersonListItem(
      person = laia,
      lastEntry = null,
      channels = listOf(ChannelEntity(id = 5, personId = 4, type = ChannelType.PHONE)),
    ),
  )
}
