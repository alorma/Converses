@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.alorma.contactnotes.designsystem.expressive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alorma.contactnotes.designsystem.components.Avatar
import com.alorma.contactnotes.designsystem.components.ExpressiveFloatingToolbar
import com.alorma.contactnotes.designsystem.components.ExpressiveToolbarDestination
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewSurface
import com.alorma.contactnotes.designsystem.theme.LocalDynamicSeed
import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.shared.AvatarShape

// Previews de les peces expressives amb dades fixes. Els tests de captures de `screenshotTest` les reutilitzen.

@Preview
@Composable
internal fun ExpressiveTypePreview() {
  ConversesPreviewSurface {
    ExpressiveTheme {
      val ink = Expressive.colors.ink
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Converses", style = ExpressiveType.display(34.sp), color = ink)
        Text("fa 2 h", style = ExpressiveType.display(44.sp), color = ink)
        Text(
          "Em va dir que no vindria, però al final va arribar el primer.",
          style = ExpressiveType.quote(27.sp),
          color = ink,
        )
        Text("JORDI PUIG · XAT", style = ExpressiveType.overline, color = ink)
        Text("Cos de text normal.", style = ExpressiveType.body, color = ink)
      }
    }
  }
}

@Preview
@Composable
internal fun WideTitleLongNamePreview() {
  ConversesPreviewSurface {
    ExpressiveTheme {
      WideTitle(
        text = "Maria Montserrat Puig i Ferrer",
        modifier = Modifier.width(360.dp),
        color = Expressive.colors.ink,
        maxSize = 44.sp,
      )
    }
  }
}

@Preview
@Composable
internal fun ShapeButtonPreview() {
  ConversesPreviewSurface {
    ExpressiveTheme {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ShapeButton(
          onClick = {},
          shape = ExpressiveShapes.Cookie9Sided,
          size = 80.dp,
          containerColor = Color(0xFF8A3E22),
          contentColor = Color.White,
          contentDescription = "Edit",
          icon = Icons.Outlined.Edit,
        )
        ShapeButton(
          onClick = {},
          shape = ExpressiveShapes.Cookie9Sided,
          size = 52.dp,
          containerColor = Expressive.colors.accent,
          contentColor = Expressive.colors.onAccent,
          contentDescription = "Add person",
          icon = Icons.Outlined.PersonAdd,
        )
        ShapeButton(
          onClick = {},
          shape = ExpressiveShapes.Clover4Leaf,
          size = 92.dp,
          containerColor = Expressive.colors.accent,
          contentColor = Expressive.colors.onAccent,
          contentDescription = "Selected",
          icon = Icons.Outlined.Edit,
          selected = true,
        )
        ShapeButton(
          onClick = {},
          shape = ExpressiveShapes.Cookie9Sided,
          size = 48.dp,
          containerColor = Expressive.colors.neutral,
          contentColor = Expressive.colors.ink,
          contentDescription = "Neutral",
          icon = Icons.Outlined.Edit,
        )
        ShapeButton(
          onClick = {},
          shape = ExpressiveShapes.Clover4Leaf,
          size = 40.dp,
          containerColor = Expressive.colors.accent,
          contentColor = Expressive.colors.onAccent,
          contentDescription = "Small",
          icon = Icons.Outlined.Edit,
        )
      }
    }
  }
}

@Preview
@Composable
internal fun ExpressiveFilterChipsPreview() {
  ConversesPreviewSurface {
    ExpressiveTheme {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ExpressiveFilterChip(selected = true, onClick = {}, label = "All", icon = Icons.Outlined.Groups)
        ExpressiveFilterChip(selected = false, onClick = {}, label = "Chat", icon = Icons.Outlined.ChatBubbleOutline)
      }
    }
  }
}

@Preview
@Composable
internal fun GroupHeaderPreview() {
  ConversesPreviewSurface {
    ExpressiveTheme {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GroupHeader(title = "Today", count = 3, countDescription = "3 people")
        GroupHeader(title = "Last week", count = 12, countDescription = "12 people")
      }
    }
  }
}

@Preview
@Composable
internal fun ExpressiveFloatingToolbarPreview() {
  ConversesPreviewSurface {
    ExpressiveTheme {
      ExpressiveFloatingToolbar(
        onFabClick = {},
        fabIcon = Icons.Outlined.Edit,
        fabContentDescription = "Quick note",
        modifier = Modifier.width(360.dp),
      ) {
        ExpressiveToolbarDestination(icon = Icons.Outlined.Groups, label = "People", selected = true, onClick = {})
        ExpressiveToolbarDestination(
          icon = Icons.Outlined.Inventory2,
          label = "Archived",
          selected = false,
          onClick = {},
        )
        ExpressiveToolbarDestination(icon = Icons.Outlined.Settings, label = "Settings", selected = false, onClick = {})
      }
    }
  }
}

