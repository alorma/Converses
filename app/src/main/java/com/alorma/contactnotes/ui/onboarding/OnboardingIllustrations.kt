package com.alorma.contactnotes.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.NoAccounts
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.designsystem.components.Avatar
import com.alorma.contactnotes.designsystem.components.AvatarColor
import com.alorma.contactnotes.designsystem.components.AvatarShape
import com.alorma.contactnotes.designsystem.components.TimelineMarker
import com.alorma.contactnotes.designsystem.components.TimelineMarkerStyle
import com.alorma.contactnotes.designsystem.components.TimelineRow
import com.alorma.contactnotes.designsystem.components.asShape
import com.alorma.contactnotes.designsystem.theme.SuccessColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

internal typealias Progress = Animatable<Float, AnimationVector1D>

private val Bounce = spring<Float>(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow)
private val SlowBounce = spring<Float>(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)
private val InitialsColor = Color(0xFF1E1D24)

/**
 * Llista d'`Animatable` (0 → 1) que es reinicia i es torna a llançar quan la pàgina queda assentada
 * o canvia `replayKey`. Amb moviment reduït es queda directament a l'estat final.
 */
@Composable
internal fun rememberSequence(
  count: Int,
  active: Boolean,
  replayKey: Int,
  reduced: Boolean,
  run: suspend CoroutineScope.(List<Progress>) -> Unit,
): List<Progress> {
  val animations = remember(reduced) { List(count) { Animatable(if (reduced) 1f else 0f) } }
  LaunchedEffect(active, replayKey, reduced) {
    if (reduced) {
      animations.forEach { it.snapTo(1f) }
      return@LaunchedEffect
    }
    animations.forEach { it.snapTo(0f) }
    if (active) run(animations)
  }
  return animations
}

internal fun CoroutineScope.go(progress: Progress, startMs: Int, spec: AnimationSpec<Float>) {
  launch {
    delay(startMs.toLong())
    progress.animateTo(1f, spec)
  }
}

// ---------------------------------------------------------------------------------------------
// Pas 1 · Recorda cada conversa
// ---------------------------------------------------------------------------------------------

private class DemoAvatar(
  val initials: String,
  val shape: AvatarShape,
  val color: AvatarColor,
  val x: Int,
  val y: Int,
  val size: Int,
)

private val DemoAvatars = listOf(
  DemoAvatar("JP", AvatarShape.Standard, AvatarColor.Mint, 18, 40, 72),
  DemoAvatar("LF", AvatarShape.Standard, AvatarColor.Peach, 250, 20, 88),
  DemoAvatar("R", AvatarShape.Standard, AvatarColor.Sky, 270, 250, 64),
  DemoAvatar("MS", AvatarShape.Standard, AvatarColor.Rose, 40, 260, 84),
  DemoAvatar("A", AvatarShape.Standard, AvatarColor.Lemon, 160, 300, 48),
)

private const val BubbleCenterX = 175
private const val BubbleCenterY = 190
private const val EntryDistance = 500f

