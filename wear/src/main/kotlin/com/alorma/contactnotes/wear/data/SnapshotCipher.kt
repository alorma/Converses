package com.alorma.contactnotes.wear.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Xifra i desxifra la còpia local. Retorna `null` quan no es pot (la clau no és utilitzable, per exemple perquè el
 * rellotge és bloquejat): en aquest cas la còpia no es desa o no es llegeix, però mai es deixa en clar. És una interfície
 * perquè els tests de JVM no tenen Keystore.
 */
interface SnapshotCipher {
  fun encrypt(plain: String): String?

  fun decrypt(encoded: String): String?
}

/**
 * AES-GCM amb una clau d'Android Keystore que només es pot fer servir amb el rellotge desbloquejat
 * (`setUnlockedDeviceRequired`): amb el rellotge bloquejat, la còpia és il·legible. La clau no surt del Keystore i no
 * viatja a cap còpia de seguretat. Si la clau desapareix (per exemple en treure el bloqueig del rellotge), se'n crea
 * una de nova i la còpia antiga es dona per perduda; es torna a sincronitzar des del mòbil.
 */
class KeystoreSnapshotCipher(
  private val alias: String = KEY_ALIAS,
) : SnapshotCipher {
  override fun encrypt(plain: String): String? =
    try {
      val cipher = Cipher.getInstance(TRANSFORMATION)
      cipher.init(Cipher.ENCRYPT_MODE, key())
      Base64.encodeToString(cipher.iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    } catch (_: GeneralSecurityException) {
      null
    }

  override fun decrypt(encoded: String): String? =
    try {
      val bytes = Base64.decode(encoded, Base64.NO_WRAP)
      val cipher = Cipher.getInstance(TRANSFORMATION)
      cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES))
      String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES), Charsets.UTF_8)
    } catch (_: GeneralSecurityException) {
      null
    } catch (_: IllegalArgumentException) {
      // Base64 malmès o massa curt
      null
    }

  private fun key(): SecretKey {
    val store = KeyStore.getInstance(PROVIDER).apply { load(null) }
    (store.getKey(alias, null) as? SecretKey)?.let { return it }
    val spec =
      KeyGenParameterSpec
        .Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(KEY_BITS)
        .setUnlockedDeviceRequired(true)
        .build()
    return KeyGenerator
      .getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
      .apply { init(spec) }
      .generateKey()
  }

  private companion object {
    const val PROVIDER = "AndroidKeyStore"
    const val KEY_ALIAS = "converses_wear_snapshot"
    const val TRANSFORMATION = "AES/GCM/NoPadding"
    const val KEY_BITS = 256
    const val TAG_BITS = 128
    const val IV_BYTES = 12
  }
}
