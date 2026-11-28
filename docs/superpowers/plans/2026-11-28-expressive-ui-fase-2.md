# UI Expressive · Fase 2 (llistat 14a, 14e) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** El llistat de persones (i Arxivades) i la barra flotant del mòbil amb el tema expressiu: capçalera, xips, grups per dates, files-targeta i barra amb el FAB `Cookie9Sided`.

**Architecture:** `PeopleListScreen` es redibuixa dins d'`ExpressiveTheme` amb el mateix `PeopleListUiState`. L'agrupació per dates és una funció pura a `ui/people`. Els components nous van a `designsystem.expressive` sobre components M3 (`FilterChip`, `Card`, `HorizontalFloatingToolbar`, `ToggleButton`, `MediumFloatingActionButton`). L'única dada nova és `lastActivityAt` a la consulta del llistat (projecció, sense canvi d'esquema).

**Tech Stack:** Kotlin, Compose, Material 3 1.6.0-alpha01 (Expressive), Room (només la consulta), JUnit JVM, screenshot tests.

**Spec:** `docs/superpowers/specs/2026-11-27-expressive-ui-design.md`; valors a `docs/superpowers/specs/expressive-ui/INSTRUCCIONS.md` §6 «14a · Llistat» i `pantalles.html` (taulers 14a, 14e). Fase 1: `docs/superpowers/plans/2026-11-27-expressive-ui-fase-1.md`.

## Global Constraints

- Branca `expressive-ui`, sense feature flag: es modifica la pantalla directament.
- Components M3 sempre que existeixin; tots els `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` (i de `MaterialShapes`) només a `:designsystem`.
- Textos a `strings.xml` en `values/` (en), `values-ca/` i `values-es/`.
- `Scaffold` a la pantalla, sense insets manuals nous; la llista deixa lloc a la barra amb `LocalFloatingBarPadding`.
- Persones protegides: cap text, canal ni tasca abans de desbloquejar (el repositori ja els buida; `lastActivityAt` és l'única dada que es manté, decisió de Bernat del 2026-11-28).
- Cap canvi d'esquema de Room: `app/schemas` no ha de canviar.
- Below 600 dp no es toca res més que aquesta pantalla i la barra; ≥ 600 dp (rail, panell) ha de funcionar però no es poleix.
- Colors: `MaterialTheme.colorScheme` dins d'`ExpressiveTheme` (primary = tinta, secondary* = accent, tertiary* = préssec) i `rememberPersonPalette(person.color)` per a cada fila.

## Decisions preses (per revisar)

Bernat (2026-11-28): protegides al grup de la seva data real; sense entrades, per data de creació; «Aquesta setmana» = setmana del calendari de l'idioma.

Proposades en aquest pla (la proposta no en diu res):
1. Fora el subtítol «N persones» de la capçalera (la proposta només hi té el títol i dos botons).
2. Fora la fila discontínua «Afegeix una persona» del final: la capçalera ja té el botó.
3. Fora la línia de canals de cada fila (l'overline passa a ser el nom); la pastilla de tasques (Pro) es queda, sota l'hora.
4. «Totes» està marcada quan no hi ha cap canal triat; tocar-la desmarca tots els canals (crida `onFilterToggled` per a cada canal marcat, sense canviar el ViewModel).
5. Mesos: «Novembre» aquest any, «Novembre de 2025» els anys anteriors (`LLLL` / `LLLL 'de' yyyy` en ca i es, `LLLL yyyy` en en), amb majúscula inicial.
6. Insígnia de recompte: és l'únic component sense equivalent M3 (el `Badge` d'M3 és de 16 dp). Es fa amb `Surface` de 34 dp, radi 11. **Cal el vistiplau de Bernat abans de la tasca 3.**
7. Fila seleccionada (panell de pantalla gran): vora de 2 dp en `colorScheme.primary`.

## Review Focus

1. Persona protegida en un grup: surt al grup de la seva data, amb `lock` i «Protegida», i cap text ni hora de l'entrada (l'hora a la dreta tampoc: revelaria més que el grup). → test a la tasca 2 (agrupació) i preview/captura a la tasca 4.
2. Dates al futur (entrada programada o rellotge canviat): van a «Avui», no a un grup nou ni a un mes futur. → test a la tasca 2.
3. Un dilluns (ca) o un diumenge (en): «Aquesta setmana» no surt i ahir continua sent «Ahir». → test a la tasca 2.
4. Nom molt llarg i text al 200 %: el nom de l'overline i l'última línia es tallen amb «…» i l'hora no s'encongeix. → captura amb font gran a la tasca 4.
5. Filtre que deixa la llista buida i Arxivades buida: es manté l'estat buit actual, sense capçaleres de grup soltes. → test a la tasca 2 (llista buida → cap grup) i captura existent `peopleListEmpty`.

