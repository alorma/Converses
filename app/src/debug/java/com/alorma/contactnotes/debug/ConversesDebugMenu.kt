package com.alorma.contactnotes.debug

import android.app.Activity
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import android.widget.Toast
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.db.ConversesDatabase
import com.alorma.contactnotes.data.insertSampleData
import com.alorma.contactnotes.data.flags.FeatureFlag
import com.alorma.contactnotes.data.flags.LocalFeatureFlagRunner
import com.alorma.contactnotes.data.settings.OnboardingFlag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.getKoin
import org.koin.compose.koinInject

// No module may show names or entry text: protected people must not leak through the menu
@Composable
fun ConversesDebugMenu() {
  FeatureFlagsModule()
  DataModule()
  OnboardingModule()
}

@Composable
private fun DataModule() {
  val database: ConversesDatabase = koinInject()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val done = stringResource(R.string.debug_data_reseeded)
  DebugModule(stringResource(R.string.debug_data_title)) {
    DebugCell(
      title = stringResource(R.string.debug_data_reseed),
      subtitle = stringResource(R.string.debug_data_reseed_summary),
      onClick = {
        scope.launch {
          withContext(Dispatchers.IO) {
            database.clearAllTables()
            database.insertSampleData()
          }
          Toast.makeText(context, done, Toast.LENGTH_SHORT).show()
        }
      },
    )
  }
}

@Composable
private fun OnboardingModule() {
  val flag: OnboardingFlag = koinInject()
  val activity = LocalContext.current as? Activity
  DebugModule(stringResource(R.string.debug_onboarding_title)) {
    DebugCell(
      title = stringResource(R.string.debug_onboarding_show),
      subtitle = stringResource(R.string.debug_onboarding_show_summary),
      onClick = {
        flag.reset()
        activity?.recreate()
      },
    )
  }
}

@Composable
private fun FeatureFlagsModule() {
  val runner: LocalFeatureFlagRunner = koinInject()
  val flags = getKoin().getAll<FeatureFlag>()
  // Reads below depend on this, so the switches follow every override
  runner.changes.collectAsState().value
  val proFlags = flags.filter { it.pro }
  DebugModule(stringResource(R.string.debug_flags_title)) {
    DebugCell(
      title = stringResource(R.string.debug_flags_pro_mode),
      subtitle = stringResource(R.string.debug_flags_pro_mode_summary),
      trailing = {
        Switch(
          checked = proFlags.isNotEmpty() && proFlags.all { it.isEnabled() },
          onCheckedChange = { on -> proFlags.forEach { runner.setOverride(it.key, on) } },
        )
      },
    )
    flags.forEach { flag ->
      DebugCell(
        title = flag.key,
        subtitle = stringResource(if (runner.hasOverride(flag.key)) R.string.debug_flags_overridden else R.string.debug_flags_default),
        trailing = { Switch(checked = flag.isEnabled(), onCheckedChange = { runner.setOverride(flag.key, it) }) },
      )
    }
    DebugCell(
      title = stringResource(R.string.debug_flags_reset),
      subtitle = stringResource(R.string.debug_flags_reset_summary),
      onClick = { runner.clearAllOverrides() },
    )
  }
}
