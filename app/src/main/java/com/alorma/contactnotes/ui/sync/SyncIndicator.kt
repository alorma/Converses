package com.alorma.contactnotes.ui.sync

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.flags.rememberFeatureEnabled
import com.alorma.contactnotes.data.sync.SyncAccount
import com.alorma.contactnotes.data.sync.SyncFlag
import com.alorma.contactnotes.data.sync.SyncRepository
import com.alorma.contactnotes.data.sync.SyncState
import org.koin.compose.koinInject

/**
 * El que l'indicador de compte (13a), el rail (13r) i la fila d'Ajustos (13b) ensenyen de la sincronització. Encara no
 * hi ha ni «sincronitzant», ni «sense connexió», ni «dispositiu tret»: arriben amb el motor (fases 4 i 5).
 */
sealed interface SyncIndicatorState {
  data object SignedOut : SyncIndicatorState

  /** Sessió iniciada i clau al dispositiu. [initial] és la lletra de l'avatar. */
  data class On(
    val initial: String,
  ) : SyncIndicatorState

  /** Sessió iniciada, però falten les paraules de seguretat. */
  data class NeedsKey(
    val initial: String,
  ) : SyncIndicatorState
}

/** Mentre es comprova la sessió o es fa l'activació encara no hi ha res actiu: es veu com sense sessió. */
fun SyncState.toIndicatorState(): SyncIndicatorState =
  when (this) {
    SyncState.SignedOut, SyncState.Checking, is SyncState.Activating -> SyncIndicatorState.SignedOut
    is SyncState.NeedsKey -> SyncIndicatorState.NeedsKey(account.initial())
    is SyncState.Ready -> SyncIndicatorState.On(account.initial())
  }

/** La primera lletra del nom o, si el compte no en té, del correu. */
private fun SyncAccount.initial(): String {
  val source = name?.trim().orEmpty().ifEmpty { email?.trim().orEmpty() }
  return source.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"
}

@StringRes
fun SyncIndicatorState.descriptionRes(): Int =
  when (this) {
    SyncIndicatorState.SignedOut -> R.string.sync_indicator_off
    is SyncIndicatorState.On -> R.string.sync_indicator_on
    is SyncIndicatorState.NeedsKey -> R.string.sync_indicator_key
  }

/**
 * L'estat de l'indicador, o `null` amb [SyncFlag] apagat: llavors no es dibuixa res i ni tan sols es demana el
 * repositori.
 */
@Composable
fun currentSyncIndicatorState(): SyncIndicatorState? =
  if (rememberFeatureEnabled<SyncFlag>()) {
    val repository: SyncRepository = koinInject()
    val state by repository.state.collectAsStateWithLifecycle()
    state.toIndicatorState()
  } else {
    null
  }

/** Indicador de compte de la capçalera del llistat (13a): àrea tàctil de 48 dp al voltant del dibuix de 40. */
@Composable
fun SyncIndicator(
  state: SyncIndicatorState,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val description = stringResource(state.descriptionRes())
  Box(
    modifier =
      modifier
        .size(TouchTarget)
        .clip(CircleShape)
        .clickable(role = Role.Button, onClick = onClick)
        .semantics { contentDescription = description },
    contentAlignment = Alignment.Center,
  ) {
    SyncIndicatorVisual(state = state)
  }
}

/**
 * Només el dibuix de 40 dp, sense clic ni semàntica: els posa qui l'envolta (la capçalera o el rail).
 *
 * @param background el color que hi ha a sota, per a la vora de la insígnia.
 */
@Composable
fun SyncIndicatorVisual(
  state: SyncIndicatorState,
  modifier: Modifier = Modifier,
  background: Color = MaterialTheme.colorScheme.surface,
) {
  val scheme = MaterialTheme.colorScheme
  Box(modifier = modifier.size(VisualSize).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
    when (state) {
      SyncIndicatorState.SignedOut ->
        Box(
          modifier = Modifier.size(VisualSize).border(1.5.dp, scheme.outlineVariant, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Outlined.Cloud,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
          )
        }
      is SyncIndicatorState.On -> AccountAvatar(state.initial)
      is SyncIndicatorState.NeedsKey -> {
        AccountAvatar(state.initial)
        // Insígnia de 18 dp que surt 3 dp de l'avatar, amb una vora de 2 dp del color de fons
        Box(
          modifier =
            Modifier
              .align(Alignment.BottomEnd)
              .offset(x = 5.dp, y = 5.dp)
              .size(22.dp)
              .background(background, CircleShape)
              .padding(2.dp)
              .background(scheme.primary, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.Key,
            contentDescription = null,
            tint = scheme.onPrimary,
            modifier = Modifier.size(12.dp),
          )
        }
      }
    }
  }
}

/** La inicial del compte: la foto de Google demanaria una biblioteca d'imatges i una connexió més. */
@Composable
private fun AccountAvatar(initial: String) {
  Box(
    modifier = Modifier.size(VisualSize).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = initial,
      color = MaterialTheme.colorScheme.onPrimaryContainer,
      fontSize = 16.sp,
      fontWeight = FontWeight.ExtraBold,
      maxLines = 1,
    )
  }
}

private val TouchTarget = 48.dp
private val VisualSize = 40.dp
