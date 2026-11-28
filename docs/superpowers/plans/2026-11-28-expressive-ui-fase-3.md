# UI Expressive · Fase 3 (targeta destacada) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** La targeta destacada del llistat (14a, 14e): la persona de l'entrada més recent i no protegida, amb la cita, i una galeta per afegir-li una entrada. Sense la zona de tasca (fase 8).

**Architecture:** Una funció pura a `ui/people` tria la persona de les que el llistat ja mostra (`PeopleListUiState.people`, ja filtrades pels xips). El component visual (`FeaturedCard`) va a `designsystem.expressive` perquè fa servir `MaterialShapes` (`Flower`, `toShape()`), sobre el `Card` d'M3. `PeopleListScreen` el posa com a primer element del `LazyColumn` i la navegació obre la nota ràpida amb la persona triada. Cap canvi de ViewModel, de Room ni de dades.

**Tech Stack:** Kotlin, Compose, Material 3 1.6.0-alpha01 (Expressive), JUnit JVM, screenshot tests.

**Spec:** `docs/superpowers/specs/2026-11-27-expressive-ui-design.md` (fase 3); valors a `docs/superpowers/specs/expressive-ui/INSTRUCCIONS.md` §6 «14a · Llistat › Targeta destacada» i `pantalles.html` (taulers 14a i 14e, `<section aria-label="Última conversa">`). Fases anteriors: `docs/superpowers/plans/2026-11-28-expressive-ui-fase-2.md`.

## Global Constraints

- Branca `expressive-ui`, sense feature flag.
- Components M3 sempre que existeixin; tots els `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` (i `MaterialShapes`/`toShape()`) només a `:designsystem`.
- Textos a `strings.xml` en `values/` (en), `values-ca/` i `values-es/`.
- Persones protegides: la targeta no en mostra mai res (ni nom, ni text, ni canal, ni hora). Si la més recent és protegida, surt la següent no protegida.
- Cap canvi d'esquema de Room ni del ViewModel: `app/schemas` i `PeopleListViewModel.kt` no canvien.
- Colors de la targeta: `rememberPersonPalette(person.color)` → `featured` (fons) i `onFeatured` (text). Ja tenen contrast AA comprovat a `PersonPaletteContrastTest`; colors dinàmics i persona sense color ja els resol `rememberPersonPalette`.
- Mides (proposta): alçada 268 dp, radi 40, marge intern 24 dp, avatar 30 dp, separació 12 dp entre overline i cita, cita 27 sp amb interlineat 1,2, `Flower` de 250 dp desplaçada `x = +70 dp` fora per la dreta i `y = −60 dp` per dalt, galeta `Cookie9Sided` de 84 dp amb icona `add` de 36 dp, ancorada a baix a la dreta amb 24 dp de marge.

## Decisions preses

Bernat (2026-11-28):
1. La targeta és el **primer element del `LazyColumn`**, sota la capçalera i els xips fixos; fa scroll amb la llista.
2. **Segueix el filtre:** es tria entre `state.people` (ja filtrades). Amb un filtre que deixa la llista buida, no hi ha targeta.
3. **També surt al panell** de pantalla gran (≥ 600 dp), sense polir.
3b. **A pantalla gran, la destacada surt al panell de detall** quan no hi ha cap persona oberta, en lloc del panell buit
   (`EmptyDetailPane`). No s'hi navega: la pila no canvia, i si es plega o es passa a mòbil es torna al llistat. Si la
   destacada canvia (filtre, entrada nova), el panell la segueix. La seva fila queda marcada com a oberta. Només a Persones,
   no a Arxivades. Tasca 4.

