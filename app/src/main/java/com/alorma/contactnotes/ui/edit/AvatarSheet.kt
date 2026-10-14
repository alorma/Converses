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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alorma.contactnotes.R
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

/** Avatar de la fila del nom: amb Pro és un botó amb una insígnia d'edició que obre el full «Avatar». */
@Composable
internal fun NameAvatar(
  name: String,
  shape: AvatarShape,
  color: AvatarColor,
  editable: Boolean,
  onClick: () -> Unit,
  badgeBorder: Color,
) {
  val description = stringResource(R.string.avatar_edit_cd)
  // El clic es retalla amb la forma de l'avatar, no amb un cercle: si no, es mengen les puntes de la forma
  val avatarShape = shape.asShape()
  Box(modifier = Modifier.size(56.dp)) {
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .then(
            if (editable) {
              Modifier
                .clip(avatarShape)
                .selectable(selected = false, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description }
            } else {
              Modifier
            },
          ),
    ) {
      EditorAvatar(name = name, shape = shape, color = color, size = 56.dp, initialsSize = 20)
    }
    if (editable) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarSheet(
  name: String,
  shape: AvatarShape?,
  color: AvatarColor?,
  onShape: (AvatarShape) -> Unit,
  onColor: (AvatarColor?) -> Unit,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
  ) {
    AvatarSheetContent(name, shape, color, onShape, onColor, onDone = onDismiss)
  }
}

/** Contingut del full «Avatar»; el `ModalBottomSheet` és una finestra a part. */
@Composable
fun AvatarSheetContent(
  name: String,
  shape: AvatarShape?,
  color: AvatarColor?,
  onShape: (AvatarShape) -> Unit,
  onColor: (AvatarColor?) -> Unit,
  onDone: () -> Unit,
) {
  val resolvedShape = shape ?: AvatarShape.Standard
  val resolvedColor = color ?: AvatarColor.forName(name)
  val shapeName = stringResource(shapeNameRes(resolvedShape))
  val colorName = stringResource(colorNameRes(resolvedColor.name))
  val summary =
    if (color ==
      null
    ) {
      stringResource(R.string.avatar_summary_auto, shapeName, colorName)
    } else {
      "$shapeName · $colorName"
    }

  Column(
    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 28.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
      EditorAvatar(
        name = name,
        shape = resolvedShape,
        color = resolvedColor,
        size = 88.dp,
        initialsSize = 30,
        blankPlaceholder = false,
      )
      Column {
        Text(
          stringResource(R.string.avatar_sheet_title),
          style = SheetTitleStyle,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          summary,
          style = MaterialTheme.typography.bodySmall,
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    SheetSection(stringResource(R.string.avatar_shape)) {
      // Dues files de 3: amb 6 formes en una fila les etiquetes no hi caben
      Column(modifier = Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AvatarShape.entries.chunked(ShapesPerRow).forEach { row ->
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEachIndexed { index, option ->
              ShapeButton(
                shape = option,
                selected = option == resolvedShape,
                first = index == 0,
                last = index == row.lastIndex,
                onClick = { onShape(option) },
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
        Swatch(color = null, selected = color == null, onClick = { onColor(null) })
        AvatarColor.entries.forEach { option ->
          Swatch(color = option, selected = color == option, onClick = { onColor(option) })
        }
      }
    }

    Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(56.dp), shape = CircleShape) {
      Text(stringResource(R.string.avatar_done), fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
  Column(
    modifier =
      modifier
        .height(84.dp)
        .clip(buttonShape)
        .background(if (selected) scheme.primaryContainer else scheme.surfaceContainerLowest)
        .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
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
      text = stringResource(shapeNameRes(shape)),
      fontSize = 12.sp,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
      color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
    )
  }
}

/** Mostra de 30 dp dins d'un anell de 40 dp; `null` és «Automàtic». */
@Composable
private fun Swatch(
  color: AvatarColor?,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val scheme = MaterialTheme.colorScheme
  val description = stringResource(if (color == null) R.string.avatar_color_auto else colorNameRes(color.name))
  Box(
    modifier =
      Modifier
        .size(40.dp)
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
}