@Composable
fun StepOneIllustration(active: Boolean, replayKey: Int, reduced: Boolean, modifier: Modifier = Modifier) {
  val p = rememberSequence(count = 3 + DemoAvatars.size, active = active, replayKey = replayKey, reduced = reduced) { a ->
    go(a[0], 150, SlowBounce)
    go(a[1], 1300, tween(800))
    go(a[2], 1700, tween(650))
    DemoAvatars.indices.forEach { i -> go(a[3 + i], 900 + i * 250, SlowBounce) }
  }
  val bubble = AvatarShape.Cookie9Sided.asShape()
  val scheme = MaterialTheme.colorScheme

  Box(modifier = modifier.size(width = 350.dp, height = 380.dp)) {
    Box(
      modifier = Modifier
        .offset(BubbleCenterX.dp - 80.dp, BubbleCenterY.dp - 80.dp)
        .size(160.dp)
        .graphicsLayer {
          scaleX = p[0].value
          scaleY = p[0].value
          rotationZ = -40f * (1f - p[0].value)
        }
        .clip(bubble)
        .background(scheme.primary),
    ) {
      Column(
        modifier = Modifier.align(Alignment.CenterStart).padding(start = 40.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        TextLine(widthDp = 80, progress = { p[1].value }, color = scheme.onPrimary)
        TextLine(widthDp = 56, progress = { p[2].value }, color = scheme.onPrimary)
      }
    }
    DemoAvatars.forEachIndexed { i, avatar ->
      FloatingAvatar(index = i, avatar = avatar, progress = p[3 + i], floating = active && !reduced)
    }
  }
}

@Composable
private fun TextLine(widthDp: Int, progress: () -> Float, color: Color) {
  Box(
    modifier = Modifier
      .width(widthDp.dp)
      .height(10.dp)
      .graphicsLayer {
        scaleX = progress()
        transformOrigin = TransformOrigin(0f, 0.5f)
      }
      .clip(CircleShape)
      .background(color),
  )
}

@Composable
private fun FloatingAvatar(index: Int, avatar: DemoAvatar, progress: Progress, floating: Boolean) {
  // Entren des de fora de la pantalla, en la direcció que s'allunya de la bafarada
  val fromX = avatar.x + avatar.size / 2f - BubbleCenterX
  val fromY = avatar.y + avatar.size / 2f - BubbleCenterY
  val length = sqrt(fromX * fromX + fromY * fromY)
  val dx = fromX / length * EntryDistance
  val dy = fromY / length * EntryDistance
  var floatY: State<Float>? = null
  var spin: State<Float>? = null
  if (floating) {
    val transition = rememberInfiniteTransition(label = "avatar$index")
    val period = listOf(3000, 3600, 4200)[index % 3]
    floatY = transition.animateFloat(
      initialValue = 0f,
      targetValue = -7f,
      animationSpec = infiniteRepeatable(
        animation = tween(period / 2, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse,
        initialStartOffset = StartOffset(3000 + index * 300),
      ),
      label = "float",
    )
    spin = transition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec = infiniteRepeatable(tween(18_000 + index * 4_000, easing = LinearEasing)),
      label = "spin",
    )
  }
  val shape = avatar.shape.asShape()
  val fontSize = with(LocalDensity.current) { (avatar.size * 0.36f).dp.toSp() }
  Box(
    modifier = Modifier
      .offset(avatar.x.dp, avatar.y.dp)
      .size(avatar.size.dp)
      .graphicsLayer {
        val q = progress.value
        translationX = dx.dp.toPx() * (1f - q)
        translationY = dy.dp.toPx() * (1f - q) + (floatY?.value ?: 0f).dp.toPx()
        rotationZ = -90f * (1f - q)
      },
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer { rotationZ = spin?.value ?: 0f }
        .clip(shape)
        .background(avatar.color.color),
    )
    Text(text = avatar.initials, color = InitialsColor, fontWeight = FontWeight.Bold, fontSize = fontSize)
  }
}

// ---------------------------------------------------------------------------------------------
// Pas 2 · Tot en una cronologia
// ---------------------------------------------------------------------------------------------

private fun Modifier.typing(progress: () -> Float): Modifier = drawWithContent {
  clipRect(right = size.width * progress()) { this@drawWithContent.drawContent() }
}

private const val StepTwoRows = 3

