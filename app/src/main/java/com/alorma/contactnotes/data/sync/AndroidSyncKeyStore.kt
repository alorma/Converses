package com.alorma.contactnotes.data.sync

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import com.alorma.contactnotes.data.tracking.ErrorSites
import com.alorma.contactnotes.data.tracking.Tracker
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * El fitxer amb la clau a `noBackupFilesDir`, xifrat amb una clau AES-256-GCM de l'Android Keystore que no es pot
 * exportar. La clau del Keystore no demana autenticació de l'usuari: el motor de sincronització l'ha de poder llegir
 * sol. L'empremta per veure les paraules la demana la UI.
 *
 * El fitxer és `nonce (12 bytes) ‖ text xifrat i etiqueta`; el que hi ha a dins ho descriu [StoredKeysCodec].
 * Cap mètode peta: un fitxer que no es pot obrir és com si no hi fos, i llavors calen les paraules.
 */
class AndroidSyncKeyStore(
  private val context: Context,
  private val tracker: Tracker,
) : SyncKeyStore {
  // `noBackupFilesDir` toca el disc: no es resol fins a la primera crida
  private val file by lazy { AtomicFile(File(context.noBackupFilesDir, FILE_NAME)) }

  @Synchronized
  override fun load(): StoredKeys? {
    if (!file.baseFile.exists()) return null
    return try {
      val key = keystore().getKey(KEY_ALIAS, null) as? SecretKey ?: throw UnreadableKeysException()
      val stored = file.readFully()
      if (stored.size <= NONCE_BYTES) throw UnreadableKeysException()
      val cipher = Cipher.getInstance(TRANSFORMATION)
      cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, stored, 0, NONCE_BYTES))
      val plain = cipher.doFinal(stored, NONCE_BYTES, stored.size - NONCE_BYTES)
      try {
        StoredKeysCodec.decode(plain) ?: throw UnreadableKeysException()
      } finally {
        plain.fill(0)
      }
    } catch (e: Exception) {
      // El fitxer no s'esborra: si el Keystore ha fallat només aquesta vegada, la propera es podrà llegir
      tracker.trackError(e, ErrorSites.SYNC_KEY_STORE)
      null
    }
  }

  /**
   * Si no es pot desar, el dispositiu funciona amb la clau en memòria fins que el procés mori; després demanarà les
   * paraules, com en un dispositiu nou.
   */
  @Synchronized
  override fun save(keys: StoredKeys) {
    try {
      val cipher = Cipher.getInstance(TRANSFORMATION)
      // El nonce el tria el Keystore: no accepta que el posi l'app
      cipher.init(Cipher.ENCRYPT_MODE, existingOrNewKey())
      val plain = StoredKeysCodec.encode(keys)
      val encrypted =
        try {
          cipher.doFinal(plain)
        } finally {
          plain.fill(0)
        }
      check(cipher.iv.size == NONCE_BYTES) { "Unexpected nonce size" }
      val stream = file.startWrite()
      try {
        stream.write(cipher.iv)
        stream.write(encrypted)
        file.finishWrite(stream)
      } catch (e: Exception) {
        file.failWrite(stream)
        throw e
      }
    } catch (e: Exception) {
      tracker.trackError(e, ErrorSites.SYNC_KEY_STORE)
    }
  }

  @Synchronized
  override fun clear() {
    try {
      file.delete()
      keystore().deleteEntry(KEY_ALIAS)
    } catch (e: Exception) {
      tracker.trackError(e, ErrorSites.SYNC_KEY_STORE)
    }
  }

  private fun keystore(): KeyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }

  private fun existingOrNewKey(): SecretKey {
    (keystore().getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
    val spec =
      KeyGenParameterSpec
        .Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
        .setKeySize(KEY_BITS)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .build()
    return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
      init(spec)
      generateKey()
    }
  }

  /** Sense cap dada al missatge: el fitxer o la clau del Keystore ja no serveixen. */
  private class UnreadableKeysException : Exception("The stored sync keys cannot be read")

  private companion object {
    const val FILE_NAME = "sync_key"
    const val KEYSTORE = "AndroidKeyStore"
    const val KEY_ALIAS = "converses_sync_key"
    const val TRANSFORMATION = "AES/GCM/NoPadding"
    const val KEY_BITS = 256
    const val NONCE_BYTES = 12
    const val TAG_BITS = 128
  }
}
