package com.alorma.contactnotes.ui.sync

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.sync.DeviceInfo
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.components.ProBadge
import com.alorma.contactnotes.designsystem.components.SettingRow
import com.alorma.contactnotes.designsystem.components.groupedItemShape

/** El nom d'un dispositiu per mostrar-lo: el que té o, si no s'ha pogut llegir, «Dispositiu». */
@Composable
internal fun DeviceInfo.displayName(): String = name ?: stringResource(R.string.sync_device_unnamed)

/** «Fa 3 hores», o [justNow] si fa menys d'un minut; `null` si el dispositiu encara no ha sincronitzat. */
internal fun DeviceInfo.lastSyncLabel(
  now: Long,
  justNow: String,
): String? =
  lastSyncAt?.let {
    if (now - it < DateUtils.MINUTE_IN_MILLIS) {
      justNow
    } else {
      DateUtils.getRelativeTimeSpanString(it, now, DateUtils.MINUTE_IN_MILLIS).toString()
    }
  }

/**
 * La secció «Dispositius» de 13o: capçalera «N de M», una fila per dispositiu (aquest primer) i, amb el límit gratuït,
 * la fila de Pro. [onRename] i [onRemove] reben l'id; treure demana confirmació aquí mateix (13q).
 */
@Composable
fun DevicesSection(
  devices: List<DeviceInfo>,
  limit: Int,
  now: Long,
  onRename: (DeviceInfo) -> Unit,
  onRemove: (DeviceInfo) -> Unit,
  onOpenPro: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.sync_devices),
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = stringResource(R.string.sync_slots, devices.size, limit),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Column(verticalArrangement = Arrangement.spacedBy(GroupedListDefaults.ItemSpacing)) {
      devices.forEachIndexed { index, device ->
        DeviceRow(
          device = device,
          now = now,
          shape = groupedItemShape(index, devices.size),
          onRename = { onRename(device) },
          onRemove = { onRemove(device) },
        )
      }
    }
    if (limit <= FREE_DEVICE_SLOTS) ProDevicesRow(onClick = onOpenPro)
  }
}

@Composable
private fun DeviceRow(
  device: DeviceInfo,
  now: Long,
  shape: Shape,
  onRename: () -> Unit,
  onRemove: () -> Unit,
) {
  val last = device.lastSyncLabel(now, stringResource(R.string.sync_just_now))
  // «Aquest dispositiu» va a sota, amb la data: al costat del nom l'esclafa en pantalles estretes
  val thisDevice = if (device.isThis) stringResource(R.string.sync_this_dev) else null
  SettingRow(
    title = device.displayName(),
    subtitle = listOfNotNull(thisDevice, last).joinToString(" · ").ifEmpty { null },
    icon = Icons.Rounded.PhoneAndroid,
    shape = shape,
    trailing = { DeviceMenu(canRemove = !device.isThis, onRename = onRename, onRemove = onRemove) },
  )
}

@Composable
private fun DeviceMenu(
  canRemove: Boolean,
  onRename: () -> Unit,
  onRemove: () -> Unit,
) {
  var open by remember { mutableStateOf(false) }
  Box {
    IconButton(onClick = { open = true }) {
      Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more_options))
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
      DropdownMenuItem(
        text = { Text(stringResource(R.string.sync_rename)) },
        onClick = {
          open = false
          onRename()
        },
      )
      if (canRemove) {
        DropdownMenuItem(
          text = { Text(stringResource(R.string.sync_remove)) },
          onClick = {
            open = false
            onRemove()
          },
        )
      }
    }
  }
}

/** La fila discontínua «Fins a 5 dispositius amb Pro»: porta al paywall amb l'origen `sync`. */
@Composable
fun ProDevicesRow(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(GroupedListDefaults.OuterCorner)
  val outline = MaterialTheme.colorScheme.outline
  Surface(
    onClick = onClick,
    shape = shape,
    color = Color.Transparent,
    modifier =
      modifier.fillMaxWidth().drawBehind {
        val stroke = 2.dp.toPx()
        drawRoundRect(
          color = outline,
          topLeft = Offset(stroke / 2, stroke / 2),
          size = Size(size.width - stroke, size.height - stroke),
          cornerRadius = CornerRadius(GroupedListDefaults.OuterCorner.toPx()),
          style =
            Stroke(
              width = stroke,
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx())),
            ),
        )
      },
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Icon(Icons.Rounded.Devices, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(
        text = stringResource(R.string.sync_pro5),
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
      )
      ProBadge(text = stringResource(R.string.pro_badge), contentDescription = stringResource(R.string.pro_description))
      Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
    }
  }
}

/** «Canvia el nom»: el camp ve amb el nom actual i «Desa» no s'activa buit. */
@Composable
fun RenameDeviceDialog(
  currentName: String,
  onDismiss: () -> Unit,
  onSave: (String) -> Unit,
) {
  var text by rememberSaveable { mutableStateOf(currentName) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.sync_rename_t)) },
    text = {
      OutlinedTextField(
        value = text,
        onValueChange = { text = it.take(MAX_DEVICE_NAME) },
        label = { Text(stringResource(R.string.sync_rename_field)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
      )
    },
    confirmButton = {
      TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) { Text(stringResource(R.string.save)) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.sync_cancel)) } },
  )
}

/** «Treure el Galaxy Tab S9?» (13q): el botó principal en `pri`. */
@Composable
fun RemoveDeviceDialog(
  deviceName: String,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.sync_remove_t, deviceName)) },
    text = { Text(stringResource(R.string.sync_remove_b)) },
    confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.sync_remove)) } },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.sync_cancel)) } },
  )
}
