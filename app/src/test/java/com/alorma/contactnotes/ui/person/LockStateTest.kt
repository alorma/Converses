package com.alorma.contactnotes.ui.person

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LockStateTest {
  @Test
  fun availableDeviceOpensThePrompt() {
    assertEquals(LockState.Waiting, lockStateForAvailability(BiometricManager.BIOMETRIC_SUCCESS))
  }

  @Test
  fun deviceWithoutCredentialsShowsTheSetupState() {
    listOf(
      BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
      BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
      BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
    ).forEach { assertEquals(LockState.NoCredentials, lockStateForAvailability(it)) }
  }

  @Test
  fun cancellationsMapToCanceled() {
    listOf(
      BiometricPrompt.ERROR_USER_CANCELED,
      BiometricPrompt.ERROR_NEGATIVE_BUTTON,
      BiometricPrompt.ERROR_CANCELED,
      BiometricPrompt.ERROR_TIMEOUT,
    ).forEach { assertEquals(LockState.Canceled(), lockStateForError(it)) }
  }

  @Test
  fun lockoutStartsA30SecondCountdown() {
    assertEquals(LockState.LockedOut(30), lockStateForError(BiometricPrompt.ERROR_LOCKOUT))
  }

  @Test
  fun permanentLockoutAsksForThePin() {
    assertEquals(LockState.Canceled(permanentLockout = true), lockStateForError(BiometricPrompt.ERROR_LOCKOUT_PERMANENT))
  }

  @Test
  fun missingCredentialsAfterThePromptShowTheSetupState() {
    assertEquals(LockState.NoCredentials, lockStateForError(BiometricPrompt.ERROR_NO_BIOMETRICS))
    assertEquals(LockState.NoCredentials, lockStateForError(BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL))
  }

  @Test
  fun unknownErrorsAreTreatedAsCanceled() {
    assertEquals(LockState.Canceled(), lockStateForError(BiometricPrompt.ERROR_VENDOR))
  }

  @Test
  fun countdownEmitsDownToZero() = runTest {
    assertEquals(listOf(3, 2, 1, 0), countdown(3).toList())
  }
}
