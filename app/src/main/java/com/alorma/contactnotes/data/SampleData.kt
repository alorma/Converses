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
      name = "Marta Soler", alias = "Marta", shape = AvatarShape.Cookie9Sided, color = AvatarColor.Lavender,
      channels = listOf(ChannelType.WHATSAPP to "+34 600 111 222", ChannelType.IN_PERSON to null),
      facts = listOf("Aniversari" to "14 de març", "Feina" to "Dissenyadora a Bcn"),
      entries = listOf(
        Triple(EntryKind.SAID, ChannelType.WHATSAPP, "Li he dit que dissabte no puc venir al sopar.") to ago(0, 2),
        Triple(EntryKind.HEARD, ChannelType.WHATSAPP, "M'ha dit que ha canviat de pis.") to ago(1),
        Triple(EntryKind.HAPPENED, ChannelType.IN_PERSON, "Hem anat a veure l'exposició de fotografia.") to ago(9),
      ),
    ),
    SamplePerson(
      name = "Jordi Puig", alias = "Jordi P.", shape = AvatarShape.Clover4Leaf, color = AvatarColor.Mint,
      channels = listOf(ChannelType.DISCORD to "jordip#2201", ChannelType.TELEGRAM to "@jordip"),
      facts = listOf("Jocs" to "Elden Ring, Hades"),
      entries = listOf(
        Triple(EntryKind.HEARD, ChannelType.DISCORD, "M'ha dit que vol muntar una partida el divendres.") to ago(3),
        Triple(EntryKind.SAID, ChannelType.TELEGRAM, "Li he passat el link del servidor.") to ago(3, 4),
      ),
    ),
    SamplePerson(
      name = "Laia Ferrer", alias = null, shape = AvatarShape.Flower, color = AvatarColor.Peach,
      channels = listOf(ChannelType.REDDIT to "u/laiaf", ChannelType.PHONE to "+34 611 333 444"),
      facts = emptyList(),
      entries = listOf(
        Triple(EntryKind.HAPPENED, ChannelType.PHONE, "Trucada llarga sobre el viatge a Lisboa.") to ago(20),
      ),
    ),
    SamplePerson(
      name = "Pau Vidal", alias = "Pau", shape = AvatarShape.Sunny, color = AvatarColor.Sky,
      channels = listOf(ChannelType.OTHER to "Signal"),
      facts = listOf("Alergia" to "Fruits secs"),
      entries = emptyList(),
    ),
  )

  people.forEachIndexed { index, sample ->
    val created = ago(60L - index)
    val personId = personDao().save(
      person = PersonEntity(
        name = sample.name,
        alias = sample.alias,
        avatarShape = sample.shape,
        avatarColor = sample.color,
        createdAt = created,
        updatedAt = created,
      ),
      channels = sample.channels.map { (type, handle) -> ChannelEntity(personId = 0, type = type, handle = handle) },
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
  val channels: List<Pair<ChannelType, String?>>,
  val facts: List<Pair<String, String>>,
  val entries: List<Pair<Triple<EntryKind, ChannelType, String>, Long>>,
)
