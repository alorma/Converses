# Tanda 1 · Selector «Quan» i formulari d'entrada comú

Referència visual: llenç «Converses», secció **«Nota ràpida · selector "Quan"»**:

- **3c** · Nota ràpida, amb «Quan» com a fila.
- **3d** · Full «Quan va ser?».

Les captures del llenç ja mostren el selector «Qui» (tanda 2). En aquesta tanda, el selector de tipus actual («Li he dit · M'ha dit · Ha passat») **es queda com està**.

Es fa sobre el color de la persona (`color-persona.md`).

## Objectiu

1. Substituir el xip «Ara» per una **fila «Quan»** que obre el full «Quan va ser?».
2. Fer **un sol formulari d'entrada** (`EntryForm`), amb el mateix ordre i estil a la nota ràpida, al full de nova entrada i a l'edició d'una entrada.

Fora d'abast: el selector «Qui», la cronologia i l'exportació (tanda 2), i els pendents (backlog 6).

---

## 1. `EntryForm` comú

**Com està avui:**

- `ui/entry/EntryForm.kt` té peces comunes (`EntryFormState`, `KindSelector`, `EntryTextField`, `WhereSection`, `TimeChip`).
- El formulari es munta dues vegades: a `QuickNote.kt` i a `NewEntrySheet.kt`.
- L'edició d'una entrada reutilitza `NewEntrySheet`, passant-li `initial`.

**Què cal fer:**

- Extreure un composable `EntryForm(state, …)` únic, amb dos slots:
  - `header`: la targeta «Amb qui», que només posa la nota ràpida;
  - `saveButton`: a la nota ràpida va a dalt; al full va a baix («Desa a la cronologia»).
- `QuickNote.kt` i `NewEntrySheet.kt` només hi posen els slots i la navegació.

### Ordre i estil dels camps

1. **Amb qui:** només a la nota ràpida.
2. **Tipus:** el `KindSelector` actual, sense canvis. A la tanda 2 passarà a ser «Qui».
3. **Text**
4. **On**
5. **Quan**

- **Títols de secció:** `On` i `Quan` (i `Qui` a la tanda 2), en `labelLarge` 14/700, `onSurfaceVariant` i 6 dp de marge lateral. Entre seccions, 14 dp.
- **Text:**
  - camp sense vora ni indicador, `surfaceContainerHigh`, radi 24, alçada mínima de 120 dp i text de 17 sp;
  - el text d'ajuda continua depenent del tipus: «Què li has dit?», «Què t'ha dit?», «Què ha passat?».
- **On:**
  - `FilterChip` de selecció única, radi 12 i 36 dp d'alt;
  - seleccionat: `primaryContainer` amb `check`; no seleccionat: vora d'1 dp `outlineVariant`;
  - primer, els canals de la persona; al final, «Altres»;
  - per defecte, el canal de la seva última entrada.
- **Desa:** desactivat si el text és buit, o si a la nota ràpida no hi ha persona.

---

## 2. Fila «Quan»

- Substitueix el `TimeChip`. Fila de 72 dp, `surfaceContainerLowest` i radi 24, amb el mateix patró que la targeta «Amb qui»:
  - **cercle de 44 dp** `surfaceContainerHigh` amb la icona de l'opció;
  - **títol** de l'opció (17/700);
  - **data i hora completes** a sota (13, `onSurfaceVariant`);
  - `unfold_more` a la dreta.
- Tocar-la obre el full «Quan va ser?».

## 3. Full «Quan va ser?» (`WhenSheet`)

`ModalBottomSheet` amb el títol «Quan va ser?» (24/800) i una llista agrupada (24/6, 2 dp entre files) de 64 dp per fila:

| Opció | Icona | Subtítol (exemple) | Valor |
| --- | --- | --- | --- |
| Ara | `schedule` | Dilluns 6 d'oct. · 21:46 | Es resol **en desar** |
| Fa una hora | `history` | Dilluns 6 d'oct. · 20:46 | `now − 1 h`, fixat en triar-la |
| Ahir a aquesta hora | `event_repeat` | Diumenge 5 d'oct. · 21:46 | `now − 1 dia`, fixat en triar-la |
| Tria dia i hora… | `edit_calendar` | Calendari i rellotge | Obre els selectors |

- L'opció activa va en `primaryContainer` amb `check` a la dreta. «Tria dia i hora…» porta `chevron_right`.
- Triar una opció tanca el full i actualitza la fila.
- **Tria dia i hora…:**
  1. `DatePickerDialog` de M3, amb `selectableDates` fins a avui (no es permet el futur).
  2. `TimePicker` en un diàleg, de 24 h, amb l'hora actual per defecte.
  3. En acceptar, la fila mostra la data com a títol («Dissabte 4 d'oct.») i l'hora a sota («18:30»), amb la icona `edit_calendar`. Al full, «Tria dia i hora…» queda marcada amb aquest valor com a subtítol.
- **Editar una entrada:** la fila mostra la data desada en format personalitzat. Cap opció ràpida queda marcada.

### Estat

- Avui, `EntryFormState.occurredAt == null` ja vol dir «ara en desar», i `updateEntry` ja manté la data si no es toca.
- Cal guardar també l'**opció triada**: `sealed interface WhenChoice { Now; HourAgo(at); Yesterday(at); Custom(at); Stored(at) }`.
  - Fa servir `Stored` una entrada que s'edita, i no marca cap opció.
  - Cal afegir-la al `Saver` de l'estat.
- El subtítol d'«Ara» es refresca cada minut, amb un `LaunchedEffect` que espera fins al minut següent.

### Format

- Data: `EEEE d MMM` amb el locale de l'app i la primera lletra en majúscula («Dilluns 6 d'oct.» / «Lunes 6 oct.» / «Monday 6 Oct.»).
- Hora: `HH:mm`.

## 4. Textos (`strings.xml`)

| Clau | ca | es | en |
| --- | --- | --- | --- |
| `entry_where_title` | On | Dónde | Where |
| `entry_when_title` | Quan | Cuándo | When |
| `when_sheet_title` | Quan va ser? | ¿Cuándo fue? | When was it? |
| `when_now` | Ara | Ahora | Now |
| `when_hour_ago` | Fa una hora | Hace una hora | An hour ago |
| `when_yesterday` | Ahir a aquesta hora | Ayer a esta hora | Yesterday, same time |
| `when_pick` | Tria dia i hora… | Elegir día y hora… | Pick date and time… |
| `when_pick_sub` | Calendari i rellotge | Calendario y reloj | Calendar and clock |

S'esborren `quick_where` i `time_now` quan es deixin de fer servir.

## 5. Accessibilitat

- Fila «Quan»: «Quan: Ara, dilluns 6 d'octubre a les 21:46. Toca per canviar-ho».
- Full: opcions amb `role = RadioButton` i l'estat de selecció.

## Pla en commits

1. Strings (en, ca, es).
2. `EntryForm` comú amb l'ordre i els estils nous. `QuickNote` i `NewEntrySheet` passen a fer-lo servir.
3. Fila «Quan» + `WhenSheet` + `WhenChoice` al `Saver`.
4. `DatePickerDialog` i `TimePicker`.
5. Previews i tests.

## Proves

- [ ] Nota ràpida, full del detall i edició mostren el mateix formulari i en el mateix ordre.
- [ ] «Ara» desa l'hora del moment de desar, no la d'obrir el formulari.
- [ ] «Fa una hora» i «Ahir a aquesta hora» fixen l'hora en el moment de triar-les.
- [ ] «Tria dia i hora…» no deixa triar dies futurs, i el valor es manté en tornar a obrir el full i en girar la pantalla.
- [ ] Editar una entrada antiga mostra la seva data i, si no es toca, no la canvia en desar.
- [ ] El subtítol d'«Ara» canvia de minut sense tornar a obrir el formulari.
- [ ] Format de data correcte en ca, es i en.
- [ ] Previews: `EntryForm` (text buit o ple), `WhenSheet` (cada opció marcada) i la fila «Quan» amb data personalitzada.
- [ ] Tests unitaris: opcions de `WhenChoice` → instant, i format de data.
