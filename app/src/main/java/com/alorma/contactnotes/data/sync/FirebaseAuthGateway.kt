package com.alorma.contactnotes.data.sync

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.tracking.ErrorSites
import com.alorma.contactnotes.data.tracking.Tracker
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * La sessió de Firebase Auth sobre la instància `sync`, amb el full «Continua amb Google» de Credential Manager.
 * Qualsevol mètode inicialitza la instància ([SyncFirebase]); el constructor, no.
 */
class FirebaseAuthGateway(
  private val context: Context,
  private val firebase: SyncFirebase,
  private val tracker: Tracker,
) : AuthGateway {
  override suspend fun currentAccount(): SyncAccount? =
    try {
      auth().currentUser?.toAccount()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      tracker.trackError(e, ErrorSites.SYNC_SIGN_IN)
      null
    }

  override suspend fun signIn(activity: Activity): SignInResult =
    try {
      // Primer la instància: si no es pot crear, no cal ensenyar el full de Google
      val auth = auth()
      val idToken = googleIdToken(activity)
      val user = auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).awaitTask().user
      SignInResult.Success(checkNotNull(user) { "Signed in without a user" }.toAccount())
    } catch (e: CancellationException) {
      throw e
    } catch (_: GetCredentialCancellationException) {
      // L'usuari ha tancat el full: ni és un error ni es registra
      SignInResult.Cancelled
    } catch (e: Exception) {
      if (e.isNetworkFailure()) {
        SignInResult.Offline
      } else {
        tracker.trackError(e, ErrorSites.SYNC_SIGN_IN)
        SignInResult.Failed
      }
    }

  /** El full s'obre sobre l'[activity]: amb el context de l'aplicació Credential Manager no el pot ensenyar. */
  private suspend fun googleIdToken(activity: Activity): String {
    val option = GetSignInWithGoogleOption.Builder(activity.getString(R.string.default_web_client_id)).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    val credential = CredentialManager.create(activity).getCredential(activity, request).credential
    check(credential is CustomCredential && credential.type in GOOGLE_ID_TOKEN_TYPES) { "Not a Google ID token" }
    return GoogleIdTokenCredential.createFrom(credential.data).idToken
  }

  /** No peta mai: tancar la sessió és el camí de sortida de tots els errors de [SyncRepository]. */
  override suspend fun signOut() {
    try {
      auth().signOut()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      tracker.trackError(e, ErrorSites.SYNC_SIGN_IN)
    }
    try {
      // Perquè el proper «Continua amb Google» torni a deixar triar el compte. Si falla, no hi ha res a fer
      CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
    }
  }

  /** Crear la instància i recuperar la sessió desada llegeix del disc. */
  private suspend fun auth(): FirebaseAuth = withContext(Dispatchers.IO) { firebase.auth }

  private fun FirebaseUser.toAccount() = SyncAccount(uid = uid, name = displayName, email = email)

  private companion object {
    val GOOGLE_ID_TOKEN_TYPES =
      setOf(
        GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
        GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL,
      )
  }
}
