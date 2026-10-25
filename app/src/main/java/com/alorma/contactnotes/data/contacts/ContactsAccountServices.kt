package com.alorma.contactnotes.data.contacts

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.app.Service
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.Intent
import android.content.SyncResult
import android.os.Bundle
import android.os.IBinder

// Compte local «Converses» (docs/contacts): l'Android només mostra les files d'una app a Contactes si té una compte
// amb sync adapter i contacts.xml. No hi ha credencials ni servidor; tot el que fa la compte ho escriu el gateway.

/** No crea comptes des d'Ajustos ni dona tokens: la compte la crea només [ContactsGateway.ensureAccount]. */
class ContactsAuthenticator(
  context: Context,
) : AbstractAccountAuthenticator(context) {
  override fun editProperties(
    response: AccountAuthenticatorResponse?,
    accountType: String?,
  ): Bundle? = null

  override fun addAccount(
    response: AccountAuthenticatorResponse?,
    accountType: String?,
    authTokenType: String?,
    requiredFeatures: Array<out String>?,
    options: Bundle?,
  ): Bundle? = null

  override fun confirmCredentials(
    response: AccountAuthenticatorResponse?,
    account: Account?,
    options: Bundle?,
  ): Bundle? = null

  override fun getAuthToken(
    response: AccountAuthenticatorResponse?,
    account: Account?,
    authTokenType: String?,
    options: Bundle?,
  ): Bundle? = null

  override fun getAuthTokenLabel(authTokenType: String?): String? = null

  override fun updateCredentials(
    response: AccountAuthenticatorResponse?,
    account: Account?,
    authTokenType: String?,
    options: Bundle?,
  ): Bundle? = null

  override fun hasFeatures(
    response: AccountAuthenticatorResponse?,
    account: Account?,
    features: Array<out String>?,
  ): Bundle? = null
}

class ContactsAuthenticatorService : Service() {
  private val authenticator by lazy { ContactsAuthenticator(this) }

  override fun onBind(intent: Intent?): IBinder? = authenticator.iBinder
}

/** No sincronitza res: només existeix perquè Contactes llegeixi el `contacts.xml`. */
class ContactsSyncAdapter(
  context: Context,
) : AbstractThreadedSyncAdapter(context, true) {
  override fun onPerformSync(
    account: Account?,
    extras: Bundle?,
    authority: String?,
    provider: ContentProviderClient?,
    syncResult: SyncResult?,
  ) = Unit
}

class ContactsSyncAdapterService : Service() {
  private val adapter by lazy { ContactsSyncAdapter(this) }

  override fun onBind(intent: Intent?): IBinder? = adapter.syncAdapterBinder
}
