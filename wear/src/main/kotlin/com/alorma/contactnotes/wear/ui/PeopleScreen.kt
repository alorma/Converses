package com.alorma.contactnotes.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhonelinkOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.wear.R
import com.alorma.contactnotes.wear.components.PersonRow
import com.alorma.contactnotes.wear.components.groupedItemShape
import com.alorma.contactnotes.wear.theme.ListTitleStyle
import com.alorma.contactnotes.wear.theme.RowSubtitleStyle
import com.alorma.contactnotes.wear.theme.WearColors

/** Una persona a la llista. Si és protegida, `lastEntry` ha de ser sempre `null`: no se'n mostra res més que el nom. */
data class PersonItem(
  val id: Long,
  val name: String,
  val initials: String,
  val color: AvatarColor,
  val lastEntry: String?,
  val isProtected: Boolean,
)

/** Pantalles 1 i 8: la llista de persones; amb `dataAgeMinutes` informat, l'avís «Mòbil no connectat». */
@Composable
fun PeopleContent(
  people: List<PersonItem>,
  onPersonClick: (PersonItem) -> Unit,
  modifier: Modifier = Modifier,
  dataAgeMinutes: Int? = null,
) {
  val state = rememberTransformingLazyColumnState()
  val spec = rememberTransformationSpec()
  ScreenScaffold(scrollState = state, modifier = modifier) { padding ->
    TransformingLazyColumn(
      state = state,
      contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 36.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      item {
        Text(
          text = stringResource(R.string.app_name),
          style = ListTitleStyle,
          color = WearColors.OnSurface,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
      }
      if (dataAgeMinutes != null) {
        item { PhoneOfflineBanner(dataAgeMinutes = dataAgeMinutes, modifier = Modifier.padding(bottom = 4.dp)) }
      }
      items(people.size, key = { people[it].id }) { index ->
        val person = people[index]
        PersonRow(
          name = person.name,
          initials = person.initials,
          color = person.color,
          lastEntry = person.lastEntry,
          isProtected = person.isProtected,
          shape = groupedItemShape(index, people.size),
          onClick = { onPersonClick(person) },
          modifier = Modifier.scrollTransformed(this, spec),
        )
      }
    }
  }
}

@Composable
private fun PhoneOfflineBanner(
  dataAgeMinutes: Int,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(WearColors.SurfaceCardHigh)
        .padding(horizontal = 14.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      Icons.Rounded.PhonelinkOff,
      contentDescription = null,
      tint = WearColors.OnSurfaceVariant,
      modifier = Modifier.size(22.dp),
    )
    Column {
      Text(
        text = stringResource(R.string.phone_not_connected),
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = WearColors.OnSurface,
      )
      Text(
        text = stringResource(R.string.data_from_minutes, dataAgeMinutes),
        style = RowSubtitleStyle,
        color = WearColors.OnSurfaceVariant,
      )
    }
  }
}

internal val SamplePeople =
  listOf(
    PersonItem(1, "Kael", "K", AvatarColor.Sky, "Fa 2 h", isProtected = false),
    PersonItem(2, "Luna Roja", "LR", AvatarColor.Lavender, "Ahir", isProtected = false),
    PersonItem(3, "Marta", "M", AvatarColor.Peach, null, isProtected = true),
  )

@WearScreenPreviews
@Composable
fun PeoplePreview() = WearPreview { PeopleContent(people = SamplePeople, onPersonClick = {}) }

@WearScreenPreviews
@Composable
fun PeoplePhoneOfflinePreview() =
  WearPreview { PeopleContent(people = SamplePeople.take(2), onPersonClick = {}, dataAgeMinutes = 14) }
