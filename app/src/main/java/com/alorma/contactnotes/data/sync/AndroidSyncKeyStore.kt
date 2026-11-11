package com.alorma.contactnotes.data.sync

import android.content.Context

/** Pendent (fase 3, tasca A): el fitxer de `noBackupFilesDir` xifrat amb l'Android Keystore. Ara no guarda res. */
class AndroidSyncKeyStore(
  @Suppress("unused") private val context: Context,
) : SyncKeyStore {
  override fun load(): StoredKeys? = null

  override fun save(keys: StoredKeys) = Unit

  override fun clear() = Unit
}
