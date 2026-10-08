package com.alorma.contactnotes.data

import com.alorma.contactnotes.data.db.ChannelEntity
import com.alorma.contactnotes.data.db.ChannelType
import com.alorma.contactnotes.data.db.ConversesDatabase
import com.alorma.contactnotes.data.db.EntryEntity
import com.alorma.contactnotes.data.db.EntryKind
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.db.PinnedFactEntity
import com.alorma.contactnotes.designsystem.components.AvatarColor
import com.alorma.contactnotes.designsystem.components.AvatarShape
import java.util.concurrent.TimeUnit

internal suspend fun ConversesDatabase.insertSampleData(now: Long = System.currentTimeMillis()) {
  fun ago(days: Long, hours: Long = 0) = now - TimeUnit.DAYS.toMillis(days) - TimeUnit.HOURS.toMillis(hours)

  val people = listOf(
    SamplePerson(
      name = "Luna Roja", alias = "Luna", shape = AvatarShape.Cookie9Sided, color = AvatarColor.Lavender,
      channels = listOf(ChannelType.CHAT, ChannelType.SOCIAL),
      facts = listOf("Secret" to "This is private"),
      entries = listOf(
        Triple(EntryKind.SAID, ChannelType.CHAT, "I told her Kael comes from the north.") to ago(0, 1),
      ),
      isProtected = true,
    ),
    SamplePerson(
      name = "Marta Soler", alias = "Marta", shape = AvatarShape.Cookie9Sided, color = AvatarColor.Lavender,
      channels = listOf(ChannelType.CHAT, ChannelType.IN_PERSON),
      facts = listOf("Birthday" to "March 14", "Job" to "Designer in Barcelona"),
      entries = listOf(
        Triple(EntryKind.SAID, ChannelType.CHAT, "I told her I can't make it to dinner on Saturday.") to ago(0, 2),
        Triple(EntryKind.HEARD, ChannelType.CHAT, "She told me she moved to a new flat.") to ago(1),
        Triple(EntryKind.HAPPENED, ChannelType.IN_PERSON, "We went to see the photography exhibition.") to ago(9),
      ),
    ),
    SamplePerson(
      name = "Jordi Puig", alias = "Jordi P.", shape = AvatarShape.Clover4Leaf, color = AvatarColor.Mint,
      channels = listOf(ChannelType.CHAT, ChannelType.OTHER),
      facts = listOf("Games" to "Elden Ring, Hades"),
      entries = listOf(
        Triple(EntryKind.HEARD, ChannelType.CHAT, "He told me he wants to set up a game on Friday.") to ago(3),
        Triple(EntryKind.SAID, ChannelType.CHAT, "I sent him the server link.") to ago(3, 4),
      ),
    ),
    SamplePerson(
      name = "Laia Ferrer", alias = null, shape = AvatarShape.Flower, color = AvatarColor.Peach,
      channels = listOf(ChannelType.PHONE, ChannelType.SOCIAL),
      facts = emptyList(),
      entries = listOf(
        Triple(EntryKind.HAPPENED, ChannelType.PHONE, "Long call about the trip to Lisbon.") to ago(20),
      ),
    ),
    SamplePerson(
      name = "Pau Vidal", alias = "Pau", shape = AvatarShape.Sunny, color = AvatarColor.Sky,
      channels = listOf(ChannelType.CHAT),
      facts = listOf("Allergy" to "Nuts"),
      entries = emptyList(),
    ),
  )

  people.forEachIndexed { index, sample ->
    val created = ago(60L - index)
    val personId = personDao().save(
      person = PersonEntity(
        name = sample.name,
        alias = sample.alias,
        isProtected = sample.isProtected,
        avatarShape = sample.shape,
        avatarColor = sample.color,
        createdAt = created,
        updatedAt = created,
      ),
      channels = sample.channels.map { ChannelEntity(personId = 0, type = it) },
      facts = sample.facts.map { (label, value) ->
        PinnedFactEntity(personId = 0, label = label, value = value, position = 0)
      },
    )
    sample.entries.forEach { (entry, occurredAt) ->
      val (kind, channel, text) = entry
      entryDao().insert(
        EntryEntity(personId = personId, kind = kind, channelType = channel, text = text, occurredAt = occurredAt),
      )
    }
  }
}

private class SamplePerson(
  val name: String,
  val alias: String?,
  val shape: AvatarShape,
  val color: AvatarColor,
  val channels: List<ChannelType>,
  val facts: List<Pair<String, String>>,
  val entries: List<Pair<Triple<EntryKind, ChannelType, String>, Long>>,
  val isProtected: Boolean = false,
)