Proposades en aquest pla (la proposta no en diu res):
4. No surt a Arxivades.
5. La persona destacada també surt a la seva fila del grup (no es treu del llistat).
6. Overline: `NOM · CANAL · TEMPS` en majúscules. El canal és el grup (`ChannelType.label()`), no el canal propi Pro (el `PersonListItem` no en porta el nom). El temps és relatiu (`DateUtils.getRelativeTimeSpanString(at, now, MINUTE_IN_MILLIS, FORMAT_ABBREV_RELATIVE)`: «fa 20 min», «ahir», i la data quan passa d'una setmana); amb menys d'un minut o una data futura, «Ara mateix».
7. Cometes: `SAID` i `HEARD` entre cometes de l'idioma (`«…»` en ca i es, `“…”` en en); `HAPPENED` (apunt) sense cometes.
8. Colors de la galeta: fons `onFeatured`, icona `featured` (al clar surt blanca amb la icona fosca; al fosc, fosca amb la icona pastel, com als taulers 14a i 14e). `Flower` en `onFeatured` al 12 %.
9. Text llarg o lletra gran: l'alçada és un mínim (`heightIn(min = 268.dp)`), la cita té `maxLines = 4` amb «…» i mai trepitja la galeta.

## Review Focus

1. **La persona més recent és protegida:** la targeta salta a la següent no protegida; si totes les que tenen entrades són protegides, no hi ha targeta. → tests a la tasca 1.
2. **Lletra al 200 % amb una cita llarga:** la targeta creix, la cita es talla amb «…» i la galeta continua a baix a la dreta, sense tapar text. → captura `peopleListFeaturedHugeFont` a la tasca 3.
3. **Persona sense color o amb «Color de la persona» apagat:** la targeta surt en tinta amb el text del fons, llegible, en clar i fosc. → preview `FeaturedCardNeutralPreview` a la tasca 2.
4. **Entrada amb data futura** (programada o rellotge canviat): «Ara mateix», no «d'aquí a 2 h». → `featuredTimeAt` amb test a la tasca 1.
5. **Pantalla gran, la destacada canvia mentre és al panell de detall:** el detall mostra la persona nova amb les seves dades, no el ViewModel de l'anterior. → `key` al ViewModel a la tasca 4 i comprovació a l'emulador.
6. **TalkBack:** tocar la targeta llegeix nom, canal, temps i cita en un sol node i la galeta és un botó a part amb «Afegeix una entrada a {nom}». → comprovació manual a l'emulador a la tasca 3 (pas de verificació).

---

### Task 1: Triar la persona destacada

**Files:**
- Create: `app/src/main/java/com/alorma/contactnotes/ui/people/PeopleFeatured.kt`
- Test: `app/src/test/java/com/alorma/contactnotes/ui/people/PeopleFeaturedTest.kt`

**Interfaces:**
- Produces:
  - `fun List<PersonListItem>.featured(): PersonListItem?` — la de `lastEntry.occurredAt` més gran entre les que tenen `lastEntry != null` i `!person.isProtected`; en empat, la primera de la llista; `null` si no n'hi ha cap.
  - `fun featuredTimeAt(occurredAt: Long, now: Long): Long?` — `null` («Ara mateix») si `now - occurredAt < 60_000` (inclou les futures); si no, `occurredAt`. La UI fa servir el `null` per escriure el text fix i el valor per a `DateUtils`.

- [ ] **Step 1: Tests que fallen** (fes servir els constructors de `PersonEntity`/`EntryEntity` com a `PeopleGroupingTest`):
  - `picksTheMostRecentEntry` — tres persones amb entrades a 1 000, 3 000 i 2 000 → la de 3 000.
  - `skipsProtectedPeople` — la de 3 000 és `isProtected = true` (amb `lastEntry` no nul, per si el repositori canviés) → la de 2 000.
  - `noneWhenOnlyProtectedOrWithoutEntries` — una protegida i una sense `lastEntry` → `null`.
  - `emptyListHasNone` — `emptyList<PersonListItem>().featured() == null`.
  - `tieKeepsListOrder` — dues amb 2 000 → la primera.
  - `justNowUnderAMinute` — `featuredTimeAt(10_000, 69_999) == null`.
  - `futureIsJustNow` — `featuredTimeAt(100_000, 10_000) == null`.
  - `olderKeepsTheTime` — `featuredTimeAt(10_000, 70_000) == 10_000L`.
- [ ] **Step 2:** `./gradlew :app:testDebugUnitTest --tests '*PeopleFeaturedTest'` → FAIL (no compila).
- [ ] **Step 3:** Implementa les dues funcions a `PeopleFeatured.kt` (`filter` + `maxByOrNull`, que ja es queda la primera en empat).
- [ ] **Step 4:** `./gradlew :app:testDebugUnitTest --tests '*PeopleFeaturedTest'` → PASS.
- [ ] **Step 5:** `./gradlew ktlintCheck` i commit: `Add the featured person pick for the people list`.

### Task 2: `FeaturedCard` al sistema de disseny

**Files:**
- Create: `designsystem/src/main/java/com/alorma/contactnotes/designsystem/expressive/FeaturedCard.kt`
- Modify: `designsystem/src/main/java/com/alorma/contactnotes/designsystem/expressive/ExpressivePreviews.kt` (previews)
- Modify: el fitxer de captures del `:designsystem` a `designsystem/src/screenshotTest/` (el mateix on hi ha les captures de `ShapeButton`/`GroupHeader`; buscar-lo amb `grep -rln GroupHeader designsystem/src/screenshotTest`)

**Interfaces:**
- Consumes: `PersonPalette` (`featured`, `onFeatured`), `ExpressiveShapes.Flower`/`Cookie9Sided`, `ShapeButton(...)`, `ExpressiveType.overline`, `ExpressiveType.quote(size)`.
- Produces:
```kotlin
@Composable
fun FeaturedCard(
  overline: String,          // ja en majúscules
  quote: String,             // ja amb les cometes que toquin
  palette: PersonPalette,
  onClick: () -> Unit,
  onAdd: () -> Unit,
  addDescription: String,    // «Afegeix una entrada a {nom}»
  avatar: @Composable () -> Unit,
  modifier: Modifier = Modifier,
)
```

- [ ] **Step 1:** Implementa `FeaturedCard` amb el `Card(onClick)` d'M3: `RoundedCornerShape(40.dp)`, `containerColor = palette.featured`, `contentColor = palette.onFeatured`, `fillMaxWidth().heightIn(min = 268.dp)`, i les mides de Global Constraints. Dins, un `Box`:
  - la `Flower` decorativa (250 dp, `offset(x = 70.dp, y = (-60).dp)` des de dalt a la dreta, `onFeatured.copy(alpha = 0.12f)`, sense semàntica; el `Card` ja retalla);
  - una `Column` (padding 24, fins a 24 + 84 + 16 dp del fons perquè la cita no arribi a la galeta) amb una `Row` (avatar, 10 dp, overline amb `alpha 0.9`, 1 línia, «…») i la cita (`quote(27.sp)` amb `lineHeight = 1.2.em`, `maxLines = 4`, «…»);
  - el `ShapeButton(shape = Cookie9Sided, size = 84.dp, containerColor = palette.onFeatured, contentColor = palette.featured, icon = Icons.Rounded.Add, contentDescription = addDescription)` a `Alignment.BottomEnd` amb padding 24.
  - Semàntica: el `Card` fusiona avatar, overline i cita en un node; el botó queda a part.
- [ ] **Step 2:** Previews a `ExpressivePreviews.kt`: `FeaturedCardPreview` (menta), `FeaturedCardNeutralPreview` (`neutralPalette`, avatar pel nom) i `FeaturedCardLongQuotePreview` (cita de ~300 caràcters, `fontScale = 2f`); el fosc el donen les captures amb `@ComponentPreviews` (clar i fosc), no cal una preview apart. Captures amb noms camelCase: `featuredCard`, `featuredCardNeutral`, `featuredCardLongQuote`.
- [ ] **Step 3:** `./gradlew :designsystem:testDebugUnitTest ktlintCheck :designsystem:compileDebugKotlin` → BUILD SUCCESSFUL. Les referències noves les genera el CI (etiqueta `update-screenshots`); no cal generar-les en local.
- [ ] **Step 4:** Commit: `Add the expressive featured card`.

### Task 3: La targeta al llistat i a la navegació

**Files:**
- Modify: `app/src/main/java/com/alorma/contactnotes/ui/people/PeopleListScreen.kt`
- Modify: `app/src/main/java/com/alorma/contactnotes/ui/AppNavigation.kt:238-258` (les dues `PeopleListRoute`)
- Modify: `app/src/main/java/com/alorma/contactnotes/ui/people/PeopleListPreviews.kt`
- Modify: `app/src/screenshotTest/kotlin/com/alorma/contactnotes/ScreensScreenshotTests.kt`
- Modify: `app/src/main/res/values/strings.xml`, `values-ca/strings.xml`, `values-es/strings.xml`

**Interfaces:**
- Consumes: `featured()`, `featuredTimeAt()` (tasca 1), `FeaturedCard` (tasca 2), `ChannelType.label()`.
- Produces: paràmetre nou `onAddEntry: (PersonEntity) -> Unit = {}` a `PeopleListRoute`, `PeopleListScreen` i `PeopleListContent`.

- [ ] **Step 1: Textos** (en / ca / es):
  - `featured_add_entry`: `Add an entry for %1$s` / `Afegeix una entrada a %1$s` / `Añade una entrada a %1$s`
  - `featured_quote`: `“%1$s”` / `«%1$s»` / `«%1$s»`
  - `featured_just_now`: `Just now` / `Ara mateix` / `Ahora mismo`
- [ ] **Step 2:** A `PeopleListContent`, `val featured = remember(state.people) { if (archived) null else state.people.featured() }` i, abans dels grups, `item(key = "featured") { … }` amb `Modifier.animateItem()` i un espai inferior de `GroupGap` (la primera capçalera de grup continua sense padding superior). Una composable privada `FeaturedPerson(item, onClick, onAdd)` munta:
  - overline: `"${name} · ${stringResource(channelType.label())} · ${time}".uppercase(locale)`, amb `time` = `featured_just_now` si `featuredTimeAt(...)` és `null`, o `DateUtils.getRelativeTimeSpanString(at, now, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE)`; `now = remember(item) { System.currentTimeMillis() }`;
  - cita: `featured_quote` per a `SAID`/`HEARD`, el text tal qual per a `HAPPENED`;
  - avatar: `Avatar(initials, shape, color, size = 30.dp)`;
  - `onClick = onPersonClick(item.person)`, `onAdd = onAddEntry(item.person)`.
  Es mostra també amb `inPane = true`.
- [ ] **Step 3:** A `AppNavigation.kt`, la `PeopleListRoute` de `PeopleRoute` rep `onAddEntry = { navigation.navigate(QuickNoteRoute(personId = it.id)) }` (Arxivades no en necessita).
- [ ] **Step 4:** Previews i captures: les existents (`peopleListLoaded`, `peopleListTasks`, panell…) ja mostraran la targeta. El fosc el donen les captures amb `@ScreenPreviews` (clar i fosc). Afegeix `PeopleListScreenFeaturedHugeFontPreview` (`fontScale = 2f`) amb captura `peopleListFeaturedHugeFont`; i `PeopleListScreenProtectedNewestPreview`, on la persona amb l'entrada més recent de `PreviewData.listPeople` és protegida (copia-la amb `isProtected = true`), captura `peopleListProtectedNewest`.
- [ ] **Step 5:** `./gradlew ktlintCheck testDebugUnitTest :app:assembleDebug` → BUILD SUCCESSFUL.
- [ ] **Step 6: Emulador** (`converses_37_2`, dades de mostra del menú de debug): la targeta surt amb la persona més recent; tocar-la obre el detall; la galeta obre la nota ràpida amb la persona triada; amb el xip «Xat» canvia a la més recent de xat; Arxivades sense targeta; mode fosc; lletra al 200 %; amb TalkBack, la targeta és un node (nom, canal, temps, cita) i la galeta un botó amb «Afegeix una entrada a {nom}»; protegeix la persona destacada i comprova que salta a la següent. Captures a `.superpowers/sdd/2026-11-28-expressive-ui-fase-3/`.
- [ ] **Step 7:** Commit: `Show the featured card at the top of the people list`.

### Task 4: La destacada al panell de detall (pantalla gran)

**Files:**
- Modify: `app/src/main/java/com/alorma/contactnotes/ui/AppNavigation.kt` (entrada `PeopleRoute`, entrada `PersonRoute:333-347`)
- Modify: `app/src/main/java/com/alorma/contactnotes/ui/people/PeopleListScreen.kt` (`PeopleListRoute`)
- Modify: `app/src/main/java/com/alorma/contactnotes/ui/person/PersonDetailScreen.kt:155` (clau del ViewModel)

**Interfaces:**
- Consumes: `featured()` (tasca 1), `PersonDetailRoute(...)`, `DetailPanel`, `EmptyDetailPane`.
- Produces: paràmetre nou `onFeaturedChanged: (Long?) -> Unit = {}` a `PeopleListRoute`, cridat amb un `LaunchedEffect(featuredId)` amb l'id de la destacada (o `null` si no n'hi ha o és Arxivades).

