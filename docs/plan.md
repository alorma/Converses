# Converses — Pla i estat

Actualitzat el 8 d'oct. de 2026 · @Bernat · **la v1 és publicada a Google Play (pista beta).** Aquest document només recull l'estat actual, el que queda per fer i la referència tècnica; el detall de la feina ja feta és a l'historial de git.

## Resum

Converses és una app Android per recordar què has dit, què t'han dit i què ha passat amb cada persona amb qui parles, sigui per xat, per xarxes o en persona. Es construeix amb Jetpack Compose i Material 3 Expressive, en monocrom amb color dinàmic. El color apareix als avatars (pastel), i als errors (vermell); amb el color dinàmic desactivat, l'app fa servir el tema Grafit, en grisos neutres.

- **Referència visual:** el llenç de disseny «Converses» i el design system «Converses Design System» (tokens, components i pautes).
- **Abast de la v1:** onboarding, llistat, Arxivades, detall amb cronologia, nova entrada (full i nota ràpida), crear i editar persona, persona protegida, bloqueig de l'app, accions d'entrada, exportar notes, arxivar, esborrar i Ajustos. **La v1 no té Pro.**
- **Fora de la v1:** vincle amb l'agenda (descartat), cerca (ara a la iteració 2), còpies de seguretat pròpies, widgets i etiquetes.

## Regla de la base de dades

**La v1 ja és publicada: tot canvi de la base de dades ha d'incloure una migració.** Qualsevol canvi d'esquema puja la `version` de `ConversesDatabase`, desa el nou JSON a `app/schemas` (el `1.json` no es toca mai més), afegeix la `Migration` (o `AutoMigration`) registrada a `di/AppModules.kt` i el seu test amb `MigrationTestHelper` (`room-testing` ja és a `androidTestImplementation`). La recreació automàtica (`fallbackToDestructiveMigration`) només és en debug; a release esborraria les dades dels usuaris.

## On som

