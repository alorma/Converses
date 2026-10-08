package com.alorma.contactnotes.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Una dada fixada: etiqueta i valor. */
data class FactItem(val label: String, val value: String)

/**
 * Dades d'una persona en un únic bloc plegable, sense títol: plegat només mostra la primera dada
 * (amb la pastilla «+N» si n'hi ha més); desplegat, la resta baixen com a files connectades.
 * Amb una sola dada la fila no és clicable.
 */
@Composable
fun CollapsibleFacts(
  facts: List<FactItem>,
  expanded: Boolean,
  onToggle: () -> Unit,
  lessLabel: String,
  toggleDescription: String,
  modifier: Modifier = Modifier,
) {
  if (facts.isEmpty()) return
  val first = facts.first()
  val rest = facts.drop(1)
  val expandable = rest.isNotEmpty()
  val open = expanded && expandable
  val bottomCorner by animateDpAsState(
    if (open) GroupedListDefaults.InnerCorner else GroupedListDefaults.OuterCorner,
    label = "factsCorner",
  )
  val scheme = MaterialTheme.colorScheme

  Column(
    modifier = modifier.fillMaxWidth().animateContentSize(),
    verticalArrangement = Arrangement.spacedBy(GroupedListDefaults.ItemSpacing),
  ) {
    Surface(
      onClick = onToggle,
      enabled = expandable,
      shape = RoundedCornerShape(
        topStart = GroupedListDefaults.OuterCorner,
        topEnd = GroupedListDefaults.OuterCorner,
        bottomStart = bottomCorner,
        bottomEnd = bottomCorner,
      ),
      color = GroupedListDefaults.containerColor,
      modifier = Modifier
        .fillMaxWidth()
        .then(
          if (expandable) {
            Modifier.semantics {
              role = Role.Button
              contentDescription = toggleDescription
              if (open) collapse { onToggle(); true } else expand { onToggle(); true }
            }
          } else {
            Modifier
          },
        ),
    ) {
      Row(
        modifier = Modifier
          .heightIn(min = 56.dp)
          .padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Column(modifier = Modifier.weight(1f)) {
          FactLabel(first.label)
          Text(
            text = first.value,
            fontSize = 15.sp,
            maxLines = if (open) Int.MAX_VALUE else 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        if (expandable) {
          Surface(shape = CircleShape, color = scheme.surfaceContainerHigh, modifier = Modifier.heightIn(min = 32.dp)) {
            Row(
              modifier = Modifier.padding(start = 12.dp, end = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              Text(
                text = if (open) lessLabel else "+${rest.size}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 6.dp),
              )
              Icon(
                imageVector = if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }
    }
    AnimatedVisibility(visible = open) {
      Column(verticalArrangement = Arrangement.spacedBy(GroupedListDefaults.ItemSpacing)) {
        rest.forEachIndexed { index, fact ->
          Surface(
            shape = RoundedCornerShape(
              topStart = GroupedListDefaults.InnerCorner,
              topEnd = GroupedListDefaults.InnerCorner,
              bottomStart = if (index == rest.lastIndex) GroupedListDefaults.OuterCorner else GroupedListDefaults.InnerCorner,
              bottomEnd = if (index == rest.lastIndex) GroupedListDefaults.OuterCorner else GroupedListDefaults.InnerCorner,
            ),
            color = GroupedListDefaults.containerColor,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
              FactLabel(fact.label)
              Text(text = fact.value, fontSize = 15.sp)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FactLabel(label: String) {
  Text(
    text = label.uppercase(),
    fontSize = 12.sp,
    fontWeight = FontWeight.Bold,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}
