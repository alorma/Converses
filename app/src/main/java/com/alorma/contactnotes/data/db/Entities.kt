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
  /** Marcada per fer-ne seguiment (Pro). Continua sent pendent mentre [doneAt] sigui `null`. */
  @ColumnInfo(defaultValue = "0") val isPending: Boolean = false,
  /** Quan es va marcar com a feta; `null` si no és pendent o encara no s'ha fet. */
  val doneAt: Long? = null,
)

/** Pendent i encara no fet: surt a la secció «Pendents» del detall i porta l'etiqueta PENDENT. */
val EntryEntity.isOpenFollowUp: Boolean get() = isPending && doneAt == null

/**
 * L'entrada amb el pendent obert ([open]) o no. Si ja ho era, no canvia (un pendent fet continua fet si no
 * es torna a obrir); obrir-lo esborra [EntryEntity.doneAt] i treure'l deixa l'entrada sense pendent.
 */
fun EntryEntity.withOpenFollowUp(open: Boolean): EntryEntity =
  when {
    open == isOpenFollowUp -> this
    open -> copy(isPending = true, doneAt = null)
    else -> copy(isPending = false, doneAt = null)
  }

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
  /** Pendents sense fer ([isOpenFollowUp]); 0 per a les persones protegides (ho posa el repositori). */
  val openFollowUps: Int = 0,
)

/** Canal propi amb el nombre d'entrades que el fan servir (Ajustos › Canals propis). */
data class CustomChannelWithCount(
  @Embedded val channel: CustomChannelEntity,
  val entryCount: Int,
)