| Fase | Estat |
| --- | --- |
| 0 · Base (projecte, dades, pantalles) | Fet |
| M1 · Polit visual | Fet (el design system es va sincronitzant a part) |
| M2 · Fluxos que falten | Fet |
| M3 · Privacitat | Fet |
| Backlog de disseny (tandes 1–3) i de bugs | Fet |
| Onboarding (tanda 4) | Fet |
| M4 · Qualitat | Fet (amb excepcions acceptades) |
| Versió gratuïta (flags, avatar estàndard, redisseny de l'editor) | Fet |
| Menú de debug | Fet |
| M5 · Publicació | Publicada a beta; fitxa de Play feta, queda provar `release.yml` amb Gradle Play Publisher (a `internal`) |
| Iteració 2 | Després de la publicació |
| Pro | Planificat (`docs/pro.md`) |

## Pendent

### Abans de producció (M5)

- [x] **Mida de la font:** `roboto_flex.ttf` passa d'1,7 MB a 151 KB: subconjunt llatí (en/ca/es) i eixos limitats als que fa servir `Type.kt` (opsz 14–72, wght 400–800, wdth 100–118; la resta fixos). Es regenera amb `designsystem/fonts/subset.sh <original>`. Els caràcters fora del llatí (noms en altres alfabets) cauen a la font del sistema.
- [x] **Provar el release sencer:** l'APK de release (R8) provat a l'emulador: llistat, crear persona, detall, nova entrada, nota ràpida amb persona, persona protegida (PIN amb `BiometricPrompt`), Ajustos (tema, color dinàmic, idioma) i exportar. Sense crashs ni regles de Koin, `NavKey` o Room: les regles de les llibreries n'hi ha prou.
- [x] **`isShrinkResources`:** no cal; amb `optimization { enable = true }` AGP 9 ja redueix els recursos (`resources.txt` al mapping).
- [ ] **Símbols natius:** `ndk { debugSymbolLevel = "SYMBOL_TABLE" }` és al `buildTypes.release` i funciona: amb l'NDK 28.2.13676358 (el que demana AGP 9.4) instal·lat, l'AAB porta els `debugsymbols` de `libdatastore_shared_counter.so` en les quatre arquitectures. `libandroidx.graphics.path.so` ja ve sense símbols i no es pot arreglar. Falta comprovar-ho al CI: només el `release.yml` construeix l'AAB; mirar a la primera release que l'AAB els porti i que Play deixi d'avisar. No bloqueja.
- [x] **Fitxa de Play per a producció:** textos, icona, gràfic de funcions i 5 captures en en/ca/es a `app/src/main/play`, publicats amb `publish-listing.yml`; formulari de seguretat de les dades fet.

### Proves manuals

- [x] **Res de Pro visible** amb tots els flags apagats (llista de «Proves» a `docs/pro.md`) i els dos modes amb el «Pro mode» del menú de debug.
- [x] **Onboarding:** primera obertura i no tornar a sortir després de Comença/Omet; enrere al pas 1 surt sense marcar-lo; lliscar llança l'animació de la pàgina d'arribada; amb les animacions del sistema desactivades es veu l'estat final; clar i fosc; ca/es/en amb font gran; les animacions infinites del pas 1 s'aturen en sortir de la pàgina.
- [x] **Menú de debug:** arrossegar el botó, tornar a mostrar l'onboarding, `FLAG_SECURE` al full.
- [x] **Bloqueig desactivat sol** (dispositiu real): quan el dispositiu es queda sense bloqueig de pantalla, les protegides continuen protegides i el desbloqueig ofereix «Configura la seguretat».
- [x] **Bloqueig d'una protegida** a dispositiu real: massa intents i empremta.
- [x] **Color de la persona** amb `PersonColorFeature` encès (emulador).
- [x] **TalkBack real:** l'ordre de lectura i els fulls.

### Petites millores

- [x] **Snackbar de desfer:** el detall de persona usa `Scaffold` (split button al slot `floatingActionButton`, snackbar a `snackbarHost` amb 16 dp de marge), així l'snackbar s'apila sol sobre el botó sense `snackbarLift`.
- [x] **`Scaffold` a totes les pantalles:** llista/Arxivades (barra flotant al slot FAB amb `FabPosition.Center`), Ajustos, editor, nota ràpida, detall, bloqueig d'app, persona protegida i onboarding. Les capçaleres (enrere, títol, accions) són al `topBar` i no fan scroll: Ajustos (gran), editor i nota ràpida (mitjana) usen `ConversesTopBar` (`MediumTopAppBar`/`LargeTopAppBar` amb el títol a la segona línia i estils de títol del disseny); llista i detall de persona mantenen capçalera pròpia. Regla al `CLAUDE.md`. La pantalla de persona protegida no es pot capturar a l'emulador (`FLAG_SECURE`): es valida amb les captures de preview del CI.
- Descartat: portar l'estat d'snackbar d'`alorma/Caducity` (cua, `CompositionLocal`, animació); amb el `Scaffold` l'`SnackbarHost` de M3 ja posiciona i apila bé.
- [ ] **Font 2×:** l'avatar petit del xip de filtre de la cronologia es veu mal alineat; els títols grans (44/40 sp) podrien tallar-se (`autoSize` amb mínim de 28 sp o `maxLines = 2`). Acceptat tal com està.
- [x] **Transicions entre pantalles:** substituït el fade pla de 700 ms de Nav3 per transicions suaus properes a les natives d'Android a `ui/AppNavigation.kt` (`nativeTransition`): endavant, la pantalla nova llisca poc (1/12 de l'amplada) amb fade; enrere, només la que surt es redueix (0.9) i fa fade, com el back predictiu. El gest de back predictiu (`predictiveBack`) segueix el dit: la pantalla es redueix i es mou cap al costat del gest, i fa fade només al final. Les modals (QuickNote, EditPerson) entren en vertical i no mouen la pantalla de sota.
- [ ] **CI:** els tests de migració (`MigrationTestHelper`) són instrumentats i el CI no en corre cap; decidir-ho quan arribi la primera migració.

### Design system (feina de Bernat a Claude Design)

Sense data: es va sincronitzant amb l'app a mesura que convé i no bloqueja res. Canvis acumulats a l'app: colors d'error fixos; split button de 56 dp i el seu menú; full d'accions; full de nova entrada i nota ràpida; full de triar persona; pantalla de bloqueig amb els quatre estats; avatar protegit; tema d'Ajustos com a `ButtonGroup` connectat; filtre de canal com a `FilterChip` de multiselecció; files d'Ajustos; pastilles de canal; estats buits (el del llistat sense persones, a pantalla completa) i snackbar; barra flotant amb tres destinacions; fila «Quan» i full «Quan va ser?»; selector «Qui» i marcadors; verd d'èxit de l'onboarding; avatar estàndard i full «Avatar».

