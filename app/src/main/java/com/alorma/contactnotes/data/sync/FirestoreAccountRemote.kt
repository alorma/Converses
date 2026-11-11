package com.alorma.contactnotes.data.sync

import android.content.Context

/** Pendent (fase 3, tasca A): el document `users/{uid}` de Firestore sobre la instància `sync`. Ara no fa res. */
class FirestoreAccountRemote(
  @Suppress("unused") private val context: Context,
) : AccountRemote {
  override suspend fun load(uid: String): RemoteResult<RemoteAccount?> = RemoteResult.Failed

  override suspend fun create(
    uid: String,
    account: RemoteAccount,
  ): CreateResult = CreateResult.Failed
}
