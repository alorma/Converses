package com.alorma.contactnotes.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.alorma.contactnotes.designsystem.components.AvatarColor
import com.alorma.contactnotes.designsystem.components.AvatarShape

/** L'ordre de declaració és l'ordre fix amb què es mostren els canals, del més habitual al menys. */
/** Els 5 grups fixos de canals. Els valors es desen pel nom: la migració 1 → 2 va fusionar els antics (WhatsApp, X…) al seu grup. */
enum class ChannelType { IN_PERSON, PHONE, CHAT, SOCIAL, OTHER }

enum class EntryKind { SAID, HEARD, HAPPENED }

@Entity(tableName = "person")
data class PersonEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val alias: String? = null,
  /** `null` vol dir avatar estàndard (Galeta i color automàtic pel nom); usa [shape] i [color] per mostrar-lo. */
  val avatarShape: AvatarShape? = null,
  val avatarColor: AvatarColor? = null,
  val isProtected: Boolean = false,
  val archived: Boolean = false,
  val notes: String? = null,
  val createdAt: Long,
  val updatedAt: Long,
)

val PersonEntity.shape: AvatarShape get() = avatarShape ?: AvatarShape.Standard

val PersonEntity.color: AvatarColor get() = avatarColor ?: AvatarColor.forName(name)

@Entity(
  tableName = "entry",
  foreignKeys = [
    ForeignKey(
      entity = PersonEntity::class,
      parentColumns = ["id"],
      childColumns = ["personId"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [Index("personId", "occurredAt")],
)
data class EntryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val personId: Long,
  val kind: EntryKind,
  val channelType: ChannelType,
  val text: String,
  val occurredAt: Long,
)

@Entity(
  tableName = "pinned_fact",
  foreignKeys = [
    ForeignKey(
      entity = PersonEntity::class,
      parentColumns = ["id"],
      childColumns = ["personId"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [Index("personId")],
)
data class PinnedFactEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val personId: Long,
  val label: String,
  val value: String,
  val position: Int,
)

data class KindCount(val kind: EntryKind, val count: Int)

data class PersonListItem(
  @Embedded val person: PersonEntity,
  @Embedded(prefix = "last_") val lastEntry: EntryEntity?,
  /** Grups de canal de les seves entrades (amb repeticions); la taula `channel` ja no s'usa. */
  @Relation(parentColumn = "id", entityColumn = "personId", entity = EntryEntity::class, projection = ["channelType"])
  val channels: List<ChannelType>,
)
