# Converses — Pla i estat (v2)

Actualitzat el 7 d'oct. de 2026 · @Bernat · fets M1 a M4, el color de persona, el backlog de disseny, el backlog de bugs i l'onboarding (implementat). **La v1 no té Pro.** Abans de la release 1 queden els canvis de la versió gratuïta (avatar estàndard; vegeu «Versió gratuïta i Pro») i **M5**; el menú de debug es fa en paral·lel en un worktree apart. Follow-ups i gestor de canals passen a l'Iteració 2, després de la publicació (vegeu «On som»)

## Resum

Converses és una app Android per recordar què has dit, què t'han dit i què ha passat amb cada persona amb qui parles, sigui per xat, per xarxes o en persona. Es construeix amb Jetpack Compose i Material 3 Expressive, en monocrom amb color dinàmic. El color apareix als avatars (pastel), als errors (vermell), al tema triat si es desactiva el color dinàmic i, dins de cada persona, com a tema pres del color del seu avatar.

- **Referència visual:** el llenç de disseny «Converses» i el design system «Converses Design System» (tokens, components i pautes).
- **Abast de la v1:** llistat, detall amb cronologia, nova entrada (full i nota ràpida), crear i editar persona, persona protegida, accions d'entrada, arxivar, esborrar i Ajustos.
- **Fora de la v1:** vincle amb l'agenda (descartat), cerca, cerca dins el text de les entrades, còpies de seguretat pròpies, widgets i etiquetes.

## On som

La v1 funcional està feta: interfície polida, fluxos complets, privacitat tancada, color de persona i onboarding. Abans de publicar queden els canvis de la versió gratuïta (avatar estàndard; a la v1 no hi ha Pro) i la feina de publicació (M5), més unes quantes coses soltes.

| Fase | Objectiu | Estat |
| --- | --- | --- |
| 0 · Base | Projecte, dades, totes les pantalles de la v1 | Fet |
| M1 · Polit visual | La UI coincideix amb el disseny i el design system | Fet (falta sincronitzar el design system) |
| M2 · Fluxos que falten | Triar i crear persona, arxivades, estats buits, desfer | Fet |
| M3 · Privacitat | Les persones protegides no es filtren enlloc | Fet (falta provar el bloqueig desactivat sol a dispositiu) |
| Color de la persona | Les pantalles de cada persona prenen el color del seu avatar | Fet, però és Pro (`PersonColorFeature`): apagat a la v1; falta provar-lo a l'emulador amb el flag encès |
| Backlog de disseny | Tanda 1 «Quan», tanda 2 «Qui» i tanda 3 bloqueig | Fet (falta provar el bloqueig a dispositiu real: massa intents i empremta) |
| Onboarding · tanda 4 | Benvinguda animada de 3 passos, només el primer cop (vegeu «Onboarding») | Implementat; falten les proves manuals i les referències de captures (`docs/onboarding/`) |
| Backlog de bugs | Àrea clicable, etiqueta «Other», biometria sense desar | Fet (`docs/backlog.md`) |
| M4 · Qualitat | Tests, accessibilitat, mode fosc, dispositiu real | Fet (amb excepcions acceptades) |
| M5 · Publicació | minSdk (fet), R8 (arrencada arreglada), signatura, mida de la font, CI de release, Play Store | En curs |
| Versió gratuïta (v1 sense Pro) | Avatar estàndard; el Pro queda per després de la v1 (vegeu «Versió gratuïta i Pro») | **Per fer abans de la release 1**; va abans de M5 (`docs/pro.md`) |
| Menú de debug | Botó flotant i full amb mòduls, només en builds de debug (vegeu «Menú de debug») | Esquelet i mòduls Dades i Onboarding fets; falta el mòdul Pro (interruptor gratuït/Pro) i provar-lo a l'emulador |
| Iteració 2 | Follow-ups i gestor de canals | Després de la publicació (vegeu «Iteració 2») |
| Després de la v1 | Cerca, còpia de seguretat, etiquetes, widget | Idees |

**Coses soltes:**

- Sincronitzar el design system a Claude Design (vegeu M1 · Design system); és feina de Bernat.
- Provar a l'emulador el color de persona i, a dispositiu real, el bloqueig desactivat sol.
- Afinar el snackbar de desfer (posició i marges) si no queda bé.
- Features pendents: vegeu `docs/backlog.md`; només queden les de l'Iteració 2 (issue #5).
- Onboarding: proves manuals, pop del botó «Comença» al pas 3, test del runner del flag i referències de captures (etiqueta `update-screenshots`); vegeu «Onboarding».
- Canvis de la versió gratuïta: vegeu «Versió gratuïta i Pro»; són obligatoris abans de la release.
- Redisseny de Nova persona / Editar persona: pas 3 de «Versió gratuïta i Pro»; especificació a `docs/nova-persona/`.
- Versió gratuïta i Pro: document de treball a `docs/pro.md` (vegeu «Versió gratuïta i Pro»).
- Menú de debug: esquelet fet; falta el mòdul Pro (vegeu «Menú de debug»).

## Decisions (totes preses)

| # | Decisió | Recomanació | Afecta |
| --- | --- | --- | --- |
| D1 | **minSdk.** Era 36 (només Android 16+) | **Decidit: 31** (Android 12, color dinàmic garantit). Fet | M5, Ajustos |
| D2 | **Barra flotant del llistat.** Ara només té Persones i Ajustos | **Persones · Arxivades · Ajustos**, i treure «Arxivades» dels filtres de canal **Decidit:** Persones · Arxivades · Ajustos (fet). | M2 |
| D3 | **Xips de canal del detall.** Ara no fan res | Tocar obre l'app quan hi ha enllaç directe (WhatsApp `wa.me`, Telegram `t.me`, Telèfon `tel:`); mantenir premut copia l'usuari; «En persona» no és clicable **Decidit:** tocar obre l'app i mantenir premut copia, sense snackbar (fet). | M2 |
| D4 | **Còpia de seguretat d'Android.** Per defecte, la base de dades amb les notes va a Google Drive | Excloure la base de dades del backup automàtic a la v1 (són notes privades) i oferir l'exportació manual **Decidit:** excloure la base de dades i el DataStore del backup (fet). | M3 |
| D5 | **Persona per defecte a la nota ràpida** | Cap: «Tria una persona». Mai una persona protegida per defecte **Decidit:** cap persona per defecte (fet). | M2, M3 |
| D6 | **Ordre del llistat amb protegides.** Ordenar per l'última activitat delata quan has parlat amb una persona protegida | Opcional: ordenar les protegides per nom, al final o al principi **Decidit:** mantenir l'ordre per última activitat (no canvia res). | M3 |

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

