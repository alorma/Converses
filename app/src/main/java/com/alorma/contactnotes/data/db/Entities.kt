package com.alorma.contactnotes.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.shared.AvatarShape
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind

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
  /**
   * Contacte del dispositiu vinculat (`docs/contacts`); tots tres `null` vol dir sense vincle. Es resol amb
   * `Contacts.getLookupUri(contactId, contactLookupKey)`; [contactName] és la còpia del nom triat, per mostrar el
   * vincle sense `READ_CONTACTS`. No surt mai de Room (ni Wear, ni Auto, ni exportació).
   */
  val contactId: Long? = null,
  val contactLookupKey: String? = null,
  val contactName: String? = null,
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

data class KindCount(
  val kind: EntryKind,
  val count: Int,
)

data class PersonListItem(
  @Embedded val person: PersonEntity,
  @Embedded(prefix = "last_") val lastEntry: EntryEntity?,
  /** Grups de canal de les seves entrades (amb repeticions); la taula `channel` ja no s'usa. */
  @Relation(parentColumn = "id", entityColumn = "personId", entity = EntryEntity::class, projection = ["channelType"])
  val channels: List<ChannelType>,
)
