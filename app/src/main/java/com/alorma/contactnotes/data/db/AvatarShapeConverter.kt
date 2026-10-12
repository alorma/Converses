package com.alorma.contactnotes.data.db

import androidx.room.TypeConverter
import com.alorma.contactnotes.shared.AvatarShape

/**
 * Desa la forma de l'avatar com a text, i desacobla el valor desat del nom de l'enum: els noms antics
 * es queden aquí (`LegacyNames`) i el domini no els arrossega. Un valor desconegut és l'avatar estàndard (`null`).
 */
class AvatarShapeConverter {
  @TypeConverter
  fun toDb(shape: AvatarShape?): String? = shape?.name

  @TypeConverter
  fun fromDb(value: String?): AvatarShape? =
    value?.let { name ->
      LegacyNames[name] ?: AvatarShape.entries.firstOrNull { it.name == name }
    }

  private companion object {
    /** Noms desats per versions anteriors: `Flower` va passar a `Clover8Leaf` (forma més suau). */
    val LegacyNames = mapOf("Flower" to AvatarShape.Clover8Leaf)
  }
}
