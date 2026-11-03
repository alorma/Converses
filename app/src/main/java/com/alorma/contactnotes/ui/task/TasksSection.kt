package com.alorma.contactnotes.ui.task

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.db.TaskEntity
import com.alorma.contactnotes.designsystem.components.GroupedList
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.theme.SectionTitleStyle
import com.alorma.contactnotes.ui.dateFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * Secció «Pendents» del detall (9d–9h), entre «Dades» i «Cronologia». [canCreate] (Pro) mostra «+ Nova tasca» i,
 * sense pendents, la targeta buida; sense Pro, la secció només surt si ja hi ha tasques (es poden marcar).
 */
@Composable
fun TasksSection(
  open: List<TaskEntity>,
  done: List<TaskEntity>,
  canCreate: Boolean,
  today: LocalDate,
  onNewTask: () -> Unit,
  onOpen: (TaskEntity) -> Unit,
  onDone: (TaskEntity) -> Unit,
  onReopen: (TaskEntity) -> Unit,
  modifier: Modifier = Modifier,
  /** Només per a les previews (9f): «Fets» desplegat. */
  initialDoneExpanded: Boolean = false,
) {
  val locale = LocalConfiguration.current.locales[0]
  var doneExpanded by rememberSaveable { mutableStateOf(initialDoneExpanded) }
  Column(modifier = modifier.padding(top = 24.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f).semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(Icons.Rounded.Flag, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(text = stringResource(R.string.tasks_title), style = SectionTitleStyle)
        if (open.isNotEmpty()) {
          Text(
            text = "· ${open.size}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      if (canCreate) {
        FilledTonalButton(
          onClick = onNewTask,
          modifier = Modifier.height(36.dp),
          contentPadding = PaddingValues(start = 10.dp, end = 14.dp),
          colors =
            ButtonDefaults.filledTonalButtonColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
          Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Text(
            text = stringResource(R.string.task_new),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp),
          )
        }
      }
    }

    if (open.isEmpty()) {
      if (canCreate) {
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = GroupedListDefaults.containerColor,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = stringResource(R.string.tasks_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
          )
        }
      }
    } else {
      GroupedList(open) { task ->
        TaskRow(
          label = dueLabel(task.dueDate?.let(LocalDate::ofEpochDay), today).text(locale),
          text = task.text,
          done = false,
          onCheck = { onDone(task) },
          onOpen = { onOpen(task) },
        )
      }
    }

    if (done.isNotEmpty()) {
      val expandedState = stringResource(if (doneExpanded) R.string.facts_hide else R.string.facts_show)
      Row(
        modifier =
          Modifier
            .padding(top = 6.dp)
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { doneExpanded = !doneExpanded }
            .semantics { stateDescription = expandedState }
            .padding(start = 16.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        val color = MaterialTheme.colorScheme.onSurfaceVariant
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = color)
        Text(
          text = stringResource(R.string.tasks_done_count, done.size),
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = color,
          modifier = Modifier.weight(1f),
        )
        Icon(if (doneExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = color)
      }
      if (doneExpanded) {
        GroupedList(done) { task ->
          TaskRow(
            label = doneLabel(doneDate(task.doneAt ?: 0L), today).text(locale),
            text = task.text,
            done = true,
            onCheck = { onReopen(task) },
            onOpen = { onOpen(task) },
          )
        }
      }
    }
  }
}

private fun doneDate(at: Long): LocalDate = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate()

@Composable
private fun TaskRow(
  label: String,
  text: String,
  done: Boolean,
  onCheck: () -> Unit,
  onOpen: () -> Unit,
) {
  val variant = MaterialTheme.colorScheme.onSurfaceVariant
  Row(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(start = 4.dp, end = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    val checkDescription = stringResource(if (done) R.string.task_reopen else R.string.task_mark_done)
    IconButton(onClick = onCheck, modifier = Modifier.semantics { contentDescription = checkDescription }) {
      if (done) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = variant)
      } else {
        Box(Modifier.size(24.dp).border(2.dp, variant, CircleShape))
      }
    }
    Column(
      modifier = Modifier.weight(1f).padding(vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.4.sp,
        color = variant,
      )
      Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        fontSize = 15.sp,
        color = if (done) variant else MaterialTheme.colorScheme.onSurface,
        textDecoration = if (done) TextDecoration.LineThrough else null,
      )
    }
  }
}

@Composable
private fun DueLabel.text(locale: Locale): String =
  when (this) {
    DueLabel.None -> stringResource(R.string.task_no_date)
    DueLabel.Today -> stringResource(R.string.today)
    DueLabel.Tomorrow -> stringResource(R.string.task_tomorrow)
    DueLabel.Yesterday -> stringResource(R.string.yesterday)
    is DueLabel.Weekday -> dayName(date, locale)
    is DueLabel.Date -> formatDueDate(date, locale)
    is DueLabel.Overdue -> pluralStringResource(R.plurals.task_overdue_days, days, days)
  }.uppercase(locale)

@Composable
private fun DoneLabel.text(locale: Locale): String =
  when (this) {
    DoneLabel.Today -> stringResource(R.string.task_done_today)
    DoneLabel.Yesterday -> stringResource(R.string.task_done_yesterday)
    is DoneLabel.On -> stringResource(R.string.task_done_on, date.format(dateFormatter("d MMMM", locale)))
  }.uppercase(locale)