@Composable
fun StepTwoIllustration(active: Boolean, replayKey: Int, reduced: Boolean, modifier: Modifier = Modifier) {
  // 0: targeta; per fila r: 1+4r marcador, 2+4r entrada, 3+4r línia, 4+4r text
  val p = rememberSequence(count = 1 + 4 * StepTwoRows, active = active, replayKey = replayKey, reduced = reduced) { a ->
    go(a[0], 0, tween(500, easing = FastOutSlowInEasing))
    repeat(StepTwoRows) { r ->
      val base = 500 + r * 750
      go(a[1 + 4 * r], base, keyframes { durationMillis = 450; 1.25f at 250 })
      go(a[2 + 4 * r], base + 150, tween(450, easing = FastOutSlowInEasing))
      go(a[3 + 4 * r], base + 350, tween(500))
      go(a[4 + 4 * r], base + 350, tween(700, easing = LinearEasing))
    }
  }
  val labels = listOf(R.string.onb_sample_1_label, R.string.onb_sample_2_label, R.string.onb_sample_3_label)
  val texts = listOf(R.string.onb_sample_1_text, R.string.onb_sample_2_text, R.string.onb_sample_3_text)
  val scheme = MaterialTheme.colorScheme

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(top = 70.dp)
      .graphicsLayer {
        alpha = p[0].value
        translationY = (1f - p[0].value) * 24.dp.toPx()
      }
      .clip(RoundedCornerShape(32.dp))
      .background(scheme.surfaceContainerLowest)
      .padding(horizontal = 20.dp, vertical = 16.dp),
  ) {
    repeat(StepTwoRows) { r ->
      TimelineRow(
        isFirst = r == 0,
        isLast = r == StepTwoRows - 1,
        lineProgress = { p[3 + 4 * r].value },
        marker = {
          Box(
            modifier = Modifier.graphicsLayer {
              scaleX = p[1 + 4 * r].value
              scaleY = p[1 + 4 * r].value
            },
          ) { StepTwoMarker(r) }
        },
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
              alpha = p[2 + 4 * r].value
              translationX = (1f - p[2 + 4 * r].value) * 28.dp.toPx()
            }
            .clip(RoundedCornerShape(18.dp))
            .background(scheme.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
          Text(
            text = stringResource(labels[r]),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurfaceVariant,
          )
          Text(
            text = stringResource(texts[r]),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurface,
            modifier = Modifier.typing { p[4 + 4 * r].value },
          )
        }
      }
    }
  }
}

@Composable
private fun StepTwoMarker(row: Int) {
  when (row) {
    0 -> TimelineMarker { Avatar("L", AvatarShape.Cookie9Sided, AvatarColor.Lavender, size = 32.dp) }
    1 -> TimelineMarker(Icons.Rounded.Person, TimelineMarkerStyle.Filled)
    else -> TimelineMarker(Icons.Rounded.EditNote, TimelineMarkerStyle.Outlined)
  }
}

// ---------------------------------------------------------------------------------------------
// Pas 3 · Només per a tu
// ---------------------------------------------------------------------------------------------

private val FingerprintShape = RoundedCornerShape(28.dp)

