package com.alorma.contactnotes.ui.edit

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.db.ChannelType
import com.alorma.contactnotes.designsystem.components.Avatar
import com.alorma.contactnotes.designsystem.components.AvatarColor
import com.alorma.contactnotes.designsystem.components.AvatarShape
import com.alorma.contactnotes.designsystem.components.ColorSwatches
import com.alorma.contactnotes.designsystem.components.ConversesFilterChip
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.components.asShape
import com.alorma.contactnotes.designsystem.components.groupedItemShape
import com.alorma.contactnotes.designsystem.theme.DialogTitleStyle
import com.alorma.contactnotes.designsystem.theme.PersonTheme
import com.alorma.contactnotes.designsystem.theme.ScreenTitleStyle
import com.alorma.contactnotes.ui.people.initialsOf
import com.alorma.contactnotes.ui.people.label
import com.alorma.contactnotes.ui.person.canUnlockWithDevice
import com.alorma.contactnotes.ui.person.icon
import com.alorma.contactnotes.ui.person.promptUnlock
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.alorma.contactnotes.data.settings.SecureWindow
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EditPersonScreen(
  personId: Long,
  onClose: () -> Unit,
  onSaved: (Long) -> Unit,
  onDeleted: () -> Unit,
  viewModel: EditPersonViewModel = koinViewModel(parameters = { parametersOf(personId) }),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val activity = LocalActivity.current as FragmentActivity
  val canAuthenticate = remember { activity.canUnlockWithDevice() }
  val authTitle = stringResource(R.string.protected_person)
  val authSubtitle = stringResource(R.string.unlock_subtitle)
  val secureWindow: SecureWindow = koinInject()
  val secure = state.loaded && (state.baseline.isProtected || state.isProtected)
  DisposableEffect(secure) {
    if (secure) secureWindow.acquire()
    onDispose { if (secure) secureWindow.release() }
  }

  EditPersonContent(
    state = state,
    canAuthenticate = canAuthenticate,
    actions = viewModel,
    onClose = onClose,
    onSaved = onSaved,
    onDeleted = onDeleted,
    onProtectedChange = { value -> activity.promptUnlock(authTitle, authSubtitle) { viewModel.onProtected(value) } },
  )
}

