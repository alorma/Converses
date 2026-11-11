package com.alorma.contactnotes.data.sync

import android.app.Activity
import android.content.Context

/** Pendent (fase 3, tasca A): Credential Manager i Firebase Auth sobre la instància `sync`. Ara no fa res. */
class FirebaseAuthGateway(
  @Suppress("unused") private val context: Context,
) : AuthGateway {
  override suspend fun currentAccount(): SyncAccount? = null

  override suspend fun signIn(activity: Activity): SignInResult = SignInResult.Failed

  override suspend fun signOut() = Unit
}
