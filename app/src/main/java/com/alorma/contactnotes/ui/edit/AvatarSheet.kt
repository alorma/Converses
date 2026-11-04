package com.alorma.contactnotes.ui.edit

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.designsystem.components.ProBadge
import com.alorma.contactnotes.designsystem.components.ProPill
import com.alorma.contactnotes.designsystem.components.asShape
import com.alorma.contactnotes.designsystem.components.color
import com.alorma.contactnotes.designsystem.theme.SheetTitleStyle
import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.shared.AvatarShape
import com.alorma.contactnotes.shared.initialsOf
import com.alorma.contactnotes.ui.people.colorNameRes

private const val ShapesPerRow = 3

private val InitialsColor = Color(0xFF1E1D24)

private fun shapeNameRes(shape: AvatarShape): Int =
  when (shape) {
    AvatarShape.Cookie9Sided -> R.string.shape_cookie
    AvatarShape.Clover4Leaf -> R.string.shape_clover
    AvatarShape.Clover8Leaf -> R.string.shape_flower
    AvatarShape.Sunny -> R.string.shape_sunny
    AvatarShape.Ghostish -> R.string.shape_ghost
    AvatarShape.PuffyDiamond -> R.string.shape_pillow
  }

/**
 * Avatar del formulari i del full. Les inicials i el color canvien mentre s'escriu el nom; amb el nom buit surt un «?»
 * sobre un fons neutre (si [blankPlaceholder]).
 */
@Composable
internal fun EditorAvatar(
  name: String,
  shape: AvatarShape,
  color: AvatarColor,
  size: Dp,
  initialsSize: Int,
  modifier: Modifier = Modifier,
  blankPlaceholder: Boolean = true,
) {
  val blank = blankPlaceholder && name.isBlank()
  val background by animateColorAsState(
    targetValue = if (blank) MaterialTheme.colorScheme.surfaceContainerHigh else color.color,
    animationSpec = tween(300),
    label = "avatarColor",
  )
  Box(
    modifier = modifier.size(size).clip(shape.asShape()).background(background),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = initialsOf(name),
      color = InitialsColor,
      fontWeight = FontWeight.ExtraBold,
      fontSize = with(LocalDensity.current) { initialsSize.dp.toSp() },
    )
  }
}

/**
 * Avatar de la fila del nom: és un botó que obre el full «Avatar». Amb Pro porta la insígnia d'edició; sense Pro
 * ([locked]), la píndola PRO (11a).
 */
@Composable
internal fun NameAvatar(
  name: String,
  shape: AvatarShape,
  color: AvatarColor,
  onClick: () -> Unit,
  badgeBorder: Color,
  locked: Boolean = false,
) {
  val description = stringResource(if (locked) R.string.avatar_edit_locked_cd else R.string.avatar_edit_cd)
  // El clic es retalla amb la forma de l'avatar, no amb un cercle: si no, es mengen les puntes de la forma
  val avatarShape = shape.asShape()
  Box(modifier = Modifier.size(56.dp)) {
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .clip(avatarShape)
          .selectable(selected = false, role = Role.Button, onClick = onClick)
          .semantics { contentDescription = description },
    ) {
      EditorAvatar(name = name, shape = shape, color = color, size = 56.dp, initialsSize = 20)
    }
    if (locked) {
      ProPill(
        text = stringResource(R.string.pro_badge),
        borderColor = badgeBorder,
        modifier = Modifier.align(Alignment.BottomEnd).offset(x = 10.dp, y = 6.dp),
      )
    } else {
      Box(
        modifier =
          Modifier
            .align(Alignment.BottomEnd)
            .offset(x = 4.dp, y = 4.dp)
            .size(24.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .border(2.dp, badgeBorder, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          Icons.Rounded.Edit,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.size(13.dp),
        )
      }
    }
  }
}

/**
 * Estat del full «Avatar» sense Pro (11b–11d). Galeta i Automàtic són lliures, i també la forma i el color desats (si
 * ha deixat de ser Pro, els pot mantenir). Tocar qualsevol altra opció només la prova: no es desa mai.
 */
