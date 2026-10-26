package com.alorma.contactnotes.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PhonelinkOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.shared.AvatarShape
import com.alorma.contactnotes.wear.R
import com.alorma.contactnotes.wear.components.PersonRow
import com.alorma.contactnotes.wear.theme.ListTitleStyle
import com.alorma.contactnotes.wear.theme.PersonNameStyle
import com.alorma.contactnotes.wear.theme.WearColors

/** Una persona a la llista. Si és protegida, `lastEntry` ha de ser sempre `null`: no se'n mostra res més que el nom. */
data class PersonItem(
  val id: Long,
  val name: String,
  val initials: String,
  val color: AvatarColor,
  val avatarShape: AvatarShape,
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
  discardedNotes: Int = 0,
  onDiscardedNotesClick: () -> Unit = {},
) {
  val state = rememberTransformingLazyColumnState()
  val spec = rememberTransformationSpec()
  ScreenScaffold(scrollState = state, modifier = modifier) { padding ->
    TransformingLazyColumn(
      state = state,
      contentPadding = padding,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      item { WearListTitle(text = stringResource(R.string.app_name), style = ListTitleStyle) }
      if (dataAgeMinutes != null) {
        item {
          PhoneOfflineBanner(
            dataAgeMinutes = dataAgeMinutes,
            modifier = Modifier.transformedHeight(this, spec),
            transformation = SurfaceTransformation(spec),
          )
        }
      }
      if (discardedNotes > 0) {
        item {
          DiscardedNotesBanner(
            count = discardedNotes,
            onClick = onDiscardedNotesClick,
            modifier = Modifier.transformedHeight(this, spec),
            transformation = SurfaceTransformation(spec),
          )
        }
      }
      items(people.size, key = { people[it].id }) { index ->
        val person = people[index]
        PersonRow(
          name = person.name,
          initials = person.initials,
          color = person.color,
          avatarShape = person.avatarShape,
          lastEntry = person.lastEntry,
          isProtected = person.isProtected,
          onClick = { onPersonClick(person) },
          modifier = Modifier.transformedHeight(this, spec),
          transformation = SurfaceTransformation(spec),
        )
      }
    }
  }
}

@Composable
private fun DiscardedNotesBanner(
  count: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  transformation: SurfaceTransformation? = null,
) {
  OutlinedButton(
    onClick = onClick,
    modifier = modifier.fillMaxWidth(),
    transformation = transformation,
    icon = { Icon(Icons.Rounded.Info, contentDescription = null) },
    secondaryLabel = { Text(text = stringResource(R.string.discarded_notes_hint)) },
  ) {
    Text(text = pluralStringResource(R.plurals.discarded_notes_title, count, count))
  }
}

/** Avís que no es pot tocar: una `TitleCard` nativa amb la icona, el títol i l'antiguitat de les dades. */
@Composable
private fun PhoneOfflineBanner(
  dataAgeMinutes: Int,
  modifier: Modifier = Modifier,
  transformation: SurfaceTransformation? = null,
) {
  TitleCard(
    modifier = modifier.fillMaxWidth(),
    transformation = transformation,
    title = {
      Icon(
        Icons.Rounded.PhonelinkOff,
        contentDescription = null,
        modifier = Modifier.padding(end = 4.dp).size(18.dp),
      )
      Text(text = stringResource(R.string.phone_not_connected))
    },
  ) {
    Text(
      text =
        when {
          dataAgeMinutes < 1 -> stringResource(R.string.data_just_now)
          dataAgeMinutes < 60 -> stringResource(R.string.data_from_minutes, dataAgeMinutes)
          else -> stringResource(R.string.data_from_hours, dataAgeMinutes / 60)
        },
    )
  }
}

internal val SamplePeople =
  listOf(
    PersonItem(1, "Kael", "K", AvatarColor.Sky, AvatarShape.Cookie9Sided, "Fa 2 h", isProtected = false),
    PersonItem(2, "Luna Roja", "LR", AvatarColor.Lavender, AvatarShape.Clover4Leaf, "Ahir", isProtected = false),
    PersonItem(3, "Marta", "M", AvatarColor.Peach, AvatarShape.Sunny, null, isProtected = true),
  )

@WearScreenPreviews
@Composable
fun PeoplePreview() = WearPreview { PeopleContent(people = SamplePeople, onPersonClick = {}) }

/** Play va rebutjar l'app per un nom llarg tallat a la vora rodona: aquesta captura ho vigila. */
@WearScreenPreviews
@Composable
fun PeopleLongNamePreview() =
  WearPreview {
    PeopleContent(
      people =
        listOf(
          PersonItem(1, "Maria Antònia Fontcuberta", "MF", AvatarColor.Sky, AvatarShape.Cookie9Sided, "Fa 2 h", false),
          PersonItem(2, "Bartomeu Puigdollers", "BP", AvatarColor.Lavender, AvatarShape.Clover4Leaf, null, true),
        ),
      onPersonClick = {},
    )
  }

@WearScreenPreviews
@Composable
fun PeoplePhoneOfflinePreview() =
  WearPreview { PeopleContent(people = SamplePeople.take(2), onPersonClick = {}, dataAgeMinutes = 14) }

@WearScreenPreviews
@Composable
fun PeopleDiscardedNotesPreview() =
  WearPreview { PeopleContent(people = SamplePeople, onPersonClick = {}, discardedNotes = 1) }

/** Pantalla 1/8 connectada al ViewModel; sense instantani encara, explica què cal fer. */
@Composable
fun PeopleScreen(
  viewModel: PeopleViewModel,
  onPersonClick: (PersonItem) -> Unit,
) {
  when (val state = viewModel.state.collectAsStateWithLifecycle().value) {
    PeopleUiState.Loading -> LoadingContent()
    PeopleUiState.NoData -> NoDataContent()
    PeopleUiState.NoPeople -> NoPeopleContent()
    is PeopleUiState.Loaded ->
      PeopleContent(
        people = state.people,
        onPersonClick = onPersonClick,
        dataAgeMinutes = state.dataAgeMinutes,
        discardedNotes = state.discardedNotes,
        onDiscardedNotesClick = viewModel::dismissDiscardedNotes,
      )
  }
}

/** El mòbil encara no ha enviat cap instantani. */
@Composable
fun NoDataContent(modifier: Modifier = Modifier) {
  StatusContent(
    icon = Icons.Rounded.PhonelinkOff,
    iconBackground = WearColors.SurfaceCardHigh,
    iconTint = WearColors.OnSurface,
    title = stringResource(R.string.no_data_title),
    subtitle = stringResource(R.string.no_data_hint),
    modifier = modifier,
    titleStyle = PersonNameStyle,
  )
}

@WearScreenPreviews
@Composable
fun NoDataPreview() = WearPreview { NoDataContent() }

/** El mòbil ha sincronitzat però no té cap persona: com l'estat buit del mòbil. */
@Composable
fun NoPeopleContent(modifier: Modifier = Modifier) {
  StatusContent(
    icon = Icons.Rounded.PersonAdd,
    iconBackground = WearColors.SurfaceCardHigh,
    iconTint = WearColors.OnSurface,
    title = stringResource(R.string.no_people_title),
    subtitle = stringResource(R.string.no_people_hint),
    modifier = modifier,
    titleStyle = PersonNameStyle,
  )
}

@WearScreenPreviews
@Composable
fun NoPeoplePreview() = WearPreview { NoPeopleContent() }
