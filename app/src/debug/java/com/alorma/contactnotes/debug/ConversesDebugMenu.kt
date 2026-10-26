package com.alorma.contactnotes.debug

import android.Manifest
import android.app.Activity
import android.provider.ContactsContract.Contacts
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.ConversesRepository
import com.alorma.contactnotes.data.contacts.ContactsGateway
import com.alorma.contactnotes.data.db.ConversesDatabase
import com.alorma.contactnotes.data.flags.FeatureFlag
import com.alorma.contactnotes.data.flags.LocalFeatureFlagRunner
import com.alorma.contactnotes.data.insertSampleData
import com.alorma.contactnotes.data.pro.OverridableProDataSource
import com.alorma.contactnotes.data.settings.OnboardingFlag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
  ContactsModule()
  OnboardingModule()
}

/**
 * Crea les files de Contactes sense la UI de la fase 2: el contacte triat s'enllaça amb la persona que es diu igual
 * (les dades de mostra coincideixen amb els contactes de prova). Els missatges no diuen cap nom.
 */
@Composable
private fun ContactsModule() {
  val repository: ConversesRepository = koinInject()
  val gateway: ContactsGateway = koinInject()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val linked = stringResource(R.string.debug_contacts_linked)
  val noPerson = stringResource(R.string.debug_contacts_no_person)
  val failed = stringResource(R.string.debug_contacts_failed)
  val cleared = stringResource(R.string.debug_contacts_cleared)
  val toast = { message: String -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }

  val pick =
    rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
      uri ?: return@rememberLauncherForActivityResult
      scope.launch {
        val message =
          runCatching {
            val (contactId, lookupKey, name) =
              withContext(Dispatchers.IO) {
                context.contentResolver
                  .query(
                    uri,
                    arrayOf(Contacts._ID, Contacts.LOOKUP_KEY, Contacts.DISPLAY_NAME),
                    null,
                    null,
                    null,
                  )!!
                  .use {
                    it.moveToFirst()
                    Triple(it.getLong(0), it.getString(1), it.getString(2))
                  }
              }
            val people = repository.observePeople().first() + repository.observePeople(archived = true).first()
            val person = people.map { it.person }.firstOrNull { it.name == name } ?: return@runCatching noPerson
            gateway.ensureAccount()
            val contact = gateway.addRows(person.id, contactId, lookupKey) ?: return@runCatching failed
            repository.linkContact(person, contact.id, contact.lookupKey, contact.name)
            linked
          }.getOrElse { failed }
        toast(message)
      }
    }
  val permissions =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
      if (result.values.all { it }) pick.launch(null) else toast(failed)
    }

  DebugModule(stringResource(R.string.debug_contacts_title)) {
    DebugCell(
      title = stringResource(R.string.debug_contacts_link),
      subtitle = stringResource(R.string.debug_contacts_link_summary),
      onClick = {
        permissions.launch(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS))
      },
    )
    DebugCell(
      title = stringResource(R.string.debug_contacts_clear),
      subtitle = stringResource(R.string.debug_contacts_clear_summary),
      onClick = {
        scope.launch {
          runCatching {
            gateway.rows().forEach { gateway.removeRows(it.personId) }
            gateway.removeAccount()
          }
          toast(cleared)
        }
      },
    )
  }
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

/** Being Pro, apart from the flags: the override, the fake switch and RevenueCat (Test Store). */
@Composable
private fun ProModule() {
  val pro: OverridableProDataSource = koinInject()
  val overridden = pro.isOverridden.collectAsState().value
  val localPro =
    pro.local.isPro
      .collectAsState()
      .value
  val remotePro =
    pro.remote.isPro
      .collectAsState()
      .value
  val activity = LocalActivity.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  DebugModule(stringResource(R.string.debug_pro_title)) {
    DebugCell(
      title = stringResource(R.string.debug_flags_pro_override),
      subtitle = stringResource(R.string.debug_flags_pro_override_summary),
      trailing = { Switch(checked = overridden, onCheckedChange = pro::setOverride) },
    )
    DebugCell(
      title = stringResource(R.string.debug_flags_pro_user),
      subtitle = stringResource(R.string.debug_flags_pro_user_summary),
      trailing = { Switch(checked = localPro, onCheckedChange = pro.local::setPro, enabled = overridden) },
    )
    DebugCell(
      title = stringResource(R.string.debug_pro_revenuecat),
      subtitle = stringResource(R.string.debug_pro_revenuecat_state, remotePro.toString()),
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
