package com.alorma.contactnotes.ui.people

import android.text.format.DateUtils
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.db.PersonEntity
import com.alorma.contactnotes.data.db.PersonListItem
import com.alorma.contactnotes.data.db.color
import com.alorma.contactnotes.data.db.shape
import com.alorma.contactnotes.designsystem.components.Avatar
import com.alorma.contactnotes.designsystem.components.WelcomeEmptyState
import com.alorma.contactnotes.designsystem.components.rememberConversesHaptics
import com.alorma.contactnotes.designsystem.expressive.ExpressiveFilterChip
import com.alorma.contactnotes.designsystem.expressive.ExpressiveShapes
import com.alorma.contactnotes.designsystem.expressive.ExpressiveTheme
import com.alorma.contactnotes.designsystem.expressive.ExpressiveType
import com.alorma.contactnotes.designsystem.expressive.FeaturedCard
import com.alorma.contactnotes.designsystem.expressive.GroupHeader
import com.alorma.contactnotes.designsystem.expressive.ShapeButton
import com.alorma.contactnotes.designsystem.expressive.WideTitle
import com.alorma.contactnotes.designsystem.expressive.rememberAvatarPalette
import com.alorma.contactnotes.designsystem.expressive.rememberPersonPalette
import com.alorma.contactnotes.shared.ChannelType
import com.alorma.contactnotes.shared.EntryKind
import com.alorma.contactnotes.shared.initialsOf
import com.alorma.contactnotes.ui.navigation.LocalFloatingBarPadding
import com.alorma.contactnotes.ui.person.icon
import com.alorma.contactnotes.ui.sync.RemovedNotice
import com.alorma.contactnotes.ui.sync.SyncIndicator
import com.alorma.contactnotes.ui.sync.SyncIndicatorState
import com.alorma.contactnotes.ui.sync.currentSyncIndicatorState
import java.time.LocalDate
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val ScreenMargin = 16.dp
private val RowSpacing = 6.dp
private val GroupGap = 14.dp // + RowSpacing = 20 dp abans de cada capçalera de grup

@Composable
fun PeopleListRoute(
  archived: Boolean,
  onPersonClick: (PersonEntity) -> Unit,
  onNewPerson: () -> Unit,
  onAddEntry: (PersonEntity) -> Unit = {},
  /** L'id de la persona destacada (o `null`: sense targeta o Arxivades); a pantalla gran, el detall la mostra. */
  onFeaturedChanged: (Long?) -> Unit = {},
  inPane: Boolean = false,
  selectedPersonId: Long? = null,
  onOpenSync: () -> Unit = {},
  viewModel: PeopleListViewModel = koinViewModel(parameters = { parametersOf(archived) }),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  // Amb el rail (pantalla gran) l'indicador és al rail, no a la capçalera; amb SyncFlag apagat és `null`
  val syncIndicator = currentSyncIndicatorState()
  PeopleListScreen(
    state = state,
    archived = archived,
    onFilterToggled = viewModel::onFilterToggled,
    onPersonClick = onPersonClick,
    onNewPerson = onNewPerson,
    onAddEntry = onAddEntry,
    onFeaturedChanged = onFeaturedChanged,
    inPane = inPane,
    selectedPersonId = selectedPersonId,
    syncIndicator = if (inPane) null else syncIndicator,
    syncRemoved = syncIndicator is SyncIndicatorState.Removed,
    onOpenSync = onOpenSync,
  )
}

/**
 * @param inPane pantalla gran: el llistat és el panell estret al costat del detall. Els xips passen a dues línies en
 *   lloc de fer scroll, els avatars s'encongeixen i la fila de [selectedPersonId] (la persona oberta) queda marcada.
 */
@Composable
fun PeopleListScreen(
  state: PeopleListUiState,
  archived: Boolean,
  onFilterToggled: (ChannelType) -> Unit,
  onPersonClick: (PersonEntity) -> Unit,
  onNewPerson: () -> Unit,
  onAddEntry: (PersonEntity) -> Unit = {},
  onFeaturedChanged: (Long?) -> Unit = {},
  inPane: Boolean = false,
  selectedPersonId: Long? = null,
  syncIndicator: SyncIndicatorState? = null,
  /** Aquest dispositiu l'han tret del compte (13c): l'avís fix a dalt, que obre Sincronització. */
  syncRemoved: Boolean = false,
  onOpenSync: () -> Unit = {},
) {
  ExpressiveTheme {
    PeopleListContent(
      state = state,
      archived = archived,
      onFilterToggled = onFilterToggled,
      onPersonClick = onPersonClick,
      onNewPerson = onNewPerson,
      onAddEntry = onAddEntry,
      onFeaturedChanged = onFeaturedChanged,
      inPane = inPane,
      selectedPersonId = selectedPersonId,
      syncIndicator = syncIndicator,
      syncRemoved = syncRemoved,
      onOpenSync = onOpenSync,
    )
  }
}

