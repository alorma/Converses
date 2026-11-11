package com.alorma.contactnotes.ui.sync

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.sync.crypto.Bip39
import com.alorma.contactnotes.designsystem.components.ConversesTopBar
import com.alorma.contactnotes.designsystem.components.readableWidth
import com.alorma.contactnotes.designsystem.components.rememberTopBarScrollBehavior
import com.alorma.contactnotes.ui.entry.SecureWhile
import com.alorma.contactnotes.ui.entry.readClipboardText
import org.koin.compose.viewmodel.koinViewModel

/**
 * Escriure les 12 paraules en un dispositiu nou (13j). [onClose] tanca la pantalla, tant si s'han acceptat (el compte
 * ja és a punt) com si l'usuari se'n va. Porta `FLAG_SECURE` mentre és visible.
 */
@Composable
fun EnterWordsScreen(
  onClose: () -> Unit,
  viewModel: EnterWordsViewModel = koinViewModel(),
) {
  SecureWhile(true)
  val state by viewModel.state.collectAsStateWithLifecycle()
  val open by viewModel.open.collectAsStateWithLifecycle()
  // Acceptades, o sessió tancada perquè el compte no tenia res: el compte ja no espera la clau. També si el procés
  // es restaura amb la pantalla oberta i l'estat ja no és «falta la clau».
  LaunchedEffect(open) { if (!open) onClose() }

  val snackbar = remember { SnackbarHostState() }
  val resources = LocalResources.current
  LaunchedEffect(viewModel) {
    viewModel.messages.collect { message ->
      val text =
        when (message) {
          EnterWordsMessage.Offline -> R.string.sync_error_offline
          EnterWordsMessage.Failed -> R.string.sync_error_failed
        }
      snackbar.showSnackbar(resources.getString(text))
    }
  }
  val context = LocalContext.current
  EnterWordsContent(
    state = state,
    snackbar = snackbar,
    onClose = onClose,
    onPaste = { readClipboardText(context)?.let(viewModel::onPaste) },
    onTextChange = viewModel::onTextChange,
    onFocusChange = viewModel::onFocusChange,
    onSuggestion = viewModel::onSuggestion,
    onSubmit = viewModel::submit,
    autoFocus = true,
  )
}

@Composable
fun EnterWordsContent(
  state: EnterWordsUiState,
  onClose: () -> Unit,
  onPaste: () -> Unit,
  onTextChange: (String) -> Unit,
  onFocusChange: (Boolean) -> Unit,
  onSuggestion: (String) -> Unit,
  onSubmit: () -> Unit,
  modifier: Modifier = Modifier,
  snackbar: SnackbarHostState = remember { SnackbarHostState() },
  /** Posa el focus al camp en obrir; les previews no ho fan. */
  autoFocus: Boolean = false,
) {
  val scrollBehavior = rememberTopBarScrollBehavior()
  val requester = remember { FocusRequester() }
  LaunchedEffect(autoFocus) { if (autoFocus) requester.requestFocus() }

  Scaffold(
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    containerColor = MaterialTheme.colorScheme.surface,
    contentWindowInsets = WindowInsets.navigationBars,
    snackbarHost = { SnackbarHost(snackbar) },
    topBar = {
      ConversesTopBar(
        title = stringResource(R.string.sync_key_words),
        scrollBehavior = scrollBehavior,
        navigationIcon = {
          FilledTonalIconButton(onClick = onClose) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
          }
        },
        actions = {
          OutlinedButton(
            onClick = onPaste,
            enabled = !state.submitting,
            modifier = Modifier.padding(end = 8.dp),
          ) {
            Icon(Icons.Outlined.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.sync_paste), modifier = Modifier.padding(start = 8.dp))
          }
        },
      )
    },
  ) { padding ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(padding)
          .consumeWindowInsets(padding)
          .imePadding(),
    ) {
      Column(
        modifier =
          Modifier
            .weight(1f)
            .readableWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (state.wrongWords) WrongWordsNote()
        NoPersonalizedLearning {
          WordsField(
            state = state,
            onTextChange = onTextChange,
            onFocusChange = onFocusChange,
            modifier = Modifier.focusRequester(requester),
          )
        }
        Text(
          text = stringResource(R.string.sync_words_count, state.wordCount, Bip39.WORD_COUNT),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 4.dp),
        )
        Suggestions(suggestions = state.suggestions, onPick = onSuggestion)
      }
      Button(
        onClick = onSubmit,
        enabled = state.canSubmit,
        modifier = Modifier.readableWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(56.dp),
      ) {
        if (state.submitting) {
          CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
          Text(stringResource(R.string.sync_continue))
        }
      }
    }
  }
}

