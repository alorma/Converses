package com.alorma.contactnotes.ui.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.compose.settings.ui.expressive.SettingsGroup
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.contacts.ContactsPermission
import com.alorma.contactnotes.data.flags.ContactLinkFeature
import com.alorma.contactnotes.data.flags.FeatureState
import com.alorma.contactnotes.data.flags.PersonColorFeature
import com.alorma.contactnotes.data.flags.rememberFeatureEnabled
import com.alorma.contactnotes.data.flags.rememberFeatureState
import com.alorma.contactnotes.data.settings.AppLockManager
import com.alorma.contactnotes.data.settings.AppSettings
import com.alorma.contactnotes.data.wear.WearLink
import com.alorma.contactnotes.designsystem.components.ConversesToggleGroup
import com.alorma.contactnotes.designsystem.components.ConversesTopBar
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.components.ProBadge
import com.alorma.contactnotes.designsystem.components.SettingRow
import com.alorma.contactnotes.designsystem.components.SettingSwitchRow
import com.alorma.contactnotes.designsystem.components.ToggleOption
import com.alorma.contactnotes.designsystem.components.groupedItemShape
import com.alorma.contactnotes.designsystem.components.rememberConversesHaptics
import com.alorma.contactnotes.designsystem.components.rememberTopBarScrollBehavior
import com.alorma.contactnotes.designsystem.theme.ThemeMode
import com.alorma.contactnotes.ui.edit.ContactsExplainSheet
import com.alorma.contactnotes.ui.edit.openAppDetailsSettings
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

  val feedbackSubject = stringResource(R.string.settings_feedback_subject)
  val feedbackBody = stringResource(R.string.settings_feedback_body, remember { versionLabel(context) })
  val haptics = rememberConversesHaptics()

  // Les aprovacions del rellotge arriben com a notificació només amb l'app en segon pla: el permís es gestiona aquí
  val wearLink: WearLink = koinInject()
  val watchPresent by wearLink.watchPresent.collectAsStateWithLifecycle(false)
  var watchNotifications by remember { mutableStateOf(context.notificationsEnabled()) }
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { watchNotifications = context.notificationsEnabled() }
  val requestNotifications =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      watchNotifications = context.notificationsEnabled()
      // Denegat per sempre: el sistema ja no mostra el diàleg, només es pot canviar als ajustos de l'app
      if (!granted && !activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
        context.openNotificationSettings()
      }
    }

  val contactsState by viewModel.contacts.collectAsStateWithLifecycle()
  val requestContacts =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
      viewModel.onContactsPermissionChanged()
    }
  // El permís es pot haver donat o retirat des dels ajustos de l'app
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onContactsPermissionChanged() }

  SettingsContent(
    settings = settings,
    contacts =
      when (rememberFeatureState<ContactLinkFeature>()) {
        FeatureState.Hidden -> null
        FeatureState.Locked -> ContactsSettingsState.Locked
        FeatureState.Unlocked -> contactsState
      },
    onContactsExplainContinue = {
      viewModel.onContactsExplained()
      requestContacts.launch(ContactsPermission.permissions)
    },
    onContactsAllow = {
      // Denegat per sempre: el sistema ja no mostra el diàleg, només es pot donar als ajustos de l'app
      val canAsk = ContactsPermission.permissions.any { activity.shouldShowRequestPermissionRationale(it) }
      if (canAsk) requestContacts.launch(ContactsPermission.permissions) else context.openAppDetailsSettings()
    },
    showWatchNotifications = watchPresent,
    watchNotifications = watchNotifications,
    onWatchNotifications = { enabled ->
      if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
      } else {
        // Per apagar-les (o abans d'Android 13) l'única via és el sistema
        context.openNotificationSettings()
      }
    },
    showPersonColor = rememberFeatureEnabled<PersonColorFeature>(),
    canUnlock = canUnlock,
    languageName = remember { currentLanguageName(context) },
    versionName = remember { versionLabel(context) },
    onBack = onBack,
    onThemeMode = viewModel::onThemeMode,
    onDynamicColor = viewModel::onDynamicColor,
    onPersonColorTheme = viewModel::onPersonColorTheme,
    onHapticFeedback = viewModel::onHapticFeedback,
    onAppLockChange = { enabled ->
      activity.promptUnlock(authTitle, authSubtitle) {
        viewModel.onAppLock(enabled)
        haptics.toggle(enabled)
        if (enabled) appLock.unlock()
      }
    },
    onRelockAfter = viewModel::onRelockAfter,
    onOpenLanguage = { context.openLanguageSettings() },
    onOpenSecuritySetup = { context.openSecuritySetup() },
    onOpenPrivacyPolicy = { context.openPrivacyPolicy() },
    onOpenFeedback = { context.openFeedback(feedbackSubject, feedbackBody) },
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
  onPersonColorTheme: (Boolean) -> Unit,
  onHapticFeedback: (Boolean) -> Unit,
  onAppLockChange: (Boolean) -> Unit,
  onRelockAfter: (Int) -> Unit,
  onOpenLanguage: () -> Unit,
  onOpenSecuritySetup: () -> Unit,
  onOpenPrivacyPolicy: () -> Unit,
  onOpenFeedback: () -> Unit,
  showPersonColor: Boolean = true,
  showWatchNotifications: Boolean = false,
  watchNotifications: Boolean = false,
  onWatchNotifications: (Boolean) -> Unit = {},
  contacts: ContactsSettingsState? = null,
  onContactsExplainContinue: () -> Unit = {},
  onContactsAllow: () -> Unit = {},
) {
  var showContactsExplain by rememberSaveable { mutableStateOf(false) }
  val scrollBehavior = rememberTopBarScrollBehavior()
  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    containerColor = MaterialTheme.colorScheme.surface,
    contentWindowInsets = WindowInsets.navigationBars,
    topBar = {
      ConversesTopBar(
        title = stringResource(R.string.settings_title),
        scrollBehavior = scrollBehavior,
        large = true,
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
          }
        },
      )
    },
  ) { padding ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(padding)
          .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      SettingsGroup(
        title = { SectionTitle(stringResource(R.string.settings_appearance)) },
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
      ) {
        val themeCount = if (showPersonColor) 3 else 2
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
        if (showPersonColor) {
          SettingSwitchRow(
            title = stringResource(R.string.settings_person_color),
            subtitle = stringResource(R.string.settings_person_color_summary),
            icon = Icons.Rounded.Face,
            checked = settings.personColorTheme,
            onCheckedChange = onPersonColorTheme,
            shape = groupedItemShape(2, themeCount),
          )
        }
      }

      SettingsGroup(
        title = { SectionTitle(stringResource(R.string.settings_interaction)) },
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
      ) {
        SettingSwitchRow(
          title = stringResource(R.string.settings_haptic),
          subtitle = stringResource(R.string.settings_haptic_summary),
          icon = Icons.Rounded.Vibration,
          checked = settings.hapticFeedback,
          onCheckedChange = onHapticFeedback,
          shape = groupedItemShape(0, if (showWatchNotifications) 2 else 1),
        )
        if (showWatchNotifications) {
          SettingSwitchRow(
            title = stringResource(R.string.settings_watch_notifications),
            subtitle = stringResource(R.string.settings_watch_notifications_summary),
            icon = Icons.Rounded.Notifications,
            checked = watchNotifications,
            onCheckedChange = onWatchNotifications,
            shape = groupedItemShape(1, 2),
          )
        }
      }

      SettingsGroup(
        title = { SectionTitle(stringResource(R.string.settings_language)) },
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
      ) {
        SettingRow(
          title = stringResource(R.string.settings_app_language),
          subtitle = languageName,
          icon = Icons.Rounded.Language,
          trailing = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null) },
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
            trailing = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null) },
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
            haptic = false,
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

      if (contacts != null) {
        ContactsSection(
          state = contacts,
          onTurnOn = { showContactsExplain = true },
          onAllow = onContactsAllow,
        )
      }

      val aboutCount = 3
      SettingsGroup(
        title = { SectionTitle(stringResource(R.string.settings_about)) },
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
      ) {
        SettingRow(
          title = stringResource(R.string.settings_version),
          subtitle = versionName,
          icon = Icons.Outlined.Info,
          shape = groupedItemShape(0, aboutCount),
        )
        ExternalLinkRow(
          title = stringResource(R.string.settings_privacy_policy),
          subtitle = stringResource(R.string.settings_privacy_policy_summary),
          icon = Icons.Outlined.Policy,
          shape = groupedItemShape(1, aboutCount),
          onClick = onOpenPrivacyPolicy,
        )
        ExternalLinkRow(
          title = stringResource(R.string.settings_feedback),
          subtitle = stringResource(R.string.settings_feedback_summary),
          icon = Icons.Outlined.Mail,
          shape = groupedItemShape(2, aboutCount),
          onClick = onOpenFeedback,
        )
      }
      MadeBy(modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 32.dp))
    }
  }

  if (showContactsExplain) {
    ContactsExplainSheet(
      onContinue = {
        showContactsExplain = false
        onContactsExplainContinue()
      },
      onDismiss = { showContactsExplain = false },
    )
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

private fun relockLabel(seconds: Int): Int =
  when (seconds) {
    0 -> R.string.relock_immediately
    60 -> R.string.relock_1_min
    else -> R.string.relock_5_min
  }

private fun relockSummary(seconds: Int): Int =
  when (seconds) {
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
        options =
          listOf(
            ToggleOption(ThemeMode.Light, stringResource(R.string.settings_theme_light), Icons.Rounded.LightMode),
            ToggleOption(ThemeMode.Dark, stringResource(R.string.settings_theme_dark), Icons.Rounded.DarkMode),
            ToggleOption(
              ThemeMode.System,
              stringResource(R.string.settings_theme_system),
              Icons.Rounded.BrightnessAuto,
            ),
          ),
        selected = selected,
        onSelect = onSelected,
      )
    }
  }
}

