package com.alorma.contactnotes.ui.approval

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.settings.SettingsRepository
import com.alorma.contactnotes.designsystem.theme.ConversesTheme
import com.alorma.contactnotes.ui.person.promptUnlock
import com.alorma.contactnotes.ui.person.unlockAvailability
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

/**
 * S'obre en tocar la notificació d'aprovació d'una persona protegida demanada des del rellotge (o directament, amb
 * l'app en primer pla) i **llança el `BiometricPrompt` sense cap pantalla pròpia**: la finestra és transparent. Amb
 * èxit s'aprova i l'activitat es tanca. Si l'usuari cancel·la el prompt, aleshores sí que es veu [ApprovalContent],
 * amb «Confirma» (reobre el prompt) i «Denega». No mostra mai el nom ni cap contingut de la persona: l'única dada que
 * rep és l'identificador de la petició. És una `FragmentActivity` perquè el `BiometricPrompt` ho necessita.
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
    val viewModel: ApprovalViewModel by viewModel { parametersOf(requestId) }
    val initialSettings = runBlocking { settingsRepository.settings.first() }

    lifecycleScope.launch {
      viewModel.state.collect { state ->
        when (state) {
          ApprovalUiState.Prompting -> if (viewModel.takePromptRequest()) prompt(viewModel)
          ApprovalUiState.Confirm -> Unit
          ApprovalUiState.Closed -> close()
          ApprovalUiState.Failed -> {
            // Petició caducada, cancel·lada al rellotge o desconeguda: no hi ha res a aprovar
            Toast.makeText(applicationContext, R.string.approval_failed, Toast.LENGTH_SHORT).show()
            close()
          }
        }
      }
    }

    setContent {
      val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialSettings)
      val state by viewModel.state.collectAsStateWithLifecycle()
      // Només després de cancel·lar el prompt; abans la finestra és transparent
      if (state == ApprovalUiState.Confirm) {
        ConversesTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor, personColorTheme = false) {
          ApprovalContent(onConfirm = viewModel::onRetry, onDeny = viewModel::onDeny, onClose = ::finish)
        }
      }
    }
  }

  // El diàleg del BiometricPrompt és del sistema i no es tanca sol amb l'activitat: s'ha de cancel·lar explícitament
  private var activePrompt: BiometricPrompt? = null

  private fun close() {
    activePrompt?.cancelAuthentication()
    activePrompt = null
    finish()
  }

  override fun onDestroy() {
    if (!isChangingConfigurations) activePrompt?.cancelAuthentication()
    super.onDestroy()
  }

  private suspend fun prompt(viewModel: ApprovalViewModel) {
    // Sense biometria ni bloqueig de pantalla no es pot aprovar: es tracta com una denegació
    if (unlockAvailability() != BiometricManager.BIOMETRIC_SUCCESS) {
      viewModel.onDeny()
      return
    }
    // El nom de la persona només surt aquí, un cop l'usuari ha actuat; la notificació no el duu mai
    val details = viewModel.details()
    activePrompt =
      promptUnlock(
        title =
          getString(
            R.string.approval_prompt_title,
            details?.watchName ?: getString(R.string.approval_watch_fallback),
            details?.personName.orEmpty(),
          ),
        subtitle = getString(R.string.unlock_subtitle),
        onError = { viewModel.onPromptCancelled() },
      ) { viewModel.onAuthenticated() }
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