data class AvatarSheetLock(
  val savedShape: AvatarShape? = null,
  val savedColor: AvatarColor? = null,
  val trialShape: AvatarShape? = null,
  val trialColor: AvatarColor? = null,
) {
  val hasTrial: Boolean get() = trialShape != null || trialColor != null

  fun isFree(shape: AvatarShape): Boolean = shape == AvatarShape.Standard || shape == savedShape

  fun isFree(color: AvatarColor?): Boolean = color == null || color == savedColor

  /** La forma o el color de l'editor són una tria Pro que es manté després d'una baixada (11d). */
  fun keepsProChoice(
    shape: AvatarShape?,
    color: AvatarColor?,
  ): Boolean = (shape ?: AvatarShape.Standard) != AvatarShape.Standard || color != null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarSheet(
  name: String,
  shape: AvatarShape?,
  color: AvatarColor?,
  onShape: (AvatarShape) -> Unit,
  onColor: (AvatarColor?) -> Unit,
  /** «Fet». */
  onDone: () -> Unit,
  /** Scrim o enrere: amb una prova, equival a «Descarta la prova». */
  onDismiss: () -> Unit,
  lock: AvatarSheetLock? = null,
  onTrialShape: (AvatarShape) -> Unit = {},
  onTrialColor: (AvatarColor) -> Unit = {},
  onUnlock: () -> Unit = {},
  onDiscardTrial: () -> Unit = {},
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
  ) {
    AvatarSheetContent(
      name = name,
      shape = shape,
      color = color,
      onShape = onShape,
      onColor = onColor,
      onDone = onDone,
      lock = lock,
      onTrialShape = onTrialShape,
      onTrialColor = onTrialColor,
      onUnlock = onUnlock,
      onDiscardTrial = onDiscardTrial,
    )
  }
}

