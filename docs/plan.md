# Converses — Pla i estat (v2)

Actualitzat el 6 d'oct. de 2026 · @Bernat · revisió de les captures del 6 d'oct. (llistat, detall, nova entrada, accions d'entrada, menú del split button, editar persona, nota ràpida, persona protegida, Ajustos)

## Resum

Converses és una app Android per recordar què has dit, què t'han dit i què ha passat amb cada persona amb qui parles, sigui per xat, per xarxes o en persona. Es construeix amb Jetpack Compose i Material 3 Expressive, en monocrom amb color dinàmic. El color només apareix en tres llocs: als avatars (pastel), als errors (vermell) i, si es desactiva el color dinàmic, al tema triat.

- **Referència visual:** el llenç de disseny «Converses» i el design system «Converses Design System» (tokens, components i pautes).
- **Abast de la v1:** llistat, detall amb cronologia, nova entrada (full i nota ràpida), crear i editar persona, persona protegida, accions d'entrada, arxivar, esborrar i Ajustos.
- **Fora de la v1:** vincle amb l'agenda (descartat), cerca, cerca dins el text de les entrades, còpies de seguretat pròpies, widgets i etiquetes.

## On som

La base funcional de la v1 ja és feta. El que queda és:

- polir la interfície perquè coincideixi amb el disseny,
- completar els fluxos que falten,
- tancar uns quants forats de privacitat,
- fer la feina de qualitat i de publicació.

| Fase | Objectiu | Estat |
| --- | --- | --- |
| 0 · Base | Projecte, dades, totes les pantalles de la v1 | Fet |
| M1 · Polit visual | La UI coincideix amb el disseny i el design system | Per fer |
| M2 · Fluxos que falten | Triar i crear persona, arxivades, estats buits, desfer | Per fer |
| M3 · Privacitat | Les persones protegides no es filtren enlloc | Per fer |
| M4 · Qualitat | Tests, accessibilitat, mode fosc, dispositiu real | Per fer |
| M5 · Publicació | minSdk, signatura, R8, mida, migracions, Play Store | Per fer |
| Després de la v1 | Cerca, còpia de seguretat, etiquetes, widget | Idees |

## Decisions que cal prendre abans de començar

| # | Decisió | Recomanació | Afecta |
| --- | --- | --- | --- |
| D1 | **minSdk.** Ara és 36 (només Android 16+), cosa que deixa fora la gran majoria de mòbils | Baixar-lo a **31** (Android 12, color dinàmic garantit). Si la Play Store no és un objectiu, es pot deixar a 36 | M5, Ajustos |
| D2 | **Barra flotant del llistat.** Ara només té Persones i Ajustos | **Persones · Arxivades · Ajustos**, i treure «Arxivades» dels filtres de canal | M2 |
| D3 | **Xips de canal del detall.** Ara no fan res | Tocar obre l'app quan hi ha enllaç directe (WhatsApp `wa.me`, Telegram `t.me`, Telèfon `tel:`); mantenir premut copia l'usuari; «En persona» no és clicable | M2 |
| D4 | **Còpia de seguretat d'Android.** Per defecte, la base de dades amb les notes va a Google Drive | Excloure la base de dades del backup automàtic a la v1 (són notes privades) i oferir l'exportació manual | M3 |
| D5 | **Persona per defecte a la nota ràpida** | Cap: «Tria una persona». Mai una persona protegida per defecte | M2, M3 |
| D6 | **Ordre del llistat amb protegides.** Ordenar per l'última activitat delata quan has parlat amb una persona protegida | Opcional: ordenar les protegides per nom, al final o al principi | M3 |

---

## M1 · Polit visual

**Fet quan:** les captures en clar i en fosc coincideixen amb el llenç i el design system.

### Llistat

- [x] **Espai entre la capçalera i els filtres:** ara hi ha uns 100 dp de buit sota «5 persones». Ha de quedar en uns 14 dp, com al disseny.
- [x] **Filtre de canal com a `ButtonGroup` connectat:** 2 dp entre botons. Els no seleccionats van en `surface-card-high` amb juntes de 8 dp i el seleccionat en pastilla `primary`. Ara són pastilles separades.
- [x] **Hora alineada a la dreta de la fila:** ara acaba abans que el text de sota («17:06», «Ds.», «16 de set.»). Ha d'anar a l'extrem de la fila, amb el mateix marge de 14 dp.
- [x] **Format de la data**, aplicat igual a tota l'app:
    - avui → `17:06`;
    - aquesta setmana → dia abreujat en minúscula (`ds.`);
    - aquest any → `16 de set.`;
    - un altre any → `16/09/2025`.
- [x] **Persona sense entrades** (Pau Vidal): ara la fila queda més baixa i coixa. Ha de mostrar «Encara no hi ha entrades» a la línia de l'últim comentari.
- [x] **Fila «Afegeix una persona» al final de la llista** (disseny 1): vora discontínua i radi 24. És complementària al botó de la capçalera.

### Detall

- [x] **Comptadors:** «1 · 1 · 3 Converses» no quadra, perquè el 3 és el total d'entrades. Hi ha dues opcions:
    - **Li he dit · M'ha dit · Ha passat** (recomanada; coincideix amb els filtres);
    - **Li he dit · M'ha dit · Entrades**.
- [x] **Espai sota la cronologia** (a comprovar): l'última entrada no pot quedar tapada pel split button. Cal `contentPadding` inferior de 96 dp més `navigationBarsPadding`.
- [x] **Menú del split button:** ara és el `DropdownMenu` per defecte, amb cantonades rectes. Ha de fer servir la forma del sistema: `radius-group` (24), elements de 52 dp i icones `on-surface-variant`.

### Nova entrada (full) i nota ràpida

- [x] **Camp de text sense línia inferior:** fons `surface-card-high`, radi 24 a les quatre cantonades i sense indicador. Ara és un `TextField` *filled* amb línia.
- [x] **El mateix ordre als dos formularis:** Persona → Tipus → Text → On → Quan. Ara la nota ràpida posa «On» abans del tipus.
- [x] **El mateix placeholder segons el tipus:**
    - «Què li has dit?»;
    - «Què t'ha dit?»;
    - «Què ha passat?».

  La nota ràpida ara diu «Què s'ha dit», sense interrogant.
- [x] **«On» a la nota ràpida:**
    - primer, els canals de la persona triada, i després «Altres»;
    - per defecte, el canal de la seva última entrada;
    - ara mostra tots els tipus i la fila apareix desplaçada, amb el primer xip tallat.

### Accions d'entrada

- [x] **El mateix ordre de metadades que a la cronologia:** `LI HE DIT · 17:06 · WHATSAPP`. Ara el full posa el canal abans de l'hora.
- [x] **«Esborrar tota la conversa»:**
    - amagar-la si aquell dia i en aquell canal només hi ha una entrada;
    - el subtítol ha de dir el nombre: «Les 3 entrades d'avui a WhatsApp».

### Editar persona

- [x] **Àlies:** el prefix `@` ha d'anar enganxat al text, sense espai.
- [x] **Desa** desactivat mentre no hi hagi canvis o el nom estigui buit.
- [x] **Subtítol de «Persona protegida» quan el dispositiu sí que té bloqueig:** «Cal l'empremta per veure-la. Al llistat no es mostra l'últim comentari.» Ara només s'ha vist el text del cas sense bloqueig.

### Ajustos

- [x] **Tema com a `ButtonGroup` connectat i amb icones** (`light_mode`, `dark_mode`, `brightness_auto`), igual que el filtre del llistat. Ara són botons amb vora i sense icona.
- [x] **Fila «Color dinàmic»:** ara té un fons tenyit diferent (és l'estil de la llibreria de tiles) i sembla seleccionada. Ha de ser `surface-card` i formar grup amb «Tema», amb radis 24/6.
    - Si `ui-tiles-expressive` no deixa canviar els colors i els radis, cal substituir-la per les files pròpies del `designsystem` (`GroupedList`). Així també desapareix l'excepció de l'`@OptIn`.
- [x] **Nota de persones protegides:** ara té mida de text de cos. Ha d'anar en `bodySmall` (13 sp) i `onSurfaceVariant`.

### Design system

- [ ] **Sincronitzar el design system amb l'app:**
    - colors d'error fixos (`#BA1A1A` / `#FFDAD6`, `#FFB4AB` / `#93000A`);
    - menú del split button;
    - full d'accions;
    - full de nova entrada;
    - pantalla de desbloqueig;
    - avatar protegit (manté el pastel i hi posa el cadenat, com a l'app).

---

## M2 · Fluxos que falten

**Fet quan:** es pot arribar a qualsevol persona, crear-ne una des de qualsevol punt i recuperar el que s'ha arxivat o esborrat per error.

- [x] **Triar persona (disseny 3b):** cal substituir el `DropdownMenu` d'«Amb qui» per un `ModalBottomSheet`. Ha de tenir:
    - la llista de persones amb l'avatar;
    - les protegides només amb el nom i el cadenat;
    - una fila **«Afegeix una persona»**, que obre la nova persona i, en desar-la, torna a la nota amb la persona ja triada;
    - si hi ha més de 8 persones, un camp per filtrar per nom.
- [x] **Sense persona per defecte a la nota ràpida (D5):** la targeta diu «Tria una persona» i **Desa** queda desactivat fins que n'hi hagi una.
- [x] **Arxivades (D2):**
    - destinació a la barra flotant, amb la mateixa llista;
    - «Desarxivar» només al menú del detall (no a les files del llistat);
    - estat buit: «No tens cap persona arxivada».
- [x] **Xips de canal (D3):**
    - obrir l'app quan hi ha enllaç directe;
    - mantenir premut per copiar l'usuari (Android ja ho confirma amb la seva previsualització; sense snackbar);
    - «En persona» i els canals sense enllaç no són clicables.
- [x] **Desfer en esborrar una entrada:** snackbar «Entrada esborrada · Desfés» d'uns 5 s, sense diàleg. «Esborrar tota la conversa» i «Esborrar persona» mantenen el diàleg de confirmació.
- [ ] **Estats buits** (a comprovar que hi són tots):
    - llistat sense persones (disseny 1b, «Afegeix la primera persona»);
    - cronologia buida;
    - filtres sense resultats («Cap entrada de M'ha dit»);
    - arxivades buides.
- [ ] **Persona nova → detall:** en crear-la, anar directament al seu detall i treure l'editor de la pila (com ja fa el desbloqueig).
- [ ] **Desbloqueig sense biometria:** substituir el botó desactivat «Desbloqueja» per **«Configura la seguretat»**, que obre `Settings.ACTION_BIOMETRIC_ENROLL` (o `ACTION_SECURITY_SETTINGS`). En tornar, comprovar-ho de nou.

---

## M3 · Privacitat

**Fet quan:** una persona protegida no deixa cap rastre fora del detall desbloquejat.

- [ ] **Backup (D4):** `android:dataExtractionRules` i `fullBackupContent` han d'excloure la base de dades i el DataStore. Cal provar-ho amb `adb shell bmgr backupnow`.
- [ ] **`FLAG_SECURE` també a l'editor** d'una persona protegida i als fulls que s'obren des del seu detall: nova entrada, accions i menú.
- [ ] **No tornar a bloquejar en passar del detall a l'editor** de la mateixa persona; sí en tornar al llistat. Cal definir-ho a nivell de ruta, no a l'`onStop` de la pantalla.
- [ ] **Nota ràpida per a una protegida:** es pot escriure sense desbloquejar, però el full no mostra cap dada de la persona (ni canals amb l'usuari, ni l'última entrada).
- [ ] **Exportar notes** d'una protegida: si el detall ja és desbloquejat, no cal tornar a demanar l'empremta; si no, cal demanar-la.
- [ ] **Ordre del llistat (D6):** aplicar la decisió presa.
- [ ] **Bloqueig desactivat sol** quan el dispositiu es queda sense bloqueig de pantalla: comprovar que les persones protegides continuen protegides i que es mostra la pantalla de M2.
- [ ] **Notificacions i dreceres:** cap drecera dinàmica ni cap suggeriment de compartir amb el nom d'una protegida.

---

## M4 · Qualitat

**Fet quan:** passen els tests, l'app es pot fer servir amb TalkBack i a una mida de text gran, i tot s'ha provat en un mòbil real.

- [ ] **Tests de ViewModel:** llistat (filtres, protegides), detall (filtres de la cronologia, comptadors), nova entrada (validació), editor (àlies duplicat, protecció) i Ajustos.
- [ ] **Tests de pantalla** (Compose UI) dels fluxos principals:
    - crear persona → afegir entrada → veure-la a la cronologia;
    - protegir → bloquejar → desbloquejar;
    - arxivar → desarxivar;
    - esborrar entrada → desfer.
- [ ] **Escala de text al 200 %:** els títols grans (44/40 sp, amplada 118) no poden tallar-se ni sortir de la pantalla. Cal fer servir `autoSize` de `BasicText` amb un mínim de 28 sp, o bé `maxLines = 2`.
- [ ] **TalkBack:**
    - etiquetes de les icones (enrere, ⋮, chevron del split button, «Nota ràpida»);
    - avatar amb el nom;
    - estat dels interruptors i dels filtres;
    - entrades de la cronologia llegides com «Li he dit, avui a les 17:06, per WhatsApp: …».
- [ ] **Àrees tàctils** de 48 dp com a mínim: ⋮ de les entrades, `−` de les dades i dels canals.
- [ ] **Mode fosc:** revisar totes les pantalles. No hi ha captures en fosc d'aquesta ronda.
- [ ] **Color dinàmic desactivat:** provar els 7 colors del tema i comprovar el contrast dels avatars sobre `surface`.
- [ ] **Teclat:** al full de nova entrada, `imePadding` perquè «Desa a la cronologia» quedi visible mentre s'escriu.
- [ ] **Dispositiu real:**
    - empremta (a l'emulador només s'ha provat el PIN);
    - bloqueig de l'app de punta a punta;
    - «Torna a bloquejar» als tres valors;
    - Recents amb `FLAG_SECURE`.
- [ ] **Rendiment:** cronologia amb més de 500 entrades (paginació o `LazyColumn` amb claus estables) i *baseline profile* del llistat i del detall.

---

## M5 · Publicació

**Fet quan:** hi ha un AAB signat i optimitzat a la pista interna de Play.

- [ ] **minSdk (D1):** si es baixa a 31, cal revisar les crides que no hi són (selector d'idioma per app: `AppCompatDelegate.setApplicationLocales` a l'API 31–32) i provar-ho en un emulador amb l'API 31.
- [ ] **material3 alpha:** fixar la versió exacta i apuntar quines API d'Expressive es fan servir, per poder actualitzar quan surti la 1.5 estable.
- [ ] **Mida de la font:** Roboto Flex variable pesa ~1,7 MB. Hi ha dues opcions:
    - (a) fonts descarregables de Google Fonts (`androidx.compose.ui.text.googlefonts`), amb Roboto com a alternativa;
    - (b) un subconjunt amb `fonttools` (llatí, eixos `wght` 400–800, `wdth` 100–118 i `opsz` limitats), que deixa el fitxer en uns 300–500 KB.
- [ ] **Migracions de Room:**
    - treure la recreació automàtica fora de debug;
    - afegir `MigrationTestHelper` de la versió 2 a la 3 abans de qualsevol canvi d'esquema.
- [ ] **Signatura i R8:**
    - signatura de release;
    - `isMinifyEnabled` i `isShrinkResources`;
    - regles de Koin, de kotlinx.serialization (rutes `NavKey`) i de Room;
    - provar l'APK de release de punta a punta.
- [ ] **CI:** el workflow també ha de construir el release (sense signar) i passar els tests JVM.
- [ ] **Play Store:**
    - fitxa en català, castellà i anglès;
    - captures en clar i en fosc;
    - icona de 512 px (ja feta);
    - política de privacitat (les dades no surten del dispositiu);
    - formulari de seguretat de les dades;
    - pista interna.
- [ ] **Idiomes:** revisar que no quedi cap text fix i que el castellà i l'anglès siguin complets (plurals amb `pluralStringResource`).

---

## Després de la v1

- **Cerca:** pel nom de la persona i dins el text de les entrades. Les protegides només es poden trobar pel nom.
- **Exportar i importar tot:** un fitxer (JSON o ZIP xifrat) per canviar de mòbil sense el backup de Google.
- **Etiquetes** i la vista de **dades fixades** de totes les persones: les dues icones del disseny original de la barra flotant.
- **Widget** de nota ràpida.
- **Recordatoris** a partir de les dades fixades (per exemple, l'aniversari).

---

## Referència (sense canvis respecte de la v1 del pla)

### Stack

Una sola activitat (`MainActivity`, una `FragmentActivity` pel `BiometricPrompt`) i dos mòduls: `:app` i `:designsystem`. Ara `minSdk = 36` (vegeu D1) i `compileSdk = 37`.

| Peça | Llibreria i versió | Notes |
| --- | --- | --- |
| UI | `material3` 1.5.0-alpha29 (BOM Compose 2026.09.00) | Les API d'Expressive no són públiques a la 1.4.0 estable |
| Formes | `graphics-shapes` 1.1.0 | Formes dels avatars |
| Color | `material-kolor` 5.0.1 | `Neutral` amb color dinàmic, `TonalSpot` amb el color triat |
| Navegació | Navigation 3 1.2.0 + `lifecycle-viewmodel-navigation3` | Rutes tipades amb `NavKey` serialitzables |
| Dades | Room 2.8.4 + KSP 2.3.12 | Base de dades versió 2, esquemes a `app/schemas` |
| DI | Koin 4.2.2 | |
| Preferències | DataStore Preferences 1.2.1 | `SettingsRepository` |
| Ajustos UI | Compose-Settings `ui-tiles-expressive` 3.3.0 | Pot desaparèixer (M1) |
| Biometria | `androidx.biometric` 1.1.0 | `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` |
| Captures | Compose Preview Screenshot Testing 0.0.1-alpha16 | Experimental; als dos mòduls (vegeu «Tests de captures») |
| Captures | Compose Preview Screenshot Testing 0.0.1-alpha16 | Experimental; als dos mòduls |
| Altres | Kotlin 2.4.20, AGP 9.4.1, lifecycle 2.11.0, coroutines 1.11.0, serialization 1.11.0 | |

### Tema

- **Color dinàmic (per defecte):** la llavor és el `primary` de `dynamicLightColorScheme`, amb l'esquema MaterialKolor `Neutral`.
- **Color dinàmic desactivat:** 7 colors (Lavanda, Menta, Préssec, Cel, Rosa, Llimona, Sorra) amb l'esquema `TonalSpot`.
- **Mode:** clar, fosc o sistema.
- **Error fix:**
    - clar: `#BA1A1A` sobre `#FFDAD6`;
    - fosc: `#FFB4AB` sobre `#93000A`.
- **Avatars:** 7 pastels fixos i 4 formes (`Cookie9Sided`, `Clover4Leaf`, `Flower`, `Sunny`). Les inicials sempre van en `#1E1D24`, i hi ha un cadenat si la persona és protegida.
- **Tipografia:** Roboto Flex. Els títols (44/40/30/28/24/22 sp) van en pes 800, amplada 118 i mida òptica 72.
- **Formes:** llistes agrupades de 24 dp als extrems i 6 dp entre files, separades per 2 dp. Els grups de botons van amb juntes de 8 dp.

### Components propis (`designsystem`)

`Avatar`, `GroupedList` / `groupedItems`, `TimelineRow` i `TimelineMarker`, `ConversesFilterChip`, `ConversesToggleGroup`, `ConversesFloatingToolbar` i `ToolbarDestination`, `ConversesSplitButton`, `ColorSwatches` i `ConversesTheme`.

A afegir a M1 i M2: `GroupedMenu` (menú del split button), `SettingRow` / `SettingSwitchRow`, `PersonPickerSheet` i `EmptyState`.

### Pantalles i navegació

| Destinació | Ruta (`NavKey`) | Notes |
| --- | --- | --- |
| Llistat | `PeopleRoute` | Filtres per canal, nova persona a la capçalera i fila final, barra flotant |
| Arxivades | `ArchivedRoute` | Nova (M2) |
| Detall | `PersonRoute(id)` | Split button «Nota ▾», ⋮ a cada entrada, `FLAG_SECURE` si és protegida |
| Desbloqueig | `PersonLockedRoute(id)` | `BiometricPrompt` automàtic; si va bé, substitueix la ruta pel detall |
| Nota ràpida | `QuickNoteRoute(personId?)` | Sense persona per defecte (M2) |
| Editar/crear | `EditPersonRoute(personId)` | `0` = persona nova; en crear, va al detall |
| Ajustos | `SettingsRoute` | |
| Fulls i diàlegs | Dins del detall i de l'editor | Nova entrada, accions d'entrada, menú del split button, triar persona, esborrar |

### Model de dades (Room, versió 2)

| Taula | Camps |
| --- | --- |
| `person` | `id`, `name`, `alias?`, `avatarShape`, `avatarColor`, `isProtected`, `archived`, `notes?`, `createdAt`, `updatedAt` |
| `channel` | `id`, `personId`, `type`, `handle?` (`IN_PERSON` sense `handle`) |
| `entry` | `id`, `personId`, `kind` (`SAID`, `HEARD`, `HAPPENED`), `channelType`, `text`, `occurredAt` |
| `pinned_fact` | `id`, `personId`, `label`, `value`, `position` |

Totes les claus `personId` tenen `onDelete = CASCADE`, i hi ha un índex a `entry(personId, occurredAt)`.

### Tests de captures

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

### Decisions ja preses

- **Avatars:** cada persona porta el seu pastel, al llistat i al detall. Les protegides el mantenen i hi afegeixen el cadenat.
- **Exportar notes:** text pla compartit amb el full del sistema. També es pot fer abans d'esborrar una persona.
- **Arxivar:** amaga la persona del llistat; es recupera des d'Arxivades.
- **«Esborrar tota la conversa»:** esborra les entrades d'aquell dia en aquell canal.
- **Idioma per defecte:** anglès (`values/`), amb català (`values-ca/`) i castellà (`values-es/`).
- **Fora de la v1:** el vincle amb l'agenda.
