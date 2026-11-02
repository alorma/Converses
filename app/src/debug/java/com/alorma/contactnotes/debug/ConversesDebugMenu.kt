package com.alorma.contactnotes.debug

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.db.ConversesDatabase
import com.alorma.contactnotes.data.flags.FeatureFlag
import com.alorma.contactnotes.data.flags.LocalFeatureFlagRunner
import com.alorma.contactnotes.data.insertSampleData
import com.alorma.contactnotes.data.pro.OverridableProDataSource
import com.alorma.contactnotes.data.settings.OnboardingFlag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.getKoin
import org.koin.compose.koinInject

// No module may show names or entry text: protected people must not leak through the menu
@Composable
fun ConversesDebugMenu() {
  ProModule()
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

/**
 * Being Pro, apart from the flags. The switch shows the real state: on when RevenueCat (Test Store) says it was bought,
 * and then it cannot be turned off; otherwise turning it on overrides Pro without buying.
 */
@Composable
private fun ProModule() {
  val pro: OverridableProDataSource = koinInject()
  val isPro = pro.isPro.collectAsState().value
  val overridden = pro.isOverridden.collectAsState().value
  val bought =
    pro.remote.isPro
      .collectAsState()
      .value
  val activity = LocalActivity.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  DebugModule(stringResource(R.string.debug_pro_title)) {
    DebugCell(
      title = stringResource(R.string.debug_pro_user),
      subtitle =
        stringResource(
          when {
            bought -> R.string.debug_pro_user_bought
            overridden -> R.string.debug_pro_user_overridden
            else -> R.string.debug_pro_user_not_bought
          },
        ),
      trailing = { Switch(checked = isPro, onCheckedChange = pro::setOverride, enabled = !bought) },
    )
    // Always against RevenueCat, whatever the override says; the result name is enough for debugging
    DebugCell(
      title = stringResource(R.string.debug_pro_buy),
      onClick = {
        activity ?: return@DebugCell
        scope.launch {
          val result = pro.remote.purchase(activity)
          Toast.makeText(context, result.toString(), Toast.LENGTH_SHORT).show()
        }
      },
    )
    DebugCell(
      title = stringResource(R.string.debug_pro_restore),
      onClick = {
        scope.launch {
          val result = pro.remote.restore()
          Toast.makeText(context, result.toString(), Toast.LENGTH_SHORT).show()
        }
      },
    )
  }
}

@Composable
private fun FeatureFlagsModule() {
  val runner: LocalFeatureFlagRunner = koinInject()
  val pro: OverridableProDataSource = koinInject()
  val flags = getKoin().getAll<FeatureFlag>()
  // Reads below depend on these, so the switches follow every override and every Pro change
  runner.changes.collectAsState().value
  pro.isPro.collectAsState().value
  val states = flags.associateWith { it.state() }
  val proFlags = flags.filter { it.pro }
  DebugModule(stringResource(R.string.debug_flags_title)) {
    DebugCell(
      title = stringResource(R.string.debug_flags_pro_mode),
      subtitle = stringResource(R.string.debug_flags_pro_mode_summary),
      trailing = {
        Switch(
          checked = proFlags.isNotEmpty() && proFlags.all { runner.isEnabled(it) },
          onCheckedChange = { on -> proFlags.forEach { runner.setOverride(it.key, on) } },
        )
      },
    )
    flags.forEach { flag ->
      DebugCell(
        title = flag.key,
        subtitle =
          stringResource(
            if (runner.hasOverride(flag.key)) R.string.debug_flags_overridden else R.string.debug_flags_default,
            states.getValue(flag).name,
          ),
        trailing = { Switch(checked = runner.isEnabled(flag), onCheckedChange = { runner.setOverride(flag.key, it) }) },
      )
    }
    DebugCell(
      title = stringResource(R.string.debug_flags_reset),
      subtitle = stringResource(R.string.debug_flags_reset_summary),
      onClick = { runner.clearAllOverrides() },
    )
  }
}
