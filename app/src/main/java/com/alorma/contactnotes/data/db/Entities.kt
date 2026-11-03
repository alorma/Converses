package com.alorma.contactnotes.data.db

import androidx.room.ColumnInfo
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
)

val PersonEntity.shape: AvatarShape get() = avatarShape ?: AvatarShape.Standard

val PersonEntity.color: AvatarColor get() = avatarColor ?: AvatarColor.forName(name)

/**
 * Canal propi (Pro): global, dins d'un dels cinc grups, amb nom i sense valor. El nom és únic dins del grup sense
 * distingir majúscules; ho valida el repositori, i l'índex únic només n'és la xarxa de seguretat.
 */
@Entity(
  tableName = "custom_channel",
  indices = [Index("channelType", "name", unique = true)],
)
data class CustomChannelEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val channelType: ChannelType,
  val name: String,
  val createdAt: Long,
)

@Entity(
  tableName = "entry",
  foreignKeys = [
    ForeignKey(
      entity = PersonEntity::class,
      parentColumns = ["id"],
      childColumns = ["personId"],
      onDelete = ForeignKey.CASCADE,
    ),
    // Si s'esborra el canal propi, l'entrada es queda al seu grup (`channelType`).
    ForeignKey(
      entity = CustomChannelEntity::class,
      parentColumns = ["id"],
      childColumns = ["customChannelId"],
      onDelete = ForeignKey.SET_NULL,
    ),
  ],
  indices = [Index("personId", "occurredAt"), Index("customChannelId")],
)
data class EntryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val personId: Long,
  val kind: EntryKind,
  /** Grup del canal; si hi ha [customChannelId], és el grup d'aquell canal. */
  val channelType: ChannelType,
  val text: String,
  val occurredAt: Long,
  /** Canal propi opcional (Pro), dins de [channelType]. */
  val customChannelId: Long? = null,
)

/**
 * Tasca (Pro): una cosa per fer amb una persona. Independent de les entrades: no surt a la cronologia.
 * Es queda a «Pendents» mentre [doneAt] sigui `null`.
 */
@Entity(
  tableName = "task",
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
data class TaskEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val personId: Long,
  val text: String,
  /** Dia previst com a `LocalDate.toEpochDay()` (sense hora ni zona); `null` és «Sense data». */
  val dueDate: Long? = null,
  /** «Avisa'm» (notificació aquell dia a les 9:00). Encara no es fa servir: la fila surt més endavant. */
  @ColumnInfo(defaultValue = "0") val remind: Boolean = false,
  val createdAt: Long,
  /** Quan es va marcar com a feta; `null` mentre és pendent. */
  val doneAt: Long? = null,
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
  /** Tasques pendents (sense fer); 0 per a les persones protegides (ho posa el repositori). */
  val openTasks: Int = 0,
)

/** Canal propi amb el nombre d'entrades que el fan servir (Ajustos › Canals propis). */
data class CustomChannelWithCount(
  @Embedded val channel: CustomChannelEntity,
  val entryCount: Int,
)
