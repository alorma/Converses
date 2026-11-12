package com.alorma.contactnotes.data.sync.record

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * El contingut d'un registre, abans de xifrar-lo. El tipus (`type`) el posa el codec com a discriminador; cada classe
 * porta la versió del seu format (`v`) i la data de modificació del client, que decideix els conflictes.
 *
 * Les referències a altres files van per `syncId`, mai per l'id local. Els enums viatgen pel nom, com a text: un valor
 * que una versió més nova de l'app afegeixi no trenca la lectura, simplement no s'aplica.
 *
 * Cap `toString()` mostra el contingut: un registre no ha de sortir mai en un log.
 */
@Serializable
sealed interface SyncRecord {
  val v: Int
  val modifiedAt: Long
}

/** Un fet fixat, a l'ordre en què es mostra. Viatja dins de la persona. */
@Serializable
data class FactRecord(
  val label: String,
  val value: String,
) {
  override fun toString(): String = "FactRecord"
}

/** [modifiedAt] és el `updatedAt` de la persona. */
@Serializable
@SerialName("person")
data class PersonRecord(
  val name: String,
  val alias: String?,
  val avatarShape: String?,
  val avatarColor: String?,
  val isProtected: Boolean,
  val archived: Boolean,
  val notes: String?,
  val createdAt: Long,
  val facts: List<FactRecord>,
  override val modifiedAt: Long,
  override val v: Int = 1,
) : SyncRecord {
  override fun toString(): String = "PersonRecord"
}

/** [personId] i [customChannelId] són `syncId`. */
@Serializable
@SerialName("entry")
data class EntryRecord(
  val personId: String,
  val kind: String,
  val channelType: String,
  val text: String,
  val occurredAt: Long,
  val customChannelId: String?,
  override val modifiedAt: Long,
  override val v: Int = 1,
) : SyncRecord {
  override fun toString(): String = "EntryRecord"
}

/** [personId] és un `syncId`. */
@Serializable
@SerialName("task")
data class TaskRecord(
  val personId: String,
  val text: String,
  val dueDate: Long?,
  val remind: Boolean,
  val createdAt: Long,
  val doneAt: Long?,
  override val modifiedAt: Long,
  override val v: Int = 1,
) : SyncRecord {
  override fun toString(): String = "TaskRecord"
}

@Serializable
@SerialName("channel")
data class ChannelRecord(
  val channelType: String,
  val name: String,
  val createdAt: Long,
  override val modifiedAt: Long,
  override val v: Int = 1,
) : SyncRecord {
  override fun toString(): String = "ChannelRecord"
}

/**
 * El contingut d'un registre esborrat. Sense la data del client no es pot decidir si un esborrat guanya a una edició;
 * no revela res que el camp `deleted` no digui ja.
 */
@Serializable
data class DeletedRecord(
  val deletedAt: Long,
)