@Composable
fun EditPersonContent(
  state: EditPersonUiState,
  canAuthenticate: Boolean,
  actions: EditPersonActions,
  onClose: () -> Unit,
  onSaved: (Long) -> Unit,
  onDeleted: () -> Unit,
  onProtectedChange: (Boolean) -> Unit,
) {
  var channelDialog by remember { mutableStateOf<ChannelDialogState?>(null) }
  var showDelete by remember { mutableStateOf(false) }

  PersonTheme(state.color) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
      if (!state.loaded) return@Surface
      Column(
        modifier = Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.statusBars)
          .windowInsetsPadding(WindowInsets.navigationBars)
          .imePadding()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          FilledTonalIconButton(onClick = onClose) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
          }
          Button(onClick = { actions.save(onSaved) }, enabled = state.canSave) {
            Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
          }
        }
        Text(
          text = stringResource(if (state.isNew) R.string.new_person else R.string.edit_person),
          style = ScreenTitleStyle,
          modifier = Modifier.padding(start = 4.dp),
        )

        AppearanceCard(state, actions)

        Column(verticalArrangement = Arrangement.spacedBy(GroupedListDefaults.ItemSpacing)) {
          FieldCard(index = 0, count = if (state.aliasTaken) 3 else 2) {
            LabeledField(label = stringResource(R.string.field_name), value = state.name, onValueChange = actions::onName)
          }
          FieldCard(index = 1, count = if (state.aliasTaken) 3 else 2, error = state.aliasTaken) {
            LabeledField(
              label = stringResource(R.string.field_alias),
              value = state.alias,
              onValueChange = actions::onAlias,
              prefix = "@",
              isError = state.aliasTaken,
            )
          }
          if (state.aliasTaken) {
            FieldCard(index = 2, count = 3) {
              Text(
                text = stringResource(R.string.alias_taken, state.alias),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
              )
            }
          }
        }

        SectionHeader(icon = true, title = stringResource(R.string.pinned_facts), hint = stringResource(R.string.pinned_hint))
        FactsEditor(state.facts, actions)

        SectionHeader(icon = false, title = stringResource(R.string.channels_title), hint = null)
        Column(verticalArrangement = Arrangement.spacedBy(GroupedListDefaults.ItemSpacing)) {
          val count = state.channels.size + 1
          state.channels.forEachIndexed { index, channel ->
            FieldCard(index = index, count = count, onClick = { channelDialog = ChannelDialogState(channel) }) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
              ) {
                Icon(channel.type.icon(), contentDescription = null)
                Column(modifier = Modifier.weight(1f)) {
                  Text(stringResource(channel.type.label()), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                  if (channel.handle.isNotBlank()) {
                    Text(channel.handle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
                IconButton(onClick = { actions.onRemoveChannel(channel.key) }) {
                  Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = stringResource(R.string.remove))
                }
              }
            }
          }
          FieldCard(index = state.channels.size, count = count, onClick = { channelDialog = ChannelDialogState(null) }) {
            AddRow(stringResource(R.string.add_channel))
          }
        }

        FieldCard(index = 0, count = 1) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            Icon(Icons.Rounded.Fingerprint, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
              Text(stringResource(R.string.protected_person), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              Text(
                text = stringResource(if (canAuthenticate) R.string.protected_person_summary else R.string.protected_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = state.isProtected,
              enabled = canAuthenticate,
              onCheckedChange = onProtectedChange,
            )
          }
        }
        if (!state.isNew) {
          OutlinedButton(
            onClick = { showDelete = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          ) {
            Icon(Icons.Rounded.Delete, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.delete_person), fontWeight = FontWeight.Bold)
          }
        }
        Spacer(Modifier.size(24.dp))
      }
    }

    if (showDelete) {
      DeletePersonDialog(
        name = state.name,
        factsCount = state.facts.size,
        entriesCount = state.entryCount,
        onDismiss = { showDelete = false },
        onConfirm = { exportFirst ->
          showDelete = false
          actions.delete(exportFirst, onDeleted)
        },
      )
    }

    channelDialog?.let { dialog ->
      ChannelDialog(
        initial = dialog.channel,
        onDismiss = { channelDialog = null },
        onConfirm = { type, handle ->
          actions.onSaveChannel(dialog.channel?.key, type, handle)
          channelDialog = null
        },
      )
    }
  }
}

private data class ChannelDialogState(val channel: ChannelDraft?)

@Composable
private fun AppearanceCard(state: EditPersonUiState, actions: EditPersonActions) {
  Surface(shape = MaterialTheme.shapes.extraLarge, color = GroupedListDefaults.containerColor, modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(
          initials = initialsOf(state.name.ifBlank { "?" }),
          shape = state.shape,
          color = state.color,
          size = 96.dp,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          SmallLabel(stringResource(R.string.shape_label))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AvatarShape.entries.forEach { shape ->
              ShapeOption(shape = shape, selected = shape == state.shape, onClick = { actions.onShape(shape) })
            }
          }
        }
      }
      SmallLabel(stringResource(R.string.color_label))
      ColorSwatches(entries = AvatarColor.entries, selected = state.color, colorOf = { it.color }, onSelect = actions::onColor)
    }
  }
}

@Composable
private fun ShapeOption(shape: AvatarShape, selected: Boolean, onClick: () -> Unit) {
  val scheme = MaterialTheme.colorScheme
  Box(
    modifier = Modifier
      .size(52.dp)
      .then(if (selected) Modifier.border(BorderStroke(2.dp, scheme.primary), RoundedCornerShape(16.dp)) else Modifier)
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier
        .size(34.dp)
        .clip(shape.asShape())
        .background(if (selected) scheme.primary else scheme.outline),
    )
  }
}

