package com.alorma.contactnotes.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: els 11 canals passen als 5 grups fixos (backlog 13). El tipus es desa com a text, així que
 * l'esquema no canvia; només es reescriuen els valors i es deduplica `channel` per persona.
 * [onMigrated] s'executa en acabar i serveix per marcar l'avís als usuaris que venen de la v1.
 */
class Migration1To2(private val onMigrated: () -> Unit = {}) : Migration(1, 2) {
  override fun migrate(db: SupportSQLiteDatabase) {
    val toGroup = """
      CASE %1${'$'}s
        WHEN 'WHATSAPP' THEN 'CHAT' WHEN 'TELEGRAM' THEN 'CHAT' WHEN 'SIGNAL' THEN 'CHAT'
        WHEN 'DISCORD' THEN 'CHAT' WHEN 'EMAIL' THEN 'CHAT'
        WHEN 'X' THEN 'SOCIAL' WHEN 'INSTAGRAM' THEN 'SOCIAL' WHEN 'LINKEDIN' THEN 'SOCIAL' WHEN 'REDDIT' THEN 'SOCIAL'
        ELSE %1${'$'}s
      END
    """.trimIndent()
    db.execSQL("UPDATE channel SET type = ${toGroup.format("type")}")
    db.execSQL("UPDATE entry SET channelType = ${toGroup.format("channelType")}")
    db.execSQL("DELETE FROM channel WHERE id NOT IN (SELECT MIN(id) FROM channel GROUP BY personId, type)")
    onMigrated()
  }
}
