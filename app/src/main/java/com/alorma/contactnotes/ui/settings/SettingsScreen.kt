package com.alorma.contactnotes.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.compose.settings.ui.core.LocalSettingsTileColors
import com.alorma.compose.settings.ui.expressive.SettingsButtonGroup
import com.alorma.compose.settings.ui.expressive.SettingsGroup
import com.alorma.compose.settings.ui.expressive.SettingsMenuLink
import com.alorma.compose.settings.ui.expressive.SettingsSwitch
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.settings.AppLockManager
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.theme.ScreenTitleStyle
import com.alorma.contactnotes.designsystem.theme.ThemeColor
import com.alorma.contactnotes.designsystem.theme.ThemeMode
import com.alorma.contactnotes.ui.person.canUnlockWithDevice
import com.alorma.contactnotes.ui.person.promptUnlock
import java.util.Locale
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private val RelockOptions = listOf(0, 60, 300)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
  onBack: () -> Unit,
  viewModel: SettingsViewModel = koinViewModel(),
  appLock: AppLockManager = koinInject(),
) {
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val activity = context as FragmentActivity
  var canUnlock by remember { mutableStateOf(activity.canUnlockWithDevice()) }
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { canUnlock = activity.canUnlockWithDevice() }
  val authTitle = stringResource(R.string.app_lock_prompt_title)
  val authSubtitle = stringResource(R.string.unlock_subtitle)

  val tileColors = ListItemDefaults.colors(containerColor = GroupedListDefaults.containerColor)
  CompositionLocalProvider(LocalSettingsTileColors provides tileColors) {
  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .windowInsetsPadding(WindowInsets.statusBars)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
      }
      Text(
        text = stringResource(R.string.settings_title),
        style = ScreenTitleStyle,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
      )

      SettingsGroup(
        title = { SectionTitle(stringResource(R.string.settings_appearance)) },
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
      ) {
        val themeCount = if (settings.dynamicColor) 2 else 3
        SettingsButtonGroup(
          title = { TileTitle(stringResource(R.string.settings_theme)) },
          items = listOf(ThemeMode.Light, ThemeMode.Dark, ThemeMode.System),
          selectedItem = settings.themeMode,
          itemTitleMap = { mode ->
            context.getString(
              when (mode) {
                ThemeMode.Light -> R.string.settings_theme_light
                ThemeMode.Dark -> R.string.settings_theme_dark
                ThemeMode.System -> R.string.settings_theme_system
              },
            )
          },
          onItemSelected = viewModel::onThemeMode,
          shapes = ListItemDefaults.segmentedShapes(index = 0, count = themeCount),
        )
        SettingsSwitch(
          state = settings.dynamicColor,
          title = { TileTitle(stringResource(R.string.settings_dynamic_color)) },
          subtitle = { Text(stringResource(R.string.settings_dynamic_color_summary)) },
          icon = { Icon(Icons.Rounded.Palette, contentDescription = null) },
          shapes = ListItemDefaults.segmentedShapes(index = 1, count = themeCount),
          onCheckedChange = viewModel::onDynamicColor,
        )
        if (!settings.dynamicColor) {
          ThemeColorPicker(
            selected = settings.themeColor,
            onSelected = viewModel::onThemeColor,
            shape = ListItemDefaults.segmentedShapes(index = 2, count = themeCount).shape,
          )
        }
      }

      SettingsGroup(
        title = { SectionTitle(stringResource(R.string.settings_language)) },
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
      ) {
        SettingsMenuLink(
          title = { TileTitle(stringResource(R.string.settings_app_language)) },
          subtitle = { Text(remember { currentLanguageName(context) }) },
          icon = { Icon(Icons.Rounded.Language, contentDescription = null) },
          action = { Icon(Icons.Rounded.OpenInNew, contentDescription = null) },
          shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
          onClick = {
            context.startActivity(
              Intent(Settings.ACTION_APP_LOCALE_SETTINGS).setData(Uri.fromParts("package", context.packageName, null)),
            )
          },
        )
      }

      SettingsGroup(
        title = { SectionTitle(stringResource(R.string.settings_security)) },
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
      ) {
        if (!canUnlock) {
          SettingsMenuLink(
            title = { TileTitle(stringResource(R.string.settings_security_setup)) },
            subtitle = { Text(stringResource(R.string.settings_security_setup_summary)) },
            icon = { Icon(Icons.Rounded.Fingerprint, contentDescription = null) },
            action = { Icon(Icons.Rounded.OpenInNew, contentDescription = null) },
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
            onClick = { openSecuritySetup(context) },
          )
        } else {
          val lockCount = if (settings.appLock) 2 else 1
          SettingsSwitch(
            state = settings.appLock,
            title = { TileTitle(stringResource(R.string.settings_app_lock)) },
            subtitle = { Text(stringResource(R.string.settings_app_lock_summary)) },
            icon = { Icon(Icons.Rounded.Fingerprint, contentDescription = null) },
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = lockCount),
            onCheckedChange = { enabled ->
              activity.promptUnlock(authTitle, authSubtitle) {
                viewModel.onAppLock(enabled)
                if (enabled) appLock.unlock()
              }
            },
          )
          if (settings.appLock) {
            RelockRow(
              seconds = settings.relockAfterSeconds,
              onSelected = viewModel::onRelockAfter,
              shapes = ListItemDefaults.segmentedShapes(index = 1, count = lockCount),
            )
          }
        }
      }
      Text(
        text = stringResource(R.string.settings_protected_note),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
      )
      Text(
        text = stringResource(R.string.settings_footer, versionName(context)),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 32.dp),
      )
    }
  }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RelockRow(
  seconds: Int,
  onSelected: (Int) -> Unit,
  shapes: androidx.compose.material3.ListItemShapes,
) {
  var expanded by remember { mutableStateOf(false) }
  Box {
    SettingsMenuLink(
      title = { TileTitle(stringResource(R.string.settings_relock)) },
      subtitle = { Text(stringResource(relockSummary(seconds))) },
      icon = { Icon(Icons.Rounded.Timer, contentDescription = null) },
      action = { Icon(Icons.Rounded.UnfoldMore, contentDescription = null) },
      shapes = shapes,
      onClick = { expanded = true },
    )
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      RelockOptions.forEach { option ->
        DropdownMenuItem(
          text = { Text(stringResource(relockLabel(option))) },
          onClick = {
            onSelected(option)
            expanded = false
          },
        )
      }
    }
  }
}