## Iteració 2 (després de la publicació)

El detall és a `docs/backlog.md`. Els punts 6, 8 i 13 necessiten migració (vegeu «Regla de la base de dades»); el 11 no. Els punts 12, 14, 15 i 17 vénen del feedback de la beta (issues #40, #41, #43 i #44; el #45 s'ha descartat) i són tasques independents; el 13 (canals) va primer i porta la migració v1 → v2.

- **Refactor dels canals (13, issue #42), la primera tasca:** `ChannelType` passa a En persona, Telèfon, Xat, Social i Altres; els canals actuals es migren al seu grup (migració v1 → v2, equivalències a decidir a la tasca), avís genèric als usuaris i badge «beta» al títol de la llista. Gratis.
- **Follow-ups / entrades pendents o planificades (6, issue #5):** flag `pending` a `EntryEntity` i interruptor a `EntryForm`, sota de «Quan». Primera migració (v1 → v2). És Pro.
- **Sub-canals (8):** dins dels 5 grups, amb nom lliure (únic per persona) i valor opcional. Absorbeix «Empresa» (7). És Pro. Depèn del 13.
- **Importar text a la nota ràpida, «share invers» (11, issue #30):** compartir text des d'altres apps (`ACTION_SEND`), seleccionar text (`ACTION_PROCESS_TEXT`) i un botó «Enganxa» al porta-retalls; la nota ràpida s'obre amb el text posat i només cal triar la persona. Sense canvis a Room, no cal migració. És gratis.
- **Feedback de la beta, tasques independents (després del 13):**
  - **12 · Nota ràpida (bug, #40):** el xip «Persona» de «Qui» sense persona obre el selector en comptes de ser desactivat.
  - **14 · Accions del detall (#44):** FAB estès només per a «Nota»; a la `topBar`, icones Editar i Compartir i un «⋮» amb Arxivar.
  - **15 · Ampliar «Quant a» als Ajustos (#43):** la secció ja existeix (privadesa i versió); s'hi afegeixen autor, repositori i feedback, sense llicències de moment. Dades per decidir.
  - **17 · Cerca (#41):** nom i text de les entrades; les protegides només pel nom. `LIKE` o FTS (FTS exigeix migració).

## Després de la v1

- **Pro** (pagament únic): vegeu `docs/pro.md`.
- **Exportar i importar tot:** un fitxer (JSON o ZIP xifrat) per canviar de mòbil sense el backup de Google.
- **Etiquetes** i la vista de **dades** de totes les persones.
- **Widgets:** de nota ràpida i, més endavant, altres (per exemple, persones recents).
- **App per a Wear OS:** nota ràpida des del rellotge. Cal veure què comparteix amb el mòbil i com es sincronitzen les dades, perquè les protegides no han de sortir del telèfon.
- **Recordatoris** a partir de les dades (per exemple, l'aniversari).
- **Splash que segueixi el tema triat a l'app:** ara segueix el mode del sistema. Es resoldria amb `core-splashscreen` (`installSplashScreen()`, `postSplashScreenTheme` i `setKeepOnScreenCondition`). Sense pressa.

---

## Referència

### Stack

Una sola activitat (`MainActivity`, una `FragmentActivity` pel `BiometricPrompt`) i dos mòduls: `:app` i `:designsystem`. `minSdk = 31` (Android 12, color dinàmic garantit) i `compileSdk = 37`.

| Peça | Llibreria i versió | Notes |
| --- | --- | --- |
| UI | `material3` 1.5.0-beta01 (BOM Compose 2026.09.00) | Les API d'Expressive no són públiques a la 1.4.0 estable |
| Formes | `graphics-shapes` 1.1.0 | Formes dels avatars |
| Color | `material-kolor` 5.0.1 | `Neutral` amb color dinàmic, `Monochrome` (Grafit) sense |
| Navegació | Navigation 3 1.2.0 + `lifecycle-viewmodel-navigation3` | Rutes tipades amb `NavKey` serialitzables |
| Dades | Room 2.8.5 + KSP 2.3.12 | Versió 1 publicada; tot canvi necessita migració |
| DI | Koin 4.2.2 | |
| Preferències | DataStore Preferences 1.2.1 | `SettingsRepository` i `FeatureFlagStore` |
| Haptic | `ConversesHaptics` (`:designsystem`) | Única font del feedback haptic: `Confirm` en desar i esborrar, `Reject` en tocar Desa desactivat, `ToggleOn/Off` als interruptors, `SegmentTick` als selectors i xips. Mai al scroll ni a la navegació. Respecta el sistema i l'ajust «Feedback tàctil» d'Ajustos (`hapticFeedback`, per defecte actiu) |
| Ajustos UI | Compose-Settings `ui-tiles-expressive` 3.3.0 | Només `SettingsGroup`; les files són pròpies (`SettingRow`) |
| Biometria | `androidx.biometric` 1.1.0 | `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` |
| Onboarding | FireAndForget 1.3.0 (`com.github.alorma.fire-and-forget`, `core`) | `OnboardingFlag` amb un runner propi sobre DataStore |
| Captures | Compose Preview Screenshot Testing 0.0.1-alpha16 | Als dos mòduls |
| Altres | Kotlin 2.4.20, AGP 9.4.1, lifecycle 2.11.0, coroutines 1.11.0, serialization 1.11.0 | |

### Pantalles i navegació

| Destinació | Ruta (`NavKey`) | Notes |
| --- | --- | --- |
| Onboarding | `OnboardingRoute` | Només el primer cop (`OnboardingFlag`); en acabar substitueix la pila per `PeopleRoute` |
| Llistat | `PeopleRoute` | Xips de filtre per canal (multiselecció, OR entre grups; cap marcat = tothom), nova persona a la capçalera i fila final, barra flotant (Persones · Arxivades · Ajustos) |
| Arxivades | `ArchivedRoute` | Sense filtres de canal ni fila d'afegir; desarxivar només des del detall |
| Detall | `PersonRoute(id)` | Només «Cronologia» és títol de secció; les dades són un bloc plegable sense títol (`CollapsibleFacts`: plegat mostra la primera dada i la pastilla «+N»; l'estat és efímer). Split button «Nota ▾», llapis d'editar, ⋮ a cada entrada, `FLAG_SECURE` si és protegida. Persona amb poques dades: subtítol «Des de {mes any}», pastilla «+ On hi parles», fila «Afegeix una dada» i entrada fantasma amb Jo / {nom} / Apunt (obre Nova entrada amb aquest «Qui» i el teclat); desapareixen quan hi ha el primer canal, dada o entrada |
| Desbloqueig | `PersonLockedRoute(id)` | `BiometricPrompt` automàtic; si va bé, substitueix la ruta pel detall |
| Nota ràpida | `QuickNoteRoute(personId?)` | Sense persona per defecte; una protegida demana l'empremta en triar-la |
| Editar/crear | `EditPersonRoute(personId, focus)` | `0` = persona nova; en crear, va al detall; `focus` (`EditFocus.CHANNELS` / `NEW_FACT`) fa scroll fins a «On hi parles» o afegeix una dada i en posa el focus |
| Ajustos | `SettingsRoute` | Inclou la política de privadesa |
| Fulls i diàlegs | Dins del detall i de l'editor | Nova entrada, accions d'entrada, menú del split button, triar persona, «Quan», «Avatar», esborrar |

### Model de dades (Room, versió 1)

| Taula | Camps |
| --- | --- |
| `person` | `id`, `name`, `alias?`, `avatarShape?`, `avatarColor?` (`null` = estàndard), `isProtected`, `archived`, `notes?`, `createdAt`, `updatedAt` |
| `channel` | `id`, `personId`, `type` (conjunt de canals de la persona, sense usuari) |
| `entry` | `id`, `personId`, `kind` (`SAID`, `HEARD`, `HAPPENED`), `channelType`, `text`, `occurredAt` |
| `pinned_fact` | `id`, `personId`, `label`, `value`, `position` |

Totes les claus `personId` tenen `onDelete = CASCADE`, i hi ha un índex a `entry(personId, occurredAt)`. Canals (`ChannelType`, en aquest ordre): En persona · WhatsApp · Telegram · X · Instagram · Telèfon · Correu · Discord · Signal · LinkedIn · Reddit. Filtres del llistat: Chat = WhatsApp, Telegram, Signal, Discord, Correu; Social = X, Instagram, LinkedIn, Reddit.

### Tema

- **Color dinàmic (per defecte):** la llavor és el `primary` de `dynamicLightColorScheme`, amb l'esquema MaterialKolor `Neutral`.
- **Color dinàmic desactivat (Grafit):** grisos purs, amb llavor negra i l'esquema MaterialKolor `Monochrome` (`primary` negre en clar i blanc en fosc). No hi ha selector de color: a Ajustos, «Color dinàmic» és l'última fila d'Aparença (sense el flag de Pro) i el subtítol és sempre «Pren el to del fons de pantalla». Proves: `GraphiteThemeTest` (grisos purs i contrast ≥ 4,5:1). Les previews i les captures fan servir aquest tema.
- **Mode:** clar, fosc o sistema.
- **Error fix:** clar `#BA1A1A` sobre `#FFDAD6`; fosc `#FFB4AB` sobre `#93000A`. **Èxit** (només a l'onboarding): `SuccessColors`, fixos.
- **Avatars:** 7 pastels i 4 formes (`Cookie9Sided`, `Clover4Leaf`, `Flower`, `Sunny`). A la v1 tots són estàndard: Galeta i color automàtic pel nom (`AvatarColor.forName`, hash FNV-1a del nom normalitzat). Inicials en `#1E1D24` (primera lletra del primer i de l'últim mot), cadenat si la persona és protegida.
- **Tipografia:** Roboto Flex. Els títols (44/40/30/28/24/22 sp) van en pes 800, amplada 118 i mida òptica 72.
- **Formes:** llistes agrupades de 24 dp als extrems i 6 dp entre files, separades per 2 dp. Els grups de botons van amb juntes de 8 dp.
- **Icona i splash:** adaptive icon (bombolla amb boletes) amb variant fosca del fons; l'splash de plataforma fa servir `window_background` (neutres dinàmics). En debug la icona és vermella.

### Color de la persona (Pro, apagat a la v1)

Darrere de `PersonColorFeature`. Encès, cada persona porta el color de l'avatar com a llavor del tema de les seves pantalles (detall, desbloqueig, editor, nota ràpida amb persona i els seus fulls); el llistat, Arxivades, Ajustos i triar persona es queden amb el tema de l'app. `PersonTheme` al `designsystem` (MaterialKolor `TonalSpot` amb `AvatarColor.seed`). Llavors: Lavanda `#6B55B5`, Menta `#2F7A52`, Préssec `#B5602E`, Cel `#2D6FA3`, Rosa `#B04670`, Llimona `#8A7A12`, Sorra `#86704C`. Ajust «Color de la persona» (`person_color_theme`), només visible amb el flag encès. Proves: `PersonThemeContrastTest` i captures `personDetailThemed`.

### Formulari d'entrada, «Quan» i «Qui»

- `EntryForm` és l'únic formulari (nota ràpida, nova entrada i edició), amb els slots `header` («Amb qui», només a la nota ràpida) i `saveButton`. Ordre: Qui, text, On, Quan.
- «On»: `FilterChip` de selecció única amb els canals de la persona (o tots si no en té); per defecte, el canal de la seva última entrada.
- «Quan»: fila `WhenRow` que obre `WhenSheet` («Quan va ser?»): Ara, Fa una hora, Ahir a aquesta hora i Tria dia i hora…. `WhenChoice` (`Now`, `HourAgo`, `Yesterday`, `Custom`, `Stored`); «Ara» es resol en desar i un `Custom` en el futur es retalla a l'hora actual.
- «Qui»: `EntryKind` no canvia; es mostra com **Jo**, **{primer nom}** i **Apunt**. `whoLabel(...)` és l'única funció d'etiquetes (cronologia, full d'accions i exportació). Selector `WhoSelector` (`ConversesToggleGroup` prominent). Cronologia amb marcadors de 32 dp i filtres de multiselecció.
- Exportació: text pla `[data · canal] Jo: text`, compartit amb el full del sistema.

### Privacitat i bloqueig

- `PersonLockedScreen` només rep el nom, la forma i el color de l'avatar; res de la persona es carrega fins que el desbloqueig va bé. `LockState`: `Waiting`, `Canceled(permanentLockout)`, `LockedOut(seconds)` i `NoCredentials` (amb «Configura la seguretat»).
- `FLAG_SECURE` (`SecureWindow`) al detall, l'editor, la nota ràpida i els fulls d'una protegida. No es torna a bloquejar del detall a l'editor de la mateixa persona; sí en tornar al llistat.
- La base de dades i els DataStore són fora del backup d'Android. Cap drecera ni suggeriment de compartir amb el nom d'una protegida. El llistat manté l'ordre per última activitat.

### Feature flags i menú de debug

- **Flags** (`data/flags/`, estil Caducity): una classe per flag amb `key`, `defaultValue` i `pro` (`CustomAvatarFeature`, `PersonColorFeature`), `LocalFeatureFlagRunner` amb *overrides* persistits al DataStore `feature_flags` (`FeatureFlagStore`). Tots apagats a la v1. A Compose, `rememberFeatureEnabled<...>()`. Cada flag es registra a Koin amb `bind FeatureFlag::class`.
- **Menú de debug** (dins de `:app`): `src/debug/` té `DebugMenuHost` (botó flotant arrossegable i `ModalBottomSheet`), `DebugModule`, `DebugCell` i els mòduls Dades (sembrar dades d'exemple), Onboarding (tornar-lo a mostrar) i Feature flags («Pro mode», un interruptor per flag, «Reset flags»). `src/release/` només té un `DebugMenuHost` que crida el contingut; l'APK de release no conté res del menú. Cap mòdul mostra dades de persones protegides.

### Components propis (`designsystem`)

`Avatar`, `GroupedList` / `groupedItems`, `TimelineRow` i `TimelineMarker`, `ConversesFilterChip`, `ConversesChannelChip`, `ConversesToggleGroup`, `ConversesFloatingToolbar` i `ToolbarDestination`, `ConversesSplitButton` i `ConversesMenuItem`, `SettingRow` / `SettingSwitchRow`, `DashedActionRow`, `EmptyState` i `WelcomeEmptyState` (llistat sense cap persona), `ColorSwatches`, `ConversesLoadingIndicator`, `ConversesTheme`, `PersonTheme` i `AppColors`. A l'app: `PersonPickerSheet`, `ChannelsCard`, `AvatarSheet`.

### Tests

- **Unitaris (JVM):** `ViewModel`s, repositori, flags (`FeatureFlagTest`), avatar (`AvatarColorTest`) i contrast (`PersonThemeContrastTest`). `FakeDb` amb DAOs en memòria i `MainDispatcherRule`; sense Room ni Robolectric. Sense tests de UI instrumentats (decisió: compliquen el CI).
- **Captures** ([Compose Preview Screenshot Testing](https://developer.android.com/studio/preview/compose-screenshot-testing)): previews amb `PreviewData` i `ConversesPreviewTheme`; tests a `src/screenshotTest` (`ComponentsScreenshotTests`, `ScreensScreenshotTests`), noms en camelCase sense `_`, clar i fosc, 411×891 dp; algunes pantalles també en ca, es i font 2×. Referències a `<mòdul>/src/screenshotTestDebug/reference/`, llindar de l'1 %. Sense captura possible: el menú del split button i el snackbar de desfer.

### CI i release

- `pr-build.yml` i `build-apk.yml`: tests unitaris, APK de debug i release.
- `publish-listing.yml` (manual): puja la fitxa de Play (`publishReleaseListing`) des de `app/src/main/play`. Comparteix el grup de concurrència `play-publish` amb `release.yml`, perquè Play només admet una edició oberta a la vegada.
- `screenshot-tests.yml`: valida les captures a cada PR i deixa un comentari amb les diferències; l'etiqueta `update-screenshots` regenera les referències; a `main`, si alguna falla, obre una PR des de `screenshots/main-update`.
- `release.yml` (manual, sobre `main`, entrada `track`, beta per defecte): construeix l'APK i l'AAB signats, puja l'AAB i les notes de la versió de `app/src/main/play/release-notes` amb Gradle Play Publisher (`publishReleaseBundle`, `SERVICE_ACCOUNT_JSON` com a `ANDROID_PUBLISHER_CREDENTIALS`, paquet `com.alorma.contactnotes`; el mapping de R8 va dins l'AAB), crea el tag i la GitHub Release i puja la versió menor a `main`.
- **Signatura:** la `signingConfig` `release` només existeix si hi ha `certs/release.keystore` (fora del repo) i `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`; a la CI la keystore ve del secret `RELEASE_KEYSTORE_BASE64`. Debug: `app/debug.keystore` al repo i `applicationIdSuffix = ".debug"`.
- **Versió:** plugin `io.github.alorma.version` a partir de `version.properties`; no s'escriu a mà a `build.gradle.kts`.
