package com.alorma.contactnotes.ui.consent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.designsystem.components.ConversesSaveButton
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.components.asShape
import com.alorma.contactnotes.designsystem.components.groupedItemShape
import com.alorma.contactnotes.designsystem.theme.SheetTitleStyle
import com.alorma.contactnotes.shared.AvatarShape

/**
 * Full d'una sola vegada de les estadístiques d'ús (10a, 10b). «D'acord» crida [onChoice] amb la tria. Tancar-lo d'una
 * altra manera (lliscant, tocant fora o enrere) compta com a «No», encara que hi hagués «Sí» marcat: el consentiment
 * només es dona confirmant-lo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsentSheet(onChoice: (Boolean) -> Unit) {
  var choice by rememberSaveable { mutableStateOf<Boolean?>(null) }
  ModalBottomSheet(
    onDismissRequest = { onChoice(false) },
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor = MaterialTheme.colorScheme.surface,
    dragHandle = null,
  ) {
    ConsentSheetContent(
      choice = choice,
      onChoiceChange = { choice = it },
      onConfirm = { choice?.let(onChoice) },
    )
  }
}

@Composable
fun ConsentSheetContent(
  choice: Boolean?,
  onChoiceChange: (Boolean) -> Unit,
  onConfirm: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scheme = MaterialTheme.colorScheme
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Box(
      modifier =
        Modifier
          .padding(horizontal = 4.dp)
          .size(56.dp)
          .clip(AvatarShape.Cookie9Sided.asShape())
          .background(scheme.primaryContainer),
      contentAlignment = Alignment.Center,
    ) {
      Icon(Icons.Rounded.Insights, contentDescription = null, tint = scheme.onPrimaryContainer)
    }
    Column(modifier = Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(stringResource(R.string.consent_title), style = SheetTitleStyle)
      Text(
        stringResource(R.string.consent_body),
        style = MaterialTheme.typography.bodyLarge,
        fontSize = 15.sp,
        color = scheme.onSurfaceVariant,
      )
    }
    Column(verticalArrangement = Arrangement.spacedBy(GroupedListDefaults.ItemSpacing)) {
      InfoRow(Icons.Rounded.Check, R.string.consent_sent_label, R.string.consent_sent, index = 0)
      InfoRow(Icons.Rounded.Block, R.string.consent_never_label, R.string.consent_never, index = 1)
    }
    Column(
      modifier = Modifier.selectableGroup(),
      verticalArrangement = Arrangement.spacedBy(GroupedListDefaults.ItemSpacing),
    ) {
      ChoiceRow(
        title = stringResource(R.string.consent_yes),
        summary = stringResource(R.string.consent_yes_summary),
        selected = choice == true,
        onClick = { onChoiceChange(true) },
        index = 0,
      )
      ChoiceRow(
        title = stringResource(R.string.consent_no),
        summary = stringResource(R.string.consent_no_summary),
        selected = choice == false,
        onClick = { onChoiceChange(false) },
        index = 1,
      )
    }
    ConversesSaveButton(
      onClick = onConfirm,
      enabled = choice != null,
      modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
      Text(
        stringResource(R.string.consent_ok),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun InfoRow(
  icon: ImageVector,
  label: Int,
  text: Int,
  index: Int,
) {
  val labelText = stringResource(label)
  val bodyText = stringResource(text)
  Surface(shape = groupedItemShape(index, 2), color = GroupedListDefaults.containerColor) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
      Text(
        buildAnnotatedString {
          withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(labelText) }
          append(" ")
          append(bodyText)
        },
        style = MaterialTheme.typography.bodyMedium,
      )
    }
  }
}

@Composable
private fun ChoiceRow(
  title: String,
  summary: String,
  selected: Boolean,
  onClick: () -> Unit,
  index: Int,
) {
  val scheme = MaterialTheme.colorScheme
  val shape = if (selected) groupedItemShape(0, 1) else groupedItemShape(index, 2)
  Surface(shape = shape, color = if (selected) scheme.primaryContainer else GroupedListDefaults.containerColor) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .heightIn(min = 60.dp)
          .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
          .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // La fila sencera és el radio; el botó és només visual
      RadioButton(
        selected = selected,
        onClick = null,
        colors = RadioButtonDefaults.colors(selectedColor = scheme.onPrimaryContainer),
      )
      Column {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
          summary,
          style = MaterialTheme.typography.bodySmall,
          color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
        )
      }
    }
  }
}