/** Contingut del full «Avatar»; el `ModalBottomSheet` és una finestra a part. Sense Pro, [lock] no és `null`. */
@Composable
fun AvatarSheetContent(
  name: String,
  shape: AvatarShape?,
  color: AvatarColor?,
  onShape: (AvatarShape) -> Unit,
  onColor: (AvatarColor?) -> Unit,
  onDone: () -> Unit,
  lock: AvatarSheetLock? = null,
  onTrialShape: (AvatarShape) -> Unit = {},
  onTrialColor: (AvatarColor) -> Unit = {},
  /** «Desbloqueja amb Pro»: obre la pantalla de Pro (origen `People`). */
  onUnlock: () -> Unit = {},
  onDiscardTrial: () -> Unit = {},
) {
  val trial = lock?.hasTrial == true
  // L'avatar gran i la selecció mostren la prova; l'avatar de l'editor, no
  val shownShape = lock?.trialShape ?: shape ?: AvatarShape.Standard
  val selectedColor = lock?.trialColor ?: color
  val shownColor = selectedColor ?: AvatarColor.forName(name)
  val shapeName = stringResource(shapeNameRes(shownShape))
  val colorName = stringResource(colorNameRes(shownColor.name))
  val summary =
    when {
      trial -> stringResource(R.string.avatar_trial_summary, shapeName, colorName)
      color == null -> stringResource(R.string.avatar_summary_auto, shapeName, colorName)
      else -> "$shapeName · $colorName"
    }

  Column(
    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 28.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
      EditorAvatar(
        name = name,
        shape = shownShape,
        color = shownColor,
        size = 88.dp,
        initialsSize = 30,
        blankPlaceholder = false,
      )
      Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            stringResource(R.string.avatar_sheet_title),
            style = SheetTitleStyle,
            color = MaterialTheme.colorScheme.onSurface,
          )
          if (lock != null) {
            ProBadge(
              text = stringResource(R.string.pro_badge),
              contentDescription = stringResource(R.string.pro_description),
            )
          }
        }
        Text(
          summary,
          style = MaterialTheme.typography.bodySmall,
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = if (lock != null) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier,
        )
      }
    }

    if (lock != null) {
      when {
        trial ->
          AvatarProCard(
            icon = Icons.Rounded.WorkspacePremium,
            title = stringResource(R.string.avatar_trial_title),
            text = stringResource(R.string.avatar_trial_text),
            highlighted = true,
          )
        lock.keepsProChoice(shape, color) ->
          AvatarProCard(
            icon = Icons.Rounded.Lock,
            title = stringResource(R.string.avatar_kept_title),
            text = stringResource(R.string.avatar_kept_text),
          )
        else ->
          AvatarProCard(
            icon = Icons.Rounded.Palette,
            title = stringResource(R.string.avatar_pro_info_title),
            text = stringResource(R.string.avatar_pro_info_text),
          )
      }
    }

    SheetSection(stringResource(R.string.avatar_shape)) {
      // Dues files de 3: amb 6 formes en una fila les etiquetes no hi caben
      Column(modifier = Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AvatarShape.entries.chunked(ShapesPerRow).forEach { row ->
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEachIndexed { index, option ->
              val proLocked = lock != null && !lock.isFree(option)
              ShapeButton(
                shape = option,
                selected = option == shownShape,
                first = index == 0,
                last = index == row.lastIndex,
                proLocked = proLocked,
                onClick = { if (proLocked) onTrialShape(option) else onShape(option) },
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }
    }

    SheetSection(stringResource(R.string.avatar_color)) {
      Row(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Swatch(color = null, selected = selectedColor == null, proLocked = false, onClick = { onColor(null) })
        AvatarColor.entries.forEach { option ->
          val proLocked = lock != null && !lock.isFree(option)
          Swatch(
            color = option,
            selected = selectedColor == option,
            proLocked = proLocked,
            onClick = { if (proLocked) onTrialColor(option) else onColor(option) },
          )
        }
      }
    }

    if (trial) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Button(onClick = onUnlock, modifier = Modifier.fillMaxWidth().height(56.dp), shape = CircleShape) {
          Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(Modifier.width(8.dp))
          Text(stringResource(R.string.avatar_unlock_pro), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        TextButton(onClick = onDiscardTrial, modifier = Modifier.fillMaxWidth().height(48.dp)) {
          Text(stringResource(R.string.avatar_discard_trial), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
      }
    } else {
      Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(56.dp), shape = CircleShape) {
        Text(stringResource(R.string.avatar_done), fontWeight = FontWeight.Bold, fontSize = 16.sp)
      }
    }
  }
}

/** Targeta del full sense Pro (11b, 11c, 11d); en prova ([highlighted]) va sobre `primaryContainer`. */
@Composable
private fun AvatarProCard(
  icon: ImageVector,
  title: String,
  text: String,
  highlighted: Boolean = false,
) {
  val scheme = MaterialTheme.colorScheme
  val content = if (highlighted) scheme.onPrimaryContainer else scheme.onSurface
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(if (highlighted) scheme.primaryContainer else scheme.surfaceContainerLowest)
        .padding(horizontal = 16.dp, vertical = 14.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = content)
      Text(
        text,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = if (highlighted) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun SheetSection(
  title: String,
  content: @Composable () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    content()
  }
}

@Composable
private fun ShapeButton(
  shape: AvatarShape,
  selected: Boolean,
  first: Boolean,
  last: Boolean,
  proLocked: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scheme = MaterialTheme.colorScheme
  val outer = 20.dp
  val inner = 6.dp
  val buttonShape =
    if (selected) {
      RoundedCornerShape(outer)
    } else {
      RoundedCornerShape(
        topStart = if (first) outer else inner,
        bottomStart = if (first) outer else inner,
        topEnd = if (last) outer else inner,
        bottomEnd = if (last) outer else inner,
      )
    }
  val label = stringResource(shapeNameRes(shape))
  // El cadenat és decoratiu: l'estat va a la descripció («Trèvol, Pro»)
  val description = if (proLocked) stringResource(R.string.avatar_option_pro, label) else label
  Box(
    modifier =
      modifier
        .height(84.dp)
        .clip(buttonShape)
        .background(if (selected) scheme.primaryContainer else scheme.surfaceContainerLowest)
        .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
        .semantics { contentDescription = description },
  ) {
    Column(
      modifier = Modifier.fillMaxSize().clearAndSetSemantics {},
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Box(
        modifier =
          Modifier
            .size(32.dp)
            .clip(shape.asShape())
            .background(if (selected) scheme.onPrimaryContainer else scheme.outline),
      )
      Spacer(Modifier.height(6.dp))
      Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
      )
    }
    if (proLocked) {
      Icon(
        Icons.Rounded.Lock,
        contentDescription = null,
        tint = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
        modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp).size(16.dp),
      )
    }
  }
}

/** Mostra de 30 dp dins d'un anell de 40 dp; `null` és «Automàtic». Sense Pro, els colors Pro porten un cadenat. */
@Composable
private fun Swatch(
  color: AvatarColor?,
  selected: Boolean,
  proLocked: Boolean,
  onClick: () -> Unit,
) {
  val scheme = MaterialTheme.colorScheme
  val name = stringResource(if (color == null) R.string.avatar_color_auto else colorNameRes(color.name))
  val description = if (proLocked) stringResource(R.string.avatar_option_pro, name) else name
  Box(modifier = Modifier.size(40.dp)) {
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .clip(CircleShape)
          .then(if (selected) Modifier.border(BorderStroke(2.dp, scheme.primary), CircleShape) else Modifier)
          .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
          .semantics { contentDescription = description },
      contentAlignment = Alignment.Center,
    ) {
      Box(
        modifier =
          Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(color?.color ?: scheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
      ) {
        when {
          color == null ->
            Icon(
              Icons.Rounded.Autorenew,
              contentDescription = null,
              tint = scheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
          selected ->
            Icon(
              Icons.Rounded.Check,
              contentDescription = null,
              tint = InitialsColor,
              modifier = Modifier.size(18.dp),
            )
        }
      }
    }
    if (proLocked) {
      // Cercle del color del full (el de `ModalBottomSheet`) perquè el cadenat es llegeixi sobre qualsevol color
      Box(
        modifier =
          Modifier
            .align(Alignment.BottomEnd)
            .offset(x = 2.dp, y = 2.dp)
            .size(18.dp)
            .clip(CircleShape)
            .background(scheme.surfaceContainerLow),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          Icons.Rounded.Lock,
          contentDescription = null,
          tint = scheme.onSurfaceVariant,
          modifier = Modifier.size(12.dp),
        )
      }
    }
  }
}
