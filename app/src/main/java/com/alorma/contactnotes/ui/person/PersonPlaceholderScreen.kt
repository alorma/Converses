package com.alorma.contactnotes.ui.person

import androidx.activity.compose.LocalActivity
import android.view.WindowManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.contactnotes.R
import com.alorma.contactnotes.designsystem.theme.PersonNameStyle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Detall provisional fins al pas 4; ja aplica `FLAG_SECURE` a les persones protegides. */
@Composable
fun PersonPlaceholderScreen(
  personId: Long,
  onBack: () -> Unit,
  viewModel: PersonViewModel = koinViewModel(parameters = { parametersOf(personId) }),
) {
  val person by viewModel.person.collectAsStateWithLifecycle()
  val window = LocalActivity.current?.window
  val secure = person?.isProtected == true
  DisposableEffect(secure, window) {
    if (secure) window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
  }

  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
    Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
      IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
      }
      person?.let { p ->
        Text(text = p.name, style = PersonNameStyle, modifier = Modifier.padding(horizontal = 24.dp))
        Text(
          text = stringResource(R.string.detail_pending),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(24.dp),
        )
      }
    }
  }
}
