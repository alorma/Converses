package com.alorma.contactnotes.ui.sync

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.sync.DeviceInfo
import com.alorma.contactnotes.data.sync.SlotsPrompt
import com.alorma.contactnotes.designsystem.components.ConversesTopBar
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.components.groupedItemShape
import com.alorma.contactnotes.designsystem.components.readableWidth
import com.alorma.contactnotes.designsystem.components.rememberTopBarScrollBehavior
import org.koin.compose.viewmodel.koinViewModel

/**
 * Places plenes (13l). Es mostra sola quan el repositori té una pregunta de places i es tanca quan desapareix. Enrere i
 * «Ara no» són el mateix: no continuar. Mai mostra dades de persones, només els dispositius del compte.
 */
@Composable
fun SlotsScreen(
  onOpenPro: () -> Unit,
  viewModel: SlotsViewModel = koinViewModel(),
) {
  val prompt by viewModel.prompt.collectAsStateWithLifecycle()
  val working by viewModel.working.collectAsStateWithLifecycle()
  // L'id sobreviu a girar la pantalla; si el dispositiu ja no hi és, la selecció es descarta sola
  var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
  val snackbar = remember { SnackbarHostState() }
  val resources = LocalResources.current
  LaunchedEffect(viewModel) {
    viewModel.messages.collect { message ->
      val text =
        when (message) {
          SyncMessage.Offline, SyncMessage.SignInOffline -> R.string.sync_error_offline
          SyncMessage.Failed, SyncMessage.SignInFailed -> R.string.sync_error_failed
        }
      snackbar.showSnackbar(resources.getString(text))
    }
  }
  BackHandler(onBack = viewModel::onNotNow)
  val current = prompt ?: return
  SlotsContent(
    prompt = current,
    selectedId = selectedId?.takeIf { id -> current.devices.any { it.id == id } },
    working = working,
    snackbar = snackbar,
    onSelect = { selectedId = it },
    onRemoveAndContinue = { selectedId?.let(viewModel::onRemoveAndContinue) },
    onNotNow = viewModel::onNotNow,
    onOpenPro = onOpenPro,
  )
}

/** Sense res triat per defecte: «Treu-lo i continua» s'activa en triar un dispositiu. [now] es fixa a les previews. */
@Composable
fun SlotsContent(
  prompt: SlotsPrompt,
  selectedId: String?,
  working: Boolean,
  snackbar: SnackbarHostState,
  onSelect: (String) -> Unit,
  onRemoveAndContinue: () -> Unit,
  onNotNow: () -> Unit,
  onOpenPro: () -> Unit,
  now: Long = remember { System.currentTimeMillis() },
) {
  val scrollBehavior = rememberTopBarScrollBehavior()
  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    containerColor = MaterialTheme.colorScheme.surface,
    topBar = {
      ConversesTopBar(
        title = stringResource(R.string.sync_full_t),
        scrollBehavior = scrollBehavior,
        navigationIcon = {
          IconButton(onClick = onNotNow) {
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
          text = stringResource(R.string.sync_full_b, prompt.devices.size, prompt.limit),
          style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 4.dp),
        )
        Column(
          modifier = Modifier.selectableGroup(),
          verticalArrangement = Arrangement.spacedBy(GroupedListDefaults.ItemSpacing),
        ) {
          prompt.devices.forEachIndexed { index, device ->
            SlotRow(
              device = device,
              now = now,
              selected = device.id == selectedId,
              enabled = !working,
              shape = groupedItemShape(index, prompt.devices.size),
              onSelect = { onSelect(device.id) },
            )
          }
        }
        if (prompt.canUpgrade) ProDevicesRow(onClick = onOpenPro)
      }
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .readableWidth()
            .padding(PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Button(
          onClick = onRemoveAndContinue,
          enabled = selectedId != null && !working,
          modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
          Text(stringResource(R.string.sync_remove_go), fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onNotNow, enabled = !working, modifier = Modifier.fillMaxWidth().height(48.dp)) {
          Text(stringResource(R.string.sync_not_now), fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun SlotRow(
  device: DeviceInfo,
  now: Long,
  selected: Boolean,
  enabled: Boolean,
  shape: Shape,
  onSelect: () -> Unit,
) {
  Surface(
    shape = shape,
    color = GroupedListDefaults.containerColor,
    modifier =
      Modifier
        .fillMaxWidth()
        .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect),
  ) {
    ListItem(
      headlineContent = { Text(device.displayName(), fontWeight = FontWeight.SemiBold) },
      supportingContent =
        device.lastSyncLabel(now)?.let { last -> { Text(stringResource(R.string.sync_last_sync, last)) } },
      leadingContent = { Icon(Icons.Rounded.PhoneAndroid, contentDescription = null) },
      trailingContent = { RadioButton(selected = selected, onClick = null, enabled = enabled) },
      colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
  }
}