@Composable
fun StepThreeIllustration(active: Boolean, replayKey: Int, reduced: Boolean, modifier: Modifier = Modifier) {
  // 0 cercle, 1 avatar, 2 botó, 3 pols, 4 escaneig, 5 verd, 6 empremta surt, 7 check entra,
  // 8 cadenat obert, 9 rebot de l'avatar, 10 pastilles
  val p = rememberSequence(count = 11, active = active, replayKey = replayKey, reduced = reduced) { a ->
    go(a[0], 2500, tween(700, easing = FastOutSlowInEasing))
    go(a[1], 100, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
    go(a[2], 500, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
    launch {
      delay(900)
      repeat(2) {
        a[3].snapTo(0f)
        a[3].animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
      }
    }
    launch {
      delay(1100)
      repeat(2) {
        a[4].snapTo(0f)
        a[4].animateTo(1f, tween(700, easing = LinearEasing))
      }
    }
    go(a[5], 2400, tween(350))
    go(a[6], 2400, tween(200))
    go(a[7], 2550, Bounce)
    go(a[8], 2600, tween(250))
    go(a[9], 2800, tween(400))
    go(a[10], 3100, tween(500, easing = FastOutSlowInEasing))
  }
  val scheme = MaterialTheme.colorScheme
  val buttonColor = androidx.compose.ui.graphics.lerp(scheme.primary, SuccessColors.success, p[5].value)
  val buttonContent = androidx.compose.ui.graphics.lerp(scheme.onPrimary, SuccessColors.onSuccess, p[5].value)

  Box(modifier = modifier.size(width = 350.dp, height = 380.dp)) {
    Box(
      modifier = Modifier
        .offset(35.dp, 10.dp)
        .size(280.dp)
        .graphicsLayer {
          val s = 0.2f + 0.8f * p[0].value
          scaleX = s
          scaleY = s
          alpha = p[0].value
        }
        .clip(CircleShape)
        .background(SuccessColors.successContainer),
    )
    Box(
      modifier = Modifier
        .offset(85.dp, 60.dp)
        .size(180.dp)
        .graphicsLayer {
          val s = p[1].value * (1f + 0.08f * sin(PI.toFloat() * p[9].value))
          scaleX = s
          scaleY = s
        },
      contentAlignment = Alignment.Center,
    ) {
      Avatar(initials = "", shape = AvatarShape.Cookie9Sided, color = AvatarColor.Lavender, size = 180.dp)
      Icon(
        imageVector = Icons.Rounded.Lock,
        contentDescription = null,
        tint = InitialsColor,
        modifier = Modifier.size(76.dp).graphicsLayer { alpha = 1f - p[8].value },
      )
      Icon(
        imageVector = Icons.Rounded.LockOpen,
        contentDescription = null,
        tint = InitialsColor,
        modifier = Modifier.size(76.dp).graphicsLayer { alpha = p[8].value },
      )
    }
    // Botó d'empremta amb vora de 6 dp del color de fons
    Box(
      modifier = Modifier
        .offset(215.dp, 180.dp)
        .size(92.dp),
      contentAlignment = Alignment.Center,
    ) {
      Box(
        modifier = Modifier
          .size(80.dp)
          .graphicsLayer {
            val ringProgress = p[3].value
            val s = 1f + 0.9f * ringProgress
            scaleX = s
            scaleY = s
            alpha = if (ringProgress <= 0f) 0f else 0.55f * (1f - ringProgress)
          }
          .border(3.dp, scheme.primary, FingerprintShape),
      )
      Box(
        modifier = Modifier
          .size(92.dp)
          .graphicsLayer {
            scaleX = p[2].value
            scaleY = p[2].value
          }
          .clip(RoundedCornerShape(34.dp))
          .background(scheme.surface)
          .padding(6.dp)
          .clip(FingerprintShape)
          .background(buttonColor),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Rounded.Fingerprint,
          contentDescription = null,
          tint = buttonContent,
          modifier = Modifier.size(40.dp).graphicsLayer {
            val s = 1f - 0.4f * p[6].value
            scaleX = s
            scaleY = s
            alpha = 1f - p[6].value
          },
        )
        Icon(
          imageVector = Icons.Rounded.Check,
          contentDescription = null,
          tint = buttonContent,
          modifier = Modifier.size(40.dp).graphicsLayer {
            scaleX = p[7].value
            scaleY = p[7].value
            alpha = p[7].value.coerceIn(0f, 1f)
          },
        )
        Box(
          modifier = Modifier
            .width(40.dp)
            .height(3.dp)
            .graphicsLayer {
              translationY = (p[4].value * 2f - 1f) * 20.dp.toPx()
              alpha = sin(PI.toFloat() * p[4].value) * (1f - p[5].value)
            }
            .background(buttonContent),
        )
      }
    }
    Row(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .offset(y = 300.dp)
        .graphicsLayer {
          alpha = p[10].value
          translationY = (1f - p[10].value) * 16.dp.toPx()
        },
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      InfoPill(Icons.Rounded.CloudOff, stringResource(R.string.onb_no_cloud))
      InfoPill(Icons.Rounded.NoAccounts, stringResource(R.string.onb_no_accounts))
    }
  }
}

@Composable
private fun InfoPill(icon: ImageVector, text: String) {
  Row(
    modifier = Modifier
      .clip(CircleShape)
      .background(MaterialTheme.colorScheme.surfaceContainerHigh)
      .padding(horizontal = 14.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
    Spacer(Modifier.width(8.dp))
    Text(text = text, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
  }
}
