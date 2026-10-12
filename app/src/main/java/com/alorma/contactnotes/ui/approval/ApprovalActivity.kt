package com.alorma.contactnotes.ui.approval

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.settings.SettingsRepository
import com.alorma.contactnotes.designsystem.theme.ConversesTheme
import com.alorma.contactnotes.ui.person.promptUnlock
import com.alorma.contactnotes.ui.person.unlockAvailability
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * S'obre en tocar la notificació d'aprovació d'una persona protegida demanada des del rellotge. No mostra mai el nom
 * ni cap contingut de la persona: l'única dada que rep és l'identificador de la petició. És una `FragmentActivity`
 * perquè el `BiometricPrompt` ho necessita.
 */
class ApprovalActivity : FragmentActivity() {
  private val settingsRepository: SettingsRepository by inject()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val requestId = intent.getStringExtra(EXTRA_REQUEST_ID)
    if (requestId == null) {
      finish()
      return
    }
    enableEdgeToEdge()
    val initialSettings = runBlocking { settingsRepository.settings.first() }
    val title = getString(R.string.approval_prompt_title)
    val subtitle = getString(R.string.approval_prompt_subtitle)

    setContent {
      val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialSettings)
      ConversesTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor, personColorTheme = false) {
        val viewModel: ApprovalViewModel = koinViewModel(parameters = { parametersOf(requestId) })
        val state by viewModel.state.collectAsStateWithLifecycle()
        val failedMessage = stringResource(R.string.approval_failed)
        LaunchedEffect(state) {
          when (state) {
            ApprovalUiState.Failed -> {
              Toast.makeText(applicationContext, failedMessage, Toast.LENGTH_SHORT).show()
              finish()
            }

            ApprovalUiState.Closed -> finish()
            else -> Unit
          }
        }
        ApprovalContent(
          state = state,
          onConfirm = {
            // Sense biometria ni bloqueig de pantalla no es pot aprovar: es tracta com una denegació
            if (unlockAvailability() == BiometricManager.BIOMETRIC_SUCCESS) {
              promptUnlock(title, subtitle, onError = { viewModel.onDeny() }) { viewModel.onAuthenticated() }
            } else {
              viewModel.onDeny()
            }
          },
          onDeny = viewModel::onDeny,
          onClose = ::finish,
        )
      }
    }
  }

  companion object {
    const val EXTRA_REQUEST_ID = "request_id"

    fun intent(
      context: Context,
      requestId: String,
    ): Intent =
      Intent(context, ApprovalActivity::class.java)
        .putExtra(EXTRA_REQUEST_ID, requestId)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
  }
}