- [x] **Comptadors:** eliminats de la capçalera (#19): duplicaven els filtres de la cronologia, que ja mostra el total d'entrades. `countOf` es manté per amagar els filtres sense entrades.
- [x] **Capçalera i jerarquia (#19, #18):** nom dominant amb l'àlies petit (`bodyMedium`); la data de creació surt al final de la cronologia («Fitxa creada: …», `profile_created`); Dades i Cronologia comparteixen `SectionHeader` (`SectionTitleStyle`). «Editar persona» és un llapis a dalt a la dreta, i s'ha tret del menú del split button, que passa a fer servir `EditNote` per a «Nota».
- [x] **Canals «On hi parles» (#20, disseny `on-hi-parles`):** el diàleg «Afegeix un canal» s'ha substituït per una targeta amb `FilterChip` seleccionables (`ChannelsCard`, `FlowRow`) a Nova i Editar persona, amb comptador («Cap», «N triats»). **Ja no hi ha usuari ni número** per canal, ni «Altres» (tornarà amb els canals propis, Pro). Canals de la v1, en ordre fix (l'ordre de `ChannelType`): En persona · WhatsApp · Telegram · X · Instagram · Telèfon · Correu · Discord · Signal · LinkedIn · Reddit. Icones genèriques a `ChannelType.icon()`. El detall mostra pastilles (icona + nom) no interactives, en el mateix ordre, i s'ha tret `ChannelLinks.kt` (sense usuari no hi ha enllaç directe ni copiar). Filtres del llistat: Chat = WhatsApp, Telegram, Signal, Discord, Correu; Social = X, Instagram, LinkedIn, Reddit. El formulari d'entrada ofereix els canals de la persona o tots si no en té cap. L'esquema de Room (v1) perd la columna `handle`: en debug cal esborrar les dades de l'app (`adb shell pm clear`) perquè `fallbackToDestructiveMigration` només actua si puja la versió.
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

- [x] **Política de privadesa (#21):** fila «Política de privadesa» al grup «Quant a» d'Ajustos; obre `https://alorma.github.io/converses/privacy/#<idioma>` al navegador (sense permís `INTERNET`).
- [x] **Tema com a `ButtonGroup` connectat i amb icones** (`light_mode`, `dark_mode`, `brightness_auto`), igual que el filtre del llistat. Ara són botons amb vora i sense icona.
- [x] **Fila «Color dinàmic»:** ara té un fons tenyit diferent (és l'estil de la llibreria de tiles) i sembla seleccionada. Ha de ser `surface-card` i formar grup amb «Tema», amb radis 24/6.
    - Si `ui-tiles-expressive` no deixa canviar els colors i els radis, cal substituir-la per les files pròpies del `designsystem` (`GroupedList`). Així també desapareix l'excepció de l'`@OptIn`.
- [x] **Nota de persones protegides:** ara té mida de text de cos. Ha d'anar en `bodySmall` (13 sp) i `onSurfaceVariant`.

### Design system

- [ ] **Sincronitzar el design system amb l'app** (feina de Bernat a Claude Design):
    - colors d'error fixos (`#BA1A1A` / `#FFDAD6`, `#FFB4AB` / `#93000A`);
    - split button de 56 dp (mida mitjana) amb etiqueta en negreta, i el seu menú (forma de grup de 24 dp, elements de 52 dp, icones `on-surface-variant`);
    - full d'accions (metadades `Tipus · hora · canal`; «Esborrar tota la conversa» amagada si només hi ha una entrada);
    - full de nova entrada (camp sense línia amb radi 24 i placeholder; ordre Persona → Tipus → Text → On → Quan) i nota ràpida amb el mateix ordre;
    - full de triar persona i fila discontínua «Afegeix una persona»;
    - pantalla de desbloqueig, amb «Configura la seguretat» si no hi ha bloqueig;
    - avatar protegit (manté el pastel i hi posa el cadenat);
    - filtre de canal com a `ButtonGroup` connectat i tema d'Ajustos amb icones; files d'Ajustos amb radis 24/6;
    - xip de canal (toc obre l'app, toc llarg copia), estats buits i snackbar de desfer;
    - barra flotant amb tres destinacions (Persones · Arxivades · Ajustos);
    - tema de color de persona (7 llavors, vegeu «Color de la persona»);
    - fila «Quan» i full «Quan va ser?» (vegeu «Formulari d'entrada i "Quan"»);
    - selector «Qui», marcadors de 32 dp i filtres amb avatar (vegeu «Qui»);
    - pantalla de bloqueig amb els quatre estats (vegeu «Bloqueig d'una persona protegida»).

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
- [x] **Estats buits** (a comprovar que hi són tots):
    - llistat sense persones (disseny 1b, «Afegeix la primera persona»);
    - cronologia buida;
    - filtres sense resultats («Cap entrada de M'ha dit»);
    - arxivades buides.
- [x] **Persona nova → detall:** en crear-la, anar directament al seu detall i treure l'editor de la pila (com ja fa el desbloqueig).
- [x] **Desbloqueig sense biometria:** substituir el botó desactivat «Desbloqueja» per **«Configura la seguretat»**, que obre `Settings.ACTION_BIOMETRIC_ENROLL` (o `ACTION_SECURITY_SETTINGS`). En tornar, comprovar-ho de nou.

---

## M3 · Privacitat

**Fet quan:** una persona protegida no deixa cap rastre fora del detall desbloquejat.

- [x] **Backup (D4):** `android:dataExtractionRules` i `fullBackupContent` han d'excloure la base de dades i el DataStore. Cal provar-ho amb `adb shell bmgr backupnow`.
- [x] **`FLAG_SECURE` també a l'editor** d'una persona protegida i als fulls que s'obren des del seu detall: nova entrada, accions i menú.
- [x] **No tornar a bloquejar en passar del detall a l'editor** de la mateixa persona; sí en tornar al llistat. Cal definir-ho a nivell de ruta, no a l'`onStop` de la pantalla.
- [x] **Nota ràpida per a una protegida:** es pot escriure sense desbloquejar, però el full no mostra cap dada de la persona (ni canals amb l'usuari, ni l'última entrada).
- [x] **Exportar notes** d'una protegida: si el detall ja és desbloquejat, no cal tornar a demanar l'empremta; si no, cal demanar-la.
- [x] **Ordre del llistat (D6):** decidit mantenir l'ordre per última activitat; no canvia res.
- [ ] **Bloqueig desactivat sol** quan el dispositiu es queda sense bloqueig de pantalla: comprovar que les persones protegides continuen protegides i que es mostra la pantalla de M2. Al codi ja hi és (`AppLockScreen` → `onDeviceUnprotected` desactiva el bloqueig; les persones no es toquen i la pantalla de desbloqueig ofereix configurar la seguretat); falta provar-ho a dispositiu.
- [x] **Notificacions i dreceres:** cap drecera dinàmica ni cap suggeriment de compartir amb el nom d'una protegida.

---

## Onboarding (tanda 4)

**Fet quan:** el primer cop que s'obre l'app surt una benvinguda de 3 passos amb il·lustracions animades, i en acabar porta al llistat buit.

**Fonts a `docs/onboarding/`:**

- `tanda-4-onboarding.md`: especificació completa (flux, estructura, animacions, textos ca/es/en, accessibilitat, proves). Si hi ha cap diferència amb el pla, mana aquest document.
- `imatges/`: captures del llenç, 3 passos en clar i en fosc (0a · Recorda cada conversa, 0b · Tot en una cronologia, 0c · Només per a tu).
- `design/`: HTML exportat del llenç com a referència de lectura (vegeu `design/README.md`).

**Resum:**

- **Llibreria [FireAndForget](https://github.com/alorma/FireAndForget) 1.3.0** (Maven Central, grup `com.github.alorma.fire-and-forget`, només `core`; el README diu `com.alorma.fireandforget`, però no existeix) per decidir si es mostra l'onboarding. `OnboardingFlag : FireAndForget(name = "onboarding_done", defaultValue = true)`: `isEnabled()` és «cal mostrar-lo» i `disable()` el marca com a fet. Va al catàleg de versions (`gradle/libs.versions.toml`).
- **Runner propi sobre DataStore** (decidit; no s'usa `multiplatform-settings`, que afegiria SharedPreferences i un segon magatzem a excloure del backup). L'API de la llibreria és síncrona, així que el runner ha de llegir la clau del DataStore de manera síncrona abans del primer frame, com el tema; l'escriptura de `disable()` ha de quedar desada abans de passar a `PeopleRoute`. La clau `onboarding_done` viu a `SettingsRepository` / `AppSettings` (per defecte `false`) i el runner hi delega. Cal implementar també `getCounter` / `setCounter` (la classe base els exigeix, tot i que no es fan servir).
- `MainActivity` tria `OnboardingRoute` o `PeopleRoute` segons `OnboardingFlag.isEnabled()`, llegit de manera síncrona abans del primer frame (com el tema), així no hi ha parpelleig. **No es mira si ja hi ha persones** (decidit): només compta el flag. Per tornar a veure l'onboarding, el menú de debug té «Show onboarding again» (`OnboardingFlag.reset()`) o es poden esborrar les dades de l'app.
- **Comença (pas 3) actiu des del principi** (decidit): es pot prémer en qualsevol moment i salta l'animació que quedi; no cal esperar els 3,7 s.
- **Textos de mostra de les il·lustracions traduïts** (decidit): les etiquetes, els textos de les entrades del pas 2 («La Nyra deu diners al gremi.»…) i les inicials/noms d'exemple van a `strings.xml` en en/ca/es, amb claus pròpies (p. ex. `onb_sample_*`), a més de les `onb_*` de l'spec. L'spec només dona el text en català: cal escriure'n l'anglès i el castellà.
- **Omet** (passos 1 i 2) i **Comença** (pas 3) desen la clau i substitueixen la ruta per `PeopleRoute`. Enrere al pas 1 surt de l'app sense marcar-lo.
- `ui/onboarding/OnboardingScreen.kt` (`HorizontalPager` de 3 pàgines, punts, botons) i `OnboardingIllustrations.kt` (una il·lustració animada per pas, que es repeteix en tornar-hi (tocar-la no la repeteix: decidit); amb moviment reduït mostra l'estat final).
- Colors d'èxit nous al `designsystem` (`success`, `onSuccess`, `successContainer`), fixos i només per a aquesta pantalla.
- Textos `onb_*` a `strings.xml` (en, ca, es).

**Commits previstos:**

- [x] FireAndForget 1.3.0 al catàleg, `OnboardingFlag` amb el runner sobre DataStore (`data/settings/OnboardingFlag.kt`), `onboarding_done` a `SettingsRepository` i ruta inicial condicional a `MainActivity` (`shouldShowOnboarding`, amb la regla de les persones només en builds no debuggables).
- [x] `OnboardingScreen`: pager, textos `onb_*` i `onb_sample_*` (en/ca/es), punts, botons, Omet/Comença i enrere.
- [x] Tokens `success*` al `designsystem` (`SuccessColors`, fixos clar/fosc) i `OnboardingTitleStyle`.
- [x] Animacions dels tres passos a `OnboardingIllustrations.kt` (`rememberSequence` amb `Animatable` per element). `TimelineRow` té ara `lineProgress` per animar la línia del pas 2.
- [x] Moviment reduït (tocar la il·lustració NO la repeteix: decidit) (`ANIMATOR_DURATION_SCALE == 0`: els `Animatable` neixen a l'estat final), entrada de títol i cos, i previews dels tres passos (`OnboardingPreviews.kt`) amb tests de captures `onboardingStep1..3`; **falta generar les referències amb l'etiqueta `update-screenshots`**.
- [ ] Pendent: pop del botó «Comença» al pas 3, test del runner del flag, i les proves manuals de la llista de sota (emulador/dispositiu).

**Proves** (llista completa al document): primera obertura i no tornar a sortir; enrere al pas 1; animació de la pàgina d'arribada; repetir en tocar; moviment reduït; clar i fosc; ca/es/en amb font gran; rendiment de les animacions infinites.

**Notes:**

- La il·lustració del pas 2 ja fa servir els tipus nous (Luna · Jo · Apunt); és només un dibuix i no depèn del codi de la cronologia.
- Cal afegir les captures de l'onboarding als tests de captures (M4) i el verd d'èxit a la sincronització del design system.

---

## M4 · Qualitat

**Fet quan:** passen els tests, l'app es pot fer servir amb TalkBack i a una mida de text gran, i tot s'ha provat en un mòbil real.

- [x] **Tests de captures de totes les pantalles** (clar i fosc, en anglès, 411×891 dp). Hi ha el llistat (3), el detall (3, amb els 7 colors), el bloqueig (4 estats), la nota ràpida (2), el full de nova entrada, el full «Quan» (2), el bloqueig de l'app, triar persona (2) i el full d'accions (2), Ajustos (2), Editar persona (3), quatre diàlegs (moure entrada, confirmar esborrat, esborrar persona i canal), el filtre sense resultats i el mode d'edició. Cinc d'aquestes pantalles també tenen català, castellà i font 2×. Falta: **Resolt:** el que queda (menú del split button, snackbar) no es pot capturar i es prova a mà.
    - ~~**Bloqueig:** els quatre estats~~ (fet: `personLocked*` a `ScreensScreenshotTests`; les referències les genera el CI).
    - ~~**Nota ràpida:** sense persona i amb persona~~ (fet: `QuickNoteContent` sense `ViewModel`).
    - ~~**Nova entrada i edició**~~ (fet: `NewEntrySheetContent`; falta cobrir els tres valors de «Qui» i el mode d'edició).
    - ~~**«Quan»**~~ (fet: `WhenSheetContent` amb «Ara» i amb data personalitzada).
    - ~~**Fulls i diàlegs**~~ (fet: accions d'entrada, triar persona i els diàlegs; els `AlertDialog` sí que es renderitzen als tests). Falten el menú del split button i el snackbar de desfer.
    - ~~**Editar persona** i **Ajustos**~~ (fet: `EditPersonContent` amb `EditPersonActions`, i `SettingsContent`).
    - **Estats buits:** llistat, cronologia, filtres sense resultats i arxivades.
    - Les que depenen d'un `ViewModel` necessiten una versió sense estat (`...Content`) amb `PreviewData`, com ja fan el llistat i el detall. Les referències es generen amb l'etiqueta `update-screenshots` a la PR. Quan estigui fet, treure la línia «Fora de moment» de «Tests de captures».
- [x] **Tests de ViewModel i de repositori** (JVM, 29 tests): llistat (filtres per canal, protegides, arxivades), detall (cronologia, comptadors, filtres, afegir, editar, desfer, esborrar un dia, moure, arxivar), nova entrada (nota ràpida: persona, canals i canal de l'última entrada, protegides, pont de selecció, desar), editor (nom, àlies repetit, canviar-lo, desar amb dades netes, canals, reordenar dades, protegir a l'acte, esborrar), Ajustos i el repositori (protegides sense última entrada ni canals, ordre, àlies).
    - **Infraestructura:** `FakeDb` amb DAOs en memòria (`app/src/test/.../testing`) i `MainDispatcherRule`; no cal Room ni Robolectric. `unitTests.isReturnDefaultValues = true` perquè `PersonExporter` rep un `Context` que no fa servir.
- [x] **Escala de text al 200 %:** les captures amb font 2× ja han trobat i corregit tres talls (inicials de l'avatar, etiqueta de les dades fixades i nom del selector «Qui», que ara acaba en «…»). Pendent: l'avatar petit del xip de filtre de la cronologia a font 2× (la inicial es veu mal alineada). Resta: els títols grans (44/40 sp, amplada 118) no poden tallar-se ni sortir de la pantalla. Cal fer servir `autoSize` de `BasicText` amb un mínim de 28 sp, o bé `maxLines = 2`. **Acceptat tal com està:** els talls greus ja estan corregits; l'avatar del xip i els títols grans a 2× es milloraran si fa falta.
- [x] **TalkBack:** fet i comprovat amb `uiautomator`.
    - botó desplegable del split button («Més opcions»), formes («Forma 1…4») i colors amb nom (editor i Ajustos), amb estat de selecció (`selectable` + `RadioButton`);
    - cada entrada es llegeix sencera: «Jo, Avui a les 11:00, per WhatsApp: …» (`entry_description`), amb el menú ⋮ a part;
    - els comptadors de la capçalera es llegeixen units («1 Jo»);
    - ja eren correctes: icones d'enrere i ⋮, interruptors (`Role.Switch`), filtres, la fila «Quan», el botó de la persona al selector «Qui» (nom complet) i els marcadors, que són decoratius.
    - Pendent de provar amb TalkBack real: l'ordre de lectura i els fulls.
- [x] **Àrees tàctils** de 48 dp com a mínim: auditades a l'emulador (llistat, detall, editor, Ajustos i nota ràpida). Tots els botons, xips i interruptors arriben a 48 dp; només els camps de text inline de les dades fixades són més baixos (són camps, no botons).
- [x] **Mode fosc:** revisar totes les pantalles. No hi ha captures en fosc d'aquesta ronda. **Acceptat:** totes les pantalles tenen captures en fosc als tests i se n'han revisat les principals; si alguna falla, es millora.
- [x] **Color dinàmic desactivat:** provar els 7 colors del tema i comprovar el contrast dels avatars sobre `surface`. **Resolt:** els 7 colors tenen test de contrast (`PersonThemeContrastTest`) i captures en clar i fosc (`personDetailThemed`).
- [x] **Teclat:** al full de nova entrada el botó «Desa a la cronologia» és fix a baix, fora de la part que es desplaça, i queda sempre visible sobre el teclat (comprovat a l'emulador escrivint i desant amb el teclat obert). A la nota ràpida Desa és a dalt.
- [x] **Dispositiu real:** **Resolt:** l'empremta real l'ha provada Bernat; la resta de proves en dispositiu van a M5.
    - empremta (a l'emulador només s'ha provat el PIN);
    - bloqueig de l'app de punta a punta;
    - «Torna a bloquejar» als tres valors;
    - Recents amb `FLAG_SECURE`.
- [x] **Rendiment:** cronologia amb més de 500 entrades (paginació o `LazyColumn` amb claus estables) i *baseline profile* del llistat i del detall. **Descartat de moment:** la cronologia ja fa servir `LazyColumn` amb claus estables; no es mesura amb 500 entrades ni es fa el *baseline profile* abans de la v1.

---

## M5 · Publicació

**Fet quan:** hi ha un AAB signat i optimitzat a la pista interna de Play.

- [x] **minSdk (D1):** baixat a **31** (`app` i `designsystem`). El lint no troba cap crida que no hi sigui; l'únic cas era l'idioma per app (`Settings.ACTION_APP_LOCALE_SETTINGS`, API 33, constant inlinada que el lint no avisa): a l'API 31–32 s'obre l'idioma del sistema (`Settings.ACTION_LOCALE_SETTINGS`), que l'app ja segueix. Provat en un emulador amb l'API 31 amb l'APK de release: onboarding, color dinàmic i Ajustos → Idioma funcionen.
- [x] **material3 alpha:** fixada a `1.5.0-alpha29` al catàleg de versions. API d'Expressive en ús (tots els `@OptIn` són al `designsystem`): `MaterialExpressiveTheme` i `MotionScheme`, `MaterialShapes`, `SplitButtonLayout` / `SplitButtonDefaults`, `ToggleButton` / `ToggleButtonDefaults` / `ButtonGroupDefaults` i `HorizontalFloatingToolbar`.
- [ ] **Mida de la font:** Roboto Flex variable pesa ~1,7 MB. Hi ha dues opcions:
    - (a) fonts descarregables de Google Fonts (`androidx.compose.ui.text.googlefonts`), amb Roboto com a alternativa;
    - (b) un subconjunt amb `fonttools` (llatí, eixos `wght` 400–800, `wdth` 100–118 i `opsz` limitats), que deixa el fitxer en uns 300–500 KB.
- [ ] **Migracions de Room:**
    - la recreació automàtica només és en debug (a release no n'hi ha); la base de dades és la v1 neta, sense migracions;
    - afegir `MigrationTestHelper` quan hi hagi el primer canvi d'esquema, ja després de publicar.
- [x] **Dues apps a la vegada:** el build de debug té `applicationIdSuffix = ".debug"` (`com.alorma.contactnotes.debug`) i el nom «Converses Debug» (`src/debug/AndroidManifest.xml`), així es pot tenir instal·lada a la vegada amb la de release.
- [x] **Signatura de debug:** `app/debug.keystore` al repo (contrasenyes estàndard `android`, àlies `androiddebugkey`), perquè l'APK de debug de la CI i de qualsevol màquina tingui la mateixa signatura i s'instal·li a sobre sense esborrar l'app.
- [ ] **Signatura i R8:**
    - R8 ja és actiu a release (AGP 9: `optimization { enable = true }`). Havia un bug: amb `packageScope = androidx/kotlin/kotlinx` l'APK de release **petava en arrencar** (`IllegalAccessError` a `kotlin.text`); s'ha tret l'opció i l'APK passa de 9,2 a 5,7 MB;
    - provat a l'emulador (APK de release signat amb la keystore de debug només en local): onboarding, llistat, crear persona i detall funcionen, sense excepcions; falta provar la resta de pantalles (nota ràpida, persones protegides, Ajustos, exportar) i les regles explícites de Koin, `NavKey` i Room si en calen;
    - signatura de release (aquesta keystore **no** va al repo);
    - `isShrinkResources` (comprovar si l'equivalent és ja actiu).
- [ ] **CI:** `PR Build` i `Build APK` ja executen els tests unitaris (`testDebugUnitTest`, tots els mòduls) abans de construir i publiquen el resultat a GitHub (`mikepenz/action-junit-report`) i pugen l'informe si fallen. Falta construir també el release (sense signar).
- [ ] **Play Store:**
    - fitxa en català, castellà i anglès;
    - captures en clar i en fosc;
    - icona de 512 px (ja feta);
    - política de privacitat (les dades no surten del dispositiu);
    - formulari de seguretat de les dades;
    - pista interna.
- [x] **Idiomes:** no queda cap text fix a la UI (només dades de prova a les previews) i els tres idiomes tenen exactament les mateixes cadenes i plurals.

---

## Canvis futurs (backlog)

El detall de cada punt és a `docs/backlog.md`. Ordre proposat:

1. ~~**Bugs petits, junts i abans de M4**~~ (fets tots tres, sense tocar la base de dades):
    - ~~**1** · àrea clicable fora de la superfície (issue #6)~~ (fet);
    - ~~**5** · biometria sense desar el perfil (issue #4)~~ (fet);
    - ~~**3** · etiqueta `OTHER - SIGNAL`~~ (fet).
2. **Tests de captures de totes les pantalles** (vegeu M4): gairebé fet; només queden el menú del split button i el snackbar, que no es poden capturar, i l'avatar petit del xip a font 2×.
3. ~~**Empremta a la nota ràpida d'una persona protegida** (backlog 10)~~ (fet).
4. **M4 i M5** (qualitat i publicació).

**Migracions:** M5 només necessita el test de migració (`MigrationTestHelper`) si l'esquema de Room canvia abans de publicar. Els canvis d'esquema que hi ha previstos són a l'Iteració 2 i es faran amb la seva migració i el seu test.

## Iteració 2 (després de la publicació)

No són prioritaris: es faran quan l'app ja estigui publicada. El detall és a `docs/backlog.md`.

- **Follow-ups / entrades pendents (6, issue #5):** flag `pending` a `EntryEntity` i interruptor a `EntryForm`, sota de «Quan». Primera migració de Room (v1 → v2) amb el seu test de migració (`MigrationTestHelper`).
- **Gestor de canals (8):** l'enum `ChannelType` passa a una taula `Channel`, amb migració i test. Absorbeix «Empresa» (7); l'etiqueta «Other - valor» (3) es fa abans, com a bug. Només val la pena si es vol que els canals siguin editables.

## Versió gratuïta i Pro

El detall és a `docs/pro.md` (document de treball, es continuarà). **La v1 no té Pro: no hi ha cap funció, pantalla ni reclam de Pro.** El que s'ha de fer abans de la release 1 són els canvis de la versió gratuïta, perquè toquen el model de dades de les persones (avatar) i no es poden fer sense migració un cop publicada l'app. El Pro (pagament únic) es planifica per a després. Resum:

- **Model:** Pro de pagament únic, sense subscripció ni anuncis. Play Billing no entra encara. Regla: mai es cobra per accedir, protegir o treure les dades pròpies.
- **Pro:** avatar personalitzat (color i forma), canals propis (backlog 8), follow-ups (backlog 6) i integració amb contactes (vincle, Connected apps, avís de trucada, importació múltiple).
- **No s'esborra codi:** tot el que és Pro queda amagat darrere d'un els feature flags (sempre `false` a la v1).
- **A la primera release no es mostra res de Pro.**
- **Pendent de decidir:** canal «Empresa» (backlog 7), gratuït o Pro segons si es fa abans dels canals propis.

**Feina abans de la release 1** (en aquest ordre, un commit per pas):

- [x] **1 · Feature flags** (`data/flags/`), copiats de l'estil de [Caducity](https://github.com/alorma/Caducity) (`config/remoteconfig`) i de FireAndForget: una classe per flag amb `key` i `defaultValue` (de moment `CustomAvatarFeature` i `PersonColorFeature`; els flags de canals propis, follow-ups i integració amb contactes s'afegiran quan aquestes funcions existeixin), un `FeatureFlagRunner` abstracte i `LocalFeatureFlagRunner` (només local, sense Firebase), que admet *overrides* per flag en memòria (`setOverride`, `clearOverride`, `hasOverride`, `clearAllOverrides`). Tots els flags són **apagats** a la v1 i **no hi ha cap comprovació de pagament** (el Pro encara no existeix): una funció apagada simplement no hi és i l'app no mostra enlloc «gratuït» ni «Pro». A Compose, `rememberFeatureEnabled<CustomAvatarFeature>()` es recalcula quan canvia un override. Cada flag es registra a Koin amb `bind FeatureFlag::class`, així el menú de debug pot llistar-los amb `getAll<FeatureFlag>()`. Cada flag porta `pro`; el menú de debug té un «pro mode» que posa tots els flags de Pro a ON o a OFF (i també es poden canviar un a un). Es treballa amb flags petits, un per funció (avatar propi, etc.). Els overrides només els posa el menú de debug i **es conserven** en un DataStore propi (`feature_flags`, a `FeatureFlagStore`): es llegeixen de manera síncrona en arrencar i s'escriuen en segon pla; el fitxer és a `files/datastore`, ja exclòs del backup. Test: `FeatureFlagTest`.
- [x] **1b · `PersonColorFeature`** (Pro, apagat a la v1): el color de la persona com a tema passa a ser Pro. Apagat, `ConversesTheme` rep `personColorTheme = false` (`MainActivity`), les pantalles de la persona usen el tema de l'app i l'ajust «Color de la persona» no surt a Ajustos. Encès, tot funciona com abans.
- [x] **2 · Avatar estàndard** (fet; falta provar-lo a l'emulador amb el flag apagat i encès):
    - `AvatarColor.forName(name)` al `designsystem`: hash FNV-1a del nom normalitzat (minúscules, sense accents ni espais als extrems), estable i amb els 7 colors repartits (`AvatarColorTest`, 20 noms); `AvatarShape.Standard` = Galeta;
    - `avatarShape` i `avatarColor` nul·lables a `PersonEntity` (`null` = estàndard). Tota la UI llegeix `person.shape` / `person.color` (extensions a `Entities.kt`), que resolen l'estàndard; el tema de persona, el selector «Qui», el llistat, el detall i els fulls hi passen;
    - **Room:** la base de dades és la **versió 1** de la v1, neta i **sense cap migració**: l'esquema (`1.json`) s'ha regenerat des de zero i les dues columnes d'avatar són nul·lables. Com que l'app no és publicada, no hi ha cap instal·lació a conservar; en debug la base de dades es recrea sola si canvia l'esquema. A partir de la publicació, qualsevol canvi serà v1 → v2 amb migració i test (`MigrationTestHelper`);
    - editor: l'estat guarda `null` mentre no es tria res i l'avatar de previsualització segueix el nom mentre s'escriu; amb `CustomAvatarFeature` apagada s'amaguen la forma i el color i només queda l'avatar (`customAvatar` a `EditPersonContent`); una persona nova es desa amb `null` (test a `EditPersonViewModelTest`);
    - pendent al CI: etiqueta `update-screenshots` (canvia `editPersonNew`; les captures de l'onboarding són noves).
- [x] **3 · Redisseny de Nova persona / Editar persona** (especificació a `docs/nova-persona/redisseny-nova-persona.md`; fet i provat a l'emulador):
    - s'ha eliminat la targeta d'avatar: l'avatar (56 dp, Galeta amb color automàtic, no clicable) és a la primera fila del grup «Nom», amb transició de color de 300 ms, inicials = primera lletra del primer mot + primera de l'últim (`initialsOf`) i «?» sobre fons neutre amb el nom buit;
    - amb `CustomAvatarFeature` encesa, l'avatar és un botó amb insígnia d'edició que obre el full «Avatar» (`AvatarSheet.kt`: previsualització, forma, color amb «Automàtic», «Fet»); els canvis es veuen en directe i es desen amb la persona (`avatarColor = null` = automàtic);
    - **fila «Omple des d'un contacte»: no s'ha fet** (la importació de contactes queda fora de la v1);
    - «Dades fixades» passa a ser «Dades» / «Datos» / «Info» (clau `facts_title`) i es treuen la icona `push_pin` i el subtítol; només textos;
    - textos nous (en/ca/es); previews i captures `editPersonCustomAvatar`, `avatarSheetAuto` i `avatarSheetCustom`, i referències locals de les captures de l'editor que han canviat;
    - provat amb el menú de debug («Pro mode»): insígnia, full, desar i detall amb la forma i el color triats.
- [ ] **4 · Comprovar que res de Pro és visible** amb totes les funcions apagades (llista de `docs/pro.md` → «Proves») i, amb l'interruptor del menú de debug, que els dos modes funcionen.

**Relació amb altres fases:** el pas 4 i la prova «amb `CustomAvatar` activada» depenen del mòdul **Pro** del menú de debug (vegeu «Menú de debug»), que es fa en paral·lel. M5 no pot començar a signar ni a omplir la fitxa de Play fins que 1–3 estiguin fets.

## Menú de debug

Esquelet i mòduls implementats (passos 1 a 4), inclòs el mòdul de flags. Inspirat en el debug menu de [sevibus-android](https://github.com/Sloy/sevibus-android) (mòduls `debug-menu` i `debug-menu-noop`). Serveix, entre altres coses, per provar els dos modes del els feature flags (vegeu «Versió gratuïta i Pro»).

**Com és a SeviBus:**

- `DebugMenuHost(overlayLogger, menu = { ... }) { app }` embolcalla l'arrel de l'app: afegeix un botó flotant arrossegable i un `ModalBottomSheet` amb els mòduls.
- Cada mòdul és un `DebugModule(title, icon) { DebugCell(...) }` (acordió amb files estàndard: títol, subtítol i un slot a cada banda). Un mòdul també pot obrir una pantalla completa.
- `DebugModuleDataSource<T>` desa l'estat de cada mòdul en JSON a SharedPreferences i l'exposa com a `StateFlow`.
- Dos mòduls Gradle amb el mateix namespace: la implementació real a `debugImplementation` i una versió buida a `releaseImplementation`. L'app té una carpeta `src/debug/` amb els mòduls reals i una `src/release/` amb funcions buides.

**Decisions per a Converses:**

- **Dins de `:app`, sense mòduls Gradle nous.** Carpetes `src/debug/` (`DebugMenuHost`, `ConversesDebugMenu`, `DebugModule`, `DebugCell`, mòduls) i `src/release/` (un `DebugMenuHost` que només crida el contingut i un `ConversesDebugMenu` buit). A release no hi arriba cap codi del menú. Així `@OptIn(ExperimentalMaterial3ExpressiveApi)` continua només a `designsystem` i el CI no canvia.
- **Botó flotant sempre visible en debug** (decidit). Té l'inconvenient que sortirà a les captures: les captures de pantalles no passen per `MainActivity`, així que no s'hi veurà; s'ha de comprovar quan s'implementi.
- **On es col·loca:** a `MainActivity`, embolcallant `AppNavigation` (i el bloqueig de l'app) amb `DebugMenuHost`.
- **El menú no pot filtrar dades de persones protegides:** cap mòdul no mostra noms ni contingut d'entrades, i el full ha d'heretar `FLAG_SECURE` quan la finestra el tingui actiu.
- **L'interruptor Pro passa del menú d'Ajustos al menú de debug:** substitueix «un interruptor a Ajustos (només en builds de debug)» de `docs/pro.md`, perquè Ajustos no tingui res de debug. El mòdul Pro crida `LocalFeatureFlagRunner.setOverride(key, value)` (un interruptor per flag, llistats amb `getAll<FeatureFlag>()`) i desa l'estat; com que el menú es fa en un worktree apart, el contracte entre els dos és aquesta API (vegeu «Versió gratuïta i Pro», pas 1).

**Fet:** `app/src/debug/.../debug/` té `DebugMenuHost` (botó flotant arrossegable i `ModalBottomSheet`), `DebugModule` (acordió), `DebugCell` i `ConversesDebugMenu` amb els mòduls **Dades** (tornar a sembrar les dades d'exemple) i **Onboarding** (tornar-lo a mostrar i recrear l'activitat); `app/src/release/` només té un `DebugMenuHost` que crida el contingut. `MainActivity` embolcalla `AppNavigation` i el bloqueig amb `DebugMenuHost`. Els textos són només en anglès a `src/debug/res` (no arriben a release). El full hereta `FLAG_SECURE` per defecte (`securePolicy = Inherit`). L'APK de release no conté cap classe, cadena ni recurs del menú. No hi ha mòdul per esborrar el DataStore: `SettingsRepository` no té cap «esborrar tot» i no valia la pena afegir-lo només per al debug. Pendent: provar-lo a l'emulador (arrossegar el botó, recrear l'onboarding, FLAG_SECURE) i comprovar que el botó no surt a les captures.

**Passos:**

1. Esquelet: `DebugMenuHost` (botó flotant + full), `DebugModule` i `DebugCell`, amb la variant `release` buida.
2. ✔ Mòdul **Feature flags**: interruptor «Pro mode» (posa tots els flags amb `pro = true` a ON o OFF), un interruptor per flag (títol = clau, subtítol «Overridden» / «Default value») i «Reset flags» (`clearAllOverrides`). Els flags es llisten amb `getKoin().getAll<FeatureFlag>()`, així un flag nou apareix sol; els overrides es conserven al `feature_flags` DataStore.
3. Mòduls petits segons faci falta: tornar a sembrar les dades d'exemple, tornar a mostrar l'onboarding (`OnboardingFlag.reset()`), esborrar el DataStore.
4. Comprovar que l'APK de release no conté cap classe del menú.

## Després de la v1

- **Cerca:** pel nom de la persona i dins el text de les entrades. Les protegides només es poden trobar pel nom.
- **Exportar i importar tot:** un fitxer (JSON o ZIP xifrat) per canviar de mòbil sense el backup de Google.
- **Etiquetes** i la vista de **dades fixades** de totes les persones: les dues icones del disseny original de la barra flotant.
- **Widget** de nota ràpida.
- **Recordatoris** a partir de les dades fixades (per exemple, l'aniversari).

---

## Referència (sense canvis respecte de la v1 del pla)

### Stack

Una sola activitat (`MainActivity`, una `FragmentActivity` pel `BiometricPrompt`) i dos mòduls: `:app` i `:designsystem`. `minSdk = 31` (vegeu D1) i `compileSdk = 37`.

| Peça | Llibreria i versió | Notes |
| --- | --- | --- |
| UI | `material3` 1.5.0-alpha29 (BOM Compose 2026.09.00) | Les API d'Expressive no són públiques a la 1.4.0 estable |
| Formes | `graphics-shapes` 1.1.0 | Formes dels avatars |
| Color | `material-kolor` 5.0.1 | `Neutral` amb color dinàmic, `TonalSpot` amb el color triat |
| Navegació | Navigation 3 1.2.0 + `lifecycle-viewmodel-navigation3` | Rutes tipades amb `NavKey` serialitzables |
| Dades | Room 2.8.4 + KSP 2.3.12 | Base de dades versió 1 (neta, sense migracions), esquema a `app/schemas` |
| DI | Koin 4.2.2 | |
| Preferències | DataStore Preferences 1.2.1 | `SettingsRepository` |
| Ajustos UI | Compose-Settings `ui-tiles-expressive` 3.3.0 | Només es fa servir `SettingsGroup`; les files són pròpies (`SettingRow`) |
| Biometria | `androidx.biometric` 1.1.0 | `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` |
| Captures | Compose Preview Screenshot Testing 0.0.1-alpha16 | Experimental; als dos mòduls (vegeu «Tests de captures») |
| Altres | Kotlin 2.4.20, AGP 9.4.1, lifecycle 2.11.0, coroutines 1.11.0, serialization 1.11.0 | |

### Color de la persona

**És una funció Pro:** a la v1 queda darrere de `PersonColorFeature` (apagat), de manera que el tema de persona no s'aplica i l'ajust no surt. La resta d'aquesta secció descriu el comportament amb el flag encès.

Cada persona porta el color de l'avatar com a **llavor del tema** de les seves pantalles. Els fons i les targetes queden gairebé neutres; el color fort va als accents (split button, filtres de la cronologia, marcadors, selector de tipus).

- **Abast:** detall, desbloqueig, editor (amb el color del formulari, en directe i animat 300 ms), nota ràpida (només amb persona triada) i els fulls i diàlegs que s'hi obren. El llistat, Arxivades, Ajustos i el full de triar persona es queden amb el tema de l'app (`AppColors`).
- **Generació:** `PersonTheme` al `designsystem`; MaterialKolor `TonalSpot` amb la llavor de `AvatarColor.seed` (no amb el pastel, que dona un esquema pàl·lid). L'error continua fix; l'avatar manté el pastel i les inicials en `#1E1D24`. El color dinàmic i el color del tema dels Ajustos no hi intervenen.
- **Llavors:** Lavanda `#6B55B5`, Menta `#2F7A52`, Préssec `#B5602E`, Cel `#2D6FA3`, Rosa `#B04670`, Llimona `#8A7A12`, Sorra `#86704C` (les mateixes que `ThemeColor`).
- **Ajust:** Ajustos → Aparença → «Color de la persona» (`person_color_theme`, per defecte activat). Apagat, `PersonTheme` no fa res.
- **Proves:** `PersonThemeContrastTest` (contrast ≥ 4,5:1 amb els 7 colors, en clar i fosc) i el test de captures `personDetailThemed` (7 colors × clar i fosc).
- **Fora d'abast:** tenyir les files del llistat, colors fora dels 7 pastels i canviar la intensitat des dels Ajustos.

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

### Formulari d'entrada i «Quan»

- `EntryForm` és l'únic formulari (nota ràpida, nova entrada i edició), amb els slots `header` («Amb qui», només a la nota ràpida) i `saveButton`. Ordre: tipus, text, On, Quan.
- «On»: `FilterChip` de selecció única (radi 12, 36 dp); seleccionat en `primaryContainer` amb check, no seleccionat amb vora `outlineVariant`.
- «Quan»: fila de 72 dp (`WhenRow`) que obre `WhenSheet` («Quan va ser?»): Ara, Fa una hora, Ahir a aquesta hora i Tria dia i hora… (`DatePickerDialog` fins a avui i `TimePicker` en 24 h).
- `WhenChoice` (`Now`, `HourAgo`, `Yesterday`, `Custom`, `Stored`) és a l'estat i al `Saver`. «Ara» es resol en desar; les altres opcions fixen l'hora en triar-les. `Stored` és la data d'una entrada que s'edita i no marca cap opció. Un `Custom` en el futur es retalla a l'hora actual.
- El subtítol d'«Ara» es refresca cada minut (`rememberMinuteClock`). Format de data: `EEEE d MMM`, amb la primera lletra en majúscula.

### «Qui» (tipus d'entrada)

- `EntryKind` es manté (`SAID`, `HEARD`, `HAPPENED`) i no hi ha migració; només canvia com es mostra: **Jo**, **{primer nom}** i **Apunt** (**Apunts** als filtres i comptadors). `firstName()` agafa el primer mot i el talla a 12 caràcters amb «…».
- `whoLabel(kind, firstName, …)` és l'única funció d'etiquetes (cronologia, full d'accions i exportació); `WhoLabels.kt` també té `EntryMarker` i `PersonInitialAvatar`.
- Selector «Qui» a `EntryForm` (`WhoSelector`): `ConversesToggleGroup` en mode `prominent` (52 dp, icona sempre visible, avatar de 26 dp per a la persona). A la nota ràpida sense persona, el botó del mig diu «Persona» i està desactivat.
- Cronologia: marcadors de 32 dp (cercle amb `person`, avatar de la persona sense fons i quadrat amb `edit_note`). Filtres de multiselecció amb els tipus sense entrades amagats.
- Exportació: `[data · canal] Jo: text`.
- Pendent: el canal `OTHER - SIGNAL` a l'etiqueta (bug 3), previews i regenerar les captures de referència amb l'etiqueta `update-screenshots`.

### Bloqueig d'una persona protegida

- `PersonLockedScreen` només rep el nom, la forma i el color de l'avatar; res de la persona es carrega fins que el desbloqueig va bé i la ruta passa a `PersonRoute`. Té `FLAG_SECURE` (`SecureWindow`).
- `LockState`: `Waiting`, `Canceled(permanentLockout)`, `LockedOut(seconds)` i `NoCredentials`. `PersonLockViewModel` guarda l'estat i el compte enrere de 30 s; `lockStateForAvailability` i `lockStateForError` fan el mapeig d'errors de `BiometricPrompt`.
- Flux: en entrar es comprova `canAuthenticate` i, si va bé, s'obre el prompt sol. Cancel·lar porta a `Canceled`; un `ERROR_LOCKOUT` a `LockedOut`; sense biometria ni PIN, a `NoCredentials`. Només en aquest últim estat `ON_RESUME` torna a comprovar-ho (en tornar de la configuració de seguretat); passar a segon pla no reobre el prompt.
- Avatar i nom sempre al mateix lloc: l'àrea de l'estat reserva l'alçada de l'estat més alt. L'indicador de càrrega (`ConversesLoadingIndicator`) és al `designsystem`.
- Pendent: captures de referència del bloqueig (etiqueta `update-screenshots`) i provar-ho amb empremta real.

### Components propis (`designsystem`)

`Avatar`, `GroupedList` / `groupedItems`, `TimelineRow` i `TimelineMarker`, `ConversesFilterChip` i `ConversesConnectedFilterRow`, `ConversesChannelChip`, `ConversesToggleGroup`, `ConversesFloatingToolbar` i `ToolbarDestination`, `ConversesSplitButton` i `ConversesMenuItem`, `SettingRow` / `SettingSwitchRow`, `DashedActionRow`, `EmptyState`, `ColorSwatches`, `ConversesTheme`, `PersonTheme` i `AppColors`. A l'app: `PersonPickerSheet`.

### Pantalles i navegació

| Destinació | Ruta (`NavKey`) | Notes |
| --- | --- | --- |
| Onboarding | `OnboardingRoute` | Només el primer cop (`OnboardingFlag`); en acabar substitueix la pila per `PeopleRoute` |
| Llistat | `PeopleRoute` | Filtres per canal, nova persona a la capçalera i fila final, barra flotant |
| Arxivades | `ArchivedRoute` | Destinació de la barra flotant; sense filtres de canal ni fila d'afegir |
| Detall | `PersonRoute(id)` | Split button «Nota ▾», ⋮ a cada entrada, `FLAG_SECURE` si és protegida |
| Desbloqueig | `PersonLockedRoute(id)` | `BiometricPrompt` automàtic; si va bé, substitueix la ruta pel detall |
| Nota ràpida | `QuickNoteRoute(personId?)` | Sense persona per defecte; el tema de la persona només quan n'hi ha una de triada |
| Editar/crear | `EditPersonRoute(personId)` | `0` = persona nova; en crear, va al detall |
| Ajustos | `SettingsRoute` | |
| Fulls i diàlegs | Dins del detall i de l'editor | Nova entrada, accions d'entrada, menú del split button, triar persona, esborrar |

### Model de dades (Room, versió 1)

| Taula | Camps |
| --- | --- |
| `person` | `id`, `name`, `alias?`, `avatarShape`, `avatarColor`, `isProtected`, `archived`, `notes?`, `createdAt`, `updatedAt` |
| `channel` | `id`, `personId`, `type` (conjunt de canals de la persona) |
| `entry` | `id`, `personId`, `kind` (`SAID`, `HEARD`, `HAPPENED`), `channelType`, `text`, `occurredAt` |
| `pinned_fact` | `id`, `personId`, `label`, `value`, `position` |

Totes les claus `personId` tenen `onDelete = CASCADE`, i hi ha un índex a `entry(personId, occurredAt)`.

### Tests de captures

Amb [Compose Preview Screenshot Testing](https://developer.android.com/studio/preview/compose-screenshot-testing), a `:designsystem` i a `:app`.

- **Previews** a `main`, `internal` i amb dades fixes (al formulari d'entrada, `PreviewData.formTime` fixa l'hora perquè «Ara» no depengui del rellotge): `designsystem/.../components/ComponentPreviews.kt` i, a l'app, `PeopleListPreviews.kt` i `PersonDetailPreviews.kt` amb `ui/preview/PreviewData.kt` (dates del 2025 perquè «Avui»/«Ahir» no canviïn). Fan servir `ConversesPreviewTheme` (sense color dinàmic, Lavanda).
- **Tests** a `src/screenshotTest`: `ComponentsScreenshotTests` (`designsystem`) i `ScreensScreenshotTests` (`app`) criden les previews amb `@PreviewTest`. Els noms dels mètodes de test van en camelCase sense `_` (el CI hi separa el nom de la preview). Cada test es renderitza en clar i fosc, en anglès; les pantalles a 411×891 dp.
- **Referències** a `<mòdul>/src/screenshotTestDebug/reference/`, es versionen. Llindar de diferència: 1 %.
- **Ordres:** `./gradlew validateDebugScreenshotTest` (validar), `./gradlew updateDebugScreenshotTest` (regenerar), i per a un sol test `./gradlew :app:updateDebugScreenshotTest --tests '*ScreensScreenshotTests.peopleListLoaded'`.
- **Sense captura (no es pot amb aquest renderitzador):** el menú del split button (el popup es pinta com una caixa buida) i el snackbar de desfer (no hi surt). Es proven a mà. Idiomes (català i castellà) i font 2× només per al detall, la nota ràpida, la nova entrada, el bloqueig cancel·lat i Ajustos.

**CI** (`.github/workflows/screenshot-tests.yml`, adaptat de [Sloy/sevibus-android#16](https://github.com/Sloy/sevibus-android/pull/16)):

- A cada PR cap a `main` valida les captures i deixa un sol comentari (s'actualitza a cada execució) amb la referència, la nova i la diferència de cada test que falla. Les imatges es pugen a la branca `screenshots/pr-<número>`, que s'esborra en tancar la PR.
- L'etiqueta `update-screenshots` regenera les referències dels tests que fallen, les commiteja a la branca de la PR i es treu sola. Els commits del workflow no disparen execucions noves: cal tornar a fer push per validar-les. Les PR de forks se salten.
- A cada push a `main`, si alguna captura falla, regenera les referències a `screenshots/main-update` i obre una PR assignada a qui ha fet el push (o actualitza l'oberta); quan `main` torna a passar, la tanca. Cal activar **Settings → Actions → General → Allow GitHub Actions to create and approve pull requests**.
- El comentari el genera `.github/scripts/screenshot_report.py` a partir dels JUnit de cada mòdul; l'informe HTML també es puja com a artefacte `screenshot-report`.
- Les imatges dels comentaris i de les PR enllacen a `github.com/<repo>/blob/<sha>/<fitxer>?raw=true` i no a `raw.githubusercontent.com`, que en un repo privat no serveix res sense token.

### Decisions ja preses

- **Sense tests de UI instrumentats (Compose):** compliquen el CI. La interfície es cobreix amb els tests de captures i la lògica amb els tests de `ViewModel`.
- **Avatars:** cada persona porta el seu pastel, al llistat i al detall. Les protegides el mantenen i hi afegeixen el cadenat.
- **Exportar notes:** text pla compartit amb el full del sistema. També es pot fer abans d'esborrar una persona.
- **Arxivar:** amaga la persona del llistat; es recupera des d'Arxivades.
- **«Esborrar tota la conversa»:** esborra les entrades d'aquell dia en aquell canal.
- **Idioma per defecte:** anglès (`values/`), amb català (`values-ca/`) i castellà (`values-es/`).
- **Fora de la v1:** el vincle amb l'agenda.
