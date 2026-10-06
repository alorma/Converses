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

fun FragmentActivity.promptUnlock(title: String, subtitle: String, onSuccess: () -> Unit) {
  val callback = object : BiometricPrompt.AuthenticationCallback() {
    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
  }
  val info = BiometricPrompt.PromptInfo.Builder()
    .setTitle(title)
    .setSubtitle(subtitle)
    .setAllowedAuthenticators(Authenticators)
    .build()
  BiometricPrompt(this, ContextCompat.getMainExecutor(this), callback).authenticate(info)
}

/** Obre l'alta d'empremta o bloqueig de pantalla; si el sistema no la té, la configuració de seguretat. */
fun Context.openSecuritySetup() {
  val enroll = Intent(Settings.ACTION_BIOMETRIC_ENROLL).putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, Authenticators)
  try {
    startActivity(enroll)
  } catch (_: ActivityNotFoundException) {
    startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
  }
}
