package com.alorma.contactnotes.ui.sync

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.sync.SyncState
import com.alorma.contactnotes.designsystem.components.ConversesTopBar
import com.alorma.contactnotes.designsystem.components.rememberTopBarScrollBehavior
import org.koin.compose.viewmodel.koinViewModel

/**
 * Pantalla «Sincronització». El que mostra depèn de l'estat del compte: sense sessió (13d), l'activació al primer
 * dispositiu (13e), «falta la clau» (13i) o amb sessió (13o). Fa servir el tema de l'app, no el de cap persona, i no
 * mostra mai noms ni contingut de persones.
 */
@Composable
fun SyncScreen(
  onBack: () -> Unit,
  /** Obre les paraules de seguretat; l'empremta ja s'ha demanat. */
  onOpenWords: (activation: Boolean) -> Unit,
  onEnterWords: () -> Unit,
  viewModel: SyncViewModel = koinViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val wordsReviewedAt by viewModel.wordsReviewedAt.collectAsStateWithLifecycle()
  val status by viewModel.status.collectAsStateWithLifecycle()
  val initialSync by viewModel.initialSync.collectAsStateWithLifecycle()
  val activity = LocalActivity.current
  val snackbar = remember { SnackbarHostState() }
  val resources = LocalResources.current
  LaunchedEffect(viewModel) {
    viewModel.messages.collect { message ->
      val text =
        when (message) {
          SyncMessage.SignInOffline -> R.string.sync_error_offline
          SyncMessage.SignInFailed -> R.string.sync_error_failed
        }
      snackbar.showSnackbar(resources.getString(text))
    }
  }
  // La pujada o baixada de després d'activar ocupa tota la pantalla. «Amaga» i «Fet» hi tornen: la feina, si encara
  // corre, es veu a l'estat de 13o
  initialSync?.let { initial ->
    InitialSyncContent(
      state = initial,
      onHide = viewModel::onInitialSyncDismissed,
      onDone = viewModel::onInitialSyncDismissed,
      onMergeContinue = viewModel::onMergeContinue,
    )
    return
  }
  SyncScaffold(onBack = onBack, snackbar = snackbar) { padding ->
    when (val current = state) {
      SyncState.SignedOut ->
        SyncSignedOutContent(
          padding = padding,
          onSignIn = { activity?.let(viewModel::onSignIn) },
        )
      SyncState.Checking ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      is SyncState.Activating ->
        SyncActivatingContent(
          padding = padding,
          takePrompt = viewModel::takeActivationPrompt,
          onUnlocked = {
            viewModel.onWordsUnlocked()
            onOpenWords(true)
          },
          onCancel = viewModel::onCancelActivation,
        )
      is SyncState.NeedsKey ->
        SyncNeedsKeyContent(
          account = current.account,
          padding = padding,
          onEnterWords = onEnterWords,
          onChangeAccount = viewModel::onSignOut,
        )
      is SyncState.Ready ->
        SyncReadyContent(
          account = current.account,
          wordsReviewedAt = wordsReviewedAt,
          status = status,
          padding = padding,
          onWordsUnlocked = {
            viewModel.onWordsUnlocked()
            onOpenWords(false)
          },
          onSignOut = viewModel::onSignOut,
        )
    }
  }
}

/** El marc comú: capçalera fixa amb el títol i enrere. Cada contingut rep el `padding` i en fa el seu scroll. */
@Composable
fun SyncScaffold(
  onBack: () -> Unit,
  snackbar: SnackbarHostState = remember { SnackbarHostState() },
  content: @Composable (PaddingValues) -> Unit,
) {
  val scrollBehavior = rememberTopBarScrollBehavior()
  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      ConversesTopBar(
        title = stringResource(R.string.sync_title),
        scrollBehavior = scrollBehavior,
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
          }
        },
      )
    },
    snackbarHost = { SnackbarHost(snackbar) },
    content = content,
  )
}
