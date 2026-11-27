package com.alorma.contactnotes.designsystem.expressive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
