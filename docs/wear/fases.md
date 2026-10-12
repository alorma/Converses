# Converses per a Wear OS — Fases

Context, abast, arquitectura, decisions (P0–P16) i riscos són a `docs/wear/README.md`; les captures de referència, a `docs/wear/screenshots/`. Cada fase acaba amb alguna cosa que es pot provar; no es comença la següent sense validar l'anterior.

| Fase | Títol | Depèn de | Estat |
| --- | --- | --- | --- |
| 0 | Decisions i investigació | — | Feta, amb 3 pendents que no bloquegen (català i mecanisme de dictat: abans de la fase 6; AVD i Data Layer: fases 1–2) |
| 1 | Mòdul `:wear`, tema i previews amb dades fictícies | 0 | Feta |
| 2 | Contracte i sincronització mòbil → rellotge | 0 | Feta |
| 3 | Pantalles de lectura: Persones, Cronologia, Sense mòbil | 1, 2 | Feta |
| 4 | Més entrades sota demanda | 3 | Feta |
| 5 | Bloqueig de l'app i persones protegides | 0 (P0, P10), 3, 4 | Pendent |
| 6 | Dictat | 1 | Feta |
| 7 | Qui, Desat i nota rellotge → mòbil amb cua | 2, 3, 6 | Feta |
| 8 | Estats d'error i casos límit | 5, 7 | Pendent |
| 9 | Qualitat, privacitat i accessibilitat | 8 | Pendent |
| 10 | CI i publicació | 9 | Pendent |
| 11 | Més endavant | 10 | Idees |

---

