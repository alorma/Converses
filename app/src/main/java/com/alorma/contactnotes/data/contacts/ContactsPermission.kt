package com.alorma.contactnotes.data.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Permisos de Contactes que demana el vincle (C7): `READ_CONTACTS` i `WRITE_CONTACTS`, sempre junts. Sense tots dos no
 * es pot vincular i el detall no mostra el vincle. Fins a la fase 6 només els declara el manifest de debug (C12): a
 * release, [isGranted] sempre és `false`.
 */
interface ContactsPermission {
  fun isGranted(): Boolean

  companion object {
    val permissions = arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
  }
}

class AndroidContactsPermission(
  private val context: Context,
) : ContactsPermission {
  override fun isGranted(): Boolean =
    ContactsPermission.permissions.all {
      ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}