/** La paleta dinàmica amb una llavor fixa (blau), com si el fons de pantalla fos blau; en clar i fosc segons el `uiMode`. */
@Preview
@Composable
internal fun DynamicExpressivePreview() {
  ConversesPreviewSurface {
    CompositionLocalProvider(LocalDynamicSeed provides Color(0xFF1A73E8)) {
      ExpressiveTheme {
        Surface(color = Expressive.colors.background) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Converses", style = ExpressiveType.display(34.sp), color = Expressive.colors.ink)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              ExpressiveFilterChip(selected = true, onClick = {}, label = "All", icon = Icons.Outlined.Groups)
              ExpressiveFilterChip(
                selected = false,
                onClick = {},
                label = "Chat",
                icon = Icons.Outlined.ChatBubbleOutline,
              )
            }
            GroupHeader(title = "Today", count = 3, countDescription = "3 people")
            ExpressiveFloatingToolbar(
              onFabClick = {},
              fabIcon = Icons.Outlined.Edit,
              fabContentDescription = "Quick note",
              modifier = Modifier.width(360.dp),
            ) {
              ExpressiveToolbarDestination(
                icon = Icons.Outlined.Groups,
                label = "People",
                selected = true,
                onClick = {},
              )
              ExpressiveToolbarDestination(
                icon = Icons.Outlined.Settings,
                label = "Settings",
                selected = false,
                onClick = {},
              )
            }
          }
        }
      }
    }
  }
}

/** Una persona sense color (o amb «Color de la persona» apagat) amb la paleta dinàmica blava: tot surt dels neutres. */
@Preview
@Composable
internal fun DynamicNeutralPersonPreview() {
  ConversesPreviewSurface {
    CompositionLocalProvider(LocalDynamicSeed provides Color(0xFF1A73E8)) {
      ExpressiveTheme {
        val palette = rememberPersonPalette(null)
        Surface(color = palette.detailBackground) {
          Row(
            modifier = Modifier.padding(16.dp).width(320.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = palette.surface, modifier = Modifier.weight(1f)) {
              Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Surface(shape = CircleShape, color = palette.avatar, modifier = Modifier.size(48.dp)) {}
                Column {
                  Text("MARTA SOLER", style = ExpressiveType.overline, color = palette.accent)
                  Text("I told her I can't make it", style = ExpressiveType.body, color = Expressive.colors.ink)
                }
              }
            }
            Surface(shape = CircleShape, color = palette.featured) {
              Text("1", color = palette.onFeatured, modifier = Modifier.padding(12.dp))
            }
          }
        }
      }
    }
  }
}

private const val FeaturedQuote = "I told her I can't make it on Friday, but we could try next week."

@Composable
private fun FeaturedCardSample(
  color: AvatarColor?,
  quote: String = FeaturedQuote,
) {
  ConversesPreviewSurface {
    ExpressiveTheme {
      Surface(color = Expressive.colors.background) {
        val palette = rememberPersonPalette(color)
        FeaturedCard(
          overline = "MARTA SOLER · CHAT · 20 MIN. AGO",
          quote = "“$quote”",
          palette = palette,
          onClick = {},
          onAdd = {},
          addDescription = "Add an entry for Marta",
          avatar = {
            Avatar(
              initials = "MS",
              shape = AvatarShape.Cookie9Sided,
              color = AvatarColor.Mint,
              size = 30.dp,
            )
          },
          modifier = Modifier.padding(16.dp).width(360.dp),
        )
      }
    }
  }
}

/** La targeta destacada amb una persona de color (menta); en clar i fosc segons el `uiMode`. */
@Preview
@Composable
internal fun FeaturedCardPreview() {
  FeaturedCardSample(AvatarColor.Mint)
}

/** Persona sense color: la targeta surt en tinta amb el text del fons. */
@Preview
@Composable
internal fun FeaturedCardNeutralPreview() {
  FeaturedCardSample(null)
}

/** Una cita molt llarga: es talla amb «…» i no arriba a la galeta (es veu bé amb la lletra al doble). */
@Preview
@Composable
internal fun FeaturedCardLongQuotePreview() {
  FeaturedCardSample(
    AvatarColor.Mint,
    quote =
      "I told her I can't make it on Friday, but we could try next week, or the one after that, " +
        "or whenever the schedule finally calms down a bit, because honestly this month has been " +
        "completely out of control and I don't want to promise anything I can't keep, you know?",
  )
}
