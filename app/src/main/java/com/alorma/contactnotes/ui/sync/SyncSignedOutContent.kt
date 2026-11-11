package com.alorma.contactnotes.ui.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.components.asShape
import com.alorma.contactnotes.designsystem.components.groupedItemShape
import com.alorma.contactnotes.designsystem.components.readableWidth
import com.alorma.contactnotes.shared.AvatarShape

/**
 * Sense sessió (13d). El contingut fa scroll i el peu (el botó de Google i la nota) es queda fix. El botó és un botó
 * normal d'M3: el de marca de Google, amb els seus recursos, no s'ha afegit.
 */
@Composable
fun SyncSignedOutContent(
  padding: PaddingValues,
  onSignIn: () -> Unit,
) {
  Column(Modifier.fillMaxSize().padding(padding)) {
    Column(
      modifier =
        Modifier
          .weight(1f)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .readableWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
        Box(
          modifier =
            Modifier
              .size(72.dp)
              .clip(AvatarShape.Cookie9Sided.asShape())
              .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            Icons.Rounded.CloudSync,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(36.dp),
          )
        }
        Text(
          text = stringResource(R.string.sync_off_title),
          modifier = Modifier.padding(top = 6.dp),
          style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp, lineHeight = 30.sp),
          fontWeight = FontWeight.ExtraBold,
        )
        Text(
          text = stringResource(R.string.sync_off_body),
          style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Column(verticalArrangement = Arrangement.spacedBy(GroupedListDefaults.ItemSpacing)) {
        val points =
          listOf(
            Triple(Icons.Rounded.Lock, R.string.sync_point_encrypted_title, R.string.sync_point_encrypted_body),
            Triple(Icons.Rounded.Key, R.string.sync_point_key_title, R.string.sync_point_key_body),
            Triple(Icons.Rounded.Devices, R.string.sync_point_devices_title, R.string.sync_point_devices_body),
          )
        points.forEachIndexed { index, (icon, title, body) ->
          PointRow(
            icon = icon,
            title = stringResource(title),
            body = stringResource(body),
            index = index,
            count = points.size,
          )
        }
      }
    }
    Column(
      modifier =
        Modifier.fillMaxWidth().readableWidth().padding(
          start = 16.dp,
          end = 16.dp,
          top = 12.dp,
          bottom = 16.dp,
        ),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Text(stringResource(R.string.sync_google), fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }
      Text(
        text = stringResource(R.string.sync_account_note),
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp),
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun PointRow(
  icon: ImageVector,
  title: String,
  body: String,
  index: Int,
  count: Int,
) {
  Surface(shape = groupedItemShape(index, count), color = GroupedListDefaults.containerColor) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .defaultMinSize(
            minHeight = 76.dp,
          ).padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
      ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Text(
          text = body,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
