package com.alorma.contactnotes.ui.sync

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.alorma.contactnotes.data.sync.SyncStatus
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

/**
 * El que l'indicador de compte (13a), el rail (13r) i la fila d'Ajustos (13b) ensenyen de la sincronització. Encara no
 * hi ha «dispositiu tret»: arriba a la fase 5.
 */
sealed interface SyncIndicatorState {
  data object SignedOut : SyncIndicatorState

  /** Sessió iniciada i clau al dispositiu. [initial] és la lletra de l'avatar; [status], com van les dades. */
  data class On(
    val initial: String,
    val status: SyncIndicatorStatus = SyncIndicatorStatus.Idle,
  ) : SyncIndicatorState

  /** Sessió iniciada, però falten les paraules de seguretat. */
  data class NeedsKey(
    val initial: String,
  ) : SyncIndicatorState

  /** Un altre dispositiu l'ha tret del compte: insígnia `priority_high` sobre `err` (13a). */
  data class Removed(
    val initial: String,
  ) : SyncIndicatorState
}

/** Les dades a l'indicador (13a): sense connexió i error es veuen igual, i no en vermell. */
enum class SyncIndicatorStatus { Idle, Syncing, Offline }

/** Només «sincronitzant» es veu si dura més de 600 ms. [syncingVisible] ho diu. */
fun SyncStatus.toIndicatorStatus(syncingVisible: Boolean): SyncIndicatorStatus =
  when (this) {
    is SyncStatus.UpToDate -> SyncIndicatorStatus.Idle
    is SyncStatus.Syncing -> if (syncingVisible) SyncIndicatorStatus.Syncing else SyncIndicatorStatus.Idle
    is SyncStatus.Offline, SyncStatus.Failed -> SyncIndicatorStatus.Offline
  }

/** Mentre es comprova la sessió o es fa l'activació encara no hi ha res actiu: es veu com sense sessió. */
fun SyncState.toIndicatorState(): SyncIndicatorState =
  when (this) {
    SyncState.SignedOut, SyncState.Checking, is SyncState.Activating -> SyncIndicatorState.SignedOut
    is SyncState.NeedsKey -> SyncIndicatorState.NeedsKey(account.initial())
    is SyncState.Ready -> SyncIndicatorState.On(account.initial())
    is SyncState.Removed -> SyncIndicatorState.Removed(account.initial())
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
    is SyncIndicatorState.On ->
      when (status) {
        SyncIndicatorStatus.Idle -> R.string.sync_indicator_on
        SyncIndicatorStatus.Syncing -> R.string.sync_indicator_syncing
        SyncIndicatorStatus.Offline -> R.string.sync_indicator_offline
      }
    is SyncIndicatorState.NeedsKey -> R.string.sync_indicator_key
    is SyncIndicatorState.Removed -> R.string.sync_indicator_out
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
    val status by repository.status.collectAsStateWithLifecycle()
    // L'arc de «sincronitzant» no es mostra si la sincronització dura menys de 600 ms
    var syncingVisible by remember { mutableStateOf(false) }
    val syncing = status is SyncStatus.Syncing
    LaunchedEffect(syncing) {
      if (syncing) {
        delay(SyncingDelayMillis)
        syncingVisible = true
      } else {
        syncingVisible = false
      }
    }
    when (val indicator = state.toIndicatorState()) {
      is SyncIndicatorState.On -> indicator.copy(status = status.toIndicatorStatus(syncingVisible))
      else -> indicator
    }
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
      is SyncIndicatorState.On -> {
        AccountAvatar(state.initial)
        when (state.status) {
          SyncIndicatorStatus.Idle -> Unit
          SyncIndicatorStatus.Syncing ->
            CircularProgressIndicator(
              modifier = Modifier.size(VisualSize),
              color = scheme.primary,
              trackColor = scheme.outlineVariant,
              strokeWidth = 2.5.dp,
            )
          SyncIndicatorStatus.Offline ->
            Box(
              modifier =
                Modifier
                  .align(Alignment.BottomEnd)
                  .offset(x = 5.dp, y = 5.dp)
                  .size(22.dp)
                  .background(background, CircleShape)
                  .padding(2.dp)
                  .background(scheme.surfaceContainerHighest, CircleShape),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.CloudOff,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp),
              )
            }
        }
      }
      is SyncIndicatorState.NeedsKey -> {
        AccountAvatar(state.initial)
        StatusBadge(Icons.Rounded.Key, scheme.primary, scheme.onPrimary, background)
      }
      is SyncIndicatorState.Removed -> {
        AccountAvatar(state.initial)
        StatusBadge(Icons.Rounded.PriorityHigh, scheme.error, scheme.onError, background)
      }
    }
  }
}

/** Insígnia de 18 dp que surt 3 dp de l'avatar, amb una vora de 2 dp del color de fons. */
@Composable
private fun BoxScope.StatusBadge(
  icon: ImageVector,
  container: Color,
  content: Color,
  background: Color,
) {
  Box(
    modifier =
      Modifier
        .align(Alignment.BottomEnd)
        .offset(x = 5.dp, y = 5.dp)
        .size(22.dp)
        .background(background, CircleShape)
        .padding(2.dp)
        .background(container, CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    Icon(imageVector = icon, contentDescription = null, tint = content, modifier = Modifier.size(12.dp))
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

private const val SyncingDelayMillis = 600L
private val TouchTarget = 48.dp
private val VisualSize = 40.dp