private val SupportedLanguages = setOf("ca", "es", "en")

// Idioma efectiu de l'app; si el sistema en té un de no suportat, l'app cau a l'anglès per defecte.

/** Idioma per app des d'Android 13; abans, l'idioma del sistema, que l'app ja segueix (en, ca i es). */
private fun Context.openLanguageSettings() {
  val intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      Intent(Settings.ACTION_APP_LOCALE_SETTINGS).setData(Uri.fromParts("package", packageName, null))
    } else {
      Intent(Settings.ACTION_LOCALE_SETTINGS)
    }
  startActivity(intent)
}

private fun Context.openPrivacyPolicy() = openUrl(getString(R.string.settings_privacy_policy_url))

private fun Context.openUrl(url: String) {
  try {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
  } catch (_: ActivityNotFoundException) {
    // Sense navegador: no hi ha res a obrir.
  }
}

private fun Context.openFeedback(
  subject: String,
  body: String,
) {
  // Assumpte i cos van dins l'URI: Gmail ignora `EXTRA_SUBJECT` i `EXTRA_TEXT` amb `ACTION_SENDTO`.
  val uri = Uri.parse("mailto:${AboutLinks.FeedbackEmail}?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")
  val intent = Intent(Intent.ACTION_SENDTO, uri)
  try {
    startActivity(intent)
  } catch (_: ActivityNotFoundException) {
    // Sense app de correu: no hi ha res a obrir.
  }
}

