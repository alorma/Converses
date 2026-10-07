package com.alorma.contactnotes.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.alorma.compose.settings.ui.expressive.SettingsGroup
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.flags.PersonColorFeature
import com.alorma.contactnotes.data.flags.rememberFeatureEnabled
import com.alorma.contactnotes.data.settings.AppLockManager
import com.alorma.contactnotes.data.settings.AppSettings
import com.alorma.contactnotes.designsystem.components.ColorSwatches
import com.alorma.contactnotes.designsystem.components.ConversesToggleGroup
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.components.SettingRow
import com.alorma.contactnotes.designsystem.components.SettingSwitchRow
import com.alorma.contactnotes.designsystem.components.groupedItemShape
import com.alorma.contactnotes.designsystem.components.ToggleOption
import com.alorma.contactnotes.designsystem.theme.ScreenTitleStyle
import com.alorma.contactnotes.designsystem.theme.ThemeColor
import com.alorma.contactnotes.designsystem.theme.ThemeMode
import com.alorma.contactnotes.ui.people.colorNameRes
import com.alorma.contactnotes.ui.person.canUnlockWithDevice
import com.alorma.contactnotes.ui.person.openSecuritySetup
import com.alorma.contactnotes.ui.person.promptUnlock
import java.util.Locale
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private val RelockOptions = listOf(0, 60, 300)

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

  SettingsContent(
    settings = settings,
    showPersonColor = rememberFeatureEnabled<PersonColorFeature>(),
    canUnlock = canUnlock,
    languageName = remember { currentLanguageName(context) },
    versionName = remember { versionName(context) },
    onBack = onBack,
    onThemeMode = viewModel::onThemeMode,
    onDynamicColor = viewModel::onDynamicColor,
    onThemeColor = viewModel::onThemeColor,
    onPersonColorTheme = viewModel::onPersonColorTheme,
    onAppLockChange = { enabled ->
      activity.promptUnlock(authTitle, authSubtitle) {
        viewModel.onAppLock(enabled)
        if (enabled) appLock.unlock()
      }
    },
    onRelockAfter = viewModel::onRelockAfter,
    onOpenLanguage = { context.openLanguageSettings() },
    onOpenSecuritySetup = { context.openSecuritySetup() },
    onOpenPrivacyPolicy = { context.openPrivacyPolicy() },
  )
}

@Composable
fun SettingsContent(
  settings: AppSettings,
  canUnlock: Boolean,
  languageName: String,
  versionName: String,
  onBack: () -> Unit,
  onThemeMode: (ThemeMode) -> Unit,
  onDynamicColor: (Boolean) -> Unit,
  onThemeColor: (ThemeColor) -> Unit,
  onPersonColorTheme: (Boolean) -> Unit,
  onAppLockChange: (Boolean) -> Unit,
  onRelockAfter: (Int) -> Unit,
  onOpenLanguage: () -> Unit,
  onOpenSecuritySetup: () -> Unit,
  onOpenPrivacyPolicy: () -> Unit,
  showPersonColor: Boolean = true,
) {
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
        val themeCount = (if (settings.dynamicColor) 3 else 4) - (if (showPersonColor) 0 else 1)
        ThemeModeRow(
          selected = settings.themeMode,
          onSelected = onThemeMode,
          shape = groupedItemShape(0, themeCount),
        )
        SettingSwitchRow(
          title = stringResource(R.string.settings_dynamic_color),
          subtitle = stringResource(R.string.settings_dynamic_color_summary),
          icon = Icons.Rounded.Palette,
          checked = settings.dynamicColor,
          onCheckedChange = onDynamicColor,
          shape = groupedItemShape(1, themeCount),
        )
        if (!settings.dynamicColor) {
          ThemeColorPicker(
            selected = settings.themeColor,
            onSelected = onThemeColor,
            shape = groupedItemShape(2, themeCount),
          )
        }
        if (showPersonColor) SettingSwitchRow(
          title = stringResource(R.string.settings_person_color),
          subtitle = stringResource(R.string.settings_person_color_summary),
          icon = Icons.Rounded.Face,
          checked = settings.personColorTheme,
          onCheckedChange = onPersonColorTheme,
          shape = groupedItemShape(themeCount - 1, themeCount),
        )
      }

      SettingsGroup(
        title = { SectionTitle(stringResource(R.string.settings_language)) },
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
      ) {
        SettingRow(
          title = stringResource(R.string.settings_app_language),
          subtitle = languageName,
          icon = Icons.Rounded.Language,
          trailing = { Icon(Icons.Rounded.OpenInNew, contentDescription = null) },
          shape = groupedItemShape(0, 1),
          onClick = onOpenLanguage,
        )
      }

      SettingsGroup(
        title = { SectionTitle(stringResource(R.string.settings_security)) },
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
      ) {
        if (!canUnlock) {
          SettingRow(
            title = stringResource(R.string.settings_security_setup),
            subtitle = stringResource(R.string.settings_security_setup_summary),
            icon = Icons.Rounded.Fingerprint,
            trailing = { Icon(Icons.Rounded.OpenInNew, contentDescription = null) },
            shape = groupedItemShape(0, 1),
            onClick = { onOpenSecuritySetup() },
          )
        } else {
          val lockCount = if (settings.appLock) 2 else 1
          SettingSwitchRow(
            title = stringResource(R.string.settings_app_lock),
            subtitle = stringResource(R.string.settings_app_lock_summary),
            icon = Icons.Rounded.Fingerprint,
            checked = settings.appLock,
            shape = groupedItemShape(0, lockCount),
            onCheckedChange = onAppLockChange,
          )
          if (settings.appLock) {
            RelockRow(
              seconds = settings.relockAfterSeconds,
              onSelected = onRelockAfter,
              shape = groupedItemShape(1, lockCount),
            )
          }
        }
      }
      Text(
        text = stringResource(R.string.settings_protected_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
      )

      SettingsGroup(
        title = { SectionTitle(stringResource(R.string.settings_about)) },
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
      ) {
        SettingRow(
          title = stringResource(R.string.settings_privacy_policy),
          icon = Icons.Rounded.PrivacyTip,
          trailing = { Icon(Icons.Rounded.OpenInNew, contentDescription = null) },
          shape = groupedItemShape(0, 1),
          onClick = onOpenPrivacyPolicy,
        )
      }
      Text(
        text = stringResource(R.string.settings_footer, versionName),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 32.dp),
      )
    }
  }
}

