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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
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
    onFocus = viewModel::onFocus,
    onBlur = viewModel::onBlur,
    onWordChange = viewModel::onWordChange,
    onSuggestion = viewModel::onSuggestion,
    onSubmit = viewModel::submit,
    autoFocus = true,
  )
}

@Composable
fun EnterWordsContent(
  state: EnterWordsUiState,
  onClose: () -> Unit,
  /** Enganxa el porta-retalls; retorna el camp on cal posar el focus, si n'hi ha. */
  onPaste: () -> Int?,
  onFocus: (Int) -> Unit,
  onBlur: (Int) -> Unit,
  onWordChange: (Int, String) -> Int?,
  onSuggestion: (Int, String) -> Int?,
  onSubmit: () -> Unit,
  modifier: Modifier = Modifier,
  snackbar: SnackbarHostState = remember { SnackbarHostState() },
  /** Posa el focus al primer camp en obrir; les previews no ho fan. */
  autoFocus: Boolean = false,
) {
  val scrollBehavior = rememberTopBarScrollBehavior()
  val focusManager = LocalFocusManager.current
  val requesters = remember { List(Bip39.WORD_COUNT) { FocusRequester() } }

  // Els camps desapareixen sense focus si no hi ha on anar: es tanca el teclat
  fun moveTo(next: Int?) {
    if (next != null) requesters[next].requestFocus() else focusManager.clearFocus()
  }
  LaunchedEffect(autoFocus) { if (autoFocus) requesters[0].requestFocus() }

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
            onClick = { onPaste()?.let { moveTo(it) } },
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
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        if (state.wrongWords) WrongWordsNote()
        NoPersonalizedLearning {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Les columnes s'omplen de dalt a baix: 1–6 a l'esquerra, 7–12 a la dreta
            for (column in 0 until 2) {
              Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in 0 until Bip39.WORD_COUNT / 2) {
                  val index = column * (Bip39.WORD_COUNT / 2) + row
                  WordField(
                    number = index + 1,
                    word = state.words[index],
                    active = state.focused == index,
                    unknown = state.isUnknown(index),
                    enabled = !state.submitting,
                    isLast = index == Bip39.WORD_COUNT - 1,
                    canSubmit = state.canSubmit,
                    modifier =
                      Modifier
                        .focusRequester(requesters[index])
                        .onFocusChanged { if (it.isFocused) onFocus(index) else onBlur(index) },
                    onChange = { text -> onWordChange(index, text)?.let(requesters::get)?.requestFocus() },
                    onNext = { if (index < Bip39.WORD_COUNT - 1) requesters[index + 1].requestFocus() },
                    onDone = {
                      focusManager.clearFocus()
                      if (state.canSubmit) onSubmit()
                    },
                  )
                }
              }
            }
          }
        }
        Suggestions(
          suggestions = state.suggestions,
          onPick = { word -> state.focused?.let { moveTo(onSuggestion(it, word)) } },
        )
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

@Composable
private fun WordField(
  number: Int,
  word: String,
  active: Boolean,
  unknown: Boolean,
  enabled: Boolean,
  isLast: Boolean,
  canSubmit: Boolean,
  onChange: (String) -> Unit,
  onNext: () -> Unit,
  onDone: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val accent =
    when {
      unknown -> colors.error
      active -> colors.primary
      else -> null
    }
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .height(48.dp)
        .background(colors.surfaceContainerHigh, RoundedCornerShape(12.dp))
        .let { if (accent != null) it.border(BorderStroke(2.dp, accent), RoundedCornerShape(12.dp)) else it }
        .padding(horizontal = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = number.toString(),
      style = MaterialTheme.typography.labelMedium,
      color = if (unknown) colors.error else colors.onSurfaceVariant,
      modifier = Modifier.width(24.dp),
    )
    Box(Modifier.weight(1f)) {
      BasicTextField(
        value = word,
        onValueChange = onChange,
        enabled = enabled,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions =
          KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            // Password: els teclats no hi suggereixen ni hi aprenen; el text es veu perquè no hi ha transformació
            keyboardType = KeyboardType.Password,
            imeAction = if (isLast) ImeAction.Done else ImeAction.Next,
          ),
        keyboardActions = KeyboardActions(onNext = { onNext() }, onDone = { onDone() }),
        modifier = modifier.fillMaxWidth(),
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Suggestions(
  suggestions: List<String>,
  onPick: (String) -> Unit,
) {
  // L'alçada es reserva sempre: els camps no salten quan apareixen o desapareixen els xips
  FlowRow(
    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    suggestions.forEach { word ->
      SuggestionChip(onClick = { onPick(word) }, label = { Text(word) })
    }
  }
}
