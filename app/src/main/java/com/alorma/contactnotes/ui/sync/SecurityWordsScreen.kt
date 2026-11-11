package com.alorma.contactnotes.ui.sync

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.settings.SecureWindow
import com.alorma.contactnotes.designsystem.components.ConversesTopBar
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.components.readableWidth
import com.alorma.contactnotes.designsystem.components.rememberTopBarScrollBehavior
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Les paraules de seguretat (13f). Amb [activation], porta el peu de «Les he guardat» i «Continua», i tancar-la abans
 * tanca la sessió. [onClose] tanca la pantalla en tots els casos.
 */
@Composable
fun SecurityWordsScreen(
  activation: Boolean,
  onClose: () -> Unit,
  viewModel: SecurityWordsViewModel = koinViewModel(parameters = { parametersOf(activation) }),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val resources = LocalResources.current
  val scope = rememberCoroutineScope()
  val snackbar = remember { SnackbarHostState() }
  val currentOnClose by rememberUpdatedState(onClose)

  val secureWindow: SecureWindow = koinInject()
  DisposableEffect(Unit) {
    secureWindow.acquire()
    onDispose { secureWindow.release() }
  }

  LaunchedEffect(viewModel) {
    viewModel.events.collect { event ->
      when (event) {
        SecurityWordsEvent.Close -> currentOnClose()
        SecurityWordsEvent.Offline -> snackbar.showSnackbar(resources.getString(R.string.sync_error_offline))
        SecurityWordsEvent.Failed -> snackbar.showSnackbar(resources.getString(R.string.sync_error_failed))
      }
    }
  }
  // Tancar amb el gest també ha de passar per aquí: a l'activació tanca la sessió
  BackHandler(onBack = viewModel::close)

  val copiedText = stringResource(R.string.sync_copied)
  SecurityWordsContent(
    activation = activation,
    state = state,
    snackbar = snackbar,
    onClose = viewModel::close,
    onCopy = {
      SensitiveClipboard.copy(context, state.words.joinToString(" "))
      scope.launch {
        snackbar.currentSnackbarData?.dismiss()
        snackbar.showSnackbar(copiedText)
      }
    },
    onSavedChange = viewModel::onSavedChange,
    onContinue = viewModel::onContinue,
  )
}

@Composable
fun SecurityWordsContent(
  activation: Boolean,
  state: SecurityWordsState,
  snackbar: SnackbarHostState,
  onClose: () -> Unit,
  onCopy: () -> Unit,
  onSavedChange: (Boolean) -> Unit,
  onContinue: () -> Unit,
) {
  val scrollBehavior = rememberTopBarScrollBehavior()
  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    containerColor = MaterialTheme.colorScheme.surface,
    topBar = {
      ConversesTopBar(
        title = stringResource(R.string.sync_words_title),
        scrollBehavior = scrollBehavior,
        navigationIcon = {
          IconButton(onClick = onClose) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
          }
        },
      )
    },
    snackbarHost = { SnackbarHost(snackbar) },
  ) { padding ->
    Column(Modifier.fillMaxSize().padding(padding)) {
      Column(
        modifier =
          Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .readableWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Text(
          text = stringResource(R.string.sync_words_body),
          style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 4.dp),
        )
        WordsBlock(state.words)
        FilledTonalButton(onClick = onCopy, modifier = Modifier.height(40.dp)) {
          Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(20.dp))
          Text(
            stringResource(R.string.sync_copy),
            modifier = Modifier.padding(start = 8.dp),
            fontWeight = FontWeight.Bold,
          )
        }
        LoseNote()
      }
      if (activation) {
        ActivationFooter(
          saved = state.saved,
          working = state.working,
          onSavedChange = onSavedChange,
          onContinue = onContinue,
        )
      }
    }
  }
}

/**
 * Les paraules juntes, com un sol bloc de text dins una targeta (decisió de Bernat, no pas la graella del disseny).
 * No es parteixen amb guionet ni es tradueixen: són una clau, no una frase.
 */
@Composable
private fun WordsBlock(words: List<String>) {
  Surface(shape = RoundedCornerShape(20.dp), color = GroupedListDefaults.containerColor) {
    Text(
      text = words.joinToString(" "),
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 18.dp),
      style =
        MaterialTheme.typography.bodyLarge.copy(
          fontSize = 19.sp,
          lineHeight = 30.sp,
          fontWeight = FontWeight.SemiBold,
          hyphens = Hyphens.None,
          lineBreak = LineBreak.Simple,
        ),
    )
  }
}

@Composable
private fun LoseNote() {
  Surface(shape = RoundedCornerShape(20.dp), color = GroupedListDefaults.containerColor) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
        Icons.Outlined.Info,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(22.dp),
      )
      Text(
        text = stringResource(R.string.sync_words_lose),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

/** Fix a baix: «Continua» no s'activa fins que la casella és marcada, i fins aleshores no es puja res. */
@Composable
private fun ActivationFooter(
  saved: Boolean,
  working: Boolean,
  onSavedChange: (Boolean) -> Unit,
  onContinue: () -> Unit,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth().readableWidth().padding(
        PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
      ),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .toggleable(value = saved, enabled = !working, role = Role.Checkbox, onValueChange = onSavedChange),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Checkbox(checked = saved, onCheckedChange = null, enabled = !working, modifier = Modifier.padding(12.dp))
      Text(stringResource(R.string.sync_words_saved), style = MaterialTheme.typography.bodyLarge)
    }
    Button(
      onClick = onContinue,
      enabled = saved && !working,
      modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
      if (working) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
      } else {
        Text(stringResource(R.string.sync_continue), fontWeight = FontWeight.Bold)
      }
    }
    Text(
      text = stringResource(R.string.sync_words_nothing_uploaded),
      style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    )
  }
}