- [ ] **Step 1:** A `PersonDetailScreen.kt`, el `koinViewModel` per defecte rep `key = "person-$personId"`. Sense la clau, un `PersonDetailRoute` que canvia de `personId` al mateix lloc reaprofita el ViewModel de l'anterior i mostra una altra persona.
- [ ] **Step 2:** A `AppNavigation.kt`, un `var featuredId by rememberSaveable { mutableStateOf<Long?>(null) }` que omple la `PeopleListRoute` de `PeopleRoute` amb `onFeaturedChanged`. Treu el cos de l'entrada `PersonRoute` a una composable local `PersonDetailPane(personId: Long, openNewEntry: Boolean, onPersonGone: () -> Unit)` amb els mateixos callbacks, perquè el panell de la destacada no els dupliqui.
- [ ] **Step 3:** El `detailPlaceholder` de `PeopleRoute` passa a ser `{ featuredId?.let { key(it) { PersonDetailPane(it, openNewEntry = false, onPersonGone = {}) } } ?: EmptyDetailPane() }`. Arxivades continua amb `EmptyDetailPane()`. `onPersonGone` no fa res: si s'esborra o s'arxiva, la destacada es recalcula.
- [ ] **Step 4:** El `selectedPersonId` de la `PeopleListRoute` de `PeopleRoute` passa a ser `navigation.openPersonId ?: featuredId`, perquè la fila quedi marcada.
- [ ] **Step 5:** `./gradlew ktlintCheck testDebugUnitTest :app:assembleDebug` → BUILD SUCCESSFUL.
- [ ] **Step 6: Emulador de tauleta o plegable** (o el mòbil en horitzontal ≥ 600 dp): en obrir l'app, el detall mostra la destacada i la seva fila queda marcada. Canviar de filtre canvia el detall a la nova destacada (amb les seves dades, no les de l'anterior). Tocar una altra fila l'obre normalment; enrere torna a la destacada. Passar a < 600 dp mostra el llistat, no el detall. Arxivades mostra el panell buit. Sense cap entrada, el panell buit. Protegir la destacada canvia el panell a la següent, sense demanar l'empremta.
- [ ] **Step 7:** Commit: `Show the featured person in the empty detail pane on large screens`.
