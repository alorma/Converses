package com.alorma.contactnotes.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: els 11 canals passen als 5 grups fixos i la persona deixa de tenir canals propis (backlog 13).
 * Les entrades guarden el canal pel nom de l'enum, així que es reescriuen al grup que toca (si hi quedés
 * `WHATSAPP`, Room no el sabria llegir); la taula `channel` ja no s'usa i s'esborra.
 * No es treu mai: qualsevol BBDD a la v1 ha de passar per aquí.
 */
class Migration1To2 : Migration(1, 2) {
  override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL(
      """
      UPDATE entry SET channelType = CASE channelType
        WHEN 'WHATSAPP' THEN 'CHAT' WHEN 'TELEGRAM' THEN 'CHAT' WHEN 'SIGNAL' THEN 'CHAT'
        WHEN 'DISCORD' THEN 'CHAT' WHEN 'EMAIL' THEN 'CHAT'
        WHEN 'X' THEN 'SOCIAL' WHEN 'INSTAGRAM' THEN 'SOCIAL' WHEN 'LINKEDIN' THEN 'SOCIAL' WHEN 'REDDIT' THEN 'SOCIAL'
        ELSE channelType
      END
      """.trimIndent(),
    )
    db.execSQL("DROP TABLE channel")
  }
}

/**
 * v2 → v3: la persona es pot vincular a un contacte del dispositiu (`docs/contacts`). Tres columnes nul·lables;
 * les persones existents queden sense vincle.
 */
class Migration2To3 : Migration(2, 3) {
  override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE person ADD COLUMN contactId INTEGER")
    db.execSQL("ALTER TABLE person ADD COLUMN contactLookupKey TEXT")
    db.execSQL("ALTER TABLE person ADD COLUMN contactName TEXT")
  }
}
