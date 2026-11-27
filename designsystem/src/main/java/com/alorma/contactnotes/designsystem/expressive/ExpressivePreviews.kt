@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.alorma.contactnotes.designsystem.expressive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alorma.contactnotes.designsystem.preview.ConversesPreviewSurface

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
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ShapeButton(
          onClick = {},
          shape = MaterialShapes.Cookie9Sided,
          size = 80.dp,
          containerColor = Color(0xFF8A3E22),
          contentColor = Color.White,
          contentDescription = "Edit",
        ) {
          Icon(Icons.Outlined.Edit, contentDescription = null)
        }
        ShapeButton(
          onClick = {},
          shape = MaterialShapes.Cookie9Sided,
          size = 52.dp,
          containerColor = Expressive.colors.accent,
          contentColor = Expressive.colors.onAccent,
          contentDescription = "Add person",
        ) {
          Icon(Icons.Outlined.PersonAdd, contentDescription = null)
        }
        ShapeButton(
          onClick = {},
          shape = MaterialShapes.Clover4Leaf,
          size = 92.dp,
          containerColor = Expressive.colors.accent,
          contentColor = Expressive.colors.onAccent,
          contentDescription = "Selected",
          selected = true,
        ) {
          Icon(Icons.Outlined.Edit, contentDescription = null)
        }
        ShapeButton(
          onClick = {},
          shape = MaterialShapes.Cookie9Sided,
          size = 48.dp,
          containerColor = Expressive.colors.neutral,
          contentColor = Expressive.colors.ink,
          contentDescription = "Neutral",
        ) {
          Icon(Icons.Outlined.Edit, contentDescription = null)
        }
      }
    }
  }
}
