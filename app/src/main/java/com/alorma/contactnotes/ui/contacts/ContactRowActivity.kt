package com.alorma.contactnotes.ui.contacts

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.alorma.contactnotes.MainActivity
import com.alorma.contactnotes.data.contacts.ContactsGateway
import com.alorma.contactnotes.ui.navigation.EXTRA_CONTACT_ROW_ACTION
import com.alorma.contactnotes.ui.navigation.EXTRA_CONTACT_ROW_PERSON_ID
import com.alorma.contactnotes.ui.navigation.contactRowAction
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * S'obre en tocar «Veure notes» o «Crear nota» a la fitxa d'un contacte (`docs/contacts`). Transparent i sense
 * historial: llegeix de quina persona és la fila i obre `MainActivity`, que decideix on anar. Si no pot llegir la fila
 * (per exemple, sense permís), obre l'app normal.
 */
class ContactRowActivity : ComponentActivity() {
  private val gateway: ContactsGateway by inject()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState != null) return
    val action = contactRowAction(packageName, intent.type)
    val dataUri = intent.data?.toString()
    lifecycleScope.launch {
      val personId = if (action != null && dataUri != null) gateway.personIdForRow(dataUri) else null
      val main = Intent(this@ContactRowActivity, MainActivity::class.java)
      if (action != null && personId != null) {
        main.putExtra(EXTRA_CONTACT_ROW_ACTION, action.name).putExtra(EXTRA_CONTACT_ROW_PERSON_ID, personId)
      }
      startActivity(main)
      finish()
    }
  }
}
