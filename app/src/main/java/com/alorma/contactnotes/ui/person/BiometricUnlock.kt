package com.alorma.contactnotes.ui.person

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

private const val Authenticators = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

fun FragmentActivity.canUnlockWithDevice(): Boolean =
  BiometricManager.from(this).canAuthenticate(Authenticators) == BiometricManager.BIOMETRIC_SUCCESS

/** Codi de `BiometricManager.canAuthenticate` per a biometria forta o credencial del dispositiu. */
fun FragmentActivity.unlockAvailability(): Int = BiometricManager.from(this).canAuthenticate(Authenticators)

/**
 * `onError` rep el codi d'`onAuthenticationError`; un dit que no coincideix no fa res i el prompt continua obert. Retorna el
 * prompt perquè qui el necessiti el pugui cancel·lar (el diàleg és del sistema i no es tanca sol amb l'activitat).
 */
fun FragmentActivity.promptUnlock(
  title: String,
  subtitle: String,
  onError: (code: Int) -> Unit = {},
  onSuccess: () -> Unit,
): BiometricPrompt {
  val callback =
    object : BiometricPrompt.AuthenticationCallback() {
      override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()

      override fun onAuthenticationError(
        errorCode: Int,
        errString: CharSequence,
      ) = onError(errorCode)
    }
  val info =
    BiometricPrompt.PromptInfo
      .Builder()
      .setTitle(title)
      .setSubtitle(subtitle)
      .setAllowedAuthenticators(Authenticators)
      .build()
  return BiometricPrompt(this, ContextCompat.getMainExecutor(this), callback).also { it.authenticate(info) }
}

/** Obre l'alta d'empremta o bloqueig de pantalla; si el sistema no la té, la configuració de seguretat. */
fun Context.openSecuritySetup() {
  val enroll =
    Intent(
      Settings.ACTION_BIOMETRIC_ENROLL,
    ).putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, Authenticators)
  try {
    startActivity(enroll)
  } catch (_: ActivityNotFoundException) {
    startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
  }
}
