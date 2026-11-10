package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.alorma.contactnotes.designsystem.theme.isDarkTheme

object GroupedListDefaults {
  val OuterCorner = 24.dp
  val InnerCorner = 6.dp
  val ItemSpacing = 2.dp

  val containerColor: Color
    @Composable get() =
      if (isDarkTheme) {
        MaterialTheme.colorScheme.surfaceContainer
      } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
      }
}

/** Cantonades segons la posició: 24 dp als extrems del grup, 6 dp entre files. */
fun groupedItemShape(
  index: Int,
  count: Int,
): RoundedCornerShape {
  val top = if (index == 0) GroupedListDefaults.OuterCorner else GroupedListDefaults.InnerCorner
  val bottom = if (index == count - 1) GroupedListDefaults.OuterCorner else GroupedListDefaults.InnerCorner
  return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
fun <T> GroupedList(
  items: List<T>,
  modifier: Modifier = Modifier,
  content: @Composable (item: T) -> Unit,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(GroupedListDefaults.ItemSpacing),
  ) {
    items.forEachIndexed { index, item ->
      Surface(
        shape = groupedItemShape(index, items.size),
        color = GroupedListDefaults.containerColor,
      ) {
        content(item)
      }
    }
  }
}

private const val GroupedItemContentType = "grouped-item"

/**
 * Variant per a `LazyColumn`; el separador de 2 dp s'ha de donar amb `Arrangement.spacedBy`.
 *
 * @param selected la fila oberta al panell del costat (pantalles grans): `primaryContainer` i les quatre cantonades
 *   de 24 dp, sigui on sigui del grup.
 */
fun <T> LazyListScope.groupedItems(
  items: List<T>,
  key: ((T) -> Any)? = null,
  selected: (T) -> Boolean = { false },
  content: @Composable (item: T) -> Unit,
) {
  itemsIndexed(
    items,
    key = key?.let { k -> { _, item -> k(item) } },
    contentType = { _, _ -> GroupedItemContentType },
  ) { index, item ->
    if (selected(item)) {
      Surface(
        shape = RoundedCornerShape(GroupedListDefaults.OuterCorner),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
      ) {
        content(item)
      }
    } else {
      Surface(
        shape = groupedItemShape(index, items.size),
        color = GroupedListDefaults.containerColor,
      ) {
        content(item)
      }
    }
  }
}
