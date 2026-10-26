package com.alorma.contactnotes.ui.paywall

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.contactnotes.R
import com.alorma.contactnotes.designsystem.components.GroupedListDefaults
import com.alorma.contactnotes.designsystem.components.asShape
import com.alorma.contactnotes.designsystem.components.groupedItemShape
import com.alorma.contactnotes.designsystem.theme.ScreenTitleStyle
import com.alorma.contactnotes.shared.AvatarShape
import org.koin.compose.viewmodel.koinViewModel

/** Pantalla «Converses Pro» (8a, 8b des d'una funció) i «Ja tens Pro» (8c). No s'obre mai sola. */
@Composable
fun PaywallScreen(
  highlight: ProBenefit?,
  onClose: () -> Unit,
  viewModel: PaywallViewModel = koinViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val activity = LocalActivity.current
  val context = LocalContext.current
  val uriHandler = LocalUriHandler.current
  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(viewModel) {
    viewModel.events.collect { message ->
      val text =
        when (message) {
          PaywallMessage.Pending -> R.string.paywall_pending
          PaywallMessage.Failed -> R.string.paywall_failed
          PaywallMessage.NothingToRestore -> R.string.paywall_nothing_to_restore
        }
      snackbarHostState.showSnackbar(context.getString(text))
    }
  }
  PaywallContent(
    state = state,
    highlight = highlight,
    snackbarHostState = snackbarHostState,
    onClose = onClose,
    onBuy = { activity?.let(viewModel::onBuy) },
    onRestore = viewModel::onRestore,
    onPrivacy = {
      // Sense navegador no hi ha res a obrir
      runCatching { uriHandler.openUri(context.getString(R.string.settings_privacy_policy_url)) }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallContent(
  state: PaywallState,
  highlight: ProBenefit?,
  onClose: () -> Unit,
  onBuy: () -> Unit,
  onRestore: () -> Unit,
  onPrivacy: () -> Unit,
  snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
  val scheme = MaterialTheme.colorScheme
  Scaffold(
    containerColor = scheme.surface,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {},
        navigationIcon = {
          FilledIconButton(
            onClick = onClose,
            colors =
              IconButtonDefaults.filledIconButtonColors(
                containerColor = scheme.surfaceContainerHigh,
                contentColor = scheme.onSurface,
              ),
          ) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
      )
    },
    // Peu fix al `bottomBar`: així el snackbar surt per sobre dels botons i no els tapa
    bottomBar = {
      PaywallFooter(state = state, onBuy = onBuy, onRestore = onRestore, onPrivacy = onPrivacy, onDone = onClose)
    },
  ) { padding ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(padding)
          .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      PaywallHeader(owned = state.isPro)
      BenefitList(
        benefits = proBenefits(if (state.isPro) null else highlight),
        highlight = if (state.isPro) null else highlight,
        owned = state.isPro,
      )
      if (!state.isPro) PrivacyNote()
    }
  }
}

@Composable
private fun PaywallHeader(owned: Boolean) {
  val scheme = MaterialTheme.colorScheme
  Column(
    modifier = Modifier.padding(horizontal = 6.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Box(
      modifier =
        Modifier
          .size(64.dp)
          .background(scheme.primary, AvatarShape.Cookie9Sided.asShape()),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = if (owned) Icons.Rounded.Check else Icons.Rounded.WorkspacePremium,
        contentDescription = null,
        tint = scheme.onPrimary,
        modifier = Modifier.size(32.dp),
      )
    }
    Text(
      text = stringResource(if (owned) R.string.paywall_owned_title else R.string.paywall_title),
      style = ScreenTitleStyle,
      modifier = Modifier.padding(top = 6.dp),
    )
    Text(
      text = stringResource(if (owned) R.string.paywall_owned_subtitle else R.string.paywall_subtitle),
      style = MaterialTheme.typography.bodyLarge,
      color = scheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun BenefitList(
  benefits: List<ProBenefit>,
  highlight: ProBenefit?,
  owned: Boolean,
) {
  Column(verticalArrangement = Arrangement.spacedBy(GroupedListDefaults.ItemSpacing)) {
    benefits.forEachIndexed { index, benefit ->
      BenefitRow(
        benefit = benefit,
        highlighted = benefit == highlight,
        owned = owned,
        index = index,
        count = benefits.size,
      )
    }
  }
}

@Composable
private fun BenefitRow(
  benefit: ProBenefit,
  highlighted: Boolean,
  owned: Boolean,
  index: Int,
  count: Int,
) {
  val scheme = MaterialTheme.colorScheme
  Surface(
    shape = groupedItemShape(index, count),
    color = GroupedListDefaults.containerColor,
    border = if (highlighted) BorderStroke(2.dp, scheme.onSurface) else null,
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .heightIn(min = 68.dp)
          .padding(start = 12.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Box(
        modifier =
          Modifier
            .size(40.dp)
            .background(scheme.surfaceContainerHigh, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(benefit.icon, contentDescription = null, modifier = Modifier.size(22.dp))
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(benefit.title), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(
          text = stringResource(benefit.summary),
          style = MaterialTheme.typography.bodyMedium,
          color = scheme.onSurfaceVariant,
        )
      }
      if (owned) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = scheme.onSurface)
    }
  }
}

@Composable
private fun PrivacyNote() {
  Row(
    modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 2.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Icon(
      Icons.Rounded.Lock,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(top = 1.dp).size(18.dp),
    )
    Text(
      text = stringResource(R.string.paywall_note),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun PaywallFooter(
  state: PaywallState,
  onBuy: () -> Unit,
  onRestore: () -> Unit,
  onPrivacy: () -> Unit,
  onDone: () -> Unit,
) {
  Column(
    // Com els `bottomBar` de Material (BottomAppBar), el peu s'aparta de la barra de navegació del sistema
    modifier = Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    if (state.isPro) {
      FooterButton(text = stringResource(R.string.paywall_owned_done), enabled = true, onClick = onDone)
      return@Column
    }
    FooterButton(
      text =
        if (state.isOnline) {
          stringResource(R.string.paywall_buy, state.price ?: "…")
        } else {
          stringResource(R.string.paywall_buy_offline)
        },
      enabled = state.canBuy,
      onClick = onBuy,
    )
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      TextButton(onClick = onRestore, enabled = state.canRestore) {
        Text(stringResource(R.string.paywall_restore), fontWeight = FontWeight.Bold)
      }
      Spacer(Modifier.weight(1f))
      TextButton(onClick = onPrivacy) {
        Text(
          text = stringResource(R.string.paywall_privacy),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textDecoration = TextDecoration.Underline,
        )
      }
    }
  }
}

@Composable
private fun FooterButton(
  text: String,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
  ) {
    Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
  }
}

private val ProBenefit.icon: ImageVector
  get() =
    when (this) {
      ProBenefit.People -> Icons.Rounded.Palette
      ProBenefit.Channels -> Icons.Rounded.Forum
      ProBenefit.FollowUps -> Icons.Rounded.Flag
      ProBenefit.Contacts -> Icons.Rounded.Contacts
    }

private val ProBenefit.title: Int
  get() =
    when (this) {
      ProBenefit.People -> R.string.paywall_benefit_people
      ProBenefit.Channels -> R.string.paywall_benefit_channels
      ProBenefit.FollowUps -> R.string.paywall_benefit_follow_ups
      ProBenefit.Contacts -> R.string.paywall_benefit_contacts
    }

private val ProBenefit.summary: Int
  get() =
    when (this) {
      ProBenefit.People -> R.string.paywall_benefit_people_summary
      ProBenefit.Channels -> R.string.paywall_benefit_channels_summary
      ProBenefit.FollowUps -> R.string.paywall_benefit_follow_ups_summary
      ProBenefit.Contacts -> R.string.paywall_benefit_contacts_summary
    }
