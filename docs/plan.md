# Converses — Pla i estat

Actualitzat el 8 d'oct. de 2026 · @Bernat · **la v1 és publicada a Google Play (pista alpha).** Aquest document només recull l'estat actual, el que queda per fer i la referència tècnica; el detall de la feina ja feta és a l'historial de git.

## Resum

Converses és una app Android per recordar què has dit, què t'han dit i què ha passat amb cada persona amb qui parles, sigui per xat, per xarxes o en persona. Es construeix amb Jetpack Compose i Material 3 Expressive, en monocrom amb color dinàmic. El color apareix als avatars (pastel), i als errors (vermell); amb el color dinàmic desactivat, l'app fa servir el tema Grafit, en grisos neutres.

- **Referència visual:** el llenç de disseny «Converses» i el design system «Converses Design System» (tokens, components i pautes).
- **Abast de la v1:** onboarding, llistat, Arxivades, detall amb cronologia, nova entrada (full i nota ràpida), crear i editar persona, persona protegida, bloqueig de l'app, accions d'entrada, exportar notes, arxivar, esborrar i Ajustos. **La v1 no té Pro.**
- **Fora de la v1:** vincle amb l'agenda (descartat), cerca, còpies de seguretat pròpies, widgets i etiquetes.

## Regla de la base de dades

**La v1 ja és publicada: tot canvi de la base de dades ha d'incloure una migració.** Qualsevol canvi d'esquema puja la `version` de `ConversesDatabase`, desa el nou JSON a `app/schemas` (el `1.json` no es toca mai més), afegeix la `Migration` (o `AutoMigration`) registrada a `di/AppModules.kt` i el seu test amb `MigrationTestHelper` (`room-testing` ja és a `androidTestImplementation`). La recreació automàtica (`fallbackToDestructiveMigration`) només és en debug; a release esborraria les dades dels usuaris.

## On som