private fun currentLanguageName(context: Context): String {
  val configured = context.resources.configuration.locales[0]
  val locale = if (configured.language in SupportedLanguages) configured else Locale.ENGLISH
  return locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) }
}

/** «versionName (versionCode)». */
private fun versionLabel(context: Context): String {
  val info = context.packageManager.getPackageInfo(context.packageName, 0)
  return "${info.versionName.orEmpty()} (${PackageInfoCompat.getLongVersionCode(info)})"
}

@Composable
private fun ExternalLinkRow(
  title: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  shape: androidx.compose.ui.graphics.Shape,
  onClick: () -> Unit,
) {
  SettingRow(
    title = title,
    subtitle = subtitle,
    icon = icon,
    trailing = {
      Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = stringResource(R.string.settings_opens_outside))
    },
    shape = shape,
    onClick = onClick,
  )
}

@Composable
private fun MadeBy(modifier: Modifier = Modifier) {
  val text = stringResource(R.string.settings_made_by, AboutLinks.AuthorName)
  val start = text.indexOf(AboutLinks.AuthorName)
  val color = MaterialTheme.colorScheme.onSurface
  val linkStyles =
    TextLinkStyles(SpanStyle(color = color, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline))
  Text(
    text =
      buildAnnotatedString {
        if (start < 0) {
          append(text)
        } else {
          append(text.substring(0, start))
          withLink(LinkAnnotation.Url(AboutLinks.AuthorUrl, linkStyles)) { append(AboutLinks.AuthorName) }
          append(text.substring(start + AboutLinks.AuthorName.length))
        }
      },
    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = modifier,
  )
}

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