@Composable
private fun RelockRow(
  seconds: Int,
  onSelected: (Int) -> Unit,
  shape: androidx.compose.ui.graphics.Shape,
) {
  var expanded by remember { mutableStateOf(false) }
  Box {
    SettingRow(
      title = stringResource(R.string.settings_relock),
      subtitle = stringResource(relockSummary(seconds)),
      icon = Icons.Rounded.Timer,
      trailing = { Icon(Icons.Rounded.UnfoldMore, contentDescription = null) },
      shape = shape,
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
private fun ThemeModeRow(
  selected: ThemeMode,
  onSelected: (ThemeMode) -> Unit,
  shape: androidx.compose.ui.graphics.Shape,
) {
  Surface(shape = shape, color = GroupedListDefaults.containerColor, modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      TileTitle(stringResource(R.string.settings_theme))
      ConversesToggleGroup(
        options = listOf(
          ToggleOption(ThemeMode.Light, stringResource(R.string.settings_theme_light), Icons.Rounded.LightMode),
          ToggleOption(ThemeMode.Dark, stringResource(R.string.settings_theme_dark), Icons.Rounded.DarkMode),
          ToggleOption(ThemeMode.System, stringResource(R.string.settings_theme_system), Icons.Rounded.BrightnessAuto),
        ),
        selected = selected,
        onSelect = onSelected,
      )
    }
  }
}

@Composable
private fun ThemeColorPicker(
  selected: ThemeColor,
  onSelected: (ThemeColor) -> Unit,
  shape: androidx.compose.ui.graphics.Shape,
) {
  Surface(shape = shape, color = GroupedListDefaults.containerColor, modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(stringResource(R.string.settings_theme_color), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
      Text(
        text = stringResource(R.string.settings_theme_color_summary),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      ColorSwatches(
        entries = ThemeColor.entries,
        selected = selected,
        colorOf = { it.swatch },
        onSelect = onSelected,
        labelOf = { stringResource(colorNameRes(it.name)) },
        modifier = Modifier.padding(top = 12.dp),
      )
    }
  }
}

private val SupportedLanguages = setOf("ca", "es", "en")

/** Idioma efectiu de l'app; si el sistema en té un de no suportat, l'app cau a l'anglès per defecte. */
/** Idioma per app des d'Android 13; abans, l'idioma del sistema, que l'app ja segueix (en, ca i es). */
private fun Context.openLanguageSettings() {
  val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    Intent(Settings.ACTION_APP_LOCALE_SETTINGS).setData(Uri.fromParts("package", packageName, null))
  } else {
    Intent(Settings.ACTION_LOCALE_SETTINGS)
  }
  startActivity(intent)
}

private const val PrivacyPolicyUrl = "https://alorma.github.io/converses/privacy/"

private fun Context.openPrivacyPolicy() {
  val language = resources.configuration.locales[0].language.takeIf { it in SupportedLanguages } ?: "en"
  startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("$PrivacyPolicyUrl#$language")))
}

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