| Fase | Estat |
| --- | --- |
| 0 · Base (projecte, dades, pantalles) | Fet |
| M1 · Polit visual | Fet (falta sincronitzar el design system) |
| M2 · Fluxos que falten | Fet |
| M3 · Privacitat | Fet (falta provar el bloqueig desactivat sol a dispositiu) |
| Backlog de disseny (tandes 1–3) i de bugs | Fet |
| Onboarding (tanda 4) | Fet (falten detalls i proves manuals) |
| M4 · Qualitat | Fet (amb excepcions acceptades) |
| Versió gratuïta (flags, avatar estàndard, redisseny de l'editor) | Fet (falta la prova final de «res de Pro visible») |
| Menú de debug | Fet (falta provar-lo a l'emulador) |
| M5 · Publicació | Publicada a alpha; només queda la fitxa de Play per a producció |
| Iteració 2 | Després de la publicació |
| Pro | Planificat (`docs/pro.md`) |

## Pendent

### Abans de producció (M5)

- [x] **Mida de la font:** `roboto_flex.ttf` passa d'1,7 MB a 151 KB: subconjunt llatí (en/ca/es) i eixos limitats als que fa servir `Type.kt` (opsz 14–72, wght 400–800, wdth 100–118; la resta fixos). Es regenera amb `designsystem/fonts/subset.sh <original>`. Els caràcters fora del llatí (noms en altres alfabets) cauen a la font del sistema.
- [x] **Provar el release sencer:** l'APK de release (R8) provat a l'emulador: llistat, crear persona, detall, nova entrada, nota ràpida amb persona, persona protegida (PIN amb `BiometricPrompt`), Ajustos (tema, color dinàmic, idioma) i exportar. Sense crashs ni regles de Koin, `NavKey` o Room: les regles de les llibreries n'hi ha prou.
- [x] **`isShrinkResources`:** no cal; amb `optimization { enable = true }` AGP 9 ja redueix els recursos (`resources.txt` al mapping).
- [x] **Símbols natius:** `ndk { debugSymbolLevel = "SYMBOL_TABLE" }` al `buildTypes.release`.
- [ ] **Fitxa de Play per a producció:** revisar la fitxa en ca/es/en, les captures en clar i fosc i el formulari de seguretat de les dades.

### Proves manuals

- [ ] **Res de Pro visible** amb tots els flags apagats (llista de «Proves» a `docs/pro.md`) i els dos modes amb el «Pro mode» del menú de debug.
- [ ] **Onboarding:** primera obertura i no tornar a sortir després de Comença/Omet; enrere al pas 1 surt sense marcar-lo; lliscar llança l'animació de la pàgina d'arribada; amb les animacions del sistema desactivades es veu l'estat final; clar i fosc; ca/es/en amb font gran; les animacions infinites del pas 1 s'aturen en sortir de la pàgina.
- [ ] **Menú de debug:** arrossegar el botó, tornar a mostrar l'onboarding, `FLAG_SECURE` al full.
- [ ] **Bloqueig desactivat sol** (dispositiu real): quan el dispositiu es queda sense bloqueig de pantalla, les protegides continuen protegides i el desbloqueig ofereix «Configura la seguretat».
- [ ] **Bloqueig d'una protegida** a dispositiu real: massa intents i empremta.
- [ ] **Color de la persona** amb `PersonColorFeature` encès (emulador).
- [ ] **TalkBack real:** l'ordre de lectura i els fulls.

### Petites millores

- [ ] **Onboarding:** «pop» del botó «Comença» al pas 3 i test del runner de `OnboardingFlag`.
- [ ] **Snackbar de desfer:** afinar posició i marges si no queda bé.
- [ ] **Font 2×:** l'avatar petit del xip de filtre de la cronologia es veu mal alineat; els títols grans (44/40 sp) podrien tallar-se (`autoSize` amb mínim de 28 sp o `maxLines = 2`). Acceptat tal com està.
- [ ] **CI:** els tests de migració (`MigrationTestHelper`) són instrumentats i el CI no en corre cap; decidir-ho quan arribi la primera migració.

### Design system (feina de Bernat a Claude Design)

- [ ] Sincronitzar el design system amb l'app: colors d'error fixos; split button de 56 dp i el seu menú (radi 24, elements de 52 dp); full d'accions; full de nova entrada (camp sense línia, ordre Qui → Text → On → Quan) i nota ràpida; full de triar persona i fila discontínua «Afegeix una persona»; pantalla de bloqueig amb els quatre estats; avatar protegit; filtre de canal i tema d'Ajustos com a `ButtonGroup` connectat; files d'Ajustos amb radis 24/6; pastilles de canal «On hi parles»; estats buits i snackbar; barra flotant amb tres destinacions; fila «Quan» i full «Quan va ser?»; selector «Qui» i marcadors; verd d'èxit de l'onboarding; avatar estàndard i full «Avatar».

## Iteració 2 (després de la publicació)

El detall és a `docs/backlog.md`. Totes dues necessiten migració (vegeu «Regla de la base de dades»).

- **Follow-ups / entrades pendents (6, issue #5):** flag `pending` a `EntryEntity` i interruptor a `EntryForm`, sota de «Quan». Primera migració (v1 → v2). És Pro.
- **Gestor de canals (8):** l'enum `ChannelType` passa a una taula de canals. Absorbeix «Empresa» (7). És Pro.

## Després de la v1

- **Pro** (pagament únic): vegeu `docs/pro.md`.
- **Cerca:** pel nom de la persona i dins el text de les entrades. Les protegides només es poden trobar pel nom.
- **Exportar i importar tot:** un fitxer (JSON o ZIP xifrat) per canviar de mòbil sense el backup de Google.
- **Etiquetes** i la vista de **dades** de totes les persones.
- **Widget** de nota ràpida.
- **Recordatoris** a partir de les dades (per exemple, l'aniversari).
- **Splash que segueixi el tema triat a l'app:** ara segueix el mode del sistema. Es resoldria amb `core-splashscreen` (`installSplashScreen()`, `postSplashScreenTheme` i `setKeepOnScreenCondition`). Sense pressa.

---

## Referència

### Stack

Una sola activitat (`MainActivity`, una `FragmentActivity` pel `BiometricPrompt`) i dos mòduls: `:app` i `:designsystem`. `minSdk = 31` (Android 12, color dinàmic garantit) i `compileSdk = 37`.

| Peça | Llibreria i versió | Notes |
| --- | --- | --- |
| UI | `material3` 1.5.0-alpha29 (BOM Compose 2026.09.00) | Les API d'Expressive no són públiques a la 1.4.0 estable |
| Formes | `graphics-shapes` 1.1.0 | Formes dels avatars |
| Color | `material-kolor` 5.0.1 | `Neutral` amb color dinàmic, `Monochrome` (Grafit) sense |
| Navegació | Navigation 3 1.2.0 + `lifecycle-viewmodel-navigation3` | Rutes tipades amb `NavKey` serialitzables |
| Dades | Room 2.8.4 + KSP 2.3.12 | Versió 1 publicada; tot canvi necessita migració |
| DI | Koin 4.2.2 | |
| Preferències | DataStore Preferences 1.2.1 | `SettingsRepository` i `FeatureFlagStore` |
| Ajustos UI | Compose-Settings `ui-tiles-expressive` 3.3.0 | Només `SettingsGroup`; les files són pròpies (`SettingRow`) |
| Biometria | `androidx.biometric` 1.1.0 | `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` |
| Onboarding | FireAndForget 1.3.0 (`com.github.alorma.fire-and-forget`, `core`) | `OnboardingFlag` amb un runner propi sobre DataStore |
| Captures | Compose Preview Screenshot Testing 0.0.1-alpha16 | Als dos mòduls |
| Altres | Kotlin 2.4.20, AGP 9.4.1, lifecycle 2.11.0, coroutines 1.11.0, serialization 1.11.0 | |

### Pantalles i navegació

| Destinació | Ruta (`NavKey`) | Notes |
| --- | --- | --- |
| Onboarding | `OnboardingRoute` | Només el primer cop (`OnboardingFlag`); en acabar substitueix la pila per `PeopleRoute` |
| Llistat | `PeopleRoute` | Filtres per canal, nova persona a la capçalera i fila final, barra flotant (Persones · Arxivades · Ajustos) |
| Arxivades | `ArchivedRoute` | Sense filtres de canal ni fila d'afegir; desarxivar només des del detall |
| Detall | `PersonRoute(id)` | Split button «Nota ▾», llapis d'editar, ⋮ a cada entrada, `FLAG_SECURE` si és protegida |
| Desbloqueig | `PersonLockedRoute(id)` | `BiometricPrompt` automàtic; si va bé, substitueix la ruta pel detall |
| Nota ràpida | `QuickNoteRoute(personId?)` | Sense persona per defecte; una protegida demana l'empremta en triar-la |
| Editar/crear | `EditPersonRoute(personId)` | `0` = persona nova; en crear, va al detall |
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

`Avatar`, `GroupedList` / `groupedItems`, `TimelineRow` i `TimelineMarker`, `ConversesFilterChip` i `ConversesConnectedFilterRow`, `ConversesChannelChip`, `ConversesToggleGroup`, `ConversesFloatingToolbar` i `ToolbarDestination`, `ConversesSplitButton` i `ConversesMenuItem`, `SettingRow` / `SettingSwitchRow`, `DashedActionRow`, `EmptyState`, `ColorSwatches`, `ConversesLoadingIndicator`, `ConversesTheme`, `PersonTheme` i `AppColors`. A l'app: `PersonPickerSheet`, `ChannelsCard`, `AvatarSheet`.

### Tests

- **Unitaris (JVM):** `ViewModel`s, repositori, flags (`FeatureFlagTest`), avatar (`AvatarColorTest`) i contrast (`PersonThemeContrastTest`). `FakeDb` amb DAOs en memòria i `MainDispatcherRule`; sense Room ni Robolectric. Sense tests de UI instrumentats (decisió: compliquen el CI).
- **Captures** ([Compose Preview Screenshot Testing](https://developer.android.com/studio/preview/compose-screenshot-testing)): previews amb `PreviewData` i `ConversesPreviewTheme`; tests a `src/screenshotTest` (`ComponentsScreenshotTests`, `ScreensScreenshotTests`), noms en camelCase sense `_`, clar i fosc, 411×891 dp; algunes pantalles també en ca, es i font 2×. Referències a `<mòdul>/src/screenshotTestDebug/reference/`, llindar de l'1 %. Sense captura possible: el menú del split button i el snackbar de desfer.

### CI i release

- `pr-build.yml` i `build-apk.yml`: tests unitaris, APK de debug i release.
- `screenshot-tests.yml`: valida les captures a cada PR i deixa un comentari amb les diferències; l'etiqueta `update-screenshots` regenera les referències; a `main`, si alguna falla, obre una PR des de `screenshots/main-update`.
- `release.yml` (manual, sobre `main`, entrada `track`, alpha per defecte): construeix l'APK i l'AAB signats, puja l'AAB a Google Play (`SERVICE_ACCOUNT_JSON`, paquet `com.alorma.contactnotes`), crea el tag i la GitHub Release i puja la versió menor a `main`.
- **Signatura:** la `signingConfig` `release` només existeix si hi ha `certs/release.keystore` (fora del repo) i `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`; a la CI la keystore ve del secret `RELEASE_KEYSTORE_BASE64`. Debug: `app/debug.keystore` al repo i `applicationIdSuffix = ".debug"`.
- **Versió:** plugin `io.github.alorma.version` a partir de `version.properties`; no s'escriu a mà a `build.gradle.kts`.
