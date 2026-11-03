package com.alorma.contactnotes.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.data.CustomChannelResult
import com.alorma.contactnotes.data.db.CustomChannelEntity
import com.alorma.contactnotes.designsystem.components.ConversesSaveButton
import com.alorma.contactnotes.designsystem.theme.SheetTitleStyle
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.ui.person.channelName
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Full «Nou canal» (7b), o «Edita el canal» si hi ha [editing] (reanomenar i canviar de grup).
 * [onSaved] rep el canal desat, amb el seu id, per triar-lo a l'entrada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomChannelSheet(
  initialGroup: ChannelType,
  onDismiss: () -> Unit,
  onSaved: (CustomChannelEntity) -> Unit,
  editing: CustomChannelEntity? = null,
) {
  val repository: ConversesRepository = koinInject()
  val scope = rememberCoroutineScope()
  var name by rememberSaveable { mutableStateOf(editing?.name.orEmpty()) }
  var group by rememberSaveable { mutableStateOf(editing?.channelType ?: initialGroup) }
  // El grup on el nom ja existeix, per dir-ho sota el camp; desapareix en tocar el nom o el grup
  var takenIn by rememberSaveable { mutableStateOf<ChannelType?>(null) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    CustomChannelSheetContent(
      isNew = editing == null,
      name = name,
      onNameChange = {
        name = it
        takenIn = null
      },
      group = group,
      onGroupChange = {
        group = it
        takenIn = null
      },
      takenIn = takenIn,
      onCancel = onDismiss,
      onSave = {
        scope.launch {
          when (val result = repository.saveCustomChannel(editing, group, name)) {
            is CustomChannelResult.Saved -> repository.getCustomChannel(result.id)?.let(onSaved)
            CustomChannelResult.NameTaken -> takenIn = group
            CustomChannelResult.NameBlank -> Unit
          }
        }
      },
    )
  }
}

@Composable
fun CustomChannelSheetContent(
  isNew: Boolean,
  name: String,
  onNameChange: (String) -> Unit,
  group: ChannelType,
  onGroupChange: (ChannelType) -> Unit,
  takenIn: ChannelType?,
  onCancel: () -> Unit,
  onSave: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val focusRequester = remember { FocusRequester() }
  LaunchedEffect(Unit) { if (isNew) focusRequester.requestFocus() }
  val canSave = name.isNotBlank() && takenIn == null

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.navigationBars)
        .imePadding()
        .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
        stringResource(if (isNew) R.string.custom_channel_new_title else R.string.custom_channel_edit_title),
        style = SheetTitleStyle,
      )
      Text(
        stringResource(R.string.custom_channel_sheet_body),
        style = MaterialTheme.typography.bodyLarge,
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    TextField(
      value = name,
      onValueChange = onNameChange,
      modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
      label = { Text(stringResource(R.string.custom_channel_name)) },
      singleLine = true,
      isError = takenIn != null,
      supportingText =
        takenIn?.let { taken -> { Text(stringResource(R.string.custom_channel_name_taken, channelName(taken))) } },
      keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
      textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
      shape = RoundedCornerShape(20.dp),
      colors =
        TextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
          errorContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
          focusedIndicatorColor = Color.Transparent,
          unfocusedIndicatorColor = Color.Transparent,
          errorIndicatorColor = Color.Transparent,
        ),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
        stringResource(R.string.custom_channel_group),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.3.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
      )
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ChannelType.entries.forEach { type ->
          SelectableChip(label = channelName(type), selected = type == group, onClick = { onGroupChange(type) })
        }
      }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      ConversesSaveButton(
        onClick = onSave,
        enabled = canSave,
        modifier = Modifier.fillMaxWidth().height(56.dp),
      ) {
        Text(
          stringResource(if (isNew) R.string.custom_channel_create else R.string.save),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      }
      TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Text(stringResource(R.string.custom_channel_cancel), fontWeight = FontWeight.Bold)
      }
    }
  }
}
