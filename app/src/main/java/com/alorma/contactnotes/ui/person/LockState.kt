package com.alorma.contactnotes.ui.person

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

const val LockoutSeconds = 30

/** Estat de la pantalla de bloqueig d'una persona protegida. */
sealed interface LockState {
  /** `BiometricPrompt` obert. */
  data object Waiting : LockState

  /** L'usuari ha cancel·lat, o hi ha un error recuperable. `permanentLockout` demana el PIN del dispositiu. */
  data class Canceled(val permanentLockout: Boolean = false) : LockState

  /** Massa intents: el botó de reintent s'activa quan `seconds` arriba a 0. */
  data class LockedOut(val seconds: Int) : LockState

  /** El dispositiu no té biometria ni bloqueig de pantalla. */
  data object NoCredentials : LockState
}

/** Resultat de `BiometricManager.canAuthenticate`: només amb èxit es pot obrir el prompt. */
fun lockStateForAvailability(code: Int): LockState =
  if (code == BiometricManager.BIOMETRIC_SUCCESS) LockState.Waiting else LockState.NoCredentials

/** Error de `BiometricPrompt` convertit a estat; els desconeguts es tracten com una cancel·lació. */
fun lockStateForError(code: Int): LockState = when (code) {
  BiometricPrompt.ERROR_LOCKOUT -> LockState.LockedOut(LockoutSeconds)
  BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> LockState.Canceled(permanentLockout = true)
  BiometricPrompt.ERROR_NO_BIOMETRICS, BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> LockState.NoCredentials
  else -> LockState.Canceled()
}

/** Emet `from`, `from - 1`… fins a 0, un cop per segon. */
fun countdown(from: Int): Flow<Int> = flow {
  for (seconds in from downTo 0) {
    emit(seconds)
    if (seconds > 0) delay(1_000)
  }
}
