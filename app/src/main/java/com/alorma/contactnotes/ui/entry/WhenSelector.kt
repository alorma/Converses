package com.alorma.contactnotes.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.components.groupedItemShape
import com.alorma.contactnotes.designsystem.theme.SheetTitleStyle
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.delay

/** Hora actual, refrescada a cada canvi de minut. */
@Composable
fun rememberMinuteClock(): State<Long> {
  val now = remember { mutableStateOf(System.currentTimeMillis()) }
  LaunchedEffect(Unit) {
    while (true) {
      val current = System.currentTimeMillis()
      now.value = current
      delay(60_000L - current % 60_000L)
    }
  }
  return now
}

private data class WhenDisplay(
  val icon: ImageVector,
  val title: String,
  val subtitle: String,
)

@Composable
private fun WhenChoice.display(now: Long): WhenDisplay =
  when (this) {
    WhenChoice.Now -> WhenDisplay(Icons.Rounded.Schedule, stringResource(R.string.when_now), formatDateTime(now))
    is WhenChoice.HourAgo ->
      WhenDisplay(
        Icons.Rounded.History,
        stringResource(R.string.when_hour_ago),
        formatDateTime(at),
      )
    is WhenChoice.Yesterday ->
      WhenDisplay(
        Icons.Rounded.EventRepeat,
        stringResource(R.string.when_yesterday),
        formatDateTime(at),
      )
    is WhenChoice.Custom -> WhenDisplay(Icons.Rounded.EditCalendar, formatLongDate(at), formatTimeOfDay(at))
    is WhenChoice.Stored -> WhenDisplay(Icons.Rounded.EditCalendar, formatLongDate(at), formatTimeOfDay(at))
  }