@Composable
private fun SmallLabel(text: String) {
  Text(
    text = text.uppercase(),
    style = MaterialTheme.typography.labelMedium,
    fontWeight = FontWeight.Bold,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

@Composable
private fun SectionHeader(icon: Boolean, title: String, hint: String?) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    if (icon) Icon(Icons.Rounded.PushPin, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(1f),
    )
    if (hint != null) {
      Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
private fun FieldCard(
  index: Int,
  count: Int,
  modifier: Modifier = Modifier,
  error: Boolean = false,
  onClick: (() -> Unit)? = null,
  content: @Composable () -> Unit,
) {
  val shape = groupedItemShape(index, count)
  val color = if (error) MaterialTheme.colorScheme.errorContainer else GroupedListDefaults.containerColor
  if (onClick != null) {
    Surface(onClick = onClick, shape = shape, color = color, modifier = modifier.fillMaxWidth()) { content() }
  } else {
    Surface(shape = shape, color = color, modifier = modifier.fillMaxWidth()) { content() }
  }
}

private class PrefixTransformation(private val prefix: String) : VisualTransformation {
  override fun filter(text: AnnotatedString): TransformedText = TransformedText(
    AnnotatedString(prefix) + text,
    object : OffsetMapping {
      override fun originalToTransformed(offset: Int) = offset + prefix.length
      override fun transformedToOriginal(offset: Int) = (offset - prefix.length).coerceAtLeast(0)
    },
  )
}


@Composable
private fun LabeledField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  prefix: String? = null,
  isError: Boolean = false,
) {
  TextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier.fillMaxWidth(),
    label = { Text(label) },
    visualTransformation = if (prefix != null && value.isNotEmpty()) PrefixTransformation(prefix) else VisualTransformation.None,
    singleLine = true,
    isError = isError,
    colors = TextFieldDefaults.colors(
      focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
      unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
      errorContainerColor = androidx.compose.ui.graphics.Color.Transparent,
      focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
      unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
      errorIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
    ),
  )
}

@Composable
private fun AddRow(text: String) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Icon(Icons.Rounded.Add, contentDescription = null)
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun FactsEditor(facts: List<FactDraft>, actions: EditPersonActions) {
  var dragKey by remember { mutableStateOf<Long?>(null) }
  var dragOffset by remember { mutableFloatStateOf(0f) }
  val heights = remember { mutableStateMapOf<Long, Int>() }
  val currentFacts by rememberUpdatedState(facts)
  val count = facts.size + 1

  Column(verticalArrangement = Arrangement.spacedBy(GroupedListDefaults.ItemSpacing)) {
    facts.forEachIndexed { index, fact ->
      key(fact.key) {
      val dragging = dragKey == fact.key
      FieldCard(
        index = index,
        count = count,
        modifier = Modifier
          .onSizeChanged { heights[fact.key] = it.height }
          .zIndex(if (dragging) 1f else 0f)
          .graphicsLayer { translationY = if (dragging) dragOffset else 0f },
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Rounded.DragIndicator,
            contentDescription = stringResource(R.string.reorder),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
              .padding(12.dp)
              .pointerInput(fact.key) {
                detectDragGestures(
                  onDragStart = {
                    dragKey = fact.key
                    dragOffset = 0f
                  },
                  onDragEnd = {
                    dragKey = null
                    dragOffset = 0f
                  },
                  onDragCancel = {
                    dragKey = null
                    dragOffset = 0f
                  },
                  onDrag = { change, amount ->
                    change.consume()
                    dragOffset += amount.y
                    val list = currentFacts
                    val at = list.indexOfFirst { it.key == fact.key }
                    val height = heights[fact.key] ?: 0
                    if (dragOffset > height / 2f && at < list.lastIndex) {
                      dragOffset -= heights[list[at + 1].key] ?: height
                      actions.onMoveFact(at, at + 1)
                    } else if (dragOffset < -height / 2f && at > 0) {
                      dragOffset += heights[list[at - 1].key] ?: height
                      actions.onMoveFact(at, at - 1)
                    }
                  },
                )
              },
          )
          Column(modifier = Modifier.weight(1f)) {
            InlineField(
              value = fact.label,
              onValueChange = { actions.onFactChange(fact.key, it, fact.value) },
              placeholder = stringResource(R.string.fact_label_hint),
              small = true,
            )
            InlineField(
              value = fact.value,
              onValueChange = { actions.onFactChange(fact.key, fact.label, it) },
              placeholder = stringResource(R.string.fact_value_hint),
              small = false,
            )
          }
          IconButton(onClick = { actions.onRemoveFact(fact.key) }) {
            Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = stringResource(R.string.remove))
          }
        }
      }
      }
    }
    FieldCard(index = facts.size, count = count, onClick = { actions.onAddFact() }) {
      AddRow(stringResource(R.string.add_fact))
    }
  }
}

