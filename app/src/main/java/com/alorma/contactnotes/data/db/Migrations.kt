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
 * v2 → v3: tres columnes nul·lables a `person` per al vincle amb Contactes, retirat a la v4
 * ([ConversesDatabase.DropContactLink]); les persones existents queden sense vincle.
 */
class Migration2To3 : Migration(2, 3) {
  override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE person ADD COLUMN contactId INTEGER")
    db.execSQL("ALTER TABLE person ADD COLUMN contactLookupKey TEXT")
    db.execSQL("ALTER TABLE person ADD COLUMN contactName TEXT")
  }
}

/**
 * v5 → v6: identitat de sincronització (`docs/sync`). Persones, entrades, tasques i canals propis reben un `syncId`
 * (un UUID diferent per fila, i per això no pot ser una `AutoMigration`), la marca `dirty` i, tret de la persona, que
 * ja té `updatedAt`, la data `modifiedAt`. Les files que ja hi eren queden pendents de pujar. La taula
 * `sync_tombstone` recorda els esborrats. Sense sessió de sincronització res d'això es veu.
 * No es treu mai: qualsevol BBDD a la v5 ha de passar per aquí.
 */
class Migration5To6 : Migration(5, 6) {
  override fun migrate(db: SupportSQLiteDatabase) {
    addSyncIdentity(db, table = "person", modifiedAt = null)
    addSyncIdentity(db, table = "entry", modifiedAt = "occurredAt")
    addSyncIdentity(db, table = "task", modifiedAt = "COALESCE(doneAt, createdAt)")
    addSyncIdentity(db, table = "custom_channel", modifiedAt = "createdAt")
    db.execSQL(
      "CREATE TABLE IF NOT EXISTS sync_tombstone (syncId TEXT NOT NULL, deletedAt INTEGER NOT NULL, " +
        "dirty INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(syncId))",
    )
  }

  /** [modifiedAt] és l'expressió SQL que omple la columna nova; `null` si la taula no en porta. */
  private fun addSyncIdentity(
    db: SupportSQLiteDatabase,
    table: String,
    modifiedAt: String?,
  ) {
    db.execSQL("ALTER TABLE $table ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
    db.execSQL("ALTER TABLE $table ADD COLUMN dirty INTEGER NOT NULL DEFAULT 1")
    if (modifiedAt == null) {
      db.execSQL("UPDATE $table SET syncId = $RANDOM_UUID")
    } else {
      db.execSQL("ALTER TABLE $table ADD COLUMN modifiedAt INTEGER NOT NULL DEFAULT 0")
      db.execSQL("UPDATE $table SET syncId = $RANDOM_UUID, modifiedAt = $modifiedAt")
    }
    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_${table}_syncId ON $table (syncId)")
  }

  private companion object {
    /** Un UUID v4 en minúscules, amb el mateix format que `UUID.randomUUID()`; SQLite l'avalua per a cada fila. */
    const val RANDOM_UUID =
      "lower(hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-4' || substr(hex(randomblob(2)), 2) || '-' || " +
        "substr('89ab', (random() & 3) + 1, 1) || substr(hex(randomblob(2)), 2) || '-' || hex(randomblob(6)))"
  }
}