/** Fila «Quan»: mostra l'opció triada i obre el full «Quan va ser?». */
@Composable
fun WhenRow(
  choice: WhenChoice,
  onChoice: (WhenChoice) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showSheet by rememberSaveable { mutableStateOf(false) }
  var showPickers by rememberSaveable { mutableStateOf(false) }
  val now by rememberMinuteClock()
  val display = choice.display(now)
  val description = stringResource(R.string.when_row_description, display.title, display.subtitle)

  Surface(
    onClick = { showSheet = true },
    shape = MaterialTheme.shapes.extraLarge,
    color = GroupedListDefaults.containerColor,
    modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = description },
  ) {
    Row(
      modifier = Modifier.heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.size(44.dp),
      ) {
        Box(contentAlignment = Alignment.Center) { Icon(display.icon, contentDescription = null) }
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(display.title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(display.subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Icon(Icons.Rounded.UnfoldMore, contentDescription = null)
    }
  }

  if (showSheet) {
    WhenSheet(
      choice = choice,
      now = now,
      onDismiss = { showSheet = false },
      onChoice = {
        onChoice(it)
        showSheet = false
      },
      onPick = {
        showSheet = false
        showPickers = true
      },
    )
  }
  if (showPickers) {
    WhenPickers(
      initialAt = choice.at ?: System.currentTimeMillis(),
      onDismiss = { showPickers = false },
      onPicked = {
        onChoice(WhenChoice.Custom(it))
        showPickers = false
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WhenSheet(
  choice: WhenChoice,
  now: Long,
  onDismiss: () -> Unit,
  onChoice: (WhenChoice) -> Unit,
  onPick: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    WhenSheetContent(choice = choice, now = now, onChoice = onChoice, onPick = onPick)
  }
}

/** Contingut del full «Quan va ser?»; el `ModalBottomSheet` és una finestra a part. */
@Composable
internal fun WhenSheetContent(
  choice: WhenChoice,
  now: Long,
  onChoice: (WhenChoice) -> Unit,
  onPick: () -> Unit,
) {
  val options =
    listOf(
      SheetOption(WhenChoice.Now.display(now), choice is WhenChoice.Now) { onChoice(WhenChoice.Now) },
      SheetOption(WhenChoice.hourAgo(now).display(now), choice is WhenChoice.HourAgo) {
        onChoice(WhenChoice.hourAgo(System.currentTimeMillis()))
      },
      SheetOption(WhenChoice.yesterday(now).display(now), choice is WhenChoice.Yesterday) {
        onChoice(WhenChoice.yesterday(System.currentTimeMillis()))
      },
      SheetOption(
        display =
          WhenDisplay(
            icon = Icons.Rounded.EditCalendar,
            title = stringResource(R.string.when_pick),
            subtitle =
              if (choice is WhenChoice.Custom) {
                formatDateTime(
                  choice.at,
                )
              } else {
                stringResource(R.string.when_pick_sub)
              },
          ),
        selected = choice is WhenChoice.Custom,
        trailingChevron = true,
        onClick = onPick,
      ),
    )
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
    verticalArrangement = Arrangement.spacedBy(GroupedListDefaults.ItemSpacing),
  ) {
    Text(
      text = stringResource(R.string.when_sheet_title),
      style = SheetTitleStyle,
      modifier = Modifier.padding(start = 6.dp, bottom = 14.dp),
    )
    options.forEachIndexed { index, option ->
      WhenOptionRow(option, groupedItemShape(index, options.size))
    }
  }
}

private class SheetOption(
  val display: WhenDisplay,
  val selected: Boolean,
  val trailingChevron: Boolean = false,
  val onClick: () -> Unit,
)

@Composable
private fun WhenOptionRow(
  option: SheetOption,
  shape: androidx.compose.ui.graphics.Shape,
) {
  val scheme = MaterialTheme.colorScheme
  val container = if (option.selected) scheme.primaryContainer else GroupedListDefaults.containerColor
  val content = if (option.selected) scheme.onPrimaryContainer else scheme.onSurface
  Surface(shape = shape, color = container, contentColor = content, modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier =
        Modifier
          .selectable(selected = option.selected, role = Role.RadioButton, onClick = option.onClick)
          .heightIn(min = 64.dp)
          .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Icon(option.display.icon, contentDescription = null)
      Column(modifier = Modifier.weight(1f)) {
        Text(option.display.title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(
          option.display.subtitle,
          fontSize = 13.sp,
          color = if (option.selected) content else scheme.onSurfaceVariant,
        )
      }
      when {
        option.selected -> Icon(Icons.Rounded.Check, contentDescription = null)
        option.trailingChevron ->
          Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
          )
      }
    }
  }
}

private enum class PickerStep { Date, Time }

/** Selector de dia (fins avui) i després d'hora, en 24 h. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WhenPickers(
  initialAt: Long,
  onDismiss: () -> Unit,
  onPicked: (Long) -> Unit,
) {
  val zone = ZoneId.systemDefault()
  var step by rememberSaveable { mutableStateOf(PickerStep.Date) }
  var pickedDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
  val initial = Instant.ofEpochMilli(initialAt).atZone(zone)

  when (step) {
    PickerStep.Date -> {
      val todayUtc =
        LocalDate
          .now(zone)
          .atStartOfDay(ZoneOffset.UTC)
          .toInstant()
          .toEpochMilli()
      val state =
        rememberDatePickerState(
          initialSelectedDateMillis =
            initial
              .toLocalDate()
              .atStartOfDay(ZoneOffset.UTC)
              .toInstant()
              .toEpochMilli(),
          selectableDates =
            object : SelectableDates {
              override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayUtc

              override fun isSelectableYear(year: Int): Boolean = year <= LocalDate.now(zone).year
            },
        )
      DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
          TextButton(
            enabled = state.selectedDateMillis != null,
            onClick = {
              pickedDateMillis = state.selectedDateMillis
              step = PickerStep.Time
            },
          ) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
      ) { DatePicker(state = state) }
    }

    PickerStep.Time -> {
      val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
      AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
          TextButton(onClick = {
            val date = Instant.ofEpochMilli(pickedDateMillis ?: initialAt).atZone(ZoneOffset.UTC).toLocalDate()
            val at =
              LocalDateTime
                .of(
                  date,
                  LocalTime.of(state.hour, state.minute),
                ).atZone(zone)
                .toInstant()
                .toEpochMilli()
            onPicked(minOf(at, System.currentTimeMillis()))
          }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
        text = { TimePicker(state = state) },
      )
    }
  }
}