---

### Task 1: `lastActivityAt` al llistat

**Files:**
- Modify: `app/src/main/java/com/alorma/contactnotes/data/db/Entities.kt` (`PersonListItem`)
- Modify: `app/src/main/java/com/alorma/contactnotes/data/db/Daos.kt:149-163` (`observeList`)
- Modify: `app/src/test/java/com/alorma/contactnotes/testing/FakeDb.kt:260` (`observeList`)
- Modify: `app/src/main/java/com/alorma/contactnotes/ui/preview/PreviewData.kt` (constructors de `PersonListItem`)
- Test: el test de repositori que ja cobreix `observePeople` (buscar-lo amb `grep -rn observePeople app/src/test`)

**Interfaces:**
- Produces: `PersonListItem.lastActivityAt: Long` = `COALESCE(última entrada.occurredAt, person.createdAt)`, en epoch millis, també per a les protegides.

- [ ] **Step 1: Test que falla:** `protectedPersonKeepsOnlyTheActivityTime` — una persona protegida amb una entrada a `t`: `lastEntry == null`, `channels` buit, `openTasks == 0` i `lastActivityAt == t`. I `personWithoutEntriesUsesCreation`: `lastActivityAt == person.createdAt`.
- [ ] **Step 2:** `./gradlew :app:testDebugUnitTest --tests '*<RepositoryTest>'` → FAIL (no compila).
- [ ] **Step 3:** Afegir `val lastActivityAt: Long` a `PersonListItem` (després de `openTasks`, amb KDoc: «Data de l'última entrada, o de creació si no en té; es manté a les protegides»). A la consulta, `COALESCE(e.occurredAt, p.createdAt) AS lastActivityAt` i `ORDER BY lastActivityAt DESC`. El repositori no la toca en buidar les protegides. `FakeDb` la calcula igual i ordena per ella.
- [ ] **Step 4:** Tests → PASS; `./gradlew :app:testDebugUnitTest` sencer → PASS; `git diff --stat app/schemas` → buit.
- [ ] **Step 5: Commit** «Expose the last activity time in the people list».

### Task 2: Agrupació per dates

**Files:**
- Create: `app/src/main/java/com/alorma/contactnotes/ui/people/PeopleGrouping.kt`
- Test: `app/src/test/java/com/alorma/contactnotes/ui/people/PeopleGroupingTest.kt`
- Modify: `app/src/main/res/values{,-ca,-es}/strings.xml`

**Interfaces:**
- Consumes: `PersonListItem.lastActivityAt` (tasca 1).
- Produces:
  ```kotlin
  sealed interface ActivityPeriod {
    data object Today : ActivityPeriod
    data object Yesterday : ActivityPeriod
    data object ThisWeek : ActivityPeriod
    data class Month(val month: YearMonth) : ActivityPeriod
  }
  data class PeopleGroup(val period: ActivityPeriod, val people: List<PersonListItem>)
  fun List<PersonListItem>.groupByActivity(today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): List<PeopleGroup>
  @Composable fun ActivityPeriod.label(): String
  ```

- [ ] **Step 1: Tests que fallen** (amb `today = 2026-11-26` dijous, `ZoneOffset.UTC`):
  - `groupsTodayYesterdayWeekAndMonths`: activitats el 26, 25, 23 (dilluns), 20 i 2025-12-03 → `[Today, Yesterday, ThisWeek, Month(2026-11), Month(2025-12)]`, cada una amb la seva persona.
  - `keepsTheIncomingOrderInsideAGroup`: dues persones d'avui, en l'ordre de la llista d'entrada.
  - `futureActivityIsToday`: activitat el 2026-11-28 → `Today`.
  - `mondayHasNoThisWeekGroup` (`today = 2026-11-23`, `Locale("ca")`): el 22 → `Yesterday`; el 20 → `Month(2026-11)`; cap `ThisWeek`.
  - `weekStartsOnSundayInEnglish` (`today = 2026-11-24`, `Locale.US`): el 22 (diumenge) → `ThisWeek`.
  - `protectedPeopleUseTheirActivityTime`: protegida amb `lastEntry = null` i `lastActivityAt` d'ahir → `Yesterday`.
  - `emptyListHasNoGroups`.
- [ ] **Step 2:** `./gradlew :app:testDebugUnitTest --tests '*PeopleGroupingTest'` → FAIL.
- [ ] **Step 3:** Implementar. Dia d'activitat = `Instant.ofEpochMilli(lastActivityAt).atZone(zone).toLocalDate()`; `>= today` → Today; `today - 1` → Yesterday; `>= today.with(WeekFields.of(locale).dayOfWeek(), 1)` → ThisWeek; si no, `Month(YearMonth.from(dia))`. Els grups surten en ordre d'aparició (la llista ja ve ordenada). `label()`: strings `group_today` (Today / Avui / Hoy), `group_yesterday` (Yesterday / Ahir / Ayer), `group_this_week` (This week / Aquesta setmana / Esta semana) i el mes amb `dateFormatter` (`ui/DateFormatters.kt`) segons la decisió 5.
- [ ] **Step 4:** Tests → PASS.
- [ ] **Step 5: Commit** «Group the people list by activity date».

### Task 3: Components expressius del llistat (`:designsystem`)

**Requereix el vistiplau de la decisió 6.**

**Files:**
- Create: `designsystem/src/main/java/com/alorma/contactnotes/designsystem/expressive/ExpressiveListParts.kt` (`ExpressiveFilterChip`, `GroupHeader`)
- Create: `designsystem/src/main/java/com/alorma/contactnotes/designsystem/expressive/ShapeFab.kt`
- Modify: `designsystem/src/main/java/com/alorma/contactnotes/designsystem/components/FloatingToolbar.kt` (variant expressiva)
- Modify: `designsystem/src/main/java/com/alorma/contactnotes/designsystem/expressive/ExpressivePreviews.kt` i les captures de `:designsystem`

**Interfaces:**
- Consumes: `ExpressiveShapes.Cookie9Sided`, `MorphShape`, `ShapeButton`, `WideTitle`, rols de tipografia de la fase 1.
- Produces:
  ```kotlin
  @Composable fun ExpressiveFilterChip(selected: Boolean, onClick: () -> Unit, label: String, icon: ImageVector, modifier: Modifier = Modifier)
  @Composable fun GroupHeader(title: String, count: Int, countDescription: String, modifier: Modifier = Modifier)
  @Composable fun ShapeFab(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier)
  @Composable fun ExpressiveFloatingToolbar(onFabClick: () -> Unit, fabIcon: ImageVector, fabContentDescription: String, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit)
  @Composable fun ExpressiveToolbarDestination(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier)
  ```

- [ ] **Step 1:** `ExpressiveFilterChip` sobre `FilterChip`: alçada 52 dp, forma `CircleShape`. Marcat: contenidor `colorScheme.primary`, text `onPrimary`, icona de 22 dp sense quadrat. Sense marcar: contenidor `colorScheme.surfaceContainer` (neutre), sense vora, icona dins d'un quadrat de 36 dp, radi 12, en `colorScheme.background`. Text 16 sp, pes 800.
- [ ] **Step 2:** `GroupHeader`: `Row` amb la insígnia (`Surface` 34 dp, radi 11, `secondaryContainer`, número 17 sp pes 800, `clearAndSetSemantics { contentDescription = countDescription }`) i el títol en display de 34 sp (`WideTitle` amb `maxLines = 1`), espai de 12 dp, semàntica `heading()`.
- [ ] **Step 3:** `ShapeFab` sobre `MediumFloatingActionButton` (80 dp) amb `MorphShape` de `Cookie9Sided` a cercle en prémer (el mateix spring que `ShapeButton`, i sense morph amb «Elimina animacions»), contenidor `colorScheme.onTertiaryFixedVariant` (#8A3E22), icona `Color.White` de 32 dp.
- [ ] **Step 4:** `ExpressiveFloatingToolbar`: `HorizontalFloatingToolbar` d'alçada 68 dp, forma radi 34, `toolbarContainerColor = colorScheme.primary`, `toolbarContentColor = colorScheme.onPrimary`, i el `ShapeFab` a la dreta (mateix `Row` que `ConversesFloatingToolbar`). `ExpressiveToolbarDestination` sobre `ToggleButton`: marcat amb `secondaryContainer`/`onSecondaryContainer`, icona i text; sense marcar, només la icona amb `contentDescription = label`.
- [ ] **Step 5:** Previews a `ExpressivePreviews.kt` i captures noves (camelCase): `expressiveFilterChips`, `groupHeader`, `expressiveFloatingToolbar` (clar i fosc). `./gradlew :designsystem:updateDebugScreenshotTest --tests '*<nom>'`, mirar els PNG, `./gradlew validateDebugScreenshotTest ktlintCheck` → PASS.
- [ ] **Step 6: Commit** (un per component o un per al conjunt de xips i capçalera i un altre per a la barra).

### Task 4: Pantalla del llistat

**Files:**
- Modify: `app/src/main/java/com/alorma/contactnotes/ui/people/PeopleListScreen.kt`
- Modify: `app/src/main/java/com/alorma/contactnotes/ui/people/PeopleListPreviews.kt`
- Modify: `app/src/main/java/com/alorma/contactnotes/ui/preview/PreviewData.kt` (persones de diversos dies, una protegida, una sense entrades)
- Modify: `app/src/main/res/values{,-ca,-es}/strings.xml`
- Captures: `app/src/screenshotTest/kotlin/com/alorma/contactnotes/ScreensScreenshotTests.kt` (`peopleList*`), `SyncDevicesScreenshotTests.kt`, `SyncEntryPointsScreenshotTests.kt`

**Interfaces:**
- Consumes: `groupByActivity`, `ActivityPeriod.label()` (tasca 2); components de la tasca 3; `rememberPersonPalette`, `ShapeButton`, `WideTitle`, `ExpressiveTheme` (fase 1); `formatLastActivity` (ja existeix).
- Produces: `PeopleListScreen` amb la mateixa signatura.

- [ ] **Step 1:** `PeopleListScreen` s'embolica amb `ExpressiveTheme` (la pantalla i les seves previews; `PeopleListRoute` no canvia). Fons del `Scaffold`: `colorScheme.background`.
- [ ] **Step 2: Capçalera:** `WideTitle` «Converses» (o «Arxivades») a 34 sp; a la dreta, si no és Arxivades, `ShapeButton` de 52 dp `Cookie9Sided` en `secondaryContainer` amb `PersonAdd` («Afegeix una persona»); després, si `syncIndicator != null`, el `SyncIndicator` actual dins d'un cercle neutre de 52 dp. Decisió 1: fora el subtítol. `RemovedNotice` es queda on és.
- [ ] **Step 3: Xips:** «Totes» (`Icons.Rounded.Apps`, string `filter_all`: All / Totes / Todas) i un `ExpressiveFilterChip` per `ChannelType` amb la seva icona de canal (la que fa servir el detall per al canal). Scroll horitzontal; `FlowRow` amb `inPane`. Decisió 4.
- [ ] **Step 4: Grups:** per a cada `PeopleGroup`, un `item(key = "group-${period}")` amb `GroupHeader` (descripció: plural `people_group_count`, «%d people» / «%d persones» / «%d personas») i les files amb `items(key = { it.person.id })`, separades 6 dp; 20 dp abans de cada capçalera excepte la primera.
- [ ] **Step 5: Fila:** `Card(onClick)` (o `selectable` amb `inPane`) d'alçada mínima 92 dp, radi 32, color `palette.surface`. Avatar actual de 56 dp (48 a `inPane`) amb la forma i el color de la persona. Al mig, l'overline amb el nom (rol overline, color `palette.accent`, una línia, «…») i l'última línia (16 sp, una línia, «…»; «Encara no hi ha entrades» en `onSurfaceVariant`). A la dreta, l'hora amb `formatLastActivity(lastEntry.occurredAt)` en display de 20 sp i, a sota, `TasksPill` si `openTasks > 0`. Protegides: `lock` + «Protegida» al lloc de l'última línia i **sense hora**. Seleccionada: decisió 7.
- [ ] **Step 6:** Previews: carregat (clar i fosc), protegida, Arxivades, buit, i font al 200 %. Afegir la captura `peopleListLoadedDark` i `peopleListHugeFont` a `ScreensScreenshotTests.kt`.
- [ ] **Step 7:** `./gradlew :app:updateDebugScreenshotTest --tests '*peopleList*'` (i les dues de sync), mirar cada PNG; `./gradlew ktlintCheck testDebugUnitTest validateDebugScreenshotTest` → PASS.
- [ ] **Step 8:** Emulador `converses_37_2` (mai el 5554): `./gradlew :app:installDebug`, dades de prova des del menú de debug, comprovar el llistat clar i fosc, els xips, una protegida, el panell a ≥ 600 dp (emulador tablet o finestra redimensionada) i TalkBack a la capçalera de grup i a «Totes». Captures a `.superpowers/sdd/2026-11-28-expressive-ui-fase-2/`.
- [ ] **Step 9: Commit** «Redraw the people list with the expressive theme» (i captures en un commit a part).

### Task 5: Barra flotant expressiva a `AppShell`

**Files:**
- Modify: `app/src/main/java/com/alorma/contactnotes/ui/navigation/AppShell.kt:88-120`
- Modify: `app/src/main/java/com/alorma/contactnotes/ui/navigation/AppShellPreviews.kt` i les captures d'`AppShell`

**Interfaces:**
- Consumes: `ExpressiveFloatingToolbar`, `ExpressiveToolbarDestination` (tasca 3), `ExpressiveTheme`.

- [ ] **Step 1:** El lambda `floatingBar` dibuixa, dins d'`ExpressiveTheme`, l'`ExpressiveFloatingToolbar` amb Persones (`nav_people`), Arxivades i Ajustos (`ExpressiveToolbarDestination`), i el FAB amb `Icons.Rounded.Edit` i «Nota ràpida». El rail i la lògica de visibilitat no canvien. Si `ConversesFloatingToolbar` i `ToolbarDestination` es queden sense ús, s'esborren.
- [ ] **Step 2:** `FloatingBarContentPadding` passa de 96 a 100 dp (FAB de 80 + 16 de marge + 4); comprovar a l'emulador que l'última fila queda per sobre de la barra.
- [ ] **Step 3:** Captures d'`AppShell` regenerades i mirades; ktlint, tests i `validateDebugScreenshotTest` → PASS.
- [ ] **Step 4:** Emulador: la barra al llistat, a Arxivades, que s'amaga en obrir una persona, i el foldable obert (`floatingBarWidth`) si hi ha emulador; captures al directori de la fase.
- [ ] **Step 5: Commit** «Use the expressive floating toolbar on phones».
