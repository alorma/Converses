package com.alorma.contactnotes.wear.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.alorma.contactnotes.wear.theme.RowTitleStyle
import com.alorma.contactnotes.wear.theme.WearColors

/** Una opció del grup connectat del «Qui»: icona o avatar al davant i l'etiqueta. */
class WhoOption(
  val label: String,
  val leading: @Composable () -> Unit,
  /** L'avatar ocupa 28 dp i la icona 24: la de la persona porta menys marge a l'esquerra. */
  val leadingIsAvatar: Boolean = false,
)

/** Grup connectat de 3 opcions amb una de seleccionada (separació 2, cantonades 24 fora i 6 dins). */
@Composable
fun WhoGroup(
  options: List<WhoOption>,
  selectedIndex: Int,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GroupedDefaults.ItemSpacing)) {
    options.forEachIndexed { index, option ->
      val selected = index == selectedIndex
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(groupedItemShape(index, options.size))
            .background(if (selected) WearColors.PrimaryContainer else WearColors.SurfaceCardHigh)
            .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(index) })
            .padding(start = if (option.leadingIsAvatar) 10.dp else 14.dp, end = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        option.leading()
        Text(text = option.label, style = RowTitleStyle, color = WearColors.OnSurface, modifier = Modifier.weight(1f))
        if (selected) {
          Icon(
            Icons.Rounded.Check,
            contentDescription = null,
            tint = WearColors.OnSurface,
            modifier = Modifier.size(22.dp),
          )
        }
      }
    }
  }
}
