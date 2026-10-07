package com.alorma.contactnotes.debug

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import android.widget.Toast
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.db.ConversesDatabase
import com.alorma.contactnotes.data.insertSampleData
import com.alorma.contactnotes.data.settings.OnboardingFlag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

// No module may show names or entry text: protected people must not leak through the menu
@Composable
fun ConversesDebugMenu() {
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