@Composable
private fun PeopleListContent(
  state: PeopleListUiState,
  archived: Boolean,
  onFilterToggled: (ChannelType) -> Unit,
  onPersonClick: (PersonEntity) -> Unit,
  onNewPerson: () -> Unit,
  onAddEntry: (PersonEntity) -> Unit,
  onFeaturedChanged: (Long?) -> Unit,
  inPane: Boolean,
  selectedPersonId: Long?,
  syncIndicator: SyncIndicatorState?,
  syncRemoved: Boolean,
  onOpenSync: () -> Unit,
) {
  val listState = rememberLazyListState()
  // La targeta destacada segueix el filtre (surt de les persones que es mostren) i no surt a Arxivades
  val featured = remember(state.people, archived) { if (archived) null else state.people.featured() }
  // A pantalla gran el detall segueix la destacada. Mentre carrega no s'informa (evita el parpelleig null) i en
  // marxar (a Arxivades, per exemple) el detall ja no la segueix
  val currentOnFeaturedChanged by rememberUpdatedState(onFeaturedChanged)
  val featuredId = featured?.person?.id
  LaunchedEffect(featuredId, state.isLoading) { if (!state.isLoading) currentOnFeaturedChanged(featuredId) }
  DisposableEffect(Unit) { onDispose { currentOnFeaturedChanged(null) } }
  val listFocus = remember { FocusRequester() }
  // El mateix idioma per agrupar i per escriure els noms dels grups, perquè la setmana i els mesos no divergeixin
  val locale = LocalConfiguration.current.locales[0]
  val groups = remember(state.people, locale) { state.people.groupByActivity(LocalDate.now(), locale = locale) }
  // Drawn once the rows are laid out (or there is nothing to show), not just when the data arrives
  ReportDrawnWhen { !state.isLoading && (state.people.isEmpty() || listState.layoutInfo.visibleItemsInfo.isNotEmpty()) }
  // En canviar els xips la llista torna a dalt (no a la primera composició, perquè no perdi la posició en tornar del detall)
  var lastFilter by remember { mutableStateOf(state.filter) }
  LaunchedEffect(state.filter) {
    if (state.filter != lastFilter) {
      lastFilter = state.filter
      listState.scrollToItem(0)
    }
  }
  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.navigationBars,
    topBar = {
      // Capçalera i filtres fixos a dalt, amb fons opac perquè la llista passi per sota, també l'àrea de la barra d'estat
      Column(
        modifier =
          Modifier
            // Teclat: la fletxa avall des de la capçalera o els filtres entra a la llista. Sense això la cerca de
            // focus salta als botons flotants, que queden per sobre de les files
            .focusProperties { down = listFocus }
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = ScreenMargin),
        verticalArrangement = Arrangement.spacedBy(RowSpacing),
      ) {
        Header(
          archived = archived,
          onNewPerson = onNewPerson,
          syncIndicator = syncIndicator,
          onOpenSync = onOpenSync,
        )
        if (syncRemoved) RemovedNotice(onAction = onOpenSync)
        if (!archived) {
          FilterRow(selected = state.filter, onToggle = onFilterToggled, wrap = inPane)
        }
      }
    },
  ) { padding ->
    LazyColumn(
      state = listState,
      modifier = Modifier.fillMaxSize().focusRequester(listFocus).focusGroup(),
      contentPadding =
        PaddingValues(
          start = ScreenMargin,
          end = ScreenMargin,
          top = padding.calculateTopPadding(),
          bottom = LocalFloatingBarPadding.current.calculateBottomPadding() + padding.calculateBottomPadding(),
        ),
      verticalArrangement = Arrangement.spacedBy(RowSpacing),
    ) {
      if (!state.isLoading && state.people.isEmpty()) {
        val noPeopleAtAll = !archived && state.filter.isEmpty()
        item(key = "empty") {
          if (noPeopleAtAll) {
            WelcomeEmptyState(
              icon = Icons.Rounded.Forum,
              title = stringResource(R.string.people_empty),
              description = stringResource(R.string.people_empty_description),
              actionLabel = stringResource(R.string.add_person),
              actionIcon = Icons.Rounded.PersonAdd,
              onAction = onNewPerson,
              modifier = Modifier.fillParentMaxSize(),
            )
          } else {
            WelcomeEmptyState(
              icon = if (archived) Icons.Rounded.Archive else Icons.Rounded.Groups,
              title = stringResource(if (archived) R.string.archived_empty else R.string.people_empty_filter),
              description =
                stringResource(
                  if (archived) R.string.archived_empty_description else R.string.people_empty_filter_description,
                ),
              modifier = Modifier.fillParentMaxSize(),
            )
          }
        }
      }
      if (featured != null) {
        item(key = "featured") {
          FeaturedPerson(
            item = featured,
            onClick = { onPersonClick(featured.person) },
            onAdd = { onAddEntry(featured.person) },
            modifier = Modifier.animateItem().padding(bottom = GroupGap),
          )
        }
      }
      groups.forEachIndexed { index, group ->
        item(key = "group-${group.period}") {
          val count = group.people.size
          GroupHeader(
            title = group.period.label(),
            count = count,
            countDescription = pluralStringResource(R.plurals.people_group_count, count, count),
            modifier = Modifier.padding(top = if (index == 0) 0.dp else GroupGap),
          )
        }
        items(group.people, key = { it.person.id }) { item ->
          PersonRow(
            item = item,
            onClick = { onPersonClick(item.person) },
            inPane = inPane,
            selected = inPane && item.person.id == selectedPersonId,
          )
        }
      }
    }
  }
}