@Composable
private fun InlineField(value: String, onValueChange: (String) -> Unit, placeholder: String, small: Boolean) {
  val style = if (small) {
    MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
  } else {
    MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
  }
  BasicTextField(
    value = value,
    onValueChange = onValueChange,
    textStyle = style,
    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    decorationBox = { inner ->
      Box {
        if (value.isEmpty()) Text(placeholder, style = style.copy(color = MaterialTheme.colorScheme.outline))
        inner()
      }
    },
  )
}

@Composable
internal fun ChannelDialog(
  initial: ChannelDraft?,
  onDismiss: () -> Unit,
  onConfirm: (ChannelType, String) -> Unit,
) {
  var type by remember { mutableStateOf(initial?.type ?: ChannelType.DISCORD) }
  var handle by remember { mutableStateOf(initial?.handle.orEmpty()) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(if (initial == null) R.string.add_channel else R.string.edit_channel)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(ChannelType.entries, key = { it.name }) { option ->
            ConversesFilterChip(
              label = stringResource(option.label()),
              selected = option == type,
              onClick = { type = option },
              tonal = true,
            )
          }
        }
        if (type != ChannelType.IN_PERSON) {
          TextField(
            value = handle,
            onValueChange = { handle = it },
            singleLine = true,
            label = { Text(stringResource(R.string.channel_handle)) },
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    },
    confirmButton = { TextButton(onClick = { onConfirm(type, handle) }) { Text(stringResource(android.R.string.ok)) } },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
  )
}

@Composable
internal fun DeletePersonDialog(
  name: String,
  factsCount: Int,
  entriesCount: Int,
  onDismiss: () -> Unit,
  onConfirm: (exportFirst: Boolean) -> Unit,
) {
  var exportFirst by remember { mutableStateOf(true) }
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Box(
        modifier = Modifier.size(64.dp).clip(AvatarShape.Clover4Leaf.asShape()).background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
      }
    },
    title = { Text(stringResource(R.string.delete_person_title, name), style = DialogTitleStyle, textAlign = TextAlign.Center) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
          text = stringResource(
            R.string.delete_person_text,
            pluralStringResource(R.plurals.fact_count, factsCount, factsCount),
            pluralStringResource(R.plurals.entry_count, entriesCount, entriesCount),
          ),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth(),
        )
        Surface(
          shape = MaterialTheme.shapes.large,
          color = GroupedListDefaults.containerColor,
          modifier = Modifier.fillMaxWidth().clickable { exportFirst = !exportFirst },
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Checkbox(checked = exportFirst, onCheckedChange = { exportFirst = it })
            Column {
              Text(stringResource(R.string.export_before), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              Text(
                stringResource(R.string.export_before_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(exportFirst) },
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.error,
          contentColor = MaterialTheme.colorScheme.onError,
        ),
      ) { Text(stringResource(R.string.delete_button), fontWeight = FontWeight.Bold) }
    },
    dismissButton = { OutlinedButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
  )
}
