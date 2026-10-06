# Converses — Pla i estat

Actualitzat el 6 d'oct. de 2026 · @Bernat

## Resum

Converses és una app Android per recordar què has dit, què t'han dit i què ha passat amb cada persona amb qui parles, sigui per xat, xarxes o en persona. Es construeix amb Jetpack Compose i Material 3 Expressive, en monocrom amb color dinàmic; el color només apareix als avatars (pastel), als errors (vermell) i, si es desactiva el color dinàmic, al tema triat.

- **Referència visual:** el llenç de disseny «Converses» (Material 3 Expressive, clar i fosc); les captures ja no són al repositori.
- **Abast v1:** llistat, detall amb cronologia, nova entrada (full i nota ràpida), editar/crear persona, persona protegida, accions d'entrada, arxivar, esborrar persona i Ajustos.
- **Fora de la v1:** vincle amb l'agenda (descartat), cerca (treta de moment), cerca dins el text de les entrades, còpies de seguretat i widgets.

## Estat

| Pas | Què | Estat |
| --- | --- | --- |
| 1 | Projecte, mòdul `designsystem`, tema, `GroupedList`, avatar | Fet |
| 2 | Room: entitats, DAO, repositori, dades de prova (només debug) | Fet |
| 3 | Llistat amb filtres i barra d'eines flotant | Fet (sense cerca) |
| 4 | Detall: capçalera, dades fixades, cronologia amb filtres | Fet |
| 5 | Nova entrada: full des del detall i nota ràpida des del llistat | Fet |
| 6 | Editar/crear persona: forma, color, àlies amb validació, dades fixades, canals, protecció | Fet |
| 7 | Split button amb menú, accions d'entrada, arxivar, esborrar persona | Fet |
| 8 | Vincle amb l'agenda | Descartat de la v1 |
| — | Persona protegida (pantalla de desbloqueig i `BiometricPrompt`) | Fet |
| — | Ajustos (tema, color, idioma, bloqueig de l'app) | Fet |
| — | Idiomes: anglès (per defecte), català i castellà | Fet |
| — | Tests de captures (Compose Preview Screenshot Testing) i workflow que les valida a cada PR | Fet (falten les referències) |

## Stack (el que hi ha ara)

Una sola activitat (`MainActivity`, `FragmentActivity` pel `BiometricPrompt`) amb dos mòduls: `:app` i `:designsystem`. `minSdk = 36`, `compileSdk = 37`.

| Peça | Llibreria i versió | Notes |
| --- | --- | --- |
| UI | `material3` 1.5.0-alpha29 (BOM Compose 2026.09.00) | A la 1.4.0 estable les API d'Expressive són internes; cal l'alpha |
| Formes | `graphics-shapes` 1.1.0 | Formes dels avatars |
| Color | `material-kolor` 5.0.1 | `Neutral` amb color dinàmic, `TonalSpot` amb el color triat |
| Navegació | Navigation 3 1.2.0 + `lifecycle-viewmodel-navigation3` | Rutes tipades `NavKey` serialitzables |
| Dades | Room 2.8.4 + KSP 2.3.12 | Base de dades versió 2, esquemes a `app/schemas` |
| DI | Koin 4.2.2 | (No Hilt) |
| Preferències | DataStore Preferences 1.2.1 | `SettingsRepository` |
| Ajustos UI | Compose-Settings `ui-tiles-expressive` 3.3.0 | |
| Biometria | `androidx.biometric` 1.1.0 | `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` |
| Captures | Compose Preview Screenshot Testing 0.0.1-alpha16 | Experimental; als dos mòduls |
| Altres | Kotlin 2.4.20, AGP 9.4.1, lifecycle 2.11.0, coroutines 1.11.0, serialization 1.11.0 | |

`@OptIn(ExperimentalMaterial3ExpressiveApi)` viu al `designsystem`; l'única excepció és `SettingsScreen`, perquè la llibreria de tiles ho exigeix.

## Tema

- **Color dinàmic (per defecte):** llavor = `primary` de `dynamicLightColorScheme`, esquema MaterialKolor `Neutral`.
- **Color dinàmic desactivat:** 7 colors triables (Lavanda, Menta, Préssec, Cel, Rosa, Llimona, Sorra) amb esquema `TonalSpot`.
- **Mode:** clar, fosc o sistema. S'ha tret l'estil «Pur» (monocrom).
- **Error fix:** clar `#BA1A1A` sobre `#FFDAD6`, fosc `#FFB4AB` sobre `#93000A`.
- **Avatars:** 7 pastels fixos i 4 formes (`Cookie9Sided`, `Clover4Leaf`, `Flower`, `Sunny`); inicials sempre en `#1E1D24`; cadenat si la persona és protegida.
- **Tipografia:** Roboto Flex variable. Els títols (44/40/30/28/24/22 sp) van en pes 800, amplada 118 i mida òptica 72; la resta segueix l'escala de Material.
- **Formes:** llistes agrupades amb 24 dp als extrems i 6 dp entre files, separades 2 dp.

## Components propis (`designsystem`)

`Avatar` (forma, color, cadenat), `GroupedList` / `groupedItems`, `TimelineRow` i `TimelineMarker`, `ConversesFilterChip`, `ConversesToggleGroup`, `ConversesFloatingToolbar` i `ToolbarDestination`, `ConversesSplitButton`, `ColorSwatches`, `ConversesTheme`.

## Pantalles i navegació

| Destinació | Ruta (`NavKey`) | Notes |
| --- | --- | --- |
| Llistat | `PeopleRoute` | Filtres per canal i «Arxivades»; botó de nova persona; barra flotant amb engranatge |
| Detall | `PersonRoute(id)` | Split button «Nota ▾», ⋮ per entrada, `FLAG_SECURE` si és protegida |
| Desbloqueig | `PersonLockedRoute(id)` | `BiometricPrompt` automàtic; en èxit substitueix la ruta pel detall |
| Nota ràpida | `QuickNoteRoute(personId)` | Selector de persona, canal, tipus, text i hora |
| Editar/crear | `EditPersonRoute(personId)` | `0` = persona nova |
| Ajustos | `SettingsRoute` | |
| Nova entrada, accions d'entrada, menú del split button, esborrar persona | Dins del detall i de l'editor | `ModalBottomSheet`, `DropdownMenu` i `AlertDialog` |

## Model de dades (Room, versió 2)

| Taula | Camps |
| --- | --- |
| `person` | `id`, `name`, `alias?`, `avatarShape`, `avatarColor`, `isProtected`, `archived`, `notes?`, `createdAt`, `updatedAt` |
| `channel` | `id`, `personId`, `type`, `handle?` (`IN_PERSON` sense `handle`) |
| `entry` | `id`, `personId`, `kind` (`SAID`, `HEARD`, `HAPPENED`), `channelType`, `text`, `occurredAt` |
| `pinned_fact` | `id`, `personId`, `label`, `value`, `position` |

Totes les claus `personId` amb `onDelete = CASCADE`; índex a `entry(personId, occurredAt)`. En debug la base de dades es recrea sola si l'esquema canvia.

## Persona protegida

- S'activa a Editar persona; activar-la o desactivar-la demana l'empremta o el PIN (si el dispositiu no en té, l'interruptor es desactiva amb una explicació).
- Al llistat només es veu el nom, el cadenat i «Protegida · toca per desbloquejar»; el repositori no retorna ni última entrada ni canals.
- Per obrir-la: pantalla de desbloqueig i `BiometricPrompt`; el detall desbloquejat té `FLAG_SECURE`. Es torna a bloquejar en sortir del detall.

## Ajustos

DataStore: `theme_mode`, `dynamic_color`, `theme_color`, `app_lock`, `relock_after`.

- **Aparença:** tema (Clar · Fosc · Sistema), color dinàmic i selector de color.
- **Idioma:** obre l'idioma per app del sistema; idiomes declarats a `locales_config.xml`.
- **Seguretat:** bloqueig de l'app (empremta o PIN en obrir) amb «Torna a bloquejar» (immediatament, 1 o 5 minuts). Sense bloqueig de pantalla al dispositiu apareix «Configura la seguretat», i si hi havia el bloqueig actiu es desactiva sol per no quedar-se tancat fora.

## Tests de captures

Amb [Compose Preview Screenshot Testing](https://developer.android.com/studio/preview/compose-screenshot-testing), a `:designsystem` i a `:app`.

- **Previews** a `main`, `internal` i amb dades fixes: `designsystem/.../components/ComponentPreviews.kt` i, a l'app, `PeopleListPreviews.kt` i `PersonDetailPreviews.kt` amb `ui/preview/PreviewData.kt` (dates del 2025 perquè «Avui»/«Ahir» no canviïn). Fan servir `ConversesPreviewTheme` (sense color dinàmic, Lavanda).
- **Tests** a `src/screenshotTest`: `ComponentsScreenshotTests` (`designsystem`) i `ScreensScreenshotTests` (`app`) criden les previews amb `@PreviewTest`. Els noms dels mètodes de test van en camelCase sense `_` (el CI hi separa el nom de la preview). Cada test es renderitza en clar i fosc, en anglès; les pantalles a 411×891 dp.
- **Referències** a `<mòdul>/src/screenshotTestDebug/reference/`, es versionen. Llindar de diferència: 1 %.
- **Ordres:** `./gradlew validateDebugScreenshotTest` (validar), `./gradlew updateDebugScreenshotTest` (regenerar), i per a un sol test `./gradlew :app:updateDebugScreenshotTest --tests '*ScreensScreenshotTests.peopleListLoaded'`.
- **Fora de moment:** Editar persona, Ajustos, desbloqueig, fulls i diàlegs (depenen del ViewModel o són finestres a part).

**CI** (`.github/workflows/screenshot-tests.yml`, adaptat de [Sloy/sevibus-android#16](https://github.com/Sloy/sevibus-android/pull/16)):

- A cada PR cap a `main` valida les captures i deixa un sol comentari (s'actualitza a cada execució) amb la referència, la nova i la diferència de cada test que falla. Les imatges es pugen a la branca `screenshots/pr-<número>`, que s'esborra en tancar la PR.
- L'etiqueta `update-screenshots` regenera les referències dels tests que fallen, les commiteja a la branca de la PR i es treu sola. Els commits del workflow no disparen execucions noves: cal tornar a fer push per validar-les. Les PR de forks se salten.
- A cada push a `main`, si alguna captura falla, regenera les referències a `screenshots/main-update` i obre una PR assignada a qui ha fet el push (o actualitza l'oberta); quan `main` torna a passar, la tanca. Cal activar **Settings → Actions → General → Allow GitHub Actions to create and approve pull requests**.
- El comentari el genera `.github/scripts/screenshot_report.py` a partir dels JUnit de cada mòdul; l'informe HTML també es puja com a artefacte `screenshot-report`.

## Decisions preses

- Avatars amb el color pastel de cada persona, al llistat i al detall.
- **Exportar notes:** text pla (dades fixades, canals i cronologia) compartit amb el full del sistema; també es pot fer abans d'esborrar una persona.
- **Arxivar:** amaga la persona del llistat; hi ha un filtre «Arxivades» per recuperar-la («Desarxivar» al menú).
- **«Esborrar tota la conversa»:** esborra les entrades d'aquell dia i canal (el subtítol en diu el nombre).
- Vincle amb l'agenda i «Esborrar també de l'agenda»: fora de la v1.
- Idioma per defecte: anglès (`values/`), amb català (`values-ca/`) i castellà (`values-es/`).

## Pendent i coses conegudes

- **Xips de canal del detall:** es veuen però no fan res (no s'ha decidit si copien l'usuari o obren l'app).
- **Barra flotant del llistat:** només té «Persones» i «Ajustos»; les pestanyes del disseny (fixades, etiquetes) no tenen destinació.
- **Cerca:** treta del llistat de moment; si torna, cal respectar que les protegides només es troben pel nom.
- **Tests:** hi ha tests instrumentats del DAO (7) i tests de captures de components i de dues pantalles. Falten tests de ViewModel.
- **Referències de captures:** encara no n'hi ha (aquí no hi ha SDK d'Android). Es generen la primera vegada amb l'etiqueta `update-screenshots` a la PR; després cal revisar-les.
- **Release:** falten la signatura, les regles R8 i l'optimització del paquet; a cada PR un workflow comprova que l'APK de debug compila (`pr-build.yml`), i a cada push a `main` es construeix i es puja com a artefacte (`build-apk.yml`).
- **Mida de l'APK:** Roboto Flex variable pesa ~1,7 MB.
- **Comprovacions pendents a dispositiu real:** empremta (a l'emulador només s'ha provat el PIN) i el bloqueig de l'app de punta a punta.