private fun Context.notificationsEnabled() = NotificationManagerCompat.from(this).areNotificationsEnabled()

@Composable
private fun ContactsSection(
  state: ContactsSettingsState,
  onTurnOn: () -> Unit,
  onAllow: () -> Unit,
) {
  SettingsGroup(
    title = {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        SectionTitle(stringResource(R.string.settings_contacts))
        ProBadge(
          text = stringResource(R.string.pro_badge),
          contentDescription = stringResource(R.string.pro_description),
        )
      }
    },
    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
  ) {
    when (state) {
      ContactsSettingsState.Locked ->
        SettingRow(
          title = stringResource(R.string.settings_contacts_link),
          subtitle = stringResource(R.string.settings_contacts_on_summary),
          icon = Icons.Rounded.Contacts,
          shape = groupedItemShape(0, 1),
        )
      ContactsSettingsState.NeverOn ->
        SettingRow(
          title = stringResource(R.string.settings_contacts_link),
          subtitle = stringResource(R.string.settings_contacts_off_summary),
          icon = Icons.Rounded.Contacts,
          trailing = {
            FilledTonalButton(onClick = onTurnOn) {
              Text(stringResource(R.string.settings_contacts_turn_on), fontWeight = FontWeight.Bold)
            }
          },
          shape = groupedItemShape(0, 1),
        )
      is ContactsSettingsState.On ->
        SettingRow(
          title = pluralStringResource(R.plurals.settings_contacts_on, state.linkedCount, state.linkedCount),
          subtitle = stringResource(R.string.settings_contacts_on_summary),
          icon = Icons.Rounded.Contacts,
          shape = groupedItemShape(0, 1),
        )
      ContactsSettingsState.Denied ->
        SettingRow(
          title = stringResource(R.string.contacts_no_permission),
          subtitle = stringResource(R.string.settings_contacts_denied_summary),
          icon = Icons.Rounded.Lock,
          trailing = {
            FilledTonalButton(onClick = onAllow) {
              Text(stringResource(R.string.contacts_allow), fontWeight = FontWeight.Bold)
            }
          },
          shape = groupedItemShape(0, 1),
        )
    }
  }
  // Bloquejada (5h, gratuït) no porta la nota
  if (state != ContactsSettingsState.Locked) {
    Text(
      text = stringResource(R.string.settings_contacts_note),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 4.dp),
    )
  }
}

private fun Context.openNotificationSettings() {
  val intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
      .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  try {
    startActivity(intent)
  } catch (_: ActivityNotFoundException) {
    // Sense pantalla d'ajustos de notificacions: no hi ha res més a fer
  }
}