/** Targeta destacada (14a): la persona de l'última entrada, amb la cita; no es crida mai amb una persona protegida. */
@Composable
private fun FeaturedPerson(
  item: PersonListItem,
  onClick: () -> Unit,
  onAdd: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val person = item.person
  val entry = checkNotNull(item.lastEntry)
  val locale = LocalConfiguration.current.locales[0]
  val now = remember(item) { System.currentTimeMillis() }
  val timeAt = featuredTimeAt(entry.occurredAt, now)
  val time =
    if (timeAt == null) {
      stringResource(R.string.featured_just_now)
    } else {
      DateUtils
        .getRelativeTimeSpanString(
          timeAt,
          now,
          DateUtils.MINUTE_IN_MILLIS,
          DateUtils.FORMAT_ABBREV_RELATIVE,
        ).toString()
    }
  val overline = "${person.name} · ${stringResource(entry.channelType.label())} · $time".uppercase(locale)
  val quote = if (entry.kind == EntryKind.HAPPENED) entry.text else stringResource(R.string.featured_quote, entry.text)
  FeaturedCard(
    overline = overline,
    quote = quote,
    palette = rememberAvatarPalette(person.color),
    onClick = onClick,
    onAdd = onAdd,
    addDescription = stringResource(R.string.featured_add_entry, person.name),
    avatar = { Avatar(initials = initialsOf(person.name), shape = person.shape, color = person.color, size = 30.dp) },
    modifier = modifier,
  )
}

