package com.alorma.contactnotes.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.alorma.contactnotes.designsystem.theme.isDarkTheme

object GroupedListDefaults {
  val OuterCorner = 24.dp
  val InnerCorner = 6.dp
  val ItemSpacing = 2.dp

  val containerColor: Color
    @Composable get() = if (isDarkTheme) {
      MaterialTheme.colorScheme.surfaceContainer
    } else {
      MaterialTheme.colorScheme.surfaceContainerLowest
    }
}

/** Cantonades segons la posició: 24 dp als extrems del grup, 6 dp entre files. */
fun groupedItemShape(index: Int, count: Int): RoundedCornerShape {
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

/** Variant per a `LazyColumn`; el separador de 2 dp s'ha de donar amb `Arrangement.spacedBy`. */
fun <T> LazyListScope.groupedItems(
  items: List<T>,
  key: ((T) -> Any)? = null,
  content: @Composable (item: T) -> Unit,
) {
  itemsIndexed(items, key = key?.let { k -> { _, item -> k(item) } }) { index, item ->
    Surface(
      shape = groupedItemShape(index, items.size),
      color = GroupedListDefaults.containerColor,
    ) {
      content(item)
    }
  }
}