private fun relockLabel(seconds: Int): Int = when (seconds) {
  0 -> R.string.relock_immediately
  60 -> R.string.relock_1_min
  else -> R.string.relock_5_min
}

private fun relockSummary(seconds: Int): Int = when (seconds) {
  0 -> R.string.relock_summary_immediately
  60 -> R.string.relock_summary_1_min
  else -> R.string.relock_summary_5_min
}

@Composable
private fun ThemeColorPicker(
  selected: ThemeColor,
  onSelected: (ThemeColor) -> Unit,
  shape: androidx.compose.ui.graphics.Shape,
) {
  Surface(shape = shape, color = ListItemDefaults.containerColor, modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(stringResource(R.string.settings_theme_color), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
      Text(
        text = stringResource(R.string.settings_theme_color_summary),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).selectableGroup(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        ThemeColor.entries.forEach { color ->
          val isSelected = color == selected
          Box(
            modifier = Modifier
              .size(44.dp)
              .then(
                if (isSelected) Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), CircleShape) else Modifier,
              )
              .padding(5.dp)
              .clip(CircleShape)
              .background(color.swatch)
              .clickable(role = Role.RadioButton) { onSelected(color) },
            contentAlignment = Alignment.Center,
          ) {
            if (isSelected) {
              Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
            }
          }
        }
      }
    }
  }
}

private fun openSecuritySetup(context: Context) {
  val enroll = Intent(Settings.ACTION_BIOMETRIC_ENROLL).putExtra(
    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
  )
  try {
    context.startActivity(enroll)
  } catch (_: ActivityNotFoundException) {
    context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
  }
}

private val SupportedLanguages = setOf("ca", "es", "en")

/** Idioma efectiu de l'app; si el sistema en té un de no suportat, l'app cau a l'anglès per defecte. */
private fun currentLanguageName(context: Context): String {
  val configured = context.resources.configuration.locales[0]
  val locale = if (configured.language in SupportedLanguages) configured else Locale.ENGLISH
  return locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) }
}

private fun versionName(context: Context): String =
  context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()

@Composable
private fun SectionTitle(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.titleSmall,
    fontWeight = FontWeight.Bold,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

@Composable
private fun TileTitle(text: String) {
  Text(text = text, fontWeight = FontWeight.SemiBold)
}
