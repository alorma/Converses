package com.alorma.contactnotes.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.wear.compose.material3.AppScaffold
import com.alorma.contactnotes.wear.data.ProtectedSessions
import com.alorma.contactnotes.wear.theme.ConversesWearTheme
import com.alorma.contactnotes.wear.ui.WearNavigation
import com.alorma.contactnotes.wear.ui.WearTimeText

class MainActivity : ComponentActivity() {
  override fun onStop() {
    // Amb l'app en segon pla (o la pantalla apagada) les entrades d'una protegida marxen de la memòria
    ProtectedSessions.shared.clear()
    super.onStop()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      ConversesWearTheme {
        AppScaffold(timeText = { WearTimeText() }) { WearNavigation() }
      }
    }
  }
}