@Composable
private fun WrongWordsNote() {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.errorContainer,
    contentColor = MaterialTheme.colorScheme.onErrorContainer,
  ) {
    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Icon(Icons.Rounded.ErrorOutline, contentDescription = null)
      Text(stringResource(R.string.sync_words_wrong), style = MaterialTheme.typography.bodyMedium)
    }
  }
}

/**
 * El camp únic de les paraules. El text és el que l'usuari ha escrit; les paraules que no són de la llista es pinten
 * d'error amb una [VisualTransformation] que no canvia ni la longitud ni el cursor.
 */
@Composable
private fun WordsField(
  state: EnterWordsUiState,
  onTextChange: (String) -> Unit,
  onFocusChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val errorColor = colors.error
  val rejected = state.rejected
  val focused = state.focused
  // Les marques es calculen amb el text que arriba a la transformació, no amb el de l'estat: poden diferir un quadre
  val transformation =
    remember(rejected, focused, errorColor) {
      VisualTransformation { text ->
        val ranges = unknownRanges(text.text, rejected, lastInProgress = focused)
        val colored =
          buildAnnotatedString {
            append(text)
            ranges.forEach { addStyle(SpanStyle(color = errorColor), it.first, it.last + 1) }
          }
        TransformedText(colored, OffsetMapping.Identity)
      }
    }
  val accent =
    when {
      state.hasUnknown -> colors.error
      focused -> colors.primary
      else -> null
    }
  val label = stringResource(R.string.sync_words_field)
  BasicTextField(
    value = state.text,
    onValueChange = onTextChange,
    enabled = !state.submitting,
    minLines = 4,
    textStyle = MaterialTheme.typography.titleMedium.copy(color = colors.onSurface),
    cursorBrush = SolidColor(colors.primary),
    visualTransformation = transformation,
    keyboardOptions =
      KeyboardOptions(
        capitalization = KeyboardCapitalization.None,
        autoCorrectEnabled = false,
        // Password: els teclats no hi suggereixen ni hi aprenen; el text es veu perquè no hi ha transformació
        keyboardType = KeyboardType.Password,
      ),
    modifier =
      modifier
        .fillMaxWidth()
        .onFocusChanged { onFocusChange(it.isFocused) }
        .semantics { contentDescription = label },
    decorationBox = { inner ->
      Box(
        Modifier
          .fillMaxWidth()
          .background(colors.surfaceContainerHigh, RoundedCornerShape(16.dp))
          .let { if (accent != null) it.border(BorderStroke(2.dp, accent), RoundedCornerShape(16.dp)) else it }
          .padding(16.dp),
      ) {
        if (state.text.isEmpty()) {
          Text(label, style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant)
        }
        inner()
      }
    },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Suggestions(
  suggestions: List<String>,
  onPick: (String) -> Unit,
) {
  // L'alçada es reserva sempre: res no salta quan apareixen o desapareixen els xips
  FlowRow(
    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    suggestions.forEach { word ->
      SuggestionChip(onClick = { onPick(word) }, label = { Text(word) })
    }
  }
}