Com es treballa cada fase (tasques, commits, validació, proposta d'inici): `.claude/rules/phased-work.md`.

---

## Fase 0 · Decisions i investigació

Sense codi de producte.

- [x] **P0:** aprovació al mòbil amb empremta i notificació com a camí (decidit); revisat amb el disseny: sense credencial al rellotge i una aprovació nova a cada obertura.
- [x] Decidir si la vigència és un ajust (Ajustos → Seguretat) o queda fixa a 5 min (P11): es va fixar a 5 min, i després **es descarta**: aprovació nova a cada obertura.
- [x] Demanar el disseny de les pantalles noves (P12): rebut el 12 d'oct. de 2026 (`docs/wear/handoff/`).
- [x] Tancar P1, P6, P7, P9, P11, P14 i P15 (vegeu `README.md`).
- [x] Consultar la documentació oficial: Wear Compose Material 3 i navegació, Data Layer (`play-services-wearable`), `EdgeButton`, `TransformingLazyColumn`, reconeixement de veu i requisits de Play per a apps de Wear. Versions anotades al `README.md` i afegides al version catalog.
- [x] **Versionat:** s'amplia `alorma/VersionPlugin` (1.1.0, encara no publicada) amb `appVersion { formFactor = FormFactor.WEAR }`: enum PHONE=0 (per defecte), WEAR=2, TV=4, AUTOMOTIVE=6, i `versionCode` = base + offset (1.2.3 → 10020031; WEAR → 10020033). `:app` no canvia. Document inicial: `~/projects/personal/VersionPlugin/docs/version-code-offset.md` (parlava de `versionCodeOffset`; el disseny final és `formFactor`). **Resolt a la fase 1** (1.1.0 publicada i integrada).
- [x] `minSdk` de `:wear`: **30** (Wear OS 3+).
- [x] **P10:** comprovat amb una app de prova descartable a l'emulador Wear OS 7 (API 37), `minSdk` 30 acordat. Resultats: sense bloqueig `isDeviceSecure=false`, el credencial intent és `null` i **no es pot crear una clau que exigeixi autenticació** (cal el bloqueig de pantalla); amb PIN, `isDeviceSecure=true`, el credencial intent apunta als ajustos del rellotge, la clau es crea, i el xifratge queda **bloquejat** (`UserNotAuthenticatedException`) un cop caducada la finestra d'autenticació (provat a 40 s amb una finestra de 30 s); en treure el PIN, **la clau desapareix sola**. Limitacions: és un emulador (`insideSecureHardware=false`, Keystore per programari) i no hi ha prova a l'API 30; cal repetir-ho al rellotge real a la fase 9. Conclusió: s'adopta el xifratge amb Keystore lligat a autenticació.
- [ ] Comprovar si el reconeixedor de veu del rellotge suporta **català** (P5); si no, avisar abans de cercar alternatives. **Pendent:** la documentació no ho confirma; cal rellotge real (abans de la fase 6).
- [ ] Triar el mecanisme de dictat (`SpeechRecognizer` amb parcials o `ACTION_RECOGNIZE_SPEECH`) i explicar-ne el motiu. **Pendent**, depèn de la prova anterior; plantejament: `SpeechRecognizer` amb parcials, pla B `ACTION_RECOGNIZE_SPEECH`.
- [ ] Preparar emuladors: parell Wear + telèfon amb Google Play. Fet: telèfon (Android 17) i `Wear_OS_XL_Round` ja **emparellats**. Pendent: AVD Large Round i Small Round (fase 1) i comprovar que la Data Layer hi funciona (fase 2).
- [x] Confirmar com es publica una app de Wear emparellada a la mateixa fitxa de Play: investigat; els punts no verificats (mateixa release, GPP, formulari de dades) passen a la fase 10.

**Sortida:** decisions escrites i versions fixades.

## Fase 1 · Mòdul `:wear`, tema i previews

- [x] Afegir `:wear` (i `:shared` si es confirma) a `settings.gradle.kts` i al catàleg de versions. — `5708737` (`:shared` queda per a la fase 2)
- [x] Mateix `applicationId` que `:app`, amb sufix `.dev` i signatura de debug com al mòbil; manifest amb `type.watch` i `standalone = false`; `allowBackup=false` i regles de backup. — `5708737`
- [x] **Versionat:** `VersionPlugin` 1.1.0 (ja publicada) aplicat a `:wear` amb `appVersion { formFactor = FormFactor.WEAR }`, sense import. Comprovat: `:wear` 10060012 i `:app` 10060010 (1.6.1 snapshot).
- [x] `ConversesWearTheme` amb els tokens del brief (negre pur, `surface-card`, `surface-card-high`, `primary`, `on-surface`…, avatars pastel i tinta `#1E1D24`) i Roboto Flex (subconjunt llatí, com al mòbil). — `f5bcfcc`
- [x] Components: llista agrupada (separació 2, radi 24 fora i 6 dins), fila de persona, entrada de cronologia (radi 18), grup connectat del «Qui», Galeta d'avatar. — `components/` (previews a la tasca 5)
- [x] Les 9 pantalles com a previews amb dades fictícies, a Large i Small Round, i captures de preview comparades amb `docs/wear/screenshots/` — `ui/` i `WearScreenshotTests` (18 captures de referència a `wear/src/screenshotTestDebug/reference`);.
- [x] Textos a `strings.xml` en en/ca/es; reutilitzar els del mòbil (Jo, Apunt, Desa, grups de canal) i afegir només els nous.
- [x] ktlint al mòdul; `testDebugUnitTest`, `ktlintCheck` i els APK actuals sense trencar-se.

**Sortida:** les 9 pantalles es veuen a les previews i s'instal·la un APK de debug a l'emulador.

## Fase 2 · Contracte i sincronització mòbil → rellotge

La part més delicada: privacitat.

Decidit en proposar la fase (12 d'oct. de 2026): les persones **arxivades no van al rellotge**; el text de les entrades es **talla a ~280 caràcters**; `:shared` és el **domini compartit** (enums d'avatar i entrada, contracte i constants).

- [x] **Domini compartit:** mòdul `:shared` (Kotlin/JVM pur) amb `AvatarColor` (ARGB), `AvatarShape`, `ChannelType` i `EntryKind`; `:designsystem` i `:wear` només hi afegeixen la conversió a `Color`; el rellotge perd la seva còpia d'`AvatarColor`. Schema de Room intacte. — `d32269a`
- [x] Contracte (a `:shared`): models d'instantani amb **versió de protocol**, serialització kotlinx.serialization; `sanitized()` aplica la privacitat en codificar i en descodificar; text tallat a 280. — `3dce9ef`, `866cfed` (la versió s'ha d'escriure sempre: trobat a l'emulador)
- [x] `WearSnapshotPublisher` al mòbil: construeix l'instantani des de Room i el publica amb `DataClient` (darrere de `WearLink`); debounce, no republica si res no ha canviat, republica en connectar-se. — `a5ead83`
- [x] **Només es publica si hi ha rellotge** (decidit el 12 d'oct. de 2026): «hi ha rellotge» = algun node amb la capacitat `converses_watch` (encara que no sigui a prop) o algun node connectat; sense rellotge no es publica res i s'esborra l'instantani de la Data Layer. Play Services no té *listener* de nodes: l'estat es reavalua en arrencar i en cada canvi de capacitat. **Provat als emuladors:** amb el rellotge apagat i el mòbil reiniciat no es deixa res a la Data Layer (en tornar a encendre'l no reapareix la còpia antiga), i en connectar-se el rellotge el mòbil publica un instantani nou sense reiniciar l'app. Pendent amb un rellotge real (fase 9). Els tests de JVM cobreixen sense rellotge, apareix, desapareix i torna.
- [x] Contingut: persones no arxivades (`id`, nom, color i forma d'avatar, `protected`, hora de l'última entrada), **5 últimes entrades** per persona no protegida, i l'**estat del Bloqueig de l'app**. — `5b8ebaf`
- [x] **Filtre de protegides al mòbil:** només `id`, nom, color i `protected = true`; cap entrada, canal ni hora (consulta SQL, constructor i `sanitized()`). — `5b8ebaf`
- [x] Capacitats `converses_phone` (mòbil) i `converses_watch` (rellotge). — `a5ead83`
- [x] Còpia local al rellotge (DataStore a `files/`, fora del backup) amb l'hora de l'última sincronització, i `SnapshotListenerService`. Sense xifrar: és de la fase 5. — `b98f8c6`
- [x] Tests de JVM: protegida sense contingut, passa a protegida i perd les entrades, límit de 5, ordre, text tallat, versió, bloqueig, debounce i errors d'enviament (`:shared` 12, `:app` 18, `:wear` 7).
- [x] Room: **no es toca** (només una consulta nova de DAO; `app/schemas` intacte). Cap migració.
- [x] **Emulador** (telèfon + Wear XL Round, dades d'exemple): el rellotge rep i desa l'instantani; la protegida (Luna Roja) arriba només amb nom, color i forma; en tornar a obrir el mòbil, es republica.

**Sortida:** amb dades reals del mòbil, el rellotge rep i desa un instantani correcte; els tests de privacitat passen.

## Fase 3 · Pantalles de lectura

- [x] Persones (1) i Persones sense mòbil (8), amb «Dades de fa N min» (o «Dades d'ara mateix» i hores). Les protegides només mostren nom i cadenat; el toc no fa res fins a la fase 5. — `877e978`, `ac82280`, `1b86288`
- [x] Cronologia (2) amb l'`EdgeButton` «Nota» (sense destí fins a la fase 6/7) — `ac82280`: comprovar P2 i, si cal, forçar que sigui sempre visible. **P2 ja comprovat a la fase 1:** el d'`ScreenScaffold` només apareix en arribar al final de la llista; el mòdul usa `PinnedEdgeButton` (sempre visible, amb degradat).
- [x] Cronologia buida (P3): només el botó «Nota» gran al centre; sense disseny, pendent de confirmar. — `ac82280`
- [x] Detecció de «mòbil no connectat» amb `NodeClient`/`CapabilityClient`: als emuladors les capacitats no es propaguen, així que també compten els nodes connectats. — `d1bca50`, `eb13d67`
- [x] Navegació amb Navigation 3 (`NavDisplay` + `rememberSwipeDismissableSceneStrategy`), decidida a la fase 0. — `1b6af5e`, `ac82280`
- [x] ViewModels amb tests de JVM (`SnapshotMappingTest`, `ViewModelsTest`); captures de preview noves (cronologia buida i sense dades). — `1577d2d`, `877e978`, `ac82280`

**Sortida:** navegar Persones → Cronologia amb dades reals; estat sense mòbil correcte.

## Fase 4 · Més entrades sota demanda

- [x] Contracte a `:shared` (`/converses/entries`, petició amb cursor `occurredAt`+`id`, resposta amb `hasMore`/`denied`, pàgines de 20) i tests del codec. — `5b8eb25`
- [x] Mòbil: `WearEntriesService` + `WearEntriesResponder` (`MessageClient.sendRequest`/`onRequest`); consulta paginada al DAO (sense canvi d'esquema, sense migració). Rebutja protegides, arxivades i inexistents; tests de JVM. — `3f52dfa`
- [x] Rellotge: `MoreEntriesSource` (`GmsMoreEntriesSource`, temps màxim de 8 s). — `f788b51`
- [x] `TimelineViewModel`: les entrades extra viuen **només en memòria** i s'obliden en sortir; mòbil inaccessible: la llista s'acaba sense error (P13); límit de memòria (P7: 100 extra per persona); tests de JVM (paginació, final de llista, mòbil absent, límit, sense duplicats, res al disc). — `f660f2d`
- [x] UI: en acostar-se al final es demana la pàgina següent; indicador de càrrega al final (P16) amb captura de referència. Validat a l'emulador (Marta Soler amb 65 entrades: carrega fins a l'última i s'atura sense error). — `42a61d5`

Validada per Bernat. El cas del mòbil realment inaccessible només està cobert per tests de JVM.

**Sortida:** una persona amb moltes entrades les va carregant mentre es fa scroll.

## Fase 5 · Bloqueig de l'app i persones protegides

Depèn de **P0** i **P10**.

**Contracte**
- [x] `:shared`: `WearApproval` (petició, cancel·lació, ack, resultat amb entrades i testimoni de nota, `sanitized()` i codec) i tests (9). Camins `/converses/approval`, `/cancel` i `/result`; resultat del mòbil al rellotge per `MessageClient`. — `61546d6`

**Bloqueig de l'app**
- [x] Llegir l'estat del bloqueig de l'instantani; si no es pot verificar, assumir **actiu**. — `a8d5178` (`AppLockGate`)
- [x] Si és actiu i el rellotge no és segur (`isDeviceSecure`): pantalla 9 («Cal un bloqueig», «Configura», que obre els ajustos de seguretat si el sistema ho permet). No mostra ni desa cap entrada. — `a8d5178`
- [ ] Còpia local xifrada amb Keystore lligada a l'autenticació del dispositiu (o esborrada, segons P10).
- [x] Esborrar la còpia si es treu el bloqueig del rellotge, en obrir l'app (en tornar a primer pla) i a cada sincronització. — `a8d5178`. **Emulador** (telèfon + Wear XL Round): amb el bloqueig actiu i sense PIN es veu la pantalla 9 i la còpia s'esborra; «Configura» obre Seguretat → Bloqueig de pantalla; amb PIN, l'app s'obre; en treure'l, torna la pantalla 9; amb el bloqueig desactivat, tot torna. Tests de JVM (`AppLockGateTest`, 6; `WearSnapshotStoreTest`, 2 nous).
- [x] La cua de pendents segueix les mateixes regles: **s'esborra** (decidit el 12 d'oct. de 2026: les notes es perden, no es xifren) quan cal bloqueig i el rellotge no en té, en sincronitzar o en intentar entregar-la (`enforceLock`). «Més entrades» ja viu només en memòria i no es pot arribar a la pantalla amb la pantalla 9. Tests de JVM (3 nous a `WearSnapshotStoreTest`, 1 a `NoteOutboxTest`). — `9c14da8`

**Persones protegides**
- [x] **Disseny (P12):** rebut (pantalles 7, 10–12 al rellotge i 13–16 al mòbil).
- [ ] Toc a una protegida → pantalla 7 «Confirma-ho al mòbil» (compte enrere ~60 s, «Cancel·la»). Sense credencial al rellotge i sense finestra de vigència: una aprovació nova a cada obertura (P0, P11).
- [ ] **Aprovació al mòbil (P0):** petició amb identificador d'un sol ús i caducitat (~60 s) → notificació genèrica (13) → «Confirma» / «Denega» (14) → `BiometricPrompt` (15) → «Fet» (16) → resposta amb les entrades només d'aquella persona. «Denegat» (10) i «Ha caducat» (11, amb «Reintenta») al rellotge.
- [ ] Mòbil: petició caducada oberta des de la notificació, o cancel·lada al rellotge amb el prompt obert: tanca la pantalla amb un missatge breu («No s'ha pogut completar la petició»), sense nom.
- [ ] Limitar la freqüència de peticions i no registrar contingut.
- [ ] Testimoni de vigència curta per a notes a protegides; el mòbil rebutja les que no en tenen un de vàlid.
- [ ] Mostrar les entrades **només en memòria**; esborrar-les en sortir o amb l'app en segon pla.
- [ ] Sense mòbil: pantalla 12 «No disponible sense el mòbil».
- [ ] Gravar una nota a una protegida només dins una sessió aprovada.
- [ ] Al mòbil: servir les entrades d'una protegida **només amb una aprovació biomètrica vàlida** (P0).
- [ ] Tests de JVM: res d'una protegida al disc ni a la còpia, esborrat en sortir, estat del bloqueig, assumir actiu; una petició sense aprovació, caducada o repetida no serveix res; una nota a protegida sense testimoni vàlid es rebutja.

**Sortida:** amb el bloqueig actiu i rellotge sense PIN, només es veu la pantalla 9; amb PIN, una protegida s'obre i no queda rastre en sortir.

## Fase 6 · Dictat

- [x] `SpeechSource` (`SpeechRecognizer` amb parcials, `ACTION_RECOGNIZE_SPEECH` com a pla B no ha calt) i `DictationViewModel`; tests de JVM. — `9dea534`
- [x] Pantalla 3 amb text parcial en directe i «Acaba»; el botó «Nota» de la cronologia hi porta. — `92ab57a`
- [x] Permís `RECORD_AUDIO` (flux de petició; si el sistema ja no mostra el diàleg, «Permet» obre els ajustos de l'app). — `9dea534`, `92ab57a`
- [x] Ones lligades a l'amplitud del micròfon; quietes si s'han desactivat les animacions del sistema. — `92ab57a`
- [x] Idioma el de l'app (ca/es/en); el català depèn del reconeixedor del rellotge (P5, sense confirmar). — `92ab57a`
- [x] Estats d'error i permís denegat (P4): sense veu, sense reconeixedor, micròfon, error genèric i permís, amb captures. — `92ab57a`

Validada per Bernat. Provada a l'emulador Wear (permís, escolta, error i reintent); «Acaba» deixa la transcripció a la pantalla i la fase 7 la passarà a «Qui». El català no s'ha comprovat.

**Sortida:** dictar una frase i veure'n la transcripció al rellotge.

## Fase 7 · Qui, Desat i nota rellotge → mòbil

- [x] «Qui» (4): Jo / {persona} / Apunt amb els mateixos textos que `whoLabel(...)`; per defecte la persona; «Desa». — `6cd9421`
- [x] Cua de pendents amb UUID per nota, a DataStore (`files/`, fora del backup), i entrega amb **WorkManager 2.12.0** (sobreviu al tancament de l'app; *backoff* exponencial de 30 s; es reprogramen en arrencar i quan torna el mòbil). Les notes queden a la cua fins que el mòbil les confirma o les rebutja (P14). — `300204a`
- [x] Enviament amb `MessageClient.sendRequest` a `/converses/note`, amb contracte a `:shared` (`WearNote`, text de fins a 4.000 caràcters: no es talla com les entrades, es rebutja si hi passa). — `9de440e`, `300204a`
- [x] `WearNoteService` + `WearNoteReceiver` al mòbil: valida que la persona existeixi, no sigui arxivada ni protegida, desa com una entrada nova i ignora duplicats per UUID (registre de les últimes 500 id a DataStore). **Canal (P9):** el de la nota ràpida, és a dir, l'últim usat amb la persona o el primer. **Cap canvi d'esquema de Room: sense migració.** — `584f415`
- [x] Desat (5) i Desat sense mòbil (6): es tanca sola als 2 s i torna a la Cronologia. «Sense mòbil» es mostra si en desar el mòbil no era accessible; l'esborrat de la cua passa quan el mòbil confirma. — `6cd9421`
- [x] Haptic `Confirm` en desar (P6), pel sistema de feedback de Compose (el rellotge no té cap interruptor propi de l'app). — `6cd9421`
- [x] Tests de JVM: contracte (5), mòbil (5: desa, canal, duplicat, protegida/arxivada/inexistent, il·legible), cua i reintents (7) i «Qui» (6). Captures de referència intactes (`validateDebugScreenshotTest`).
- [x] **Emulador** (telèfon + Wear XL Round): amb el mòbil a prop, la nota arriba al mòbil i el rellotge torna a la cronologia; amb el mòbil sense resposta la nota queda a la cua i s'entrega sola en ~30 s en tornar, sense duplicats (4 «Desa» = 4 entrades). El dictat real no es pot fer a l'emulador (no genera text): per provar «Qui» s'ha arrencat temporalment a `WhoRoute` amb text fix (canvi no commitejat). **Pendent amb rellotge real:** dictat de punta a punta i «Sense mòbil» real.
- Les notes a persones **protegides** no es poden crear (la fase 5 és pendent) i el mòbil les rebutja; el descart de notes a persones esborrades o fetes protegides és de la fase 8 (R3, R4).

Validada per Bernat amb les proves de l'emulador i els tests; el rellotge físic no s'ha pogut provar (no s'engega), així que el dictat real i «Sense mòbil» real queden per a quan es pugui (fase 9).

**Sortida:** apuntar una nota des del rellotge i veure-la al mòbil, amb i sense connexió.

## Fase 8 · Estats d'error i casos límit

- [ ] Mòbil absent durant tota la sessió; rellotge reiniciat amb notes a la cua.
- [ ] Nota duplicada.
- [ ] Persona que passa a protegida (amb còpia al rellotge i amb notes encuades): el rellotge esborra les entrades i la nota no s'aplica sense decisió (R3).
- [ ] Persona esborrada al mòbil amb notes pendents (R4).
- [ ] Reconeixedor sense resultat o sense català (P5).
- [ ] Bloqueig de l'app activat o desactivat mentre el rellotge és obert.

## Fase 9 · Qualitat, privacitat i accessibilitat

- [ ] Repàs de privacitat de punta a punta: res de protegides al disc del rellotge (còpia, logs, backup, notificacions). Test que ho comprova.
- [ ] Còpia i cua fora del backup d'Android.
- [ ] Large i Small Round, mida de lletra gran, ambient, ca/es/en.
- [ ] TalkBack al rellotge.
- [ ] Rellotge real: bateria, latència, desconnexions, bloqueig i desbloqueig reals.
- [ ] Release amb R8: Koin, serialització i Data Layer.
- [ ] Permisos: només els imprescindibles (`RECORD_AUDIO`).
- [ ] Comparar la implementació amb `docs/wear/screenshots/` i anotar les diferències.

## Fase 10 · CI i publicació

- [ ] `pr-build.yml`: tests, ktlint i APK de `:wear`.
- [ ] `screenshot-tests.yml`: referències de `:wear` (etiqueta `update-screenshots`).
- [ ] `release.yml`: AAB del rellotge i Gradle Play Publisher; confirmar el flux de la Play Console.
- [ ] **Política de privacitat i «Sense núvol» (P8):** verificar com viatgen les dades a la Data Layer i ajustar l'esborrany del brief abans de publicar-lo; traduir a ca/es.
- [ ] Fitxa de Play: captures i text per a Wear OS en en/ca/es (`app/src/main/play`); formulari de seguretat de les dades si cal.
- [ ] Pista inicial `internal`, després `beta`.
- [ ] Notes de versió: proposar el punt; no es toquen sense acord.

## Fase 11 · Més endavant

- Tile i complicació de nota ràpida (no dissenyats).
- Respostes predefinides, o triar el canal des del rellotge.
- Notificacions des del mòbil (sense contingut sensible).
- Rellotge autònom amb base de dades pròpia.