@Composable
private fun Header(
  archived: Boolean,
  onNewPerson: () -> Unit,
  syncIndicator: SyncIndicatorState?,
  onOpenSync: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scheme = MaterialTheme.colorScheme
  Row(
    modifier = modifier.fillMaxWidth().padding(start = 4.dp, top = 16.dp, bottom = if (archived) 14.dp else 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    WideTitle(
      text = stringResource(if (archived) R.string.filter_archived else R.string.people_title),
      modifier = Modifier.weight(1f),
      color = scheme.onBackground,
      maxSize = 34.sp,
      minSize = 34.sp,
      maxLines = 1,
    )
    if (!archived) {
      ShapeButton(
        onClick = onNewPerson,
        shape = ExpressiveShapes.Cookie9Sided,
        size = 52.dp,
        containerColor = scheme.secondaryContainer,
        contentColor = scheme.onSecondaryContainer,
        contentDescription = stringResource(R.string.add_person),
        icon = Icons.Rounded.PersonAdd,
      )
    }
    // L'últim de la dreta, després dels botons de la capçalera
    if (syncIndicator != null) {
      Box(
        modifier = Modifier.size(52.dp).background(scheme.surfaceContainer, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        SyncIndicator(state = syncIndicator, onClick = onOpenSync)
      }
    }
  }
}

/** @param wrap panell estret d'una pantalla gran: els xips que no caben passen a la línia següent en lloc de fer scroll. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterRow(
  selected: Set<ChannelType>,
  onToggle: (ChannelType) -> Unit,
  wrap: Boolean,
) {
  val chips: @Composable () -> Unit = { FilterChips(selected = selected, onToggle = onToggle) }
  if (wrap) {
    FlowRow(
      modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      chips()
    }
  } else {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(bottom = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      chips()
    }
  }
}

@Composable
private fun FilterChips(
  selected: Set<ChannelType>,
  onToggle: (ChannelType) -> Unit,
) {
  val haptics = rememberConversesHaptics()
  // «Totes» és el filtre sense cap canal: tocar-la desmarca els canals que hi hagi
  ExpressiveFilterChip(
    selected = selected.isEmpty(),
    onClick = {
      haptics.segmentTick()
      selected.toList().forEach(onToggle)
    },
    label = stringResource(R.string.filter_all),
    icon = Icons.Rounded.Apps,
  )
  ChannelType.entries.forEach { group ->
    ExpressiveFilterChip(
      selected = group in selected,
      onClick = {
        haptics.segmentTick()
        onToggle(group)
      },
      label = stringResource(group.label()),
      icon = group.icon(),
    )
  }
}

/**
 * @param inPane fila del panell de llistat d'una pantalla gran: avatar de 48 dp i, a la semàntica, si és la
 *   [selected] (la persona oberta al detall), amb una vora de 2 dp.
 */
@Composable
private fun PersonRow(
  item: PersonListItem,
  onClick: () -> Unit,
  inPane: Boolean = false,
  selected: Boolean = false,
) {
  val person = item.person
  val palette = rememberPersonPalette(person.color)
  val scheme = MaterialTheme.colorScheme
  val shape = RoundedCornerShape(32.dp)
  val colors = CardDefaults.cardColors(containerColor = palette.surface, contentColor = scheme.onSurface)
  val border = if (selected) BorderStroke(2.dp, scheme.primary) else null
  val modifier = Modifier.fillMaxWidth()
  val content: @Composable () -> Unit = { PersonRowContent(item, palette.accent, inPane) }
  if (inPane) {
    Card(
      modifier = modifier.clip(shape).selectable(selected = selected, onClick = onClick),
      shape = shape,
      colors = colors,
      border = border,
    ) { content() }
  } else {
    Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, border = border) { content() }
  }
}

@Composable
private fun PersonRowContent(
  item: PersonListItem,
  accent: Color,
  inPane: Boolean,
) {
  val person = item.person
  val scheme = MaterialTheme.colorScheme
  // Amb la lletra molt gran l'hora no cap al costat del text i passa a sota (el text no es redueix a «M…»)
  val stacked = LocalDensity.current.fontScale >= 1.5f
  // Les protegides no mostren l'hora (ni tasques): no filtrem res abans de desbloquejar
  val entry = item.lastEntry
  val trailing: @Composable () -> Unit = {
    if (!person.isProtected && (entry != null || item.openTasks > 0)) {
      Column(
        horizontalAlignment = if (stacked) Alignment.Start else Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        if (entry != null) {
          Text(
            text = formatLastActivity(entry.occurredAt),
            style = ExpressiveType.display(20.sp),
            color = scheme.onSurface,
            maxLines = 1,
          )
        }
        if (item.openTasks > 0) TasksPill(item.openTasks)
      }
    }
  }
  Row(
    modifier = Modifier.fillMaxWidth().heightIn(min = 92.dp).padding(horizontal = 16.dp, vertical = 14.dp),
    horizontalArrangement = Arrangement.spacedBy(14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Avatar(
      initials = initialsOf(person.name),
      shape = person.shape,
      color = person.color,
      size = if (inPane) 48.dp else 56.dp,
      locked = person.isProtected,
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = person.name.uppercase(),
        style = ExpressiveType.overline,
        color = accent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (person.isProtected) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = stringResource(R.string.protected_label),
            style = ExpressiveType.body,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
          )
        }
      } else {
        Text(
          text = item.lastEntry?.text ?: stringResource(R.string.no_entries_yet),
          style = ExpressiveType.body,
          color = if (item.lastEntry == null) scheme.onSurfaceVariant else scheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (stacked) trailing()
    }
    if (!stacked) trailing()
  }
}

/** Pastilla `primaryContainer` amb `flag` i el nombre de tasques pendents (9g); mai a les protegides. */
@Composable
private fun TasksPill(count: Int) {
  val description = pluralStringResource(R.plurals.tasks_open_count, count, count)
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    modifier = Modifier.clearAndSetSemantics { contentDescription = description },
  ) {
    Row(
      modifier = Modifier.height(20.dp).padding(start = 4.dp, end = 7.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Icon(Icons.Rounded.Flag, contentDescription = null, modifier = Modifier.size(14.dp))
      Text(text = count.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
  }
}
