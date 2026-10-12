package com.alorma.contactnotes.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Text
import com.alorma.contactnotes.wear.theme.ConversesWearTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      ConversesWearTheme {
        AppScaffold {
          Text(text = getString(R.string.app_name))
        }
      }
    }
  }
}
