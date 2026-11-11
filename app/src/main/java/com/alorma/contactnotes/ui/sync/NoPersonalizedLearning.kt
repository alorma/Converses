package com.alorma.contactnotes.ui.sync

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputMethodRequest

/**
 * El camp de dins demana al teclat que no aprengui el que s'hi escriu (`IME_FLAG_NO_PERSONALIZED_LEARNING`):
 * són les paraules de la clau. Compose no té cap opció pròpia per a aquest senyal, i per això s'afegeix a l'`EditorInfo`
 * en el moment en què el camp connecta amb el teclat.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NoPersonalizedLearning(content: @Composable () -> Unit) {
  val interceptor =
    remember {
      PlatformTextInputInterceptor { request, nextHandler ->
        nextHandler.startInputMethod(
          object : PlatformTextInputMethodRequest {
            override fun createInputConnection(outAttributes: EditorInfo): InputConnection {
              val connection = request.createInputConnection(outAttributes)
              outAttributes.imeOptions = outAttributes.imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
              return connection
            }
          },
        )
      }
    }
  InterceptPlatformTextInput(interceptor, content)
}
