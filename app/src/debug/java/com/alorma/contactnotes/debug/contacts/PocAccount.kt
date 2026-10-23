package com.alorma.contactnotes.debug.contacts

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

// PROVA DE CONCEPTE (fase 0 de docs/contacts): no es fusiona mai a main.
// Via antiga (fins a l'API 37.1): compte pròpia i sync adapter perquè Contactes llegeixi el contacts.xml.

const val POC_ACCOUNT_TYPE = "com.alorma.contactnotes.dev.poc"
const val POC_ACCOUNT_NAME = "Converses"
const val POC_MIMETYPE = "vnd.android.cursor.item/vnd.com.alorma.converses.person"

val pocAccount = Account(POC_ACCOUNT_NAME, POC_ACCOUNT_TYPE)

class PocAuthenticator(
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

class PocAuthenticatorService : Service() {
  private val authenticator by lazy { PocAuthenticator(this) }

  override fun onBind(intent: Intent?): IBinder? = authenticator.iBinder
}

/** No sincronitza res: només existeix perquè Contactes llegeixi el `contacts.xml`. */
class PocSyncAdapter(
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

class PocSyncAdapterService : Service() {
  private val adapter by lazy { PocSyncAdapter(this) }

  override fun onBind(intent: Intent?): IBinder? = adapter.syncAdapterBinder
}
